package com.example.androidgpsmapapp

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import androidx.appcompat.app.AlertDialog
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import org.json.JSONObject

class SettingsActivity : AppCompatActivity() {

    private lateinit var buttonSettingsApply: Button
    private lateinit var editTextNumberMinimumSpeed: EditText
    private lateinit var editTextNumberMaximumSpeed: EditText
    private lateinit var buttonRegister: Button
    private lateinit var buttonLogin: Button
    private lateinit var spinnerSessions: Spinner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        editTextNumberMinimumSpeed = findViewById(R.id.editTextNumberMinimumSpeed)
        editTextNumberMaximumSpeed = findViewById(R.id.editTextNumberMaximumSpeed)
        buttonSettingsApply = findViewById(R.id.buttonSettingsApply)
        buttonRegister = findViewById(R.id.buttonRegister)
        buttonLogin = findViewById(R.id.buttonLogin)
        spinnerSessions = findViewById(R.id.spinnerPreviousSessions)

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

        val names = listOf("John", "Jane", "Bob", "Alice")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, names)

        val prefs = getSharedPreferences("Prefs", Context.MODE_PRIVATE)
        val savedMinSpeed = prefs.getInt("minSpeed", 5)
        val savedMaxSpeed = prefs.getInt("maxSpeed", 10)

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
            Response.Listener { response -> Log.d("Response.Listener", response);
                val sharedPref = getSharedPreferences("Prefs", Context.MODE_PRIVATE) ?: return@Listener
                Log.d("Response.Listener", response)
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
                params["email"] = email
                params["password"] = password
                params["firstName"] = firstName
                params["lastName"] = lastName

                var body = JSONObject(params as Map<*, *>).toString()
                Log.d("getBody", body)

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
            Response.Listener { response -> Log.d("Response.Listener", response);
                val sharedPref = getSharedPreferences("Prefs", Context.MODE_PRIVATE) ?: return@Listener
                with (sharedPref.edit()) {
                    putString("token", JSONObject(response).getString("token"))
                    apply()
                    buttonLogin.text = "Logout"
                } },
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
                Log.d("getBody", body)

                return body.toByteArray()
            }
        }
        handler.addToRequestQueue(httpRequest)
    }
}