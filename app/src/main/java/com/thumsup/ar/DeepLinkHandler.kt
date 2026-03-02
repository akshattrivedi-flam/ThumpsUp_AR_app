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
        if (!isSupportedCampaignUri(data)) return null
        return data.getQueryParameter("product")?.takeIf { it.isNotBlank() }
    }

    fun buildArIntent(context: Context, product: String?): Intent {
        return Intent(context, ARActivity::class.java).apply {
            putExtra(EXTRA_PRODUCT, product)
        }
    }

    private fun isSupportedCampaignUri(uri: Uri): Boolean {
        return uri.scheme == EXPECTED_SCHEME &&
            uri.host == EXPECTED_HOST &&
            uri.path == EXPECTED_PATH
    }
}
