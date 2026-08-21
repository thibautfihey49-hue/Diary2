package com.thibaut.diary2
import android.content.Context
import android.graphics.Color
import android.net.Uri

object ThemeManager {
    private const val PREF = "huntrx_m3_full"
    fun prefs(c: Context) = c.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    data class FullTheme(
        var accent: Int = Color.parseColor("#FFD700"),
        var secondary: Int = Color.parseColor("#9C27B0"),
        var background: Int = Color.parseColor("#0A0A0F"),
        var surface: Int = Color.parseColor("#15151F"),
        var wallpaper: String = "obsidian",
        var customWallpaperUri: String? = null
    )

    var current = FullTheme()

    fun load(c: Context) {
        val p = prefs(c)
        current = FullTheme(
            p.getInt("accent", Color.parseColor("#FFD700")),
            p.getInt("secondary", Color.parseColor("#9C27B0")),
            p.getInt("bg", Color.parseColor("#0A0A0F")),
            p.getInt("surface", Color.parseColor("#15151F")),
            p.getString("wall","obsidian")!!,
            p.getString("customUri", null)
        )
    }

    fun save(c: Context) {
        prefs(c).edit()
          .putInt("accent", current.accent)
          .putInt("secondary", current.secondary)
          .putInt("bg", current.background)
          .putInt("surface", current.surface)
          .putString("wall", current.wallpaper)
          .putString("customUri", current.customWallpaperUri)
          .apply()
    }
}
