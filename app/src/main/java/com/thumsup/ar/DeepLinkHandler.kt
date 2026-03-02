package com.thumsup.ar

import android.content.Context
import android.content.Intent
import android.net.Uri

object DeepLinkHandler {
    const val EXTRA_PRODUCT = "extra_product"
    private const val EXPECTED_SCHEME = "https"
    private const val EXPECTED_HOST = "yourdomain.com"
    private const val EXPECTED_PATH = "/campaign"

    fun parseProductFromIntent(intent: Intent?): String? {
        val data = intent?.data ?: return null
        return parseProductFromUri(data, strictCampaignMatch = true)
    }

    fun parseProductFromQr(rawValue: String): String? {
        val trimmed = rawValue.trim()
        if (trimmed.isEmpty()) return null

        val fromUri = runCatching { Uri.parse(trimmed) }
            .getOrNull()
            ?.let { parseProductFromUri(it, strictCampaignMatch = false) }

        if (!fromUri.isNullOrBlank()) {
            return fromUri
        }

        val uri = runCatching { Uri.parse(trimmed) }.getOrNull()
        if (uri?.scheme == "http" || uri?.scheme == "https") {
            // Generic URL QR payloads map to default campaign product.
            return "thumsup300"
        }

        // Fallback for non-URL QR payloads.
        return trimmed.takeIf { it.length >= 3 }?.let { "thumsup300" }
    }

    fun extractMp4UrlFromQr(rawValue: String): String? {
        val trimmed = rawValue.trim()
        if (trimmed.isEmpty()) return null
        if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) return null
        return trimmed.takeIf {
            it.contains(".mp4", ignoreCase = true) || it.contains("video", ignoreCase = true)
        }
    }

    fun buildArIntent(context: Context, product: String?): Intent {
        return Intent(context, ARActivity::class.java).apply {
            putExtra(EXTRA_PRODUCT, product)
        }
    }

    private fun parseProductFromUri(uri: Uri, strictCampaignMatch: Boolean): String? {
        if (strictCampaignMatch && !isSupportedCampaignUri(uri)) return null
        return uri.getQueryParameter("product")?.takeIf { it.isNotBlank() }
    }

    private fun isSupportedCampaignUri(uri: Uri): Boolean {
        return uri.scheme == EXPECTED_SCHEME &&
            uri.host == EXPECTED_HOST &&
            uri.path == EXPECTED_PATH
    }
}
