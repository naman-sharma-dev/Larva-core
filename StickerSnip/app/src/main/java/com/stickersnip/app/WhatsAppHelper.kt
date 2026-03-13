package com.stickersnip.app

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.widget.Toast

/**
 * Helper for integrating with WhatsApp's sticker pack API.
 */
object WhatsAppHelper {

    private const val AUTHORITY = "com.stickersnip.app.StickerContentProvider"
    private const val ADD_PACK_ACTION = "com.whatsapp.intent.action.ENABLE_STICKER_PACK"
    private const val STICKER_PACK_ID_EXTRA = "sticker_pack_id"
    private const val STICKER_PACK_AUTHORITY_EXTRA = "sticker_pack_authority"
    private const val STICKER_PACK_NAME_EXTRA = "sticker_pack_name"
    private const val WHATSAPP_PACKAGE = "com.whatsapp"

    /**
     * Launches an intent to add a sticker pack to WhatsApp.
     */
    fun addStickerPackToWhatsApp(activity: Activity, pack: StickerPack) {
        if (!isWhatsAppInstalled(activity)) {
            Toast.makeText(activity, R.string.whatsapp_not_installed, Toast.LENGTH_SHORT).show()
            return
        }

        if (pack.stickers.size < 3) {
            Toast.makeText(activity, R.string.min_stickers_warning, Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(ADD_PACK_ACTION).apply {
            putExtra(STICKER_PACK_ID_EXTRA, pack.identifier)
            putExtra(STICKER_PACK_AUTHORITY_EXTRA, AUTHORITY)
            putExtra(STICKER_PACK_NAME_EXTRA, pack.name)
        }

        try {
            activity.startActivityForResult(intent, 200)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(activity, R.string.whatsapp_not_installed, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Checks if WhatsApp is installed on the device.
     */
    private fun isWhatsAppInstalled(activity: Activity): Boolean {
        return try {
            @Suppress("DEPRECATION")
            activity.packageManager.getPackageInfo(WHATSAPP_PACKAGE, 0)
            true
        } catch (e: Exception) {
            false
        }
    }
}
