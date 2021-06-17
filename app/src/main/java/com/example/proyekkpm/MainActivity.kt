package com.example.proyekkpm

import android.Manifest
import android.app.Activity
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Telephony.Sms
import android.telephony.TelephonyManager
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.proyekkpm.adapters.DetailAdapter
import com.example.proyekkpm.model._Sms
import com.example.proyekkpm.services.iv
import com.example.proyekkpm.services.salt
import com.example.proyekkpm.services.secretKey
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

private const val REQUEST_DEFAULT_APP = 123
private const val MY_PERMISSIONS_REQUEST_SMS = 123
private var smsList = arrayListOf<_Sms>()
private var addressList = arrayListOf<String>()

@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity(), DetailAdapter.RecyclerViewClickListener {

    private lateinit var adapter : DetailAdapter
    private lateinit var db : FirebaseFirestore
    private lateinit var sharedPref : SharedPreferences
    private lateinit var loadingBar : ProgressDialog
    private lateinit var rvListSms : RecyclerView
    private lateinit var composeBtn : FloatingActionButton

    private lateinit var phoneNumber : String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val setSmsAppIntent = Intent(Sms.Intents.ACTION_CHANGE_DEFAULT)
        setSmsAppIntent.putExtra(Sms.Intents.EXTRA_PACKAGE_NAME, packageName)
        startActivityForResult(setSmsAppIntent, REQUEST_DEFAULT_APP)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_MMS) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_WAP_PUSH) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_NUMBERS) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.READ_SMS,
                    Manifest.permission.RECEIVE_SMS,
                    Manifest.permission.SEND_SMS,
                    Manifest.permission.RECEIVE_MMS,
                    Manifest.permission.RECEIVE_WAP_PUSH,
                    Manifest.permission.READ_PHONE_STATE),
                MY_PERMISSIONS_REQUEST_SMS)
        }

        sharedPref = getSharedPreferences("user", Context.MODE_PRIVATE)
        val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        phoneNumber = telephonyManager.line1Number

        val editor: SharedPreferences.Editor = sharedPref.edit()
        editor.putString("phone", phoneNumber)
        editor.apply()

        db = FirebaseFirestore.getInstance()
        loadingBar = ProgressDialog(this@MainActivity)
        rvListSms = findViewById(R.id.rvListSms)
        composeBtn = findViewById(R.id.composeBtn)

        enterLoading()
        getAllSmsFirestore()

        composeBtn.setOnClickListener {
            Intent(this@MainActivity, ComposeSmsActivity::class.java).also {
                startActivity(it)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != Activity.RESULT_OK){
            return
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val granted = if(checkPermissionGranted(requestCode, grantResults)) "Permission granted" else "Permission not granted"
        Toast.makeText(this,granted, Toast.LENGTH_SHORT).show()
    }

    private fun checkPermissionGranted(requestCode: Int, grantResults: IntArray): Boolean{
        when (requestCode) {
            MY_PERMISSIONS_REQUEST_SMS -> {
                // If request is cancelled, the result arrays are empty.
                return (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            }
        }
        return false
    }

    private fun getAllSmsFirestore(){
        db.collection("sms")
            .whereEqualTo("owner", phoneNumber)
            //.orderBy("time")
            .addSnapshotListener { value, e ->
                if (e != null) {
                    Log.w("TAG", "Listen failed.", e)
                    loadingBar.dismiss()
                    return@addSnapshotListener
                }

                smsList.clear()
                addressList.clear()
                setAdapter()
                for (document in value!!) {
                    val temp = _Sms(
                        document.id,
                        document.data["address"].toString(),
                        decrypt(document.data["msg"].toString()).toString(),
                        document.data["time"].toString(),
                    )
                    addressList.add(document.data["address"].toString())
                    smsList.add(temp)
                }
                setAdapter()

                val tempList = addressList.distinct()

                addressList.clear()
                for(i in tempList){
                    addressList.add(i)
                }
                Log.d("ADDRESS", addressList.toString())

                loadingBar.dismiss()
            }
    }

    private fun setAdapter() {
        rvListSms.layoutManager = LinearLayoutManager(this)
        adapter = DetailAdapter(addressList)
        rvListSms.adapter = adapter
        adapter.listener = this
    }

    private fun enterLoading() {
        loadingBar.setTitle("Updating data")
        loadingBar.setMessage("Please wait..")
        loadingBar.setCanceledOnTouchOutside(false)
        loadingBar.show()
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

    override fun navigate(view: View, dataSms: String) {
        Intent(this@MainActivity, DetailSms::class.java).also {
            it.putExtra(DetailSms.NUMBER, phoneNumber)
            it.putExtra(DetailSms.TARGET, dataSms)
            startActivity(it)
        }
    }
}