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
        findViewById<android.view.View>(R.id.btnAddContact).setOnClickListener { addContact() }
        findViewById<android.view.View>(R.id.btnEffacer).setOnClickListener { effacerTout() }
        findViewById<android.view.View>(R.id.btnSendEncrypted).setOnClickListener { envoyerChiffre() }

        refreshContacts()
        refreshHistory()
    }

    private fun addContact() {
        val name = etName.text.toString().trim()
        val num = etNumber.text.toString().trim()
        if (name.isEmpty() || num.isEmpty()) { Toast.makeText(this,"Nom + numéro requis",Toast.LENGTH_SHORT).show(); return }
        VaultStorage.addContact(this, name, num)
        etName.text.clear(); etNumber.text.clear()
        refreshContacts()
    }

    private fun refreshContacts() {
        contactsContainer.removeAllViews()
        val contacts = VaultStorage.getContacts(this)
        if (contacts.isEmpty()) {
            val tv = TextView(this).apply {
                text = "papa\n0748107513"; textSize = 16f; setTextColor(0xFFFFFFFF.toInt())
                setBackgroundResource(R.drawable.bg_glass_button); setPadding(24,18,24,18)
                setOnClickListener { etDest.setText("0748107513") }
            }
            contactsContainer.addView(tv)
            return
        }
        contacts.forEach { (name, num) ->
            val card = LayoutInflater.from(this).inflate(R.layout.item_contact, contactsContainer, false)
            card.findViewById<TextView>(R.id.tvContactName).text = name
            card.findViewById<TextView>(R.id.tvContactNumber).text = num
            card.setOnClickListener { etDest.setText(num) }
            contactsContainer.addView(card)
        }
    }

    private fun effacerTout() {
        VaultStorage.clearAll(this)
        historyContainer.removeAllViews()
        contactsContainer.removeAllViews()
        etDest.text.clear(); etMsg.text.clear()
        Toast.makeText(this,"Coffre effacé",Toast.LENGTH_SHORT).show()
        refreshContacts()
    }

    private fun envoyerChiffre() {
        val dest = etDest.text.toString().trim()
        val msg = etMsg.text.toString().trim()
        if (dest.isEmpty() || msg.isEmpty()) { Toast.makeText(this,"Numéro + message requis",Toast.LENGTH_SHORT).show(); return }
        try {
            val encrypted = Base64.encodeToString(msg.toByteArray(), Base64.NO_WRAP)
            SmsManager.getDefault().sendDataMessage(dest, null, 8090.toShort(), encrypted.toByteArray(), null, null)
            VaultStorage.addHistory(this, "-> $dest : $msg")
            refreshHistory()
            etMsg.text.clear()
            Toast.makeText(this,"Envoyé DATA SMS chiffré port 8090",Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this,"Erreur: ${e.message}",Toast.LENGTH_LONG).show()
        }
    }

    private fun refreshHistory() {
        historyContainer.removeAllViews()
        VaultStorage.getHistory(this).forEach { line ->
            val tv = TextView(this).apply { text = line; setTextColor(0xFFAAAAAA.toInt()); textSize = 12f; setPadding(8,6,8,6) }
            historyContainer.addView(tv)
        }
    }
}
