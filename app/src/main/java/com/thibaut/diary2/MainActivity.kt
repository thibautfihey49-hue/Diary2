package com.thibaut.diary2
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
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
    private var currentRecordingFile: File? = null
    private var isRecording = false
    private var player: MediaPlayer? = null

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { currentImages.add(it); addImageBlock(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager.load(this)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyTheme()

        binding.timeline.layoutManager = LinearLayoutManager(this)
        binding.timeline.adapter = DiaryAdapter(entries) { file -> playVoice(file) }

        binding.logoMoon.setOnClickListener { startActivity(Intent(this, VaultActivity::class.java)) }
        binding.btnTheme.setOnClickListener {
            binding.themePanel.visibility = if (binding.themePanel.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }

        // Thème
        binding.thObsidian.setOnClickListener { saveTheme("obsidian") }
        binding.thNeon.setOnClickListener { saveTheme("neon") }
        binding.thFrost.setOnClickListener { saveTheme("frost") }
        binding.cGold.setOnClickListener { saveAccent(Color.parseColor("#FFD700")) }
        binding.cViolet.setOnClickListener { saveAccent(Color.parseColor("#9C27B0")) }
        binding.cRose.setOnClickListener { saveAccent(Color.parseColor("#FF69B4")) }
        binding.cCyan.setOnClickListener { saveAccent(Color.parseColor("#00E5FF")) }
        binding.cGreen.setOnClickListener { saveAccent(Color.parseColor("#00FF88")) }
        binding.wObsidian.setOnClickListener { saveWall("obsidian") }
        binding.wNebula.setOnClickListener { saveWall("nebula") }
        binding.wGold.setOnClickListener { saveWall("gold") }
        binding.wAurora.setOnClickListener { saveWall("aurora") }

        binding.btnImage.setOnClickListener { pickImage.launch("image/*") }
        binding.btnVoice.setOnClickListener { if (!isRecording) startVoiceBlock() else stopVoiceBlock() }
        binding.btnSave.setOnClickListener {
            val text = binding.entryText.text.toString()
            if (text.isBlank() && currentImages.isEmpty() && currentVoices.isEmpty()) return@setOnClickListener
            val entry = DiaryEntry(text, currentImages.toList(), currentVoices.toList(), "Aujourd'hui ${java.text.SimpleDateFormat("HH:mm").format(java.util.Date())}")
            entries.add(0, entry)
            binding.timeline.adapter?.notifyItemInserted(0)
            binding.entryText.text.clear()
            binding.blocksContainer.removeAllViews()
            binding.tvRecordingStatus.text = ""
            currentImages.clear(); currentVoices.clear()
        }
        checkPerms()
    }

    private fun saveTheme(t: String) { ThemeManager.save(this, t, ThemeManager.accentColor, ThemeManager.wallpaper); applyTheme() }
    private fun saveAccent(c: Int) { ThemeManager.save(this, ThemeManager.theme, c, ThemeManager.wallpaper); applyTheme() }
    private fun saveWall(w: String) { ThemeManager.save(this, ThemeManager.theme, ThemeManager.accentColor, w); applyTheme() }

    private fun applyTheme() {
        val wallRes = when(ThemeManager.wallpaper) {
            "nebula" -> R.drawable.bg_wall_nebula
            "gold" -> R.drawable.bg_wall_gold
            "aurora" -> R.drawable.bg_wall_aurora
            else -> R.drawable.bg_wall_obsidian
        }
        binding.rootMain.setBackgroundResource(wallRes)
        binding.btnSave.backgroundTintList = android.content.res.ColorStateList.valueOf(ThemeManager.accentColor)
    }

    private fun addImageBlock(uri: Uri) {
        val view = ImageView(this).apply {
            setImageURI(uri)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 420).apply { topMargin = 14 }
            scaleType = ImageView.ScaleType.CENTER_CROP
            elevation = 6f
        }
        binding.blocksContainer.addView(view)
    }

    private fun startVoiceBlock() {
        try {
            currentRecordingFile = File(cacheDir, "voice_${System.currentTimeMillis()}.m4a")
            recorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(currentRecordingFile!!.absolutePath)
                prepare(); start()
            }
            isRecording = true
            binding.btnVoice.text = "⏹"
            binding.tvRecordingStatus.text = "● Enregistrement..."
        } catch (e: Exception) { Toast.makeText(this, "Erreur micro: ${e.message}", Toast.LENGTH_LONG).show() }
    }

    private fun stopVoiceBlock() {
        try {
            recorder?.stop(); recorder?.release(); recorder = null; isRecording = false
            binding.btnVoice.text = "🎤"; binding.tvRecordingStatus.text = "✓ Voix enregistrée"
            currentRecordingFile?.let { file ->
                currentVoices.add(file)
                val block = LayoutInflater.from(this).inflate(R.layout.item_voice_block, binding.blocksContainer, false)
                block.findViewById<TextView>(R.id.tvDuration).text = file.name
                block.findViewById<TextView>(R.id.btnPlay).setOnClickListener { playVoice(file) }
                binding.blocksContainer.addView(block)
            }
            currentRecordingFile = null
        } catch (e: Exception) { binding.btnVoice.text = "🎤"; isRecording = false }
    }

    private fun playVoice(file: File) {
        try {
            player?.release()
            player = MediaPlayer().apply { setDataSource(file.absolutePath); prepare(); start() }
            Toast.makeText(this, "Lecture ${file.name}", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) { Toast.makeText(this, "Impossible lire", Toast.LENGTH_SHORT).show() }
    }

    private fun checkPerms() {
        val perms = arrayOf(Manifest.permission.SEND_SMS, Manifest.permission.RECEIVE_SMS, Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_MEDIA_IMAGES)
        if (perms.any { ActivityCompat.checkSelfPermission(this, it)!= PackageManager.PERMISSION_GRANTED }) {
            ActivityCompat.requestPermissions(this, perms, 1001)
        }
    }
    override fun onDestroy() { super.onDestroy(); recorder?.release(); player?.release() }
}

class DiaryAdapter(private val list: List<DiaryEntry>, private val onPlay: (File)->Unit) : androidx.recyclerview.widget.RecyclerView.Adapter<DiaryAdapter.Holder>() {
    class Holder(v: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(v) {
        val tvDate: TextView = v.findViewById(R.id.tvDate)
        val tvText: TextView = v.findViewById(R.id.tvText)
        val images: LinearLayout = v.findViewById(R.id.entryImages)
        val voices: LinearLayout = v.findViewById(R.id.entryVoices)
    }
    override fun onCreateViewHolder(p: android.view.ViewGroup, t: Int) = Holder(LayoutInflater.from(p.context).inflate(R.layout.item_entry, p, false))
    override fun getItemCount() = list.size
    override fun onBindViewHolder(h: Holder, pos: Int) {
        val e = list[pos]
        h.tvDate.text = e.date; h.tvText.text = e.text
        h.images.removeAllViews()
        e.images.forEach { uri ->
            val iv = ImageView(h.itemView.context).apply { setImageURI(uri); layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 320).apply{topMargin=10}; scaleType = ImageView.ScaleType.CENTER_CROP; elevation = 4f }
            h.images.addView(iv)
        }
        h.voices.removeAllViews()
        e.voices.forEach { file ->
            val block = LayoutInflater.from(h.itemView.context).inflate(R.layout.item_voice_block, h.voices, false)
            block.findViewById<TextView>(R.id.tvDuration).text = file.name
            block.findViewById<TextView>(R.id.btnPlay).setOnClickListener { onPlay(file) }
            h.voices.addView(block)
        }
    }
}
