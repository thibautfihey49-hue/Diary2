package com.thibaut.diary2
import android.Manifest
import android.content.pm.PackageManager
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
import java.text.SimpleDateFormat
import java.util.*
import android.graphics.Color

data class DiaryEntry(val text: String, val images: List<Uri>, val voices: List<File>, val date: String, val fullDate: String)

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
    private val pickWallpaper = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            try { contentResolver.takePersistableUriPermission(it, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch(_:Exception){}
            ThemeManager.current.customWallpaperUri = it.toString()
            ThemeManager.current.wallpaper = "custom"
            ThemeManager.save(this); applyFullTheme()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeManager.load(this)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        updateAutoDate()
        applyFullTheme()
        binding.timeline.layoutManager = LinearLayoutManager(this)
        binding.timeline.adapter = DiaryAdapter(entries, { file -> playVoice(file) }, { pos ->
            entries.removeAt(pos); binding.timeline.adapter?.notifyItemRemoved(pos)
            Toast.makeText(this,"Page arrachée",Toast.LENGTH_SHORT).show()
        })
        binding.logoMoon.setOnClickListener { startActivity(android.content.Intent(this, VaultActivity::class.java)) }
        binding.btnWallpaper.setOnClickListener { pickWallpaper.launch("image/*") }
        binding.btnTheme.setOnClickListener { binding.themePanel.visibility = if (binding.themePanel.visibility == View.VISIBLE) View.GONE else View.VISIBLE }
        binding.cGold.setOnClickListener { setGlobalColor(Color.parseColor("#FFD700")) }
        binding.cViolet.setOnClickListener { setGlobalColor(Color.parseColor("#9C27B0")) }
        binding.cRose.setOnClickListener { setGlobalColor(Color.parseColor("#FF69B4")) }
        binding.cCyan.setOnClickListener { setGlobalColor(Color.parseColor("#00E5FF")) }
        binding.cGreen.setOnClickListener { setGlobalColor(Color.parseColor("#00FF88")) }
        binding.cOrange.setOnClickListener { setGlobalColor(Color.parseColor("#FF6B35")) }
        binding.btnImage.setOnClickListener { pickImage.launch("image/*") }
        binding.btnVoice.setOnClickListener { if (!isRecording) startVoiceBlock() else stopVoiceBlock() }
        binding.btnSave.setOnClickListener {
            val text = binding.entryText.text.toString()
            if (text.isBlank() && currentImages.isEmpty() && currentVoices.isEmpty()) return@setOnClickListener
            val now = Date()
            val full = SimpleDateFormat("EEEE dd MMMM yyyy — HH:mm", Locale.FRANCE).format(now)
            val short = SimpleDateFormat("dd/MM HH:mm", Locale.FRANCE).format(now)
            val textWithDate = if(text.contains("—")) text else "$full\n\n$text"
            entries.add(0, DiaryEntry(textWithDate, currentImages.toList(), currentVoices.toList(), short, full))
            binding.timeline.adapter?.notifyItemInserted(0)
            binding.entryText.text.clear(); binding.blocksContainer.removeAllViews(); binding.tvRecordingStatus.text = ""
            currentImages.clear(); currentVoices.clear()
            updateAutoDate()
        }
        checkPerms()
    }

    private fun updateAutoDate() {
        val now = Date()
        val fmt = SimpleDateFormat("EEEE dd MMMM yyyy — HH:mm", Locale.FRANCE)
        binding.tvAutoDate.text = fmt.format(now).replaceFirstChar { it.uppercase() }
    }
    private fun setGlobalColor(color: Int) { ThemeManager.current.accent = color; ThemeManager.save(this); applyFullTheme() }
    private fun applyFullTheme() {
        val accent = ThemeManager.current.accent
        try {
            if (ThemeManager.current.customWallpaperUri!= null) {
                binding.ivCustomWallpaper.setImageURI(Uri.parse(ThemeManager.current.customWallpaperUri))
                binding.ivCustomWallpaper.visibility = View.VISIBLE
            } else binding.ivCustomWallpaper.visibility = View.GONE
        } catch(_:Exception){ binding.ivCustomWallpaper.visibility = View.GONE }
        try {
            (binding.btnSave.parent as? androidx.cardview.widget.CardView)?.setCardBackgroundColor(accent)
            binding.tvAccentDot.setBackgroundColor(accent)
            binding.mainCard.setCardBackgroundColor(Color.WHITE)
            (binding.mainCard as? androidx.cardview.widget.CardView)?.strokeColor = accent
        } catch(_:Exception){}
    }
    private fun addImageBlock(uri: Uri) {
        val view = ImageView(this).apply {
            setImageURI(uri); layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 420).apply { topMargin = 14 }
            scaleType = ImageView.ScaleType.CENTER_CROP; elevation = 6f
        }
        binding.blocksContainer.addView(view)
    }
    private fun startVoiceBlock() {
        try {
            currentRecordingFile = File(cacheDir, "voice_${System.currentTimeMillis()}.m4a")
            recorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC); setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC); setOutputFile(currentRecordingFile!!.absolutePath); prepare(); start()
            }
            isRecording = true; binding.tvRecordingStatus.text = "● Enregistrement..."
        } catch (e: Exception) { Toast.makeText(this, "Erreur micro: ${e.message}", Toast.LENGTH_LONG).show() }
    }
    private fun stopVoiceBlock() {
        try {
            recorder?.stop(); recorder?.release(); recorder = null; isRecording = false
            binding.tvRecordingStatus.text = "✓ Voix enregistrée"
            currentRecordingFile?.let { file ->
                currentVoices.add(file)
                val block = LayoutInflater.from(this).inflate(R.layout.item_voice_block, binding.blocksContainer, false)
                block.findViewById<TextView>(R.id.tvDuration).text = file.name
                block.findViewById<TextView>(R.id.btnPlay).setOnClickListener { playVoice(file) }
                binding.blocksContainer.addView(block)
            }
            currentRecordingFile = null
        } catch (e: Exception) { isRecording = false }
    }
    private fun playVoice(file: File) {
        try { player?.release(); player = MediaPlayer().apply { setDataSource(file.absolutePath); prepare(); start() } }
        catch (e: Exception) { Toast.makeText(this, "Impossible lire", Toast.LENGTH_SHORT).show() }
    }
    private fun checkPerms() {
        val perms = arrayOf(Manifest.permission.SEND_SMS, Manifest.permission.RECEIVE_SMS, Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_MEDIA_IMAGES)
        if (perms.any { ActivityCompat.checkSelfPermission(this, it)!= PackageManager.PERMISSION_GRANTED }) ActivityCompat.requestPermissions(this, perms, 1001)
    }
    override fun onDestroy() { super.onDestroy(); recorder?.release(); player?.release() }
}

class DiaryAdapter(private val list: List<DiaryEntry>, private val onPlay: (File)->Unit, private val onDelete: (Int)->Unit) : androidx.recyclerview.widget.RecyclerView.Adapter<DiaryAdapter.Holder>() {
    class Holder(v: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(v) {
        val tvDate: TextView = v.findViewById(R.id.tvDate); val tvText: TextView = v.findViewById(R.id.tvText)
        val images: LinearLayout = v.findViewById(R.id.entryImages); val voices: LinearLayout = v.findViewById(R.id.entryVoices)
        val btnDelete: View = v.findViewById(R.id.btnDeleteEntry)
    }
    override fun onCreateViewHolder(p: android.view.ViewGroup, t: Int) = Holder(LayoutInflater.from(p.context).inflate(R.layout.item_entry, p, false))
    override fun getItemCount() = list.size
    override fun onBindViewHolder(h: Holder, pos: Int) {
        val e = list[pos]; h.tvDate.text = e.fullDate; h.tvText.text = e.text; h.images.removeAllViews()
        e.images.forEach { uri -> val iv = ImageView(h.itemView.context).apply { setImageURI(uri); layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 320).apply{topMargin=10}; scaleType = ImageView.ScaleType.CENTER_CROP }; h.images.addView(iv) }
        h.voices.removeAllViews()
        e.voices.forEach { file -> val block = LayoutInflater.from(h.itemView.context).inflate(R.layout.item_voice_block, h.voices, false); block.findViewById<TextView>(R.id.tvDuration).text = file.name; block.findViewById<TextView>(R.id.btnPlay).setOnClickListener { onPlay(file) }; h.voices.addView(block) }
        h.btnDelete.setOnClickListener { onDelete(pos) }
    }
}
