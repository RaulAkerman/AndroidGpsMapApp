package com.example.androidgpsmapapp

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText

class SettingsActivity : AppCompatActivity() {

    private lateinit var buttonSettingsApply: Button
    private lateinit var editTextNumberMinimumSpeed: EditText
    private lateinit var editTextNumberMaximumSpeed: EditText
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        editTextNumberMinimumSpeed = findViewById(R.id.editTextNumberMinimumSpeed)
        editTextNumberMaximumSpeed = findViewById(R.id.editTextNumberMaximumSpeed)
        buttonSettingsApply = findViewById(R.id.buttonSettingsApply)

        buttonSettingsApply.setOnClickListener {
            saveSpeedValues()
            finish()
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
}