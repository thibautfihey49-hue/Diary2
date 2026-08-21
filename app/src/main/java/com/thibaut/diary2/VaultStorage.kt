package com.thibaut.diary2
import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class VaultContact(val name: String, val number: String)
data class VaultMessage(val number: String, val text: String, val isSent: Boolean, val time: Long)

object VaultStorage {
    private const val PREF = "vault_storage"
    fun saveContact(c: Context, contact: VaultContact) {
        val prefs = c.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val arr = JSONArray(prefs.getString("contacts", "[]"))
        arr.put(JSONObject().put("name", contact.name).put("number", contact.number))
        prefs.edit().putString("contacts", arr.toString()).apply()
    }
    fun getContacts(c: Context): List<VaultContact> {
        val prefs = c.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val arr = JSONArray(prefs.getString("contacts", "[]"))
        return (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            VaultContact(o.getString("name"), o.getString("number"))
        }
    }
    fun saveMessage(c: Context, msg: VaultMessage) {
        val prefs = c.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val arr = JSONArray(prefs.getString("history", "[]"))
        arr.put(JSONObject().put("number", msg.number).put("text", msg.text).put("isSent", msg.isSent).put("time", msg.time))
        prefs.edit().putString("history", arr.toString()).apply()
    }
    fun getHistory(c: Context): List<VaultMessage> {
        val prefs = c.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val arr = JSONArray(prefs.getString("history", "[]"))
        return (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            VaultMessage(o.getString("number"), o.getString("text"), o.getBoolean("isSent"), o.getLong("time"))
        }.reversed()
    }
    fun clearHistory(c: Context) {
        c.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().remove("history").apply()
    }
}
