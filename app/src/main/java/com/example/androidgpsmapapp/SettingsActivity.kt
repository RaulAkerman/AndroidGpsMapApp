package com.example.androidgpsmapapp

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import androidx.appcompat.app.AlertDialog
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.google.gson.Gson
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale

class SettingsActivity : AppCompatActivity() {

    private lateinit var buttonSettingsApply: Button
    private lateinit var buttonDelete: Button
    private lateinit var editTextNumberMinimumSpeed: EditText
    private lateinit var editTextNumberMaximumSpeed: EditText
    private lateinit var buttonRegister: Button
    private lateinit var buttonLogin: Button
    private lateinit var spinnerSessions: Spinner
    private lateinit var loadButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("Prefs", Context.MODE_PRIVATE)
        val names = prefs.getStringSet("sessions_id_list", emptySet())!!.toList()
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, names)

        val savedMinSpeed = prefs.getInt("minSpeed", 5)
        val savedMaxSpeed = prefs.getInt("maxSpeed", 10)
        setContentView(R.layout.activity_settings)

        editTextNumberMinimumSpeed = findViewById(R.id.editTextNumberMinimumSpeed)
        editTextNumberMaximumSpeed = findViewById(R.id.editTextNumberMaximumSpeed)
        buttonSettingsApply = findViewById(R.id.buttonSettingsApply)
        buttonRegister = findViewById(R.id.buttonRegister)
        buttonLogin = findViewById(R.id.buttonLogin)
        spinnerSessions = findViewById(R.id.spinnerPreviousSessions)
        buttonDelete = findViewById(R.id.buttonDelete)
        loadButton = findViewById(R.id.buttonLoad)

        buttonSettingsApply.setOnClickListener {
            saveSpeedValues()
            finish()
        }

        buttonRegister.setOnClickListener {
            showRegisterDialog()
        }

        buttonLogin.setOnClickListener {
            if (buttonLogin.text == "Logout") {
                val sharedPref = getSharedPreferences("Prefs", Context.MODE_PRIVATE) ?: return@setOnClickListener
                with (sharedPref.edit()) {
                    putString("token", null)
                    apply()
                }
                buttonLogin.text = "Login"
            } else{
                showLoginDialog()
            }
        }

        buttonDelete.setOnClickListener {
            val prefs = getSharedPreferences("Prefs", Context.MODE_PRIVATE)
            val editor: SharedPreferences.Editor = prefs.edit()

            val sessions = prefs.getStringSet("sessions_id_list", emptySet())?.toMutableSet()

            val selectedSessionId = sessions?.elementAtOrNull(spinnerSessions.selectedItemPosition)

            selectedSessionId?.let {
                sessions?.remove(it)
            }

            editor.putStringSet("sessions_id_list", sessions)
            editor.apply()
            deleteSession(selectedSessionId!!)
            finish()
        }

        loadButton.setOnClickListener {
            val prefs = getSharedPreferences("Prefs", Context.MODE_PRIVATE)
            val sessions = prefs.getStringSet("sessions_id_list", emptySet())?.toMutableSet()
            val selectedSessionId = sessions?.elementAtOrNull(spinnerSessions.selectedItemPosition)
            getSessionData(selectedSessionId!!)
        }



        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        if (prefs.getString("token", null) != null && !prefs.getString("token", null).isNullOrEmpty()) {
            buttonLogin.text = "Logout"
        }
        spinnerSessions.adapter = adapter


        editTextNumberMinimumSpeed.setText(savedMinSpeed.toString())
        editTextNumberMaximumSpeed.setText(savedMaxSpeed.toString())
    }

    private fun saveSpeedValues() {
        val minSpeed = editTextNumberMinimumSpeed.text.toString().toInt()
        val maxSpeed = editTextNumberMaximumSpeed.text.toString().toInt()

        val prefs = getSharedPreferences("Prefs", Context.MODE_PRIVATE)
        val editor: SharedPreferences.Editor = prefs.edit()

        editor.putInt("minSpeed", minSpeed)
        editor.putInt("maxSpeed", maxSpeed)

        editor.apply()
    }



    private fun showRegisterDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.register_dialog, null)
        val emailEditText: EditText = dialogView.findViewById(R.id.editTextEmail)
        val firstNameEditText: EditText = dialogView.findViewById(R.id.editTextFirstName)
        val lastNameEditText: EditText = dialogView.findViewById(R.id.editTextLastName)
        val passwordEditText: EditText = dialogView.findViewById(R.id.editTextPassword)
        val confirmPasswordEditText: EditText = dialogView.findViewById(R.id.editTextConfirmPassword)

        val builder = AlertDialog.Builder(this)
        builder.setView(dialogView)
            .setTitle("Register")
            .setPositiveButton("Register") { dialog, _ ->
                val email = emailEditText.text.toString()
                val firstName = firstNameEditText.text.toString()
                val lastName = lastNameEditText.text.toString()
                val password = passwordEditText.text.toString()
                val confirmPassword = confirmPasswordEditText.text.toString()

                // Perform registration. Temp to show input values
                register(dialogView, email, password, firstName, lastName)

                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
        val alertDialog = builder.create()
        alertDialog.show()
    }

    private fun showLoginDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.login_dialog, null)
        val emailEditText: EditText = dialogView.findViewById(R.id.editTextLoginEmail)
        val passwordEditText: EditText = dialogView.findViewById(R.id.editTextLoginPassword)



        val builder = AlertDialog.Builder(this)
        builder.setView(dialogView)
            .setTitle("Login")
            .setPositiveButton("Login") { dialog, _ ->
                // Handle registration logic here
                val email = emailEditText.text.toString()
                val password = passwordEditText.text.toString()

                // Perform Login. Temp to show input values
                login(dialogView, email, password)

                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }

        val alertDialog = builder.create()
        alertDialog.show()
    }

    fun register(view: android.view.View, email: String, password: String, firstName: String, lastName: String) {
        var url = "https://sportmap.akaver.com/api/v1.0/account/register"
        var handler = HttpSingletonHandler.getInstance(this)

        var httpRequest = object : StringRequest(
            Request.Method.POST,
            url,
            Response.Listener { response ->
                val sharedPref = getSharedPreferences("Prefs", Context.MODE_PRIVATE) ?: return@Listener

                with (sharedPref.edit()) {
                    putString("token", JSONObject(response).getString("token"))
                    apply()
                }
            },
            Response.ErrorListener { error ->  Log.d("Response.ErrorListener", "${error.message} ${error.networkResponse.statusCode}")}
        ){
            override fun getBodyContentType(): String {
                return "application/json"
            }
            override fun getBody(): ByteArray {
                val params = HashMap<String, String>()
                params["email"] = email
                params["password"] = password
                params["firstName"] = firstName
                params["lastName"] = lastName

                var body = JSONObject(params as Map<*, *>).toString()

                return body.toByteArray()
            }
        }
        handler.addToRequestQueue(httpRequest)
    }

    fun login(view: android.view.View, email: String, password: String) {
        val url = "https://sportmap.akaver.com/api/v1.0/account/login"
        val handler = HttpSingletonHandler.getInstance(this)


        //Save the token from the response json to shared preferences
        val httpRequest = object : StringRequest(
            Request.Method.POST,
            url,
            Response.Listener { response ->
                val sharedPref = getSharedPreferences("Prefs", Context.MODE_PRIVATE) ?: return@Listener

                with(sharedPref.edit()) {
                    putString("token", JSONObject(response).getString("token"))
                    apply()
                    buttonLogin.text = "Logout"
                }
            },
            Response.ErrorListener { error ->  Log.d("Response.ErrorListener", "${error.message} ${error.networkResponse.statusCode}")}
        ){
            override fun getBodyContentType(): String {
                return "application/json"
            }
            override fun getBody(): ByteArray {
                val params = HashMap<String, String>()
                params["email"] = email
                params["password"] = password

                val body = JSONObject(params as Map<*, *>).toString()
                return body.toByteArray()
            }
        }
        handler.addToRequestQueue(httpRequest)
    }


    fun deleteSession(sessionId: String) {
        val url = "https://sportmap.akaver.com/api/v1.0/GpsSessions/$sessionId"
        val handler = HttpSingletonHandler.getInstance(this)

        val httpRequest = object : StringRequest(
            Request.Method.DELETE,
            url,
            Response.Listener { response ->
                Log.d("Response.Listener", response)
            },
            Response.ErrorListener { error ->  Log.d("Response.ErrorListener", "${error.message} ${error.networkResponse.statusCode}")}
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                val sharedPref = getSharedPreferences("Prefs", Context.MODE_PRIVATE) ?: return mutableMapOf()
                val token = sharedPref.getString("token", "")
                val headers = HashMap<String, String>()
                headers["Authorization"] = "Bearer $token"
                headers["Content-Type"] = "application/json"
                return headers
            }
        }
        handler.addToRequestQueue(httpRequest)
    }

    fun getSessionData(sessionId: String) {
        val url = "https://sportmap.akaver.com/api/v1.0/GpsLocations?GpsSessionId=$sessionId"
        val handler = HttpSingletonHandler.getInstance(this)

        val httpRequest = object : StringRequest(
            Request.Method.GET,
            url,
            Response.Listener { response ->
                val gson = Gson()

                // Parse the JSON array response
                val jsonArray = JSONArray(response)

                // Create an empty list to store LatLngTime objects
                val latLngTimeList = mutableListOf<LatLngTime>()

                // Create a second list to store LatLngTime objects with "gpsLocationTypeId": "00000000-0000-0000-0000-000000000003"
                val latLngTimeCheckpointList = mutableListOf<LatLngTime>()

                // Iterate through the JSON array and convert each item to LatLngTime
                for (i in 0 until jsonArray.length()) {
                    val jsonObject = jsonArray.getJSONObject(i)
                    val latLngTime = convertToLatLngTime(jsonObject)
                    if (latLngTime != null) {
                        latLngTimeList.add(latLngTime)
                    }

                    // Check if "gpsLocationTypeId" is "00000000-0000-0000-0000-000000000003" and add to the checkpoint list
                    if (jsonObject.getString("gpsLocationTypeId") == "00000000-0000-0000-0000-000000000003") {
                        if (latLngTime != null) {
                            latLngTimeCheckpointList.add(latLngTime)
                        }
                    }
                }
                val jsonPositions = gson.toJson(latLngTimeList)
                val jsonCheckpoints = gson.toJson(latLngTimeCheckpointList)

                val prefs = getSharedPreferences("Prefs", Context.MODE_PRIVATE)
                val editor: SharedPreferences.Editor = prefs.edit()
                editor.putString("loadedPositions", jsonPositions)
                editor.putString("loadedCheckpoints", jsonCheckpoints)
                editor.apply()
                finish()

                // Now you have latLngTimeList containing all LatLngTime objects
                // And latLngTimeCheckpointList containing LatLngTime objects with "gpsLocationTypeId": "00000000-0000-0000-0000-000000000003"

                // Optionally, you can perform additional actions here
            },
            Response.ErrorListener { error ->
                Log.d("Response.ErrorListener", "${error.message} ${error.networkResponse.statusCode}")
            }
        ) {
            override fun getHeaders(): MutableMap<String, String> {
                val sharedPref = getSharedPreferences("Prefs", Context.MODE_PRIVATE) ?: return mutableMapOf()
                val token = sharedPref.getString("token", "")
                val headers = HashMap<String, String>()
                headers["Authorization"] = "Bearer $token"
                headers["Content-Type"] = "application/json"
                return headers
            }
        }
        handler.addToRequestQueue(httpRequest)
    }

    data class LatLng(val latitude: Double, val longitude: Double)
    data class LatLngTime(val position: LatLng, val timestamp: Long)

    fun convertToLatLngTime(item: JSONObject): LatLngTime? {
        val latitude = item.getDouble("latitude")
        val longitude = item.getDouble("longitude")
        val timestamp = dateStringToSystemTimeInMillis(item.getString("recordedAt"))
        val latLng = LatLng(latitude, longitude)
        return LatLngTime(latLng, timestamp)
    }

    fun dateStringToSystemTimeInMillis(dateString: String): Long {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val date = dateFormat.parse(dateString)

        // Convert the Date object to system time in milliseconds
        return date?.time ?: 0
    }


}