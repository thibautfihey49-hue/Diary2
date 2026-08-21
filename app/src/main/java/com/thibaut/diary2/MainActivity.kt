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

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri -> uri?.let { currentImages.add(it); addImageBlock(it) } }
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
            entries.removeAt(pos); binding.timeline.adapter?.notifyDataSetChanged()
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
            entries.add(0, DiaryEntry("$full\n\n$text", currentImages.toList(), currentVoices.toList(), short, full))
            binding.timeline.adapter?.notifyItemInserted(0)
            binding.entryText.text.clear(); binding.blocksContainer.removeAllViews(); binding.tvRecordingStatus.text = ""
            currentImages.clear(); currentVoices.clear(); updateAutoDate()
        }
        checkPerms()
    }
    private fun updateAutoDate() {
        binding.tvAutoDate.text = SimpleDateFormat("EEEE dd MMMM yyyy — HH:mm", Locale.FRANCE).format(Date()).replaceFirstChar { it.uppercase() }
    }
    private fun setGlobalColor(c:Int){ ThemeManager.current.accent=c; ThemeManager.save(this); applyFullTheme() }
    private fun applyFullTheme(){
        val accent = ThemeManager.current.accent
        try {
            if (ThemeManager.current.customWallpaperUri!=null){
                binding.ivCustomWallpaper.setImageURI(Uri.parse(ThemeManager.current.customWallpaperUri))
                binding.ivCustomWallpaper.visibility=View.VISIBLE
            } else binding.ivCustomWallpaper.visibility=View.GONE
        } catch(_:Exception){ binding.ivCustomWallpaper.visibility=View.GONE }
        try { (binding.btnSave.parent as androidx.cardview.widget.CardView).setCardBackgroundColor(accent); binding.tvAccentDot.setBackgroundColor(accent) } catch(_:Exception){}
    }
    private fun addImageBlock(uri:Uri){
        val iv=ImageView(this).apply{ setImageURI(uri); layoutParams=LinearLayout.LayoutParams(-1,420).apply{topMargin=14}; scaleType=ImageView.ScaleType.CENTER_CROP; clipToOutline=true }
        binding.blocksContainer.addView(iv)
    }
    private fun startVoiceBlock(){
        try{
            currentRecordingFile=File(cacheDir,"voice_${System.currentTimeMillis()}.m4a")
            recorder=MediaRecorder().apply{ setAudioSource(MediaRecorder.AudioSource.MIC); setOutputFormat(MediaRecorder.OutputFormat.MPEG_4); setAudioEncoder(MediaRecorder.AudioEncoder.AAC); setOutputFile(currentRecordingFile!!.absolutePath); prepare(); start() }
            isRecording=true; binding.tvRecordingStatus.text="● Enregistrement..."
        } catch(e:Exception){ Toast.makeText(this,"Micro: ${e.message}",1).show() }
    }
    private fun stopVoiceBlock(){
        try{ recorder?.stop(); recorder?.release(); recorder=null; isRecording=false; binding.tvRecordingStatus.text="✓ Voix enregistrée"
            currentRecordingFile?.let{ f-> currentVoices.add(f); val b=LayoutInflater.from(this).inflate(R.layout.item_voice_block,binding.blocksContainer,false); b.findViewById<TextView>(R.id.tvDuration).text=f.name; b.findViewById<TextView>(R.id.btnPlay).setOnClickListener{ playVoice(f) }; binding.blocksContainer.addView(b) }
        } catch(_:Exception){ isRecording=false }
    }
    private fun playVoice(f:File){ try{ player?.release(); player=MediaPlayer().apply{ setDataSource(f.absolutePath); prepare(); start() } } catch(_:Exception){} }
    private fun checkPerms(){ val p=arrayOf(Manifest.permission.RECORD_AUDIO,Manifest.permission.READ_MEDIA_IMAGES); if(p.any{ ActivityCompat.checkSelfPermission(this,it)!=PackageManager.PERMISSION_GRANTED }) ActivityCompat.requestPermissions(this,p,1001) }
    override fun onDestroy(){ super.onDestroy(); recorder?.release(); player?.release() }
}
class DiaryAdapter(private val list:List<DiaryEntry>, private val onPlay:(File)->Unit, private val onDelete:(Int)->Unit): androidx.recyclerview.widget.RecyclerView.Adapter<DiaryAdapter.Holder>(){
    class Holder(v:View):androidx.recyclerview.widget.RecyclerView.ViewHolder(v){ val tvDate:TextView=v.findViewById(R.id.tvDate); val tvText:TextView=v.findViewById(R.id.tvText); val images:LinearLayout=v.findViewById(R.id.entryImages); val voices:LinearLayout=v.findViewById(R.id.entryVoices); val btnDelete:View=v.findViewById(R.id.btnDeleteEntry) }
    override fun onCreateViewHolder(p:android.view.ViewGroup,t:Int)=Holder(LayoutInflater.from(p.context).inflate(R.layout.item_entry,p,false))
    override fun getItemCount()=list.size
    override fun onBindViewHolder(h:Holder,pos:Int){ val e=list[pos]; h.tvDate.text=e.fullDate; h.tvText.text=e.text; h.images.removeAllViews(); e.images.forEach{ u-> val iv=ImageView(h.itemView.context).apply{ setImageURI(u); layoutParams=LinearLayout.LayoutParams(-1,320).apply{topMargin=10}; scaleType=ImageView.ScaleType.CENTER_CROP }; h.images.addView(iv) }; h.voices.removeAllViews(); e.voices.forEach{ f-> val b=LayoutInflater.from(h.itemView.context).inflate(R.layout.item_voice_block,h.voices,false); b.findViewById<TextView>(R.id.tvDuration).text=f.name; b.findViewById<TextView>(R.id.btnPlay).setOnClickListener{ onPlay(f) }; h.voices.addView(b) }; h.btnDelete.setOnClickListener{ onDelete(pos) } }
}
