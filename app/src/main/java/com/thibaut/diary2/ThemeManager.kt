package com.thibaut.diary2
import android.content.Context
import android.graphics.Color

object ThemeManager {
    private const val PREF = "huntrx_m3_full"
    fun prefs(c: Context) = c.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    data class FullTheme(
        var theme: String = "neon",
        var accent: Int = Color.parseColor("#FFD700"),
        var secondary: Int = Color.parseColor("#9C27B0"),
        var tertiary: Int = Color.parseColor("#FF69B4"),
        var background: Int = Color.parseColor("#0A0A0F"),
        var surface: Int = Color.parseColor("#15151F"),
        var wallpaper: String = "obsidian"
    )

    var current = FullTheme()

    fun load(c: Context) {
        val p = prefs(c)
        current = FullTheme(
            p.getString("theme","neon")!!,
            p.getInt("accent", Color.parseColor("#FFD700")),
            p.getInt("secondary", Color.parseColor("#9C27B0")),
            p.getInt("tertiary", Color.parseColor("#FF69B4")),
            p.getInt("bg", Color.parseColor("#0A0A0F")),
            p.getInt("surface", Color.parseColor("#15151F")),
            p.getString("wall","obsidian")!!
        )
    }

    fun save(c: Context) {
        prefs(c).edit()
           .putString("theme", current.theme)
           .putInt("accent", current.accent)
           .putInt("secondary", current.secondary)
           .putInt("tertiary", current.tertiary)
           .putInt("bg", current.background)
           .putInt("surface", current.surface)
           .putString("wall", current.wallpaper)
           .apply()
    }
}
