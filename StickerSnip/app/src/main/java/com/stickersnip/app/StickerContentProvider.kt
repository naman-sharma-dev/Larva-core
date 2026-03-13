package com.stickersnip.app

import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.File

/**
 * ContentProvider for serving sticker pack data to WhatsApp.
 * Authority: com.stickersnip.app.StickerContentProvider
 */
class StickerContentProvider : ContentProvider() {

    companion object {
        const val AUTHORITY = "com.stickersnip.app.StickerContentProvider"

        private const val METADATA = 1
        private const val METADATA_CODE = 2
        private const val STICKERS = 3
        private const val STICKER_FILE = 4
        private const val STICKER_ASSET = 5

        private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(AUTHORITY, "metadata", METADATA)
            addURI(AUTHORITY, "metadata/*", METADATA_CODE)
            addURI(AUTHORITY, "stickers/*", STICKERS)
            addURI(AUTHORITY, "stickers_asset/*/", STICKER_FILE)
            addURI(AUTHORITY, "stickers_asset/*/*", STICKER_ASSET)
        }
    }

    private lateinit var packManager: PackManager

    override fun onCreate(): Boolean {
        packManager = PackManager(context!!)
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        val packs = packManager.loadPacks()

        return when (uriMatcher.match(uri)) {
            METADATA -> {
                getPacksCursor(packs)
            }
            METADATA_CODE -> {
                val identifier = uri.lastPathSegment ?: return null
                val pack = packs.find { it.identifier == identifier } ?: return null
                getPacksCursor(listOf(pack))
            }
            STICKERS -> {
                val identifier = uri.lastPathSegment ?: return null
                val pack = packs.find { it.identifier == identifier } ?: return null
                getStickersCursor(pack)
            }
            else -> null
        }
    }

    private fun getPacksCursor(packs: List<StickerPack>): Cursor {
        val cursor = MatrixCursor(
            arrayOf(
                "sticker_pack_identifier",
                "sticker_pack_name",
                "sticker_pack_publisher",
                "sticker_pack_icon",
                "android_play_store_link",
                "ios_app_store_link",
                "publisher_email",
                "publisher_website",
                "privacy_policy_website",
                "license_agreement_website",
                "image_data_version",
                "avoid_cache",
                "animated_sticker_pack"
            )
        )
        for (pack in packs) {
            cursor.addRow(
                arrayOf(
                    pack.identifier,
                    pack.name,
                    pack.publisher,
                    pack.trayImageFile,
                    "",
                    "",
                    pack.publisherEmail,
                    pack.publisherWebsite,
                    pack.privacyPolicyWebsite,
                    pack.licenseAgreementWebsite,
                    pack.imageDataVersion,
                    if (pack.avoidCache) 1 else 0,
                    if (pack.animatedStickerPack) 1 else 0
                )
            )
        }
        return cursor
    }

    private fun getStickersCursor(pack: StickerPack): Cursor {
        val cursor = MatrixCursor(arrayOf("sticker_file_name", "sticker_emoji"))
        for (sticker in pack.stickers) {
            cursor.addRow(arrayOf(sticker.imageFileName, sticker.emojis.joinToString(",")))
        }
        return cursor
    }

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        val matchCode = uriMatcher.match(uri)
        if (matchCode != STICKER_ASSET && matchCode != STICKER_FILE) return null

        val segments = uri.pathSegments
        if (segments.size < 2) return null

        val packId = segments[1]
        val fileName = if (segments.size >= 3) segments[2] else ""

        val file: File = if (fileName.isNotEmpty()) {
            packManager.getStickerFile(packId, fileName)
        } else {
            // Tray icon
            val packs = packManager.loadPacks()
            val pack = packs.find { it.identifier == packId } ?: return null
            packManager.getStickerFile(packId, pack.trayImageFile)
        }

        if (!file.exists()) return null
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    }

    override fun getType(uri: Uri): String {
        return when (uriMatcher.match(uri)) {
            METADATA, METADATA_CODE -> "vnd.android.cursor.dir/vnd.$AUTHORITY.metadata"
            STICKERS -> "vnd.android.cursor.dir/vnd.$AUTHORITY.stickers"
            STICKER_FILE, STICKER_ASSET -> "image/webp"
            else -> "application/octet-stream"
        }
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int = 0
}
