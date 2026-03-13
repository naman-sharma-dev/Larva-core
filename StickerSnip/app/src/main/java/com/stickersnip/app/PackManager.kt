package com.stickersnip.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Manages sticker packs: saving/loading metadata as JSON and sticker images
 * in the app's internal storage.
 */
class PackManager(private val context: Context) {

    companion object {
        private const val PACKS_FILE = "sticker_packs.json"
        private const val STICKERS_DIR = "stickers"
        private const val STICKER_SIZE = 512
        private const val TRAY_SIZE = 96
    }

    private fun getStickersDir(): File {
        val dir = File(context.filesDir, STICKERS_DIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /**
     * Loads all sticker packs from JSON file.
     */
    fun loadPacks(): MutableList<StickerPack> {
        val file = File(context.filesDir, PACKS_FILE)
        if (!file.exists()) return mutableListOf()
        val json = file.readText()
        val array = JSONArray(json)
        val packs = mutableListOf<StickerPack>()
        for (i in 0 until array.length()) {
            packs.add(StickerPack.fromJson(array.getJSONObject(i)))
        }
        return packs
    }

    /**
     * Saves all sticker packs to JSON file.
     */
    fun savePacks(packs: List<StickerPack>) {
        val array = JSONArray()
        for (pack in packs) {
            array.put(pack.toJson())
        }
        val file = File(context.filesDir, PACKS_FILE)
        file.writeText(array.toString(2))
    }

    /**
     * Saves a bitmap as a 512x512 WEBP sticker file. Returns the filename.
     */
    fun saveStickerImage(bitmap: Bitmap, packId: String): String {
        val packDir = File(getStickersDir(), packId)
        if (!packDir.exists()) packDir.mkdirs()

        val scaled = Bitmap.createScaledBitmap(bitmap, STICKER_SIZE, STICKER_SIZE, true)
        val fileName = "sticker_${UUID.randomUUID()}.webp"
        val file = File(packDir, fileName)
        FileOutputStream(file).use { out ->
            val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Bitmap.CompressFormat.WEBP_LOSSY
            } else {
                @Suppress("DEPRECATION")
                Bitmap.CompressFormat.WEBP
            }
            scaled.compress(format, 90, out)
        }
        if (scaled !== bitmap) {
            scaled.recycle()
        }
        return fileName
    }

    /**
     * Saves the first sticker as a 96x96 PNG tray icon. Returns the filename.
     */
    fun saveTrayIcon(bitmap: Bitmap, packId: String): String {
        val packDir = File(getStickersDir(), packId)
        if (!packDir.exists()) packDir.mkdirs()

        val scaled = Bitmap.createScaledBitmap(bitmap, TRAY_SIZE, TRAY_SIZE, true)
        val fileName = "tray_${packId}.png"
        val file = File(packDir, fileName)
        FileOutputStream(file).use { out ->
            scaled.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        if (scaled !== bitmap) {
            scaled.recycle()
        }
        return fileName
    }

    /**
     * Gets the File for a sticker image within a pack directory.
     */
    fun getStickerFile(packId: String, fileName: String): File {
        return File(File(getStickersDir(), packId), fileName)
    }

    /**
     * Loads a sticker bitmap from internal storage.
     */
    fun loadStickerBitmap(packId: String, fileName: String): Bitmap? {
        val file = getStickerFile(packId, fileName)
        return if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
    }

    /**
     * Generates a unique pack identifier.
     */
    fun generatePackId(): String {
        return UUID.randomUUID().toString().replace("-", "").take(16)
    }

    /**
     * Deletes a pack and its sticker files.
     */
    fun deletePack(packId: String) {
        val packDir = File(getStickersDir(), packId)
        if (packDir.exists()) {
            packDir.deleteRecursively()
        }
        val packs = loadPacks()
        packs.removeAll { it.identifier == packId }
        savePacks(packs)
    }
}
