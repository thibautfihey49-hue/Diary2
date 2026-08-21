package com.thibaut.diary2
import android.content.Context
import android.content.SharedPreferences

object ThemeManager {
    private const val PREF = "huntrx_theme_v3"
    private fun prefs(c: Context): SharedPreferences = c.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    var accentColor: Int = 0xFFFFD700.toInt()
    var wallpaper: String = "obsidian"
    var theme: String = "neon"

    fun save(c: Context, theme: String, accent: Int, wallpaper: String) {
        prefs(c).edit().putString("theme", theme).putInt("accent", accent).putString("wall", wallpaper).apply()
        this.theme = theme; this.accentColor = accent; this.wallpaper = wallpaper
    }
    fun load(c: Context) {
        theme = prefs(c).getString("theme","neon")!!
        accentColor = prefs(c).getInt("accent", 0xFFFFD700.toInt())
        wallpaper = prefs(c).getString("wall","obsidian")!!
    }
    fun wallpaperRes(): Int = when(wallpaper) {
        "nebula" -> android.R.color.transparent
        "gold" -> android.R.color.transparent
        "aurora" -> android.R.color.transparent
        else -> android.R.color.transparent
    }
}
