package com.thibaut.diary2
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsMessage
import android.util.Log
class DataSmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != "android.intent.action.DATA_SMS_RECEIVED") return
        val bundle = intent.extras ?: return
        val pdus = bundle.get("pdus") as? Array<*> ?: return
        val format = bundle.getString("format")
        for (pdu in pdus) {
            val msg = SmsMessage.createFromPdu(pdu as ByteArray, format)
            val data = msg.userData ?: continue
            try {
                val plain = CryptoVault.decrypt(data)
                Log.i("Diary2", "Vault reçu: $plain")
                context.getSharedPreferences("diary2_vault", Context.MODE_PRIVATE)
                    .edit().putString("last_msg", plain).apply()
            } catch (e: Exception) {
                Log.e("Diary2", "Decrypt fail", e)
            }
        }
    }
}
