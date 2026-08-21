package com.thibaut.diary2
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup

class VaultActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vault)

        val rvContacts = findViewById<RecyclerView>(R.id.rvContacts)
        val rvHistory = findViewById<RecyclerView>(R.id.rvHistory)
        rvContacts.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvHistory.layoutManager = LinearLayoutManager(this)

        fun refreshContacts() {
            val contacts = VaultStorage.getContacts(this)
            rvContacts.adapter = ContactAdapter(contacts) { number ->
                findViewById<EditText>(R.id.vaultDest).setText(number)
            }
        }
        fun refreshHistory() {
            val history = VaultStorage.getHistory(this)
            rvHistory.adapter = HistoryAdapter(history)
        }
        refreshContacts()
        refreshHistory()

        findViewById<ViewGroup>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnSaveContact).setOnClickListener {
            val name = findViewById<EditText>(R.id.etContactName).text.toString()
            val number = findViewById<EditText>(R.id.etContactNumber).text.toString()
            if (name.isNotBlank() && number.isNotBlank()) {
                VaultStorage.saveContact(this, VaultContact(name, number))
                refreshContacts()
                Toast.makeText(this, "Contact $name enregistré", Toast.LENGTH_SHORT).show()
            }
        }
        findViewById<Button>(R.id.btnSendVault).setOnClickListener {
            val dest = findViewById<EditText>(R.id.vaultDest).text.toString()
            val msg = findViewById<EditText>(R.id.vaultMsg).text.toString()
            if (dest.isBlank() || msg.isBlank()) return@setOnClickListener
            val ok = DataSmsSender.sendVaultMessage(dest, msg)
            if (ok) {
                VaultStorage.saveMessage(this, VaultMessage(dest, msg, true, System.currentTimeMillis()))
                refreshHistory()
                findViewById<EditText>(R.id.vaultMsg).text.clear()
                Toast.makeText(this, "Envoyé chiffré port 8090", Toast.LENGTH_SHORT).show()
            }
        }
        findViewById<TextView>(R.id.btnClearHistory).setOnClickListener {
            VaultStorage.clearHistory(this)
            refreshHistory()
        }
    }
}

class ContactAdapter(private val list: List<VaultContact>, private val onClick: (String)->Unit) : RecyclerView.Adapter<ContactAdapter.H>() {
    class H(v: android.view.View) : RecyclerView.ViewHolder(v) {
        val tv: TextView = v.findViewById(android.R.id.text1)
    }
    override fun onCreateViewHolder(p: ViewGroup, t: Int) = H(LayoutInflater.from(p.context).inflate(android.R.layout.simple_list_item_1, p, false).apply {
        setBackgroundColor(0xFF1A1A24.toInt()); setPadding(24,16,24,16)
    })
    override fun getItemCount() = list.size
    override fun onBindViewHolder(h: H, pos: Int) {
        h.tv.text = "${list[pos].name}\n${list[pos].number}"
        h.tv.setTextColor(0xFFFFFFFF.toInt())
        h.itemView.setOnClickListener { onClick(list[pos].number) }
    }
}

class HistoryAdapter(private val list: List<VaultMessage>) : RecyclerView.Adapter<HistoryAdapter.H>() {
    class H(v: android.view.View) : RecyclerView.ViewHolder(v) {
        val tv: TextView = v.findViewById(android.R.id.text1)
    }
    override fun onCreateViewHolder(p: ViewGroup, t: Int) = H(LayoutInflater.from(p.context).inflate(android.R.layout.simple_list_item_1, p, false))
    override fun getItemCount() = list.size
    override fun onBindViewHolder(h: H, pos: Int) {
        val m = list[pos]
        h.tv.text = "${if(m.isSent) "→" else "←"} ${m.number}: ${m.text}"
        h.tv.setTextColor(0xFFAAAAAA.toInt())
    }
}
