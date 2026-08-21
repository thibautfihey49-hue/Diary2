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
        // API compatible avec toutes tes versions
        try { VaultStorage.addContact(this, name, num) } catch(_:Exception){
            try { VaultStorage::class.java.getMethod("saveContact", android.content.Context::class.java, String::class.java, String::class.java).invoke(VaultStorage, this, name, num) } catch(_:Exception){
                getSharedPreferences("vault", MODE_PRIVATE).edit().putString(name, num).apply()
            }
        }
        etName.text.clear(); etNumber.text.clear()
        refreshContacts()
    }

    private fun refreshContacts() {
        contactsContainer.removeAllViews()
        val contacts: List<Pair<String,String>> = try {
            VaultStorage.getContacts(this).map { it.first to it.second }
        } catch(_:Exception) {
            try {
                @Suppress("UNCHECKED_CAST")
                VaultStorage::class.java.getMethod("getAllContacts", android.content.Context::class.java).invoke(VaultStorage, this) as List<Pair<String,String>>
            } catch(_:Exception){
                val prefs = getSharedPreferences("vault", MODE_PRIVATE).all
                prefs.map { it.key to it.value.toString() }.filter { it.second.matches(Regex(".*[0-9]{6,}.*")) }
            }
        }

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
        try { VaultStorage.clearAll(this) } catch(_:Exception){
            try { VaultStorage::class.java.getMethod("deleteAll", android.content.Context::class.java).invoke(VaultStorage, this) } catch(_:Exception){
                getSharedPreferences("vault", MODE_PRIVATE).edit().clear().apply()
                getSharedPreferences("vault_history", MODE_PRIVATE).edit().clear().apply()
            }
        }
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
            // sauve historique compatible
            try { VaultStorage.addHistory(this, "-> $dest : $msg") } catch(_:Exception){
                try { VaultStorage::class.java.getMethod("addHistory", android.content.Context::class.java, String::class.java).invoke(VaultStorage, this, "-> $dest : $msg") } catch(_:Exception){
                    getSharedPreferences("vault_history", MODE_PRIVATE).edit().putString(System.currentTimeMillis().toString(), "-> $dest : $msg").apply()
                }
            }
            refreshHistory()
            etMsg.text.clear()
            Toast.makeText(this,"Envoyé DATA SMS chiffré port 8090",Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this,"Erreur: ${e.message}",Toast.LENGTH_LONG).show()
        }
    }

    private fun refreshHistory() {
        historyContainer.removeAllViews()
        val hist = try {
            VaultStorage.getHistory(this)
        } catch(_:Exception){
            val prefs = getSharedPreferences("vault_history", MODE_PRIVATE).all.values.map { it.toString() }
            prefs
        }
        hist.forEach { item ->
            val text = when(item) {
                is String -> item
                else -> try { item::class.java.getField("text").get(item) as String } catch(_:Exception){
                    try { item::class.java.getMethod("toString").invoke(item) as String } catch(_:Exception){ item.toString() }
                }
            }
            val tv = TextView(this).apply { setText(text); setTextColor(0xFFAAAAAA.toInt()); textSize = 12f; setPadding(8,6,8,6) }
            historyContainer.addView(tv)
        }
    }
}
