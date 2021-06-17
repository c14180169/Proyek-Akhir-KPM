package com.example.proyekkpm

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.telephony.SmsManager
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

@Suppress("DEPRECATION")
class ComposeSmsActivity : AppCompatActivity() {

    private lateinit var phoneNumber : EditText
    private lateinit var msgBody : EditText
    private lateinit var sendBtn : Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_compose_sms)

        phoneNumber = findViewById(R.id.phoneNumber)
        msgBody = findViewById(R.id.msgBody)
        sendBtn = findViewById(R.id.sendBtn)

        sendBtn.setOnClickListener {
            Log.d("Msg", msgBody.text.toString())
            Log.d("Phone", phoneNumber.text.toString())

            val sentIntent = PendingIntent.getBroadcast(this, 0, Intent("SMS_RECEIVED"), 0)
            val deliveredIntent = PendingIntent.getBroadcast(this, 0, Intent("SMS_DELIVER"), 0)

            try {
                val smsManager = SmsManager.getDefault()
                smsManager.sendTextMessage(phoneNumber.text.toString().trim(), "ME", msgBody.text.toString().trim(), sentIntent, deliveredIntent)
                Toast.makeText(applicationContext, "Message Sent", Toast.LENGTH_LONG).show()

                finish()

            } catch (e: Exception) {
                Toast.makeText(applicationContext, "Some fields is Empty", Toast.LENGTH_LONG).show()
            }
        }

    }
}