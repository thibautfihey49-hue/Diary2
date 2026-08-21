package com.thibaut.diary2
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import android.telephony.SmsManager
import android.util.Base64
import android.view.LayoutInflater

class VaultActivity : AppCompatActivity() {
    private lateinit var etName: EditText
    private lateinit var etNumber: EditText
    private lateinit var etDest: EditText
    private lateinit var etMsg: EditText
    private lateinit var contactsContainer: LinearLayout
    private lateinit var historyContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vault)
        etName = findViewById(R.id.etContactName)
        etNumber = findViewById(R.id.etContactNumber)
        etDest = findViewById(R.id.etDestNumber)
        etMsg = findViewById(R.id.etMessage)
        contactsContainer = findViewById(R.id.contactsContainer)
        historyContainer = findViewById(R.id.historyContainer)

        findViewById<android.view.View>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<android.view.View>(R.id.btnAddContact).setOnClickListener {
            val n = etName.text.toString().trim()
            val num = etNumber.text.toString().trim()
            if(n.isEmpty()||num.isEmpty()) return@setOnClickListener
            VaultStorage.addContact(this, n, num)
            etName.text.clear(); etNumber.text.clear()
            refreshContacts()
        }
        findViewById<android.view.View>(R.id.btnEffacer).setOnClickListener {
            VaultStorage.clearAll(this)
            contactsContainer.removeAllViews(); historyContainer.removeAllViews()
            etDest.text.clear(); etMsg.text.clear()
            refreshContacts()
            Toast.makeText(this,"Coffre effacé",Toast.LENGTH_SHORT).show()
        }
        findViewById<android.view.View>(R.id.btnSendEncrypted).setOnClickListener {
            val dest = etDest.text.toString().trim()
            val msg = etMsg.text.toString().trim()
            if(dest.isEmpty()||msg.isEmpty()) return@setOnClickListener
            try {
                val enc = Base64.encodeToString(msg.toByteArray(), Base64.NO_WRAP)
                SmsManager.getDefault().sendDataMessage(dest, null, 8090.toShort(), enc.toByteArray(), null, null)
                VaultStorage.addHistory(this, "-> $dest : $msg")
                refreshHistory()
                etMsg.text.clear()
                Toast.makeText(this,"Envoyé DATA SMS chiffré",Toast.LENGTH_LONG).show()
            } catch(e:Exception){ Toast.makeText(this,"Erreur ${e.message}",Toast.LENGTH_LONG).show() }
        }
        refreshContacts(); refreshHistory()
    }

    private fun refreshContacts(){
        contactsContainer.removeAllViews()
        val list = VaultStorage.getContacts(this)
        if(list.isEmpty()){
            val tv = TextView(this).apply {
                text="papa\n0748107513"; textSize=16f; setTextColor(-1)
                setBackgroundResource(R.drawable.bg_glass_button); setPadding(24,18,24,18)
                setOnClickListener{ etDest.setText("0748107513") }
            }
            contactsContainer.addView(tv); return
        }
        list.forEach { (name,num) ->
            val card = LayoutInflater.from(this).inflate(R.layout.item_contact, contactsContainer, false)
            card.findViewById<TextView>(R.id.tvContactName).text = name
            card.findViewById<TextView>(R.id.tvContactNumber).text = num
            card.setOnClickListener{ etDest.setText(num) }
            contactsContainer.addView(card)
        }
    }

    private fun refreshHistory(){
        historyContainer.removeAllViews()
        VaultStorage.getHistory(this).forEach { line ->
            val tv = TextView(this).apply { text=line; setTextColor(0xFFAAAAAA.toInt()); textSize=12f; setPadding(8,6,8,6) }
            historyContainer.addView(tv)
        }
    }
}
