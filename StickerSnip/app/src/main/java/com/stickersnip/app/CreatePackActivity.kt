package com.stickersnip.app

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class CreatePackActivity : AppCompatActivity() {

    private lateinit var packManager: PackManager
    private lateinit var etPackName: EditText
    private lateinit var etAuthor: EditText
    private lateinit var rvCropCards: RecyclerView
    private lateinit var rvSavedStickers: RecyclerView
    private lateinit var btnAddToWhatsApp: Button

    private val cropImages = mutableListOf<Bitmap>()
    private val savedStickerFileNames = mutableListOf<String>()
    private lateinit var cropAdapter: CropCardAdapter
    private lateinit var savedAdapter: SavedStickerAdapter

    private var packId: String = ""

    private val pickImagesLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            cropImages.clear()
            for (uri in uris) {
                val bitmap = loadBitmapFromUri(uri)
                if (bitmap != null) {
                    cropImages.add(bitmap)
                }
            }
            cropAdapter.notifyDataSetChanged()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_pack)

        packManager = PackManager(this)
        packId = packManager.generatePackId()

        etPackName = findViewById(R.id.etPackName)
        etAuthor = findViewById(R.id.etAuthor)
        rvCropCards = findViewById(R.id.rvCropCards)
        rvSavedStickers = findViewById(R.id.rvSavedStickers)
        btnAddToWhatsApp = findViewById(R.id.btnAddToWhatsApp)
        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        val btnPickImages = findViewById<Button>(R.id.btnPickImages)

        cropAdapter = CropCardAdapter()
        rvCropCards.layoutManager = LinearLayoutManager(this)
        rvCropCards.adapter = cropAdapter

        savedAdapter = SavedStickerAdapter()
        rvSavedStickers.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvSavedStickers.adapter = savedAdapter

        btnBack.setOnClickListener { finish() }

        btnPickImages.setOnClickListener {
            pickImagesLauncher.launch("image/*")
        }

        btnAddToWhatsApp.setOnClickListener {
            savePackAndAddToWhatsApp()
        }
    }

    private fun loadBitmapFromUri(uri: Uri): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    decoder.isMutableRequired = true
                }
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(contentResolver, uri)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun addStickerFromCrop(cropView: CropView) {
        val cropped = cropView.getCroppedBitmap() ?: return

        val fileName = packManager.saveStickerImage(cropped, packId)
        savedStickerFileNames.add(fileName)
        savedAdapter.notifyDataSetChanged()

        // Also save tray icon from the first sticker
        if (savedStickerFileNames.size == 1) {
            packManager.saveTrayIcon(cropped, packId)
        }

        cropped.recycle()
        Toast.makeText(this, R.string.sticker_added, Toast.LENGTH_SHORT).show()
    }

    private fun savePackAndAddToWhatsApp() {
        val name = etPackName.text.toString().trim()
        val author = etAuthor.text.toString().trim()

        if (name.isEmpty()) {
            etPackName.error = "Pack name required"
            return
        }
        if (author.isEmpty()) {
            etAuthor.error = "Author required"
            return
        }
        if (savedStickerFileNames.size < 3) {
            Toast.makeText(this, R.string.min_stickers_warning, Toast.LENGTH_SHORT).show()
            return
        }

        val stickers = savedStickerFileNames.map { Sticker(it) }.toMutableList()
        val trayFile = "tray_${packId}.png"

        val pack = StickerPack(
            identifier = packId,
            name = name,
            publisher = author,
            trayImageFile = trayFile,
            stickers = stickers
        )

        val packs = packManager.loadPacks()
        packs.add(pack)
        packManager.savePacks(packs)

        setResult(RESULT_OK)
        WhatsAppHelper.addStickerPackToWhatsApp(this, pack)
    }

    /**
     * Adapter for the crop card RecyclerView - shows each image with a CropView.
     */
    inner class CropCardAdapter : RecyclerView.Adapter<CropCardAdapter.CropViewHolder>() {

        inner class CropViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val cropView: CropView = itemView.findViewById(R.id.cropView)
            val btnAddToPack: Button = itemView.findViewById(R.id.btnAddToPack)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CropViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_crop_card, parent, false)
            return CropViewHolder(view)
        }

        override fun onBindViewHolder(holder: CropViewHolder, position: Int) {
            val bitmap = cropImages[position]
            holder.cropView.setImageBitmap(bitmap)
            holder.btnAddToPack.setOnClickListener {
                addStickerFromCrop(holder.cropView)
            }
        }

        override fun getItemCount(): Int = cropImages.size
    }

    /**
     * Adapter for saved sticker thumbnails shown at the bottom.
     */
    inner class SavedStickerAdapter : RecyclerView.Adapter<SavedStickerAdapter.StickerViewHolder>() {

        inner class StickerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val ivSticker: ImageView = itemView.findViewById(R.id.ivSticker)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StickerViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_saved_sticker, parent, false)
            return StickerViewHolder(view)
        }

        override fun onBindViewHolder(holder: StickerViewHolder, position: Int) {
            val bitmap = packManager.loadStickerBitmap(packId, savedStickerFileNames[position])
            if (bitmap != null) {
                holder.ivSticker.setImageBitmap(bitmap)
            }
        }

        override fun getItemCount(): Int = savedStickerFileNames.size
    }
}
