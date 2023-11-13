package com.example.androidgpsmapapp

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.PersistableBundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions

class MainActivity : AppCompatActivity(), OnMapReadyCallback {
    companion object {
        private val TAG = this::class.java.declaringClass!!.simpleName
    }

    private var locationServiceRunning = false
    private lateinit var buttonMainStartStopService: Button
    private lateinit var textViewMainLat: TextView
    private lateinit var textViewMainLon: TextView
    private lateinit var mMap: GoogleMap

    private val innerBroadcastReceiver = InnerBroadcastReceiver()
    private val innerBroadcastReceiverIntentFilter = IntentFilter()

    private val polyLineOptions = PolylineOptions().width(10f).color(Color.RED)
    private var polyline : Polyline? = null
    private var isMapReady = false
    //Create view model
    private val polylineViewModel: PolylineViewModel by lazy {
        PolylineViewModel()
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        buttonMainStartStopService = findViewById(R.id.buttonMainStartStopService)
        textViewMainLat = findViewById(R.id.textViewMainLat)
        textViewMainLon= findViewById(R.id.textViewMainLon)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        innerBroadcastReceiverIntentFilter.addAction(C.ACTION_LOCATION_UPDATE)

        checkLocationPermissions()

        createNotificationChannel()

    }

    private fun createNotificationChannel() {
        //Log.d(TAG, "createNotificationChannel")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                C.NOTIFICATION_CHANNEL,
                "Default channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            channel.description = "Default channel for location app"

            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }


    private fun checkLocationPermissions() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&

            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED  &&

            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED

        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            //Log.d(TAG, "NO LOCATION ACCESS, NO NOTIFICATIONS ")
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.POST_NOTIFICATIONS), C.REQUEST_PERMISSIONS_CODE)
        }
    }

    override fun onResume() {
        super.onResume()

        LocalBroadcastManager.getInstance(this).registerReceiver(innerBroadcastReceiver, innerBroadcastReceiverIntentFilter)
    }

    override fun onPause() {
        super.onPause()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(innerBroadcastReceiver)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        //Save polyline to view model
        polylineViewModel.polylinePoints = polyLineOptions.points
        outState.putSerializable(C.SAVED_POLYLINE_POINTS, polylineViewModel.polylinePoints as ArrayList<LatLng>)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        //Restore polyline from view model
        polylineViewModel.polylinePoints = savedInstanceState.getSerializable(C.SAVED_POLYLINE_POINTS) as ArrayList<LatLng>
    }

    fun restorePolyLine(){
        polylineViewModel.polylinePoints?.let {
            polyLineOptions.addAll(it)
            polyline = mMap.addPolyline(polyLineOptions)
        }
    }

    fun buttonMainStartStopServiceOnClick(view: View) {
        val serviceIntent = Intent(this, LocationService::class.java)
        if (locationServiceRunning) {
            stopService(serviceIntent)
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
        }
        locationServiceRunning = !locationServiceRunning

        buttonMainStartStopService.text = if (locationServiceRunning) "STOP" else "START"
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        isMapReady = true
        // Restore polyline from saved instance state
        restorePolyLine()

        // Add a marker in Sydney and move the camera
        val latLng = LatLng(59.0, 24.0)
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng))
        mMap.animateCamera(CameraUpdateFactory.zoomTo(15f))
    }


    private fun updateLocation(lat: Double, lon: Double){
        polyline?.remove()

        val latLng = LatLng(lat, lon)

        polyLineOptions.add(latLng)

        polyline = mMap.addPolyline(polyLineOptions)

        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng))
    }

    private inner class InnerBroadcastReceiver: BroadcastReceiver(){
        override fun onReceive(context: Context?, broadcastIntent: Intent?) {
            when (broadcastIntent!!.action){
                C.ACTION_LOCATION_UPDATE -> {
                    textViewMainLat.text = broadcastIntent.getDoubleExtra(C.DATA_LOCATION_UPDATE_LAT, 0.0).toString()
                    textViewMainLon.text = broadcastIntent.getDoubleExtra(C.DATA_LOCATION_UPDATE_LON, 0.0).toString()
                    updateLocation(broadcastIntent.getDoubleExtra(C.DATA_LOCATION_UPDATE_LAT, 0.0), broadcastIntent.getDoubleExtra(C.DATA_LOCATION_UPDATE_LON, 0.0))
                }
            }
        }

    }

}