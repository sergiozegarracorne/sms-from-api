package com.example.smsapp

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var apiInput: EditText
    private lateinit var saveButton: Button
    private lateinit var serviceSwitch: Switch

    private val requestPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        apiInput = findViewById(R.id.apiInput)
        saveButton = findViewById(R.id.saveButton)
        serviceSwitch = findViewById(R.id.serviceSwitch)

        val prefs = getSharedPreferences("smsapp", Context.MODE_PRIVATE)
        apiInput.setText(prefs.getString("api_url", ""))
        serviceSwitch.isChecked = prefs.getBoolean("service_on", false)

        saveButton.setOnClickListener {
            prefs.edit().putString("api_url", apiInput.text.toString()).apply()
        }

        serviceSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("service_on", isChecked).apply()
            if (isChecked) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    requestPermission.launch(Manifest.permission.SEND_SMS)
                }
                ContextCompat.startForegroundService(this, Intent(this, SmsService::class.java))
            } else {
                stopService(Intent(this, SmsService::class.java))
            }
        }
    }
}
