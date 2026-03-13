package com.stickersnip.app

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {

    private lateinit var packManager: PackManager
    private lateinit var rvPacks: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var adapter: PackAdapter
    private var packs = mutableListOf<StickerPack>()

    companion object {
        private const val REQUEST_CREATE_PACK = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        packManager = PackManager(this)
        rvPacks = findViewById(R.id.rvPacks)
        tvEmpty = findViewById(R.id.tvEmpty)
        val fabCreate = findViewById<FloatingActionButton>(R.id.fabCreate)

        adapter = PackAdapter(packs) { pack ->
            WhatsAppHelper.addStickerPackToWhatsApp(this, pack)
        }

        rvPacks.layoutManager = LinearLayoutManager(this)
        rvPacks.adapter = adapter

        fabCreate.setOnClickListener {
            val intent = Intent(this, CreatePackActivity::class.java)
            @Suppress("DEPRECATION")
            startActivityForResult(intent, REQUEST_CREATE_PACK)
        }

        loadPacks()
    }

    override fun onResume() {
        super.onResume()
        loadPacks()
    }

    @Deprecated("Use Activity Result API")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CREATE_PACK && resultCode == RESULT_OK) {
            loadPacks()
        }
    }

    private fun loadPacks() {
        packs.clear()
        packs.addAll(packManager.loadPacks())
        adapter.notifyDataSetChanged()
        tvEmpty.visibility = if (packs.isEmpty()) View.VISIBLE else View.GONE
        rvPacks.visibility = if (packs.isEmpty()) View.GONE else View.VISIBLE
    }

    /**
     * RecyclerView adapter for displaying sticker packs on the home screen.
     */
    inner class PackAdapter(
        private val packs: List<StickerPack>,
        private val onAddToWhatsApp: (StickerPack) -> Unit
    ) : RecyclerView.Adapter<PackAdapter.PackViewHolder>() {

        inner class PackViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvPackName: TextView = itemView.findViewById(R.id.tvPackName)
            val tvStickerCount: TextView = itemView.findViewById(R.id.tvStickerCount)
            val ivTrayIcon: ImageView = itemView.findViewById(R.id.ivTrayIcon)
            val btnAddToWhatsApp: Button = itemView.findViewById(R.id.btnAddToWhatsApp)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PackViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_pack, parent, false)
            return PackViewHolder(view)
        }

        override fun onBindViewHolder(holder: PackViewHolder, position: Int) {
            val pack = packs[position]
            holder.tvPackName.text = pack.name
            holder.tvStickerCount.text = getString(R.string.stickers_count, pack.stickers.size)

            // Load tray icon
            val trayBitmap = packManager.loadStickerBitmap(pack.identifier, pack.trayImageFile)
            if (trayBitmap != null) {
                holder.ivTrayIcon.setImageBitmap(trayBitmap)
            }

            holder.btnAddToWhatsApp.setOnClickListener {
                onAddToWhatsApp(pack)
            }
        }

        override fun getItemCount(): Int = packs.size
    }
}
