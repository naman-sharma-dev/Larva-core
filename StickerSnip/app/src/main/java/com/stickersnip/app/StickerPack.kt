package com.stickersnip.app

import org.json.JSONArray
import org.json.JSONObject

data class Sticker(
    val imageFileName: String,
    val emojis: List<String> = listOf("😀")
)

data class StickerPack(
    val identifier: String,
    val name: String,
    val publisher: String,
    val trayImageFile: String,
    val stickers: MutableList<Sticker> = mutableListOf(),
    val publisherEmail: String = "",
    val publisherWebsite: String = "",
    val privacyPolicyWebsite: String = "",
    val licenseAgreementWebsite: String = "",
    val imageDataVersion: String = "1",
    val avoidCache: Boolean = false,
    val animatedStickerPack: Boolean = false
) {
    fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("identifier", identifier)
        json.put("name", name)
        json.put("publisher", publisher)
        json.put("tray_image_file", trayImageFile)
        json.put("publisher_email", publisherEmail)
        json.put("publisher_website", publisherWebsite)
        json.put("privacy_policy_website", privacyPolicyWebsite)
        json.put("license_agreement_website", licenseAgreementWebsite)
        json.put("image_data_version", imageDataVersion)
        json.put("avoid_cache", avoidCache)
        json.put("animated_sticker_pack", animatedStickerPack)
        val stickersArray = JSONArray()
        for (sticker in stickers) {
            val stickerJson = JSONObject()
            stickerJson.put("image_file", sticker.imageFileName)
            val emojisArray = JSONArray()
            for (emoji in sticker.emojis) {
                emojisArray.put(emoji)
            }
            stickerJson.put("emojis", emojisArray)
            stickersArray.put(stickerJson)
        }
        json.put("stickers", stickersArray)
        return json
    }

    companion object {
        fun fromJson(json: JSONObject): StickerPack {
            val stickers = mutableListOf<Sticker>()
            val stickersArray = json.optJSONArray("stickers") ?: JSONArray()
            for (i in 0 until stickersArray.length()) {
                val stickerJson = stickersArray.getJSONObject(i)
                val emojis = mutableListOf<String>()
                val emojisArray = stickerJson.optJSONArray("emojis") ?: JSONArray()
                for (j in 0 until emojisArray.length()) {
                    emojis.add(emojisArray.getString(j))
                }
                stickers.add(Sticker(stickerJson.getString("image_file"), emojis))
            }
            return StickerPack(
                identifier = json.getString("identifier"),
                name = json.getString("name"),
                publisher = json.getString("publisher"),
                trayImageFile = json.getString("tray_image_file"),
                stickers = stickers,
                publisherEmail = json.optString("publisher_email", ""),
                publisherWebsite = json.optString("publisher_website", ""),
                privacyPolicyWebsite = json.optString("privacy_policy_website", ""),
                licenseAgreementWebsite = json.optString("license_agreement_website", ""),
                imageDataVersion = json.optString("image_data_version", "1"),
                avoidCache = json.optBoolean("avoid_cache", false),
                animatedStickerPack = json.optBoolean("animated_sticker_pack", false)
            )
        }
    }
}
