package com.example.proyekkpm

import android.app.ProgressDialog
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.proyekkpm.adapters.ListSmsAdapter
import com.example.proyekkpm.model._Sms
import com.example.proyekkpm.services.iv
import com.example.proyekkpm.services.salt
import com.example.proyekkpm.services.secretKey
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

private var smsList = arrayListOf<_Sms>()

@Suppress("DEPRECATION")
class DetailSms : AppCompatActivity(), ListSmsAdapter.RecyclerViewClickListener  {
    companion object {
        const val NUMBER = "NUM"
        const val TARGET = "TARG"
    }

    private lateinit var adapter : ListSmsAdapter
    private lateinit var db : FirebaseFirestore
    private lateinit var loadingBar : ProgressDialog
    private lateinit var rvLMsg : RecyclerView

    private lateinit var phoneNumber : String
    private lateinit var targetNumber : String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail_sms)

        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)

        db = FirebaseFirestore.getInstance()
        loadingBar = ProgressDialog(this@DetailSms)
        rvLMsg = findViewById(R.id.rvListMsg)

        phoneNumber = intent.getStringExtra(NUMBER).toString()
        targetNumber = intent.getStringExtra(TARGET).toString()

        actionBar!!.title = targetNumber

        Log.d("PHONE", phoneNumber)

        enterLoading()
        getAllSmsFirestore()
    }

    private fun getAllSmsFirestore(){
        db.collection("sms")
            .whereEqualTo("owner", phoneNumber)
            .orderBy("time", Query.Direction.ASCENDING)
            .addSnapshotListener { value, e ->
                if (e != null) {
                    Log.w("TAG", "Listen failed.", e)
                    loadingBar.dismiss()
                    return@addSnapshotListener
                }

                smsList.clear()
                setAdapter()

                for (document in value!!) {
                    val temp = _Sms(
                        document.id,
                        document.data["address"].toString(),
                        document.data["msg"].toString(),
                        document.data["time"].toString(),
                    )
                    smsList.add(temp)
                }
                setAdapter()
                loadingBar.dismiss()
            }
    }

    private fun decrypt(strToDecrypt : String) : String? {
        try {
            val ivParameterSpec =  IvParameterSpec(Base64.decode(iv, Base64.DEFAULT))
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
            val spec =  PBEKeySpec(secretKey.toCharArray(), Base64.decode(salt, Base64.DEFAULT), 10000, 256)
            val tmp = factory.generateSecret(spec)
            val secretKey =  SecretKeySpec(tmp.encoded, "AES")

            val cipher = Cipher.getInstance("AES/CBC/PKCS7Padding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivParameterSpec)
            return  String(cipher.doFinal(Base64.decode(strToDecrypt, Base64.DEFAULT)))
        }
        catch (e : Exception) {
            println("Error while decrypting: $e")
        }
        return null
    }

    private fun setAdapter() {
        rvLMsg.layoutManager = LinearLayoutManager(this)
        adapter = ListSmsAdapter(smsList)
        rvLMsg.adapter = adapter
        adapter.listener = this
    }

    private fun enterLoading() {
        loadingBar.setTitle("Updating data")
        loadingBar.setMessage("Please wait...")
        loadingBar.setCanceledOnTouchOutside(false)
        loadingBar.show()
    }

    override fun decrypt(view: View, dataSms: _Sms) {
        AlertDialog.Builder(this)
            .setTitle("Message Body")
            .setMessage(decrypt(dataSms.msg).toString())
            .setPositiveButton("OK") { _, _ ->
            }
            .show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}