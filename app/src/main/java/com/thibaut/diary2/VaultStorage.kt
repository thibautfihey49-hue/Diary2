package com.thibaut.diary2
import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object VaultStorage {
    private const val PREF_CONTACTS = "vault_contacts_v2"
    private const val PREF_HISTORY = "vault_history_v2"
    private const val KEY_CONTACTS = "contacts"
    private const val KEY_HISTORY = "history"

    fun addContact(ctx: Context, name: String, number: String) {
        val list = getContacts(ctx).toMutableList()
        list.removeAll { it.second == number }
        list.add(0, name to number)
        saveContacts(ctx, list)
    }

    fun getContacts(ctx: Context): List<Pair<String,String>> {
        val prefs = ctx.getSharedPreferences(PREF_CONTACTS, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_CONTACTS, "[]") ?: "[]"
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map {
                val o = arr.getJSONObject(it)
                o.getString("name") to o.getString("number")
            }
        } catch(_:Exception){ emptyList() }
    }

    private fun saveContacts(ctx: Context, list: List<Pair<String,String>>) {
        val arr = JSONArray()
        list.forEach { (n, num) -> arr.put(JSONObject().put("name", n).put("number", num)) }
        ctx.getSharedPreferences(PREF_CONTACTS, Context.MODE_PRIVATE).edit().putString(KEY_CONTACTS, arr.toString()).apply()
    }

    fun clearAll(ctx: Context) {
        ctx.getSharedPreferences(PREF_CONTACTS, Context.MODE_PRIVATE).edit().clear().apply()
        ctx.getSharedPreferences(PREF_HISTORY, Context.MODE_PRIVATE).edit().clear().apply()
        // nettoie aussi les anciens prefs
        ctx.getSharedPreferences("vault", Context.MODE_PRIVATE).edit().clear().apply()
        ctx.getSharedPreferences("vault_history", Context.MODE_PRIVATE).edit().clear().apply()
    }

    fun addHistory(ctx: Context, text: String) {
        val list = getHistory(ctx).toMutableList()
        list.add(0, text)
        if (list.size > 100) list.removeAt(list.size-1)
        saveHistory(ctx, list)
    }

    fun getHistory(ctx: Context): List<String> {
        val prefs = ctx.getSharedPreferences(PREF_HISTORY, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_HISTORY, "[]") ?: "[]"
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { arr.getString(it) }
        } catch(_:Exception){ emptyList() }
    }

    private fun saveHistory(ctx: Context, list: List<String>) {
        val arr = JSONArray()
        list.forEach { arr.put(it) }
        ctx.getSharedPreferences(PREF_HISTORY, Context.MODE_PRIVATE).edit().putString(KEY_HISTORY, arr.toString()).apply()
    }
}
