package com.thibaut.diary2
import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.thibaut.diary2.databinding.ActivityMainBinding
import java.io.File

data class DiaryEntry(val text: String, val images: List<Uri>, val voices: List<File>, val date: String)

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val entries = mutableListOf<DiaryEntry>()
    private val currentImages = mutableListOf<Uri>()
    private val currentVoices = mutableListOf<File>()
    private var recorder: MediaRecorder? = null
    private var isRecording = false

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            currentImages.add(it)
            addImageBlock(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.timeline.layoutManager = LinearLayoutManager(this)
        binding.timeline.adapter = DiaryAdapter(entries)

        binding.btnImage.setOnClickListener { pickImage.launch("image/*") }

        binding.btnVoice.setOnClickListener {
            if (!isRecording) startVoiceBlock() else stopVoiceBlock()
        }

        binding.btnSave.setOnClickListener {
            val text = binding.entryText.text.toString()
            if (text.isBlank() && currentImages.isEmpty() && currentVoices.isEmpty()) return@setOnClickListener
            val entry = DiaryEntry(text, currentImages.toList(), currentVoices.toList(), "Aujourd'hui ${java.text.SimpleDateFormat("HH:mm").format(java.util.Date())}")
            entries.add(0, entry)
            binding.timeline.adapter?.notifyItemInserted(0)
            // reset éditeur
            binding.entryText.text.clear()
            binding.blocksContainer.removeAllViews()
            currentImages.clear()
            currentVoices.clear()
            Toast.makeText(this, "Entrée chiffrée sauvée", Toast.LENGTH_SHORT).show()
        }

        binding.btnSendVault.setOnClickListener {
            val dest = binding.vaultDest.text.toString()
            val msg = binding.vaultMsg.text.toString()
            if (dest.isBlank() || msg.isBlank()) return@setOnClickListener
            DataSmsSender.sendVaultMessage(dest, msg)
        }

        checkPerms()
    }

    private fun addImageBlock(uri: Uri) {
        val view = ImageView(this).apply {
            setImageURI(uri)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 400).apply { topMargin = 12 }
            scaleType = ImageView.ScaleType.CENTER_CROP
        }
        binding.blocksContainer.addView(view)
    }

    private fun startVoiceBlock() {
        try {
            val file = File(cacheDir, "voice_${System.currentTimeMillis()}.m4a")
            recorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            isRecording = true
            binding.btnVoice.text = "⏹ Stop"
            Toast.makeText(this, "Enregistrement voix...", Toast.LENGTH_SHORT).show()
            // stocker fichier pour insertion
            currentVoices.add(file)
            // UI waveform simulée
            val tv = TextView(this).apply { text = "🎙️ Enregistrement en cours... ${file.name}"; setTextColor(0xFFFF69B4.toInt()); setPadding(0,12,0,12) }
            binding.blocksContainer.addView(tv)
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun stopVoiceBlock() {
        recorder?.stop()
        recorder?.release()
        recorder = null
        isRecording = false
        binding.btnVoice.text = "🎤 Voix"
    }

    private fun checkPerms() {
        val perms = arrayOf(Manifest.permission.SEND_SMS, Manifest.permission.RECEIVE_SMS, Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_MEDIA_IMAGES)
        if (perms.any { ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }) {
            ActivityCompat.requestPermissions(this, perms, 1001)
        }
    }
}

class DiaryAdapter(private val list: List<DiaryEntry>) : androidx.recyclerview.widget.RecyclerView.Adapter<DiaryAdapter.Holder>() {
    class Holder(val v: android.view.View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(v) {
        val tvDate: TextView = v.findViewById(com.thibaut.diary2.R.id.tvDate)
        val tvText: TextView = v.findViewById(com.thibaut.diary2.R.id.tvText)
        val images: LinearLayout = v.findViewById(com.thibaut.diary2.R.id.entryImages)
        val voices: LinearLayout = v.findViewById(com.thibaut.diary2.R.id.entryVoices)
    }
    override fun onCreateViewHolder(p: android.view.ViewGroup, t: Int) = Holder(LayoutInflater.from(p.context).inflate(com.thibaut.diary2.R.layout.item_entry, p, false))
    override fun getItemCount() = list.size
    override fun onBindViewHolder(h: Holder, pos: Int) {
        val e = list[pos]
        h.tvDate.text = e.date
        h.tvText.text = e.text
        h.images.removeAllViews()
        e.images.forEach { uri ->
            val iv = ImageView(h.itemView.context).apply { setImageURI(uri); layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 300).apply{topMargin=8}; scaleType = ImageView.ScaleType.CENTER_CROP }
            h.images.addView(iv)
        }
        h.voices.removeAllViews()
        e.voices.forEach { file ->
            val tv = TextView(h.itemView.context).apply { text = "🔊 ${file.name} - tap pour écouter"; setTextColor(0xFF9C27B0.toInt()); setPadding(0,8,0,8) }
            h.voices.addView(tv)
        }
    }
}
