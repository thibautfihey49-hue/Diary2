package com.thibaut.diary2
import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import java.io.File
import com.thibaut.diary2.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        checkPerms()
        binding.btnSave.setOnClickListener {
            val text = binding.entryText.text.toString()
            if (text.isBlank()) return@setOnClickListener
            saveEncrypted(text)
            Toast.makeText(this, "Entrée chiffrée sauvée", Toast.LENGTH_SHORT).show()
        }
        binding.logoMoon.setOnLongClickListener {
            Toast.makeText(this, "Coffre privé adulte déverrouillé 18+", Toast.LENGTH_SHORT).show()
            true
        }
        binding.btnSendVault.setOnClickListener {
            val dest = binding.vaultDest.text.toString()
            val msg = binding.vaultMsg.text.toString()
            if (dest.isBlank() || msg.isBlank()) {
                Toast.makeText(this, "Numéro + message requis", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val ok = DataSmsSender.sendVaultMessage(dest, msg)
            binding.logView.text = if (ok) "Envoyé port 8090 chiffré" else "Échec"
        }
    }
    private fun saveEncrypted(text: String) {
        try {
            val masterKey = MasterKey.Builder(this).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
            val file = File(filesDir, "diary_${System.currentTimeMillis()}.enc")
            val encFile = EncryptedFile.Builder(this, file, masterKey, EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB).build()
            encFile.openFileOutput().use { it.write(text.toByteArray()) }
        } catch (e: Exception) { e.printStackTrace() }
    }
    private fun checkPerms() {
        val perms = arrayOf(Manifest.permission.SEND_SMS, Manifest.permission.RECEIVE_SMS, Manifest.permission.RECORD_AUDIO)
        if (perms.any { ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }) {
            ActivityCompat.requestPermissions(this, perms, 1001)
        }
    }
}
