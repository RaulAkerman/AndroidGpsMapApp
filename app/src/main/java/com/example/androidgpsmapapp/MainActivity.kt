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
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
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
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import org.json.JSONObject
import android.hardware.SensorEventListener
import android.view.animation.Animation.RELATIVE_TO_SELF
import android.view.animation.RotateAnimation
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.Math.toDegrees

class MainActivity : AppCompatActivity(), OnMapReadyCallback, GoogleMap.OnMapClickListener, SensorEventListener {
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
    private lateinit var buttonCompass: Button
    private lateinit var buttonCenter: Button
    private lateinit var buttonNorthUp: Button
    private lateinit var buttonSetRotation: Button

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

    private val polyLineOptions = PolylineOptions().width(10f)
    private var polyline : Polyline? = null
    private var markerPolyLine: Polyline? = null
    private var isMapReady = false

    //Compass
    private lateinit var compassImage: ImageView
    private lateinit var sensorManager: SensorManager
    private lateinit var accelerometer: Sensor
    private lateinit var magnetometer: Sensor
    private var currentDegree = 0f

    private var lastAccelerometer = FloatArray(3)
    private var lastMagnetometer = FloatArray(3)
    private var lastAccelerometerSet = false
    private var lastMagnetometerSet = false

    private var isCompassVisible = false
    //====

    //Bottom info
    private var timerValue = "00:00:00"
    private var timerValueInSecond = 0
    private var timerValueAtLastCP = 0
    private var timerValueAtLastWP = 0
    private var waypointStartPosition: LatLng? = null

    private lateinit var textViewTimer: TextView
    private lateinit var textViewOverallDistance: TextView
    private lateinit var textViewOverallSpeed: TextView
    private lateinit var textViewCheckpointDistance: TextView
    private lateinit var textViewCheckpointDistanceDirect: TextView
    private lateinit var textViewCheckpointSpeed: TextView
    private lateinit var textViewWaypointDistanceDirect: TextView
    private lateinit var textViewWaypointDistance: TextView //Distance travelled since placing wp
    private lateinit var textViewWaypointSpeed: TextView //Speed since placing wp

    //Create view model
    private val polylineViewModel: PolylineViewModel by lazy {
        PolylineViewModel()
    }

    //Toggles
    private var keepCentered = true
    private var isNorthUp = false
    private var useSavedRotation = false

    private var savedRotation: Float = 0f


    //SharedPrefs
    private val TIMER_VALUE_KEY = "timerValueInSecond"
    private val TIMER_AT_LAST_CP_KEY = "timerValueAtLastCP"
    private val TIMER_AT_LAST_WP_KEY = "timerValueAtLastWP"
    private val WAYPOINT_START_POSITION_KEY = "waypointStartPosition"
    private val KEEP_CENTERED = "keepCentered"
    private val NORTH_UP = "northUp"
    private val SAVED_ROTATION = "savedRotation"
    private val USE_SAVED_ROTATION = "useSavedRotation"
    private val POSITION_WITH_TIMESTAMP = "positionWithTimestamp"

    data class LatLngTime(val position: LatLng, val timestamp: Long)
    private var latLngTime: MutableList<LatLngTime> = mutableListOf()

    private var minSpeed = 0
    private var maxSpeed = 0


    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        buttonMainStartStopService = findViewById(R.id.buttonMainStartStopService)
        textViewMainLat = findViewById(R.id.textViewMainLat)
        textViewMainLon = findViewById(R.id.textViewMainLon)
        buttonOptions = findViewById(R.id.buttonOptions)
        buttonMainAddCheckpoint = findViewById(R.id.buttonMainAddCheckpoint)
        buttonCompass = findViewById(R.id.buttonCompass)
        buttonCenter = findViewById(R.id.buttonCenter)
        buttonNorthUp = findViewById(R.id.buttonNorthUp)
        buttonSetRotation = findViewById(R.id.buttonSetRotation)

        //Bottom info
        textViewOverallDistance = findViewById(R.id.textViewOverallDistance)
        textViewTimer = findViewById(R.id.textViewSessionDurtation)
        textViewOverallSpeed = findViewById(R.id.textViewOverallSpeed)
        textViewCheckpointDistance = findViewById(R.id.textViewDistanceFromCP)
        textViewCheckpointDistanceDirect = findViewById(R.id.textViewDistanceToLastCPLine)
        textViewCheckpointSpeed = findViewById(R.id.textViewAverageSpeedFromLastCP)
        textViewWaypointDistance = findViewById(R.id.textViewDistanceToWP)
        textViewWaypointDistanceDirect = findViewById(R.id.textViewDistanceToWPLine)
        textViewWaypointSpeed = findViewById(R.id.textViewAverageSpeedWP)
        textViewTimer.text = timerValue

        textViewOverallDistance.text = "0"
        textViewOverallSpeed.text = "0"
        textViewCheckpointDistance.text = "0"
        textViewCheckpointDistanceDirect.text = "0"
        textViewCheckpointSpeed.text = "0"
        textViewWaypointDistanceDirect.text = "0"
        textViewWaypointDistance.text = "0"
        textViewWaypointSpeed.text = "0"
        //====

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        innerBroadcastReceiverIntentFilter.addAction(C.ACTION_LOCATION_UPDATE)
        innerBroadcastReceiverIntentFilter.addAction(C.ACTION_PLACE_CHECKPOINT)
        innerBroadcastReceiverIntentFilter.addAction(LocationService.ACTION_ADD_CHECKPOINT_BROADCAST)

        innerBroadcastReceiverIntentFilter.addAction(C.ACTION_TIMER)
        innerBroadcastReceiverIntentFilter.addAction(C.ACTION_TIMER_SERVICE_START)
        innerBroadcastReceiverIntentFilter.addAction(C.ACTION_TIMER_SERVICE_DESTROY)

        checkLocationPermissions()
        createNotificationChannel()
        loadSpeedValues()

        //Set on click listener for add checkpoint button
        buttonMainAddCheckpoint.setOnClickListener {
            if (userLocation != null) {
                addCheckpoint(userLocation!!)
                timerValueAtLastCP = timerValueInSecond
            }
        }

        buttonOptions.setOnClickListener {
            SwitchToSettingsActivity()
        }

        buttonCenter.setOnClickListener {
            keepCentered = !keepCentered
        }

        buttonNorthUp.setOnClickListener {
            isNorthUp = !isNorthUp
        }

        buttonSetRotation.setOnClickListener {
            if (userLocation != null) {
                if (savedRotation != 0f) {
                    savedRotation = 0f
                    useSavedRotation = false
                } else {
                    savedRotation = userHeading!!
                    useSavedRotation = true
                }
            }
        }

        //Compass
        compassImage = findViewById(R.id.imageViewCompass)
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)!!
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)!!

        buttonCompass.setOnClickListener {
            isCompassVisible = !isCompassVisible
            updateCompassState()
        }
        //====
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

    private fun loadSpeedValues() {
        val prefs = getSharedPreferences("Prefs", Context.MODE_PRIVATE)
        minSpeed = prefs.getInt("minSpeed", 5)
        maxSpeed = prefs.getInt("maxSpeed", 10)
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
        if (userLocation != null) {
            waypointStartPosition = userLocation
        }

        timerValueAtLastWP = timerValueInSecond

        drawPath()
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
        loadSpeedValues()
        super.onResume()
        LocalBroadcastManager.getInstance(this).registerReceiver(innerBroadcastReceiver, innerBroadcastReceiverIntentFilter)
        if (isCompassVisible) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
            sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    override fun onPause() {
        loadSpeedValues()
        super.onPause()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(innerBroadcastReceiver)
        if (isCompassVisible) {
            sensorManager.unregisterListener(this, accelerometer)
            sensorManager.unregisterListener(this, magnetometer)
        }

    }

    //Compass
    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor === accelerometer) {
            lowPass(event.values, lastAccelerometer)
            lastAccelerometerSet = true
        } else if (event.sensor === magnetometer) {
            lowPass(event.values, lastMagnetometer)
            lastMagnetometerSet = true
        }

        if (lastAccelerometerSet && lastMagnetometerSet) {
            val r = FloatArray(9)
            if (SensorManager.getRotationMatrix(r, null, lastAccelerometer, lastMagnetometer)) {
                val orientation = FloatArray(3)
                SensorManager.getOrientation(r, orientation)
                val degree = (toDegrees(orientation[0].toDouble()) + 360).toFloat() % 360

                val rotateAnimation = RotateAnimation(
                    currentDegree,
                    -degree,
                    RELATIVE_TO_SELF, 0.5f,
                    RELATIVE_TO_SELF, 0.5f
                )
                rotateAnimation.duration = 1000
                rotateAnimation.fillAfter = true

                compassImage.startAnimation(rotateAnimation)
                currentDegree = -degree
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    fun lowPass(input: FloatArray, output: FloatArray) {
        val alpha = 0.05f

        for (i in input.indices) {
            output[i] = output[i] + alpha * (input[i] - output[i])
        }
    }

    private fun updateCompassState() {
        if (isCompassVisible) {
            // Register the sensor listener when the compass is enabled
            sensorManager.registerListener(
                this, accelerometer, SensorManager.SENSOR_DELAY_GAME
            )
            sensorManager.registerListener(
                this, magnetometer, SensorManager.SENSOR_DELAY_GAME
            )
        } else {
            // Unregister the sensor listener when the compass is disabled
            sensorManager.unregisterListener(this, accelerometer)
            sensorManager.unregisterListener(this, magnetometer)
        }
    }
    //====

    //Ui updates on pos change
    private fun updateOverallDistance() {
        val points = polyline?.points
        var totalLength = 0.0

        if (points != null) {
            for (i in 1 until points.size) {
                val prevLatLng = points[i - 1]
                val currentLatLng = points[i]
                totalLength += calculateDistance(prevLatLng, currentLatLng)
            }
        }

        val roundedDistance = String.format("%.0f", totalLength)
        textViewOverallDistance.text = roundedDistance
        textViewOverallSpeed.text = calculateTimeToTravelOneKm(totalLength, timerValueInSecond).toString()
    }

    fun updateDistanceToLastCheckpoint(){
        val lastCheckpointIndex = checkpoints.size - 1


        // Ensure there is at least one checkpoint
        if (lastCheckpointIndex >= 0 && userLocation != null) {
            val lastCheckpoint = checkpoints[lastCheckpointIndex]

            Log.d(TAG, "lasCP: $checkpoints[lastCheckpointIndex]")
            // Calculate polyline distance
            val polylineDistance = calculatePolylineDistance(lastCheckpoint, userLocation!!)

            val directDistance = calculateDistance(lastCheckpoint, userLocation!!)
            // Update the appropriate TextView with the polyline distance


            val roundedDistance = String.format("%.0f", polylineDistance)
            val roundedDistanceDirect = String.format("%.0f", directDistance)

            textViewCheckpointDistance.text = roundedDistance
            textViewCheckpointDistanceDirect.text = roundedDistanceDirect
            textViewCheckpointSpeed.text = calculateTimeToTravelOneKm(polylineDistance.toDouble(), timerValueInSecond - timerValueAtLastCP).toString()
        }
    }

    private fun updateWaypointDistance() {
        val lastWaypoint = currentWaypoint?.position

        val distanceToWaypoint = lastWaypoint?.let { calculateDistance(userLocation!!, it) } ?: 0f
        val polylineDistanceFromWpPlacement = waypointStartPosition?.let { calculatePolylineDistance(it, userLocation!!) }

        val roundedDistance = String.format("%.0f", polylineDistanceFromWpPlacement)
        val roundedDistanceDirect = String.format("%.0f", distanceToWaypoint)

        textViewWaypointDistance.text = roundedDistance
        textViewWaypointDistanceDirect.text = roundedDistanceDirect

        if (polylineDistanceFromWpPlacement != null) {
            textViewWaypointSpeed.text = calculateTimeToTravelOneKm(polylineDistanceFromWpPlacement.toDouble(), timerValueInSecond - timerValueAtLastCP).toString()
        }
    }

    private fun calculatePolylineDistance(start: LatLng, end: LatLng): Float {
        var totalDistance = 0f
        var foundStart = false

        // Ensure the polyline and user location are not null
        if (polyline != null) {
            val points = polyline!!.points

            // Iterate through the polyline points
            for (i in 1 until points.size) {
                val prevLatLng = points[i - 1]
                val currentLatLng = points[i]

                // Start accumulating distances when the start point is found
                if (prevLatLng == start) {
                    foundStart = true
                }

                if (foundStart) {
                    Log.d(TAG, "foundStart")
                    // Accumulate distances between consecutive polyline points
                    totalDistance += calculateDistance(prevLatLng, currentLatLng)

                    // Break loop if the currentLatLng is the end point
                    if (currentLatLng == end) {
                        break
                    }
                }
            }
        }

        return totalDistance
    }

    fun calculateTimeToTravelOneKm(distanceInMeters: Double, timeInSeconds: Int): Double {
        val speed = distanceInMeters / timeInSeconds
        val time = (1000 / speed) / 60

        return String.format("%.2f", time).toDouble()
    }
    //====
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

        outState.putString("overallDistance", textViewOverallDistance.text.toString())
        outState.putString("overallSpeed", textViewOverallSpeed.text.toString())
        outState.putString("checkpointDistance", textViewCheckpointDistance.text.toString())
        outState.putString("checkpointDistanceDirect", textViewCheckpointDistanceDirect.text.toString())
        outState.putString("checkpointSpeed", textViewCheckpointSpeed.text.toString())
        outState.putString("waypointDistanceDirect", textViewWaypointDistanceDirect.text.toString())
        outState.putString("waypointDistance", textViewWaypointDistance.text.toString())
        outState.putString("waypointSpeed", textViewWaypointSpeed.text.toString())

        //Save buttonMainStartStopService text
        outState.putString("buttonMainStartStopServiceText", buttonMainStartStopService.text.toString())
        //Save user location
        if (userLocation != null) {
            outState.putDouble("userLocationLat", userLocation!!.latitude)
            outState.putDouble("userLocationLon", userLocation!!.longitude)
        }

        outState.putBoolean("isCompassVisible", isCompassVisible)

        val sharedPref = getSharedPreferences("Prefs", Context.MODE_PRIVATE)
        val editor = sharedPref.edit()

        val gson = Gson()
        val json = gson.toJson(latLngTime)

        editor.putInt(TIMER_VALUE_KEY, timerValueInSecond)
        editor.putInt(TIMER_AT_LAST_CP_KEY, timerValueAtLastCP)
        editor.putInt(TIMER_AT_LAST_WP_KEY, timerValueAtLastWP)
        editor.putBoolean(KEEP_CENTERED, keepCentered)
        editor.putBoolean(NORTH_UP, isNorthUp)
        editor.putBoolean(USE_SAVED_ROTATION, useSavedRotation)
        editor.putFloat(SAVED_ROTATION, savedRotation)
        editor.putString(POSITION_WITH_TIMESTAMP, json)

        if (waypointStartPosition != null) {
            editor.putFloat(WAYPOINT_START_POSITION_KEY + "_lat", waypointStartPosition!!.latitude.toFloat())
            editor.putFloat(WAYPOINT_START_POSITION_KEY + "_lon", waypointStartPosition!!.longitude.toFloat())
        }

        editor.apply()
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        loadSpeedValues()
        val gson = Gson()
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

        textViewOverallDistance.text = savedInstanceState.getString("overallDistance", "")
        textViewOverallSpeed.text = savedInstanceState.getString("overallSpeed", "")
        textViewCheckpointDistance.text = savedInstanceState.getString("checkpointDistance", "")
        textViewCheckpointDistanceDirect.text = savedInstanceState.getString("checkpointDistanceDirect", "")
        textViewCheckpointSpeed.text = savedInstanceState.getString("checkpointSpeed", "")
        textViewWaypointDistanceDirect.text = savedInstanceState.getString("waypointDistanceDirect", "")
        textViewWaypointDistance.text = savedInstanceState.getString("waypointDistance", "")
        textViewWaypointSpeed.text = savedInstanceState.getString("waypointSpeed", "")

        isCompassVisible = savedInstanceState.getBoolean("isCompassVisible")

        val sharedPref = getSharedPreferences("Prefs", Context.MODE_PRIVATE)

        timerValueInSecond = sharedPref.getInt(TIMER_VALUE_KEY, 0)
        timerValueAtLastCP = sharedPref.getInt(TIMER_AT_LAST_CP_KEY, 0)
        timerValueAtLastWP = sharedPref.getInt(TIMER_AT_LAST_WP_KEY, 0)
        keepCentered = sharedPref.getBoolean(KEEP_CENTERED, true)
        isNorthUp = sharedPref.getBoolean(NORTH_UP, false)
        useSavedRotation = sharedPref.getBoolean(USE_SAVED_ROTATION, false)
        savedRotation = sharedPref.getFloat(SAVED_ROTATION, 0f)

        val retrievedWaypointLat = sharedPref.getFloat(WAYPOINT_START_POSITION_KEY + "_lat", 0f)
        val retrievedWaypointLon = sharedPref.getFloat(WAYPOINT_START_POSITION_KEY + "_lon", 0f)
        val jsonFromPrefs = sharedPref.getString(POSITION_WITH_TIMESTAMP, "")

        if (jsonFromPrefs!!.isNotEmpty()) {
            val type = object : TypeToken<MutableList<LatLngTime>>() {}.type
            latLngTime = gson.fromJson(jsonFromPrefs, type)
        }

        waypointStartPosition =
            if (retrievedWaypointLat != 0f && retrievedWaypointLon != 0f) {
                LatLng(retrievedWaypointLat.toDouble(), retrievedWaypointLon.toDouble())
            } else {
                null
            }
    }

    fun restorePolyLine(){
        loadSpeedValues()
        Log.d(TAG, "onRestoreLine")
        polylineViewModel.polylinePoints?.let {
            polyLineOptions.color(Color.TRANSPARENT)
            polyLineOptions.addAll(it)
            polyline = mMap.addPolyline(polyLineOptions)

            drawPathWithSpeedColors(mMap, latLngTime)
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
        val timerServiceIntent = Intent(this, TimerService::class.java)

        if (locationServiceRunning) {
            showStopServiceConfirmationDialog(serviceIntent)
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
                startService(timerServiceIntent)
            } else {
                startService(serviceIntent)
                startService(timerServiceIntent)
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

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }

        mMap.isMyLocationEnabled = true;
        // add user directional indicatior
        if (userLocation != null && userHeading != null) {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation!!, 15f))
        }

        // Restore polyline from saved instance state
        restorePolyLine()
        restoreMarkers()
    }

    private fun updateMapOrientation() {
        if (::mMap.isInitialized && userLocation != null) {
            val builder = CameraPosition.Builder()

            // Assuming you have initialized isNorthUp, useSavedRotation, and userHeading

            userLocation?.let {
                builder.target(it)
            }

            builder.zoom(18f)
            builder.tilt(0f)

            when {
                isNorthUp -> builder.bearing(0f)
                useSavedRotation -> builder.bearing(savedRotation)
                else -> userHeading?.let { builder.bearing(it) }
            }

            val cameraPosition = builder.build()

            mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
        }
    }

    private fun updateLocation(lat: Double, lon: Double, timestamp: Long){
        polyline?.remove()

        val latLng = LatLng(lat, lon)
        val newPositionWithTime = LatLngTime(LatLng(lat, lon), timestamp)
        latLngTime.add(newPositionWithTime)

        Log.d(TAG, "Lat: $userSpeed Lng: $lon Timestamp: $timestamp")

        polyLineOptions.add(latLng).color(Color.TRANSPARENT)
        polyline = mMap.addPolyline(polyLineOptions)

        drawPathWithSpeedColors(mMap, latLngTime)

        if (keepCentered) {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation!!, 18f))
            updateMapOrientation()
        }

        updateOverallDistance()
        updateDistanceToLastCheckpoint()
        updateWaypointDistance()
    }

    private fun drawPathWithSpeedColors(googleMap: GoogleMap, points: List<LatLngTime>) {
        if (points.size < 2) {
            // Path requires at least two points
            return
        }

        for (i in 0 until points.size - 1) {
            val startPoint = points[i]
            val endPoint = points[i + 1]

            val speed = calculateSpeed(startPoint, endPoint)

            val polylineOptions = PolylineOptions()
                .add(startPoint.position, endPoint.position)
                .color(getColorForSpeed(speed))
                .width(10f) // Set your desired polyline width here

            googleMap.addPolyline(polylineOptions)
        }
    }

    private fun getColorForSpeed(speed: Double): Int {
        // Define your speed thresholds and corresponding colors
        val slowSpeedThreshold = minSpeed // Example threshold for slow speed
        val moderateSpeedThreshold = maxSpeed // Example threshold for moderate speed

        return when {
            speed < slowSpeedThreshold -> Color.GREEN
            speed < moderateSpeedThreshold -> Color.YELLOW
            else -> Color.RED
        }
    }

    private fun calculateSpeed(startPoint: LatLngTime, endPoint: LatLngTime): Double {
        val distance = calculateDistance(startPoint.position, endPoint.position)
        val timeDifference = (endPoint.timestamp - startPoint.timestamp) / 1000.0 // Convert to seconds
        return if (timeDifference != 0.0) distance / timeDifference else 0.0
    }

    private fun resetUiState() {
        val resetIntent = Intent(this, TimerService::class.java)
        stopService(resetIntent)

        timerValue = "00:00:00"
        textViewTimer.text = timerValue
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
                    textViewMainLat.text = broadcastIntent.getDoubleExtra(C.DATA_LOCATION_UPDATE_LAT, 0.0).toString()
                    textViewMainLon.text = broadcastIntent.getDoubleExtra(C.DATA_LOCATION_UPDATE_LON, 0.0).toString()
                    updateLocation(broadcastIntent.getDoubleExtra(C.DATA_LOCATION_UPDATE_LAT, 0.0), broadcastIntent.getDoubleExtra(C.DATA_LOCATION_UPDATE_LON, 0.0), broadcastIntent.getLongExtra(C.DATA_LOCATION_UPDATE_TIMESTAMP, 0L))

                    drawPath()

                    val returnIntent = Intent(C.ACTION_REMOVE_LOCATION_UPDATE)
                    returnIntent.putExtra(C.DATA_LOCATION_UPDATE_LAT, userLocation?.latitude)
                    returnIntent.putExtra(C.DATA_LOCATION_UPDATE_LON, userLocation?.longitude)
                    LocalBroadcastManager.getInstance(this@MainActivity).sendBroadcast(returnIntent)
                }
                C.ACTION_PLACE_CHECKPOINT -> {
                    Log.d(TAG, "ACTION_PLACE_CHECKPOINT")
                    val checkpointLatLng = LatLng(
                        broadcastIntent.getDoubleExtra(C.DATA_LOCATION_UPDATE_LAT, 0.0),
                        broadcastIntent.getDoubleExtra(C.DATA_LOCATION_UPDATE_LON, 0.0)
                    )
                    addCheckpoint(checkpointLatLng)
                    val returnIntent = Intent(C.ACTION_STOP_CHECKPOINT_BROADCAST)
                    //Add checkpoint to return intent
                    returnIntent.putExtra(C.DATA_LOCATION_UPDATE_LAT, checkpointLatLng.latitude)
                    returnIntent.putExtra(C.DATA_LOCATION_UPDATE_LON, checkpointLatLng.longitude)
                    LocalBroadcastManager.getInstance(this@MainActivity).sendBroadcast(returnIntent)
                }
                C.ACTION_TIMER -> {
                    val payloadTime = broadcastIntent.getStringExtra(C.PAYLOAD_TIME)
                    val seconds = payloadTime?.toIntOrNull() ?: 0
                    val formattedTime = formatTime(seconds)
                    timerValueInSecond = seconds
                    timerValue = formattedTime
                    textViewTimer.text = formattedTime
                }
            }
        }
    }

    private fun formatTime(seconds: Int): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val remainingSeconds = seconds % 60

        return String.format("%02d:%02d:%02d", hours, minutes, remainingSeconds)
    }

    private fun calculateDistance(start: LatLng, end: LatLng): Float {
        val results = FloatArray(1)
        Location.distanceBetween(start.latitude, start.longitude, end.latitude, end.longitude, results)
        return results[0]
    }

    private fun showStopServiceConfirmationDialog(serviceIntent: Intent) {
        val timerServiceIntent = Intent(this, TimerService::class.java)

        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.setTitle("Confirmation")
        alertDialogBuilder.setMessage("Are you sure you want to stop the service?")
        alertDialogBuilder.setPositiveButton("Yes") { _, _ ->
            stopService(serviceIntent)
            stopService(timerServiceIntent)
            resetUiState()
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
                val sharedPref = getSharedPreferences("Prefs", Context.MODE_PRIVATE) ?: return@Listener
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
                val sharedPref = getSharedPreferences("Prefs", Context.MODE_PRIVATE) ?: return@Listener
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
                val sharedPref = getSharedPreferences("Prefs", Context.MODE_PRIVATE) ?: return@Listener
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
                val sharedPref = getSharedPreferences("Prefs", Context.MODE_PRIVATE) ?: return@Listener
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
                val sharedPref = getSharedPreferences("Prefs", Context.MODE_PRIVATE) ?: return@Listener
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
                val savedPreferences = getSharedPreferences("Prefs", Context.MODE_PRIVATE)
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
