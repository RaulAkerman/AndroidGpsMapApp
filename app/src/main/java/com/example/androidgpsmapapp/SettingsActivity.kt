package com.example.androidgpsmapapp

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AlertDialog

class SettingsActivity : AppCompatActivity() {

    private lateinit var buttonSettingsApply: Button
    private lateinit var editTextNumberMinimumSpeed: EditText
    private lateinit var editTextNumberMaximumSpeed: EditText
    private lateinit var buttonRegister: Button
    private lateinit var buttonLogin: Button
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        editTextNumberMinimumSpeed = findViewById(R.id.editTextNumberMinimumSpeed)
        editTextNumberMaximumSpeed = findViewById(R.id.editTextNumberMaximumSpeed)
        buttonSettingsApply = findViewById(R.id.buttonSettingsApply)
        buttonRegister = findViewById(R.id.buttonRegister)
        buttonLogin = findViewById(R.id.buttonLogin)

        buttonSettingsApply.setOnClickListener {
            saveSpeedValues()
            finish()
        }

        buttonRegister.setOnClickListener {
            showRegisterDialog()
        }

        buttonLogin.setOnClickListener {
            showLoginDialog()
        }

        val prefs = getSharedPreferences("Prefs", Context.MODE_PRIVATE)
        val savedMinSpeed = prefs.getInt("minSpeed", 5)
        val savedMaxSpeed = prefs.getInt("maxSpeed", 10)

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
                println("Email: $email")
                println("First Name: $firstName")
                println("Last Name: $lastName")
                println("Password: $password")
                println("Confirm Password: $confirmPassword")

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
            .setTitle("Register")
            .setPositiveButton("Register") { dialog, _ ->
                // Handle registration logic here
                val email = emailEditText.text.toString()
                val password = passwordEditText.text.toString()

                // Perform Login. Temp to show input values
                println("Email: $email")
                println("Password: $password")

                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }

        val alertDialog = builder.create()
        alertDialog.show()
    }
}