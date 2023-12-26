package com.example.androidgpsmapapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
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
        private val TAG = this::class.java.declaringClass!!.simpleName
    }

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallBack: LocationCallback

    private var prevLocation: Location? = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        //Log.d(TAG, "onStartCommand")

        // call within 5 seconds from start
        showNotification()

        return START_STICKY
    }

    private fun showNotification() {
        // Log.d(TAG, "showNotification")

        val view = RemoteViews(packageName, R.layout.location_notif)

        view.setTextViewText(R.id.textViewLat, prevLocation?.latitude.toString())
        view.setTextViewText(R.id.textViewLon, prevLocation?.longitude.toString())

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

    override fun onCreate() {
        //Log.d(TAG, "onCreate")
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
            //Log.d(TAG, e.toString())
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
                    //Log.d(TAG, "Failed to getLastKnownLocation")
                }
            }

        } catch (e: SecurityException) {
            //Log.d(TAG, e.toString())
        }
    }

    private fun onLocationUpdate(location: Location) {
        // Log.d(TAG, "onLocationUpdate " + location.latitude + " " + location.longitude)

        if (prevLocation != null) {
            val distance = prevLocation!!.distanceTo(location)

            if (distance in 3.0..50.0) {
                // The distance is between 5 and 100 meters, send location update
                prevLocation = location
                showNotification()

                val broadcastIntent = Intent(C.ACTION_LOCATION_UPDATE)
                broadcastIntent.putExtra(C.DATA_LOCATION_UPDATE_LAT, location.latitude)
                broadcastIntent.putExtra(C.DATA_LOCATION_UPDATE_LON, location.longitude)
                broadcastIntent.putExtra(C.DATA_LOCATION_UPDATE_BEARING, location.bearing)
                broadcastIntent.putExtra(C.DATA_LOCATION_UPDATE_SPEED, location.speed)
                broadcastIntent.putExtra(C.DATA_LOCATION_UPDATE_ACCURACY, location.accuracy)
                broadcastIntent.putExtra(C.DATA_LOCATION_UPDATE_ALTITUDE, location.altitude)
                broadcastIntent.putExtra(C.DATA_LOCATION_UPDATE_VERTICAL_ACCURACY, location.verticalAccuracyMeters)

                LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent)
            }
        } else {
            // This is the first location update, send it regardless of distance
            prevLocation = location
            showNotification()

            val broadcastIntent = Intent(C.ACTION_LOCATION_UPDATE)
            broadcastIntent.putExtra(C.DATA_LOCATION_UPDATE_LAT, location.latitude)
            broadcastIntent.putExtra(C.DATA_LOCATION_UPDATE_LON, location.longitude)
            broadcastIntent.putExtra(C.DATA_LOCATION_UPDATE_BEARING, location.bearing)
            broadcastIntent.putExtra(C.DATA_LOCATION_UPDATE_SPEED, location.speed)
            broadcastIntent.putExtra(C.DATA_LOCATION_UPDATE_ACCURACY, location.accuracy)
            broadcastIntent.putExtra(C.DATA_LOCATION_UPDATE_ALTITUDE, location.altitude)
            broadcastIntent.putExtra(C.DATA_LOCATION_UPDATE_VERTICAL_ACCURACY, location.verticalAccuracyMeters)

            LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent)
        }
    }

    override fun onDestroy() {
        //Log.d(TAG, "onDestroy")
        super.onDestroy()

        fusedLocationClient.removeLocationUpdates(locationCallBack)
    }

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

}