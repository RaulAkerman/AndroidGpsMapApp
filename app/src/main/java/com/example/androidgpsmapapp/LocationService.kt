package com.example.androidgpsmapapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.Location
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

class LocationService : Service() {
    companion object {
        const val TAG = "LocationService"
        const val ACTION_ADD_CHECKPOINT_BROADCAST = "com.example.androidgpsmapapp.ADD_CHECKPOINT_BROADCAST"
    }

    private val locationQueue = mutableListOf<Location>()

    private val innerBroadcastReceiver = InnerBroadcastReceiver()
    private val innerBroadcastReceiverIntentFilter = IntentFilter()
    private val checkpoints = mutableListOf<Location>()

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallBack: LocationCallback

    private var prevLocation: Location? = null

    private var totalDistance = 0.0
    private var timerValue: Int = 0
    private val timeUpdateReceiver = TimeUpdateReceiver()

    private val handler = Handler()

    private val locationQueueProcessor: Runnable = object : Runnable {
        override fun run() {
            if (locationQueue.isNotEmpty()) {
                sendLocationUpdateBroadcast(locationQueue.elementAt(0))
            }
            handler.postDelayed(this, 1000)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // call within 5 seconds from start
        Log.d(TAG, "onStartCommand")
        showNotification()
        locationQueue.clear()
        innerBroadcastReceiverIntentFilter.addAction(C.ACTION_STOP_CHECKPOINT_BROADCAST)
        innerBroadcastReceiverIntentFilter.addAction(C.ACTION_REMOVE_LOCATION_UPDATE)
        LocalBroadcastManager.getInstance(this).registerReceiver(innerBroadcastReceiver, innerBroadcastReceiverIntentFilter)
        if (intent?.action == ACTION_ADD_CHECKPOINT_BROADCAST) {
            checkpoints.add(prevLocation!!)
            sendAddCheckpointBroadcast()
        }

        innerBroadcastReceiverIntentFilter.addAction(C.ACTION_TIMER)
        LocalBroadcastManager.getInstance(this).registerReceiver(timeUpdateReceiver, innerBroadcastReceiverIntentFilter)

        return START_STICKY
    }

    private fun showNotification() {
        val view = RemoteViews(packageName, R.layout.location_notif)

        view.setTextViewText(R.id.textViewLat, "Time spent: ${formatTime(timerValue)}")
        view.setTextViewText(R.id.textViewLon, "Distance: ${"%.2f".format(totalDistance)}m")

        // Create an intent for the button click event
        val buttonIntent = Intent(this, LocationService::class.java)
        buttonIntent.action = ACTION_ADD_CHECKPOINT_BROADCAST
        val buttonPendingIntent = PendingIntent.getService(
            this,
            0,
            buttonIntent,
            PendingIntent.FLAG_MUTABLE
        )

        // Set the click event for the button in the RemoteViews
        view.setOnClickPendingIntent(R.id.buttonPlaceCheckpoint, buttonPendingIntent)

        val builder = NotificationCompat.Builder(this, C.NOTIFICATION_CHANNEL)
            .setSmallIcon(androidx.core.R.drawable.notification_bg)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(view)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSound(null)

        // For API 26 and above, you may want to explicitly set the notification channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                C.NOTIFICATION_CHANNEL,
                "Notification channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
            builder.setChannelId(C.NOTIFICATION_CHANNEL)
        }

        startForeground(1, builder.build())
    }

    private fun formatTime(seconds: Int): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val remainingSeconds = seconds % 60
        return String.format("%02d:%02d:%02d", hours, minutes, remainingSeconds)
    }

    private fun sendAddCheckpointBroadcast() {
//        val handler = Handler()
        val delayMillis = 5000 // Set your desired delay time in milliseconds
        Log.d(TAG, "sendAddCheckpointBroadcast")
        handler.postDelayed(object : Runnable {
            override fun run() {
//                Log.d(TAG, "sendAddCheckpointBroadcast")
//                val broadcastIntent = Intent(C.ACTION_PLACE_CHECKPOINT)
//                broadcastIntent.putExtra(C.DATA_LOCATION_UPDATE_LAT, prevLocation?.latitude)
//                broadcastIntent.putExtra(C.DATA_LOCATION_UPDATE_LON, prevLocation?.longitude)
//                LocalBroadcastManager.getInstance(this@LocationService).sendBroadcast(broadcastIntent)

                //Send broadcast for every location saved in the list
                for (checkpoint in checkpoints) {
                    val broadcastIntent = Intent(C.ACTION_PLACE_CHECKPOINT)
                    broadcastIntent.putExtra(C.DATA_LOCATION_UPDATE_LAT, checkpoint.latitude)
                    broadcastIntent.putExtra(C.DATA_LOCATION_UPDATE_LON, checkpoint.longitude)
                    LocalBroadcastManager.getInstance(this@LocationService).sendBroadcast(broadcastIntent)
                }

                // Check for the condition to stop sending broadcasts
                if (checkpoints.size == 0) {
                    Log.d(TAG, "stopAddCheckpointBroadcast")
                    handler.removeCallbacks(this)
                } else {
                    handler.postDelayed(this, delayMillis.toLong())
                }
            }
        }, delayMillis.toLong())
    }

//    private fun sendAddCheckpointBroadcast() {
//        Log.d(TAG, "sendAddCheckpointBroadcast")
//        val broadcastIntent = Intent(C.ACTION_PLACE_CHECKPOINT)
//        broadcastIntent.putExtra(C.DATA_LOCATION_UPDATE_LAT, prevLocation?.latitude)
//        broadcastIntent.putExtra(C.DATA_LOCATION_UPDATE_LON, prevLocation?.longitude)
//        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent)
//    }

    override fun onCreate() {
        super.onCreate()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationCallBack = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                onLocationUpdate(locationResult.lastLocation!!)
            }
        }

        getLastKnownLocation()

        requestLocationUpdates()

        handler.postDelayed(locationQueueProcessor, 5000)
    }

    private fun requestLocationUpdates() {
        locationRequest = LocationRequest
            .Builder(1000)
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .build()

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallBack,
                Looper.myLooper()
            )
        } catch (e: SecurityException) {
            // Handle the exception
        }
    }

    private fun getLastKnownLocation() {
        try {
            fusedLocationClient.lastLocation.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    if (task.result != null) {
                        onLocationUpdate(task.result)
                    }
                } else {
                    // Handle the exception
                }
            }

        } catch (e: SecurityException) {
            // Handle the exception
        }
    }

    private fun onLocationUpdate(location: Location) {
        if (prevLocation != null) {
            val distance = prevLocation!!.distanceTo(location)

            if (distance in 1.0..50.0) {
                totalDistance += distance
                prevLocation = location
                showNotification()
                //sendLocationUpdateBroadcast(location)
                locationQueue.add(location)
            }
        } else {
            prevLocation = location
            showNotification()
            //sendLocationUpdateBroadcast(location)
            locationQueue.add(location)
        }
    }

    private fun sendLocationUpdateBroadcast(location: Location) {
        val broadcastIntent = Intent(C.ACTION_LOCATION_UPDATE)
        broadcastIntent.putExtra(C.DATA_LOCATION_UPDATE_LAT, location.latitude)
        broadcastIntent.putExtra(C.DATA_LOCATION_UPDATE_LON, location.longitude)
        broadcastIntent.putExtra(C.DATA_LOCATION_UPDATE_BEARING, location.bearing)
        broadcastIntent.putExtra(C.DATA_LOCATION_UPDATE_SPEED, location.speed)
        broadcastIntent.putExtra(C.DATA_LOCATION_UPDATE_ACCURACY, location.accuracy)
        broadcastIntent.putExtra(C.DATA_LOCATION_UPDATE_ALTITUDE, location.altitude)
        broadcastIntent.putExtra(C.DATA_LOCATION_UPDATE_VERTICAL_ACCURACY, location.verticalAccuracyMeters)
        broadcastIntent.putExtra(C.DATA_LOCATION_UPDATE_TIMESTAMP, location.time)

        Log.d(TAG, "sendLocationUpdateBroadcast")
        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent)
    }

    private inner class InnerBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == C.ACTION_STOP_CHECKPOINT_BROADCAST) {
                //Find checkpoint in the list and remove it
                val lat = intent.getDoubleExtra(C.DATA_LOCATION_UPDATE_LAT, 0.0)
                val lon = intent.getDoubleExtra(C.DATA_LOCATION_UPDATE_LON, 0.0)
                for (checkpoint in checkpoints) {
                    if (checkpoint.latitude == lat && checkpoint.longitude == lon) {
                        checkpoints.remove(checkpoint)
                        break
                    }
                }
            }
            if (intent?.action == C.ACTION_REMOVE_LOCATION_UPDATE) {
                val lat = intent.getDoubleExtra(C.DATA_LOCATION_UPDATE_LAT, 0.0)
                val lon = intent.getDoubleExtra(C.DATA_LOCATION_UPDATE_LON, 0.0)
                for (location in locationQueue) {
                    if (location.latitude == lat && location.longitude == lon) {
                        Log.d(TAG, "Location removed from queue")
                        locationQueue.remove(location)
                        break
                    }
                }
            }

        }
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallBack)
        LocalBroadcastManager.getInstance(this).unregisterReceiver(innerBroadcastReceiver)
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    private fun updateTimerVariable(seconds: Int) {
        timerValue = seconds
    }

    private inner class TimeUpdateReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, broadcastIntent: Intent?) {
            when (broadcastIntent!!.action) {
                C.ACTION_TIMER -> {
                    val payloadTime = broadcastIntent.getStringExtra(C.PAYLOAD_TIME)
                    val seconds = payloadTime?.toIntOrNull() ?: 0
                    // Handle the time update as needed
                    updateTimerVariable(seconds)
                }
            }
        }
    }

}
