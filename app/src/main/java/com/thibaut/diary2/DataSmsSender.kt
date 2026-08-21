package com.thibaut.diary2
import android.telephony.SmsManager
import android.util.Log
object DataSmsSender {
    const val PORT: Short = 8090
    const val MAX_BYTES = 133
    fun sendVaultMessage(destination: String, plainText: String): Boolean {
        return try {
            val encrypted = CryptoVault.encrypt(plainText)
            if (encrypted.size > MAX_BYTES) {
                Log.e("Diary2", "Trop long ${encrypted.size}")
                return false
            }
            SmsManager.getDefault().sendDataMessage(destination, null, PORT, encrypted, null, null)
            true
        } catch (e: Exception) {
            Log.e("Diary2", "Erreur envoi", e)
            false
        }
    }
}
