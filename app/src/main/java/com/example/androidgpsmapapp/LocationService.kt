package com.example.androidgpsmapapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
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

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallBack: LocationCallback

    private var prevLocation: Location? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // call within 5 seconds from start
        showNotification()
        Log.d(TAG, "onStartCommand")
        if (intent?.action == ACTION_ADD_CHECKPOINT_BROADCAST) {
            sendAddCheckpointBroadcast()
        }

        return START_STICKY
    }

    private fun showNotification() {
        val view = RemoteViews(packageName, R.layout.location_notif)

        view.setTextViewText(R.id.textViewLat, prevLocation?.latitude.toString())
        view.setTextViewText(R.id.textViewLon, prevLocation?.longitude.toString())

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

    private fun sendAddCheckpointBroadcast() {
        Log.d(TAG, "sendAddCheckpointBroadcast")
        val broadcastIntent = Intent(C.ACTION_PLACE_CHECKPOINT)
        broadcastIntent.putExtra(C.DATA_LOCATION_UPDATE_LAT, prevLocation?.latitude)
        broadcastIntent.putExtra(C.DATA_LOCATION_UPDATE_LON, prevLocation?.longitude)
        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent)
    }

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

            if (distance in 3.0..50.0) {
                prevLocation = location
                showNotification()
                sendLocationUpdateBroadcast(location)
            }
        } else {
            prevLocation = location
            showNotification()
            sendLocationUpdateBroadcast(location)
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
        Log.d(TAG, "sendLocationUpdateBroadcast")
        LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent)
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallBack)
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }
}
