package com.example.smsapp

import android.app.Service
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.os.IBinder
import android.telephony.SmsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

class SmsService : Service() {
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private lateinit var dbHelper: SmsDatabaseHelper

    override fun onCreate() {
        super.onCreate()
        dbHelper = SmsDatabaseHelper(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        scope.launch { loop() }
        return START_STICKY
    }

    private suspend fun loop() {
        while (isActive) {
            val prefs = getSharedPreferences("smsapp", Context.MODE_PRIVATE)
            val url = prefs.getString("api_url", null)
            if (!url.isNullOrEmpty()) {
                fetchMessages(url)
                sendPending()
            }
            delay(10000)
        }
    }

    private fun fetchMessages(urlString: String) {
        try {
            val connection = URL(urlString).openConnection() as HttpURLConnection
            connection.connectTimeout = 5000
            val data = connection.inputStream.bufferedReader().use { it.readText() }
            val array = JSONArray(data)
            val db = dbHelper.writableDatabase
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val phone = obj.getString("phone")
                val message = obj.getString("message")
                val values = ContentValues().apply {
                    put("phone", phone)
                    put("message", message)
                    put("sent", 0)
                }
                db.insert("messages", null, values)
            }
            connection.disconnect()
        } catch (_: Exception) {
        }
    }

    private suspend fun sendPending() {
        val db = dbHelper.writableDatabase
        val cursor = db.query("messages", arrayOf("id", "phone", "message"), "sent=0", null, null, null, null)
        val smsManager = SmsManager.getDefault()
        while (cursor.moveToNext()) {
            val id = cursor.getLong(0)
            val phone = cursor.getString(1)
            val message = cursor.getString(2)
            smsManager.sendTextMessage(phone, null, message, null, null)
            val values = ContentValues().apply { put("sent", 1) }
            db.update("messages", values, "id=?", arrayOf(id.toString()))
            delay(10000)
        }
        cursor.close()
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
