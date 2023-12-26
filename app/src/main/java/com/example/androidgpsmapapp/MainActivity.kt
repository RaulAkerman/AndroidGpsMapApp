package com.example.androidgpsmapapp

import HttpSingletonHandler
import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.RemoteViews
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import org.json.JSONObject

class MainActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMapClickListener {
    companion object {
        private val TAG = this::class.java.declaringClass!!.simpleName
    }

    lateinit var buttonMainAddCheckpoint: Button

    private var locationServiceRunning = false
    private lateinit var buttonMainStartStopService: Button
    private lateinit var textViewMainLat: TextView
    private lateinit var textViewMainLon: TextView
    private lateinit var mMap: GoogleMap
    private lateinit var buttonOptions: Button

    private val innerBroadcastReceiver = InnerBroadcastReceiver()
    private val innerBroadcastReceiverIntentFilter = IntentFilter()

    private var currentWaypoint: Marker? = null
    private val checkpoints = mutableListOf<LatLng>()
    private var userLocation: LatLng? = null
    private var userHeading: Float? = null
    private var userSpeed: Float? = null
    private var userAccuracy: Float? = null
    private var userAltitude: Double? = null
    private var userVerticalAccuracy: Float? = null
    private var userDirectionalIndicator: Marker? = null

    private val polyLineOptions = PolylineOptions().width(10f)
    private var polyline : Polyline? = null
    private var markerPolyLine: Polyline? = null
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
        textViewMainLon = findViewById(R.id.textViewMainLon)
        buttonOptions = findViewById(R.id.buttonOptions)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        innerBroadcastReceiverIntentFilter.addAction(C.ACTION_LOCATION_UPDATE)

        checkLocationPermissions()

        createNotificationChannel()

        buttonMainAddCheckpoint = findViewById(R.id.buttonMainAddCheckpoint)

        //Set on click listener for add checkpoint button
        buttonMainAddCheckpoint.setOnClickListener {
            if (userLocation != null) {
                addCheckpoint(userLocation!!)
            }
        }

        buttonOptions.setOnClickListener {
            SwitchToSettingsActivity()
        }
    }

    private fun SwitchToSettingsActivity() {
        val intent = Intent(this@MainActivity, SettingsActivity::class.java)
        startActivity(intent)
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

    fun addCheckpoint(checkpointLatLng: LatLng) {
        val checkpointMarker = mMap.addMarker(
            MarkerOptions()
                .title("Checkpoint")
                .position(checkpointLatLng)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
        )
        if (checkpointMarker != null) {
            //Save checkpoint LatLng
            checkpoints.add(checkpointLatLng)
        }
    }

    fun addWaypoint(waypointLatLng: LatLng) {
        currentWaypoint?.remove()
        //add waypoint as Location
        currentWaypoint = mMap.addMarker(
            MarkerOptions()
                .title("Waypoint")
                .position(waypointLatLng)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
        )
        drawPath()
    }

    fun addUserDirectionalIndicator(userLocation: LatLng, userHeading: Float) {
        userDirectionalIndicator?.remove()

        userDirectionalIndicator = mMap.addMarker(
            MarkerOptions()
                .title("User Directional Indicator")
                .position(userLocation)
                .rotation(userHeading)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW))
        )
    }

    fun polylineColor(): Int {
        return when {
            userSpeed == null -> Color.BLACK
            userSpeed!! < 3 -> Color.BLUE
            userSpeed!! < 5 -> Color.GREEN
            else -> Color.RED
        }
    }

    fun drawPath() {
        markerPolyLine?.remove()

        val markerPolyLineOptions = PolylineOptions()
            .clickable(true)
            .color(Color.MAGENTA)
            .width(10f)

        // Draw line between waypoint and user location
        if (currentWaypoint != null) {
            markerPolyLineOptions.add(currentWaypoint!!.position)
            if (userLocation == null) {
                // Initialize userLocation if it's null
                userLocation = LatLng(0.0, 0.0) // replace with actual user location
            }
            markerPolyLineOptions.add(userLocation)
        }


        markerPolyLine = mMap.addPolyline(markerPolyLineOptions)
    }
    
    override fun onResume() {
        super.onResume()
        LocalBroadcastManager.getInstance(this).registerReceiver(innerBroadcastReceiver, innerBroadcastReceiverIntentFilter)
    }

    override fun onPause() {
        super.onPause()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(innerBroadcastReceiver)
    }

    override fun onMapClick(p0: LatLng) {
        Log.d(TAG, "onMapClick")
        addWaypoint(p0)
        drawPath()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        Log.d(TAG, "onSaveInstanceState")
        super.onSaveInstanceState(outState)
        //Save polyline to view model
        polylineViewModel.polylinePoints = polyLineOptions.points
        outState.putSerializable(
            C.SAVED_POLYLINE_POINTS, polylineViewModel.polylinePoints as ArrayList<LatLng>)
        //Save markers to view model
        polylineViewModel.checkpoints = checkpoints
        outState.putSerializable(
            C.SAVED_CHECKPOINTS, polylineViewModel.checkpoints as ArrayList<LatLng>)
        //Save waypoints to view model
        if (currentWaypoint != null) {
            polylineViewModel.waypoints = mutableListOf(currentWaypoint!!.position)
            outState.putSerializable(
                C.SAVED_WAYPOINTS, polylineViewModel.waypoints as ArrayList<LatLng>
            )
        }
        //Save buttonMainStartStopService text
        outState.putString("buttonMainStartStopServiceText", buttonMainStartStopService.text.toString())
        //Save user location
        if (userLocation != null) {
            outState.putDouble("userLocationLat", userLocation!!.latitude)
            outState.putDouble("userLocationLon", userLocation!!.longitude)
        }
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        Log.d(TAG, "onRestoreInstanceState")
        super.onRestoreInstanceState(savedInstanceState)
        //Restore polyline from view model
        polylineViewModel.polylinePoints =
            savedInstanceState.getSerializable(C.SAVED_POLYLINE_POINTS) as ArrayList<LatLng>
        //Restore markers from view model
        polylineViewModel.checkpoints =
            savedInstanceState.getSerializable(C.SAVED_CHECKPOINTS) as ArrayList<LatLng>
        //Restore waypoints from view model
        if (savedInstanceState.getSerializable(C.SAVED_WAYPOINTS) != null) {
            polylineViewModel.waypoints =
                savedInstanceState.getSerializable(C.SAVED_WAYPOINTS) as ArrayList<LatLng>
        }
        //Restore buttonMainStartStopService text
        buttonMainStartStopService.text = savedInstanceState.getString("buttonMainStartStopServiceText")
        //Restore user location
        userLocation = LatLng(
            savedInstanceState.getDouble("userLocationLat"),
            savedInstanceState.getDouble("userLocationLon")
        )
    }

    fun restorePolyLine(){
        Log.d(TAG, "onRestoreLine")
        polylineViewModel.polylinePoints?.let {
            polyLineOptions.addAll(it)
            polyline = mMap.addPolyline(polyLineOptions)
        }
    }

    fun restoreMarkers(){
        if (polylineViewModel.checkpoints != null) {
            for (checkpoint in polylineViewModel.checkpoints!!) {
                addCheckpoint(checkpoint)
            }
        }
        if (polylineViewModel.waypoints != null) {
            for (waypoint in polylineViewModel.waypoints!!) {
                addWaypoint(waypoint)
            }
        }
    }

    fun buttonMainStartStopServiceOnClick(view: View) {
        val serviceIntent = Intent(this, LocationService::class.java)

        if (locationServiceRunning) {
            showStopServiceConfirmationDialog(serviceIntent)
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
            locationServiceRunning = true
            buttonMainStartStopService.text = "STOP"
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        isMapReady = true
        //Set on click listener for map
        mMap.setOnMapClickListener { latLng ->
            addWaypoint(latLng)
            drawPath()
        }

        // add user directional indicatior
        if (userLocation != null && userHeading != null) {
            addUserDirectionalIndicator(userLocation!!, userHeading!!)
        }

        // Restore polyline from saved instance state
        restorePolyLine()

        // Restore markers from saved instance state
        restoreMarkers()

        // Add a marker in Tallinn and move the camera
        //val latLng = LatLng(59.0, 24.0)
        //mMap.moveCamera(CameraUpdateFactory.newLatLng(userLocation))
        //mMap.animateCamera(CameraUpdateFactory.zoomTo(15f))
    }

    private fun updateLocation(lat: Double, lon: Double){
        polyline?.remove()

        val latLng = LatLng(lat, lon)
        Log.d(TAG, "Speed: $userSpeed")
        polyLineOptions.add(latLng).color(polylineColor())

        polyline = mMap.addPolyline(polyLineOptions)

        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng))

    }

    private inner class InnerBroadcastReceiver: BroadcastReceiver(){
        override fun onReceive(context: Context?, broadcastIntent: Intent?) {
            when (broadcastIntent!!.action){
                C.ACTION_LOCATION_UPDATE -> {
                    userLocation = LatLng(
                        broadcastIntent.getDoubleExtra(C.DATA_LOCATION_UPDATE_LAT, 0.0),
                        broadcastIntent.getDoubleExtra(C.DATA_LOCATION_UPDATE_LON, 0.0)
                    )
                    userHeading = broadcastIntent.getFloatExtra(C.DATA_LOCATION_UPDATE_BEARING, 0.0f)
                    userSpeed = broadcastIntent.getFloatExtra(C.DATA_LOCATION_UPDATE_SPEED, 0.0f)
                    userAccuracy = broadcastIntent.getFloatExtra(C.DATA_LOCATION_UPDATE_ACCURACY, 0.0f)
                    userAltitude = broadcastIntent.getDoubleExtra(C.DATA_LOCATION_UPDATE_ALTITUDE, 0.0)
                    userVerticalAccuracy = broadcastIntent.getFloatExtra(C.DATA_LOCATION_UPDATE_VERTICAL_ACCURACY, 0.0f)
                    addUserDirectionalIndicator(userLocation!!, userHeading!!)
                    textViewMainLat.text = broadcastIntent.getDoubleExtra(C.DATA_LOCATION_UPDATE_LAT, 0.0).toString()
                    textViewMainLon.text = broadcastIntent.getDoubleExtra(C.DATA_LOCATION_UPDATE_LON, 0.0).toString()
                    updateLocation(broadcastIntent.getDoubleExtra(C.DATA_LOCATION_UPDATE_LAT, 0.0), broadcastIntent.getDoubleExtra(C.DATA_LOCATION_UPDATE_LON, 0.0))
                    drawPath()
                }
            }
        }
    }

    private fun calculateDistance(start: LatLng, end: LatLng): Float {
        val results = FloatArray(1)
        Location.distanceBetween(start.latitude, start.longitude, end.latitude, end.longitude, results)
        return results[0]
    }

    private fun showStopServiceConfirmationDialog(serviceIntent: Intent) {
        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.setTitle("Confirmation")
        alertDialogBuilder.setMessage("Are you sure you want to stop the service?")
        alertDialogBuilder.setPositiveButton("Yes") { _, _ ->
            stopService(serviceIntent)
            locationServiceRunning = false
            buttonMainStartStopService.text = "START"
        }
        alertDialogBuilder.setNegativeButton("No") { _, _ -> }
        alertDialogBuilder.create().show()
    }

    //TODO: some of the values passed to the api are temporary and should be replaced later.

    fun onClickregister(view: android.view.View) {
        var url = "https://sportmap.akaver.com/api/v1.0/account/register"
        var handler = HttpSingletonHandler.getInstance(this)

        var httpRequest = object : StringRequest(
            Request.Method.POST,
            url,
            Response.Listener { response -> Log.d("Response.Listener", response);
                val sharedPref = this.getPreferences(Context.MODE_PRIVATE) ?: return@Listener
                with (sharedPref.edit()) {
                    putString("token", JSONObject(response).getString("token"))
                    apply()
                } },
            Response.ErrorListener { error ->  Log.d("Response.ErrorListener", "${error.message} ${error.networkResponse.statusCode}")}
        ){
            override fun getBodyContentType(): String {
                return "application/json"
            }
            override fun getBody(): ByteArray {
                val params = HashMap<String, String>()
                params["email"] = "user123456789@user.ee"
                params["password"] = "Foo.bar.1"
                params["firstName"] = "firstName"
                params["lastName"] = "lastName"

                var body = JSONObject(params as Map<*, *>).toString()
                Log.d("getBody", body)

                return body.toByteArray()
            }
        }
        handler.addToRequestQueue(httpRequest)
    }

    fun onClicklogin(view: android.view.View) {
        val url = "https://sportmap.akaver.com/api/v1.0/account/login"
        val handler = HttpSingletonHandler.getInstance(this)


        //Save the token from the response json to shared preferences
        val httpRequest = object : StringRequest(
            Request.Method.POST,
            url,
            Response.Listener { response -> Log.d("Response.Listener", response);
                val sharedPref = this.getPreferences(Context.MODE_PRIVATE) ?: return@Listener
                with (sharedPref.edit()) {
                    putString("token", JSONObject(response).getString("token"))
                    apply()
                } },
            Response.ErrorListener { error ->  Log.d("Response.ErrorListener", "${error.message} ${error.networkResponse.statusCode}")}
        ){
            override fun getBodyContentType(): String {
                return "application/json"
            }
            override fun getBody(): ByteArray {
                val params = HashMap<String, String>()
                params["email"] = "user123456789@user.ee"
                params["password"] = "Foo.bar.1"

                val body = JSONObject(params as Map<*, *>).toString()
                Log.d("getBody", body)

                return body.toByteArray()
            }
        }
        handler.addToRequestQueue(httpRequest)
    }

    fun startSession() {
        val url = "https://sportmap.akaver.com/api/v1.0/GpsSessions"
        val handler = HttpSingletonHandler.getInstance(this)

        val httpRequest = object : StringRequest(
            Request.Method.POST,
            url,
            Response.Listener { response -> Log.d("Response.Listener", response);
                val sharedPref = this.getPreferences(Context.MODE_PRIVATE) ?: return@Listener
                with (sharedPref.edit()) {
                    putString("session_id", JSONObject(response).getString("id"))
                    apply()
                } },
            Response.ErrorListener { error ->
                Log.d(
                    "Response.ErrorListener",
                    "${error.message} ${error.networkResponse.statusCode}"
                )
            }
        ) {
            override fun getBodyContentType(): String {
                return "application/json"
            }

            override fun getBody(): ByteArray {
                val params = HashMap<String, Any?>()
                val currentDate = java.util.Calendar.getInstance().time
                params["name"] = "sessionName"
                params["description"] = "sessionDescription"
                params["recordedAt"] = currentDate
                params["minSpeed"] = 420
                params["maxSpeed"] = 600

                val body = JSONObject(params as Map<*, *>).toString()
                Log.d("getBody", body)

                return body.toByteArray()
            }
        }
        handler.addToRequestQueue(httpRequest)
    }

    //Gets the location types, unnecessary since they are hardcoded in the backend
    fun getLocationTypes() {
        val url = "https://sportmap.akaver.com/api/v1.0/GpsLocationTypes"
        val handler = HttpSingletonHandler.getInstance(this)

        val httpRequest = object : StringRequest(
            Request.Method.GET,
            url,
            Response.Listener { response -> Log.d("Response.Listener", response);
                val sharedPref = this.getPreferences(Context.MODE_PRIVATE) ?: return@Listener
                with (sharedPref.edit()) {
                    putString("gps_location_types", response)
                    apply()
                } },
            Response.ErrorListener { error ->
                Log.d(
                    "Response.ErrorListener",
                    "${error.message} ${error.networkResponse.statusCode}"
                )
            }
        )
        {
            override fun getBodyContentType(): String {
                return "application/json"
            }
        }
        handler.addToRequestQueue(httpRequest)
    }

    fun updateSession() {
        val url = "https://sportmap.akaver.com/api/v1.0/GpsLocations"
        val handler = HttpSingletonHandler.getInstance(this)

        val httpRequest = object : StringRequest(
            Request.Method.POST,
            url,
            Response.Listener { response -> Log.d("Response.Listener", response);
                val sharedPref = this.getPreferences(Context.MODE_PRIVATE) ?: return@Listener
                with (sharedPref.edit()) {
                    remove("latest_location_update")
                    putString("latest_location_update", response)
                    apply()
                } },
            Response.ErrorListener { error ->
                Log.d(
                    "Response.ErrorListener",
                    "${error.message} ${error.networkResponse.statusCode}"
                )
            }
        ) {
            override fun getBodyContentType(): String {
                return "application/json"
            }

            override fun getBody(): ByteArray {
                val params = HashMap<String, Any?>()
                //Get shared preferences
                val savedPreferences = getPreferences(Context.MODE_PRIVATE)
                val gpsSessionId = savedPreferences.getString("session_id", "")
                params["recordedAt"] = "2021-11-11T11:11:11.111Z"
                params["latitude"] = userLocation?.latitude
                params["Longitude"] = userLocation?.longitude
                params["accuracy"] = userAccuracy
                params["altitude"] = userAltitude
                params["verticalAccuracy"] = userVerticalAccuracy
                params["gpsSessionId"] = gpsSessionId
                params["gpsLocationTypeId"] = "00000000-0000-0000-0000-000000000001"

                val body = JSONObject(params as Map<*, *>).toString()
                Log.d("getBody", body)

                return body.toByteArray()
            }
        }
        handler.addToRequestQueue(httpRequest)
    }

}
