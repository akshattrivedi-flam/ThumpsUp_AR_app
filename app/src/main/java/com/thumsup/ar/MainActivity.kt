package com.thumsup.ar

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.thumsup.ar.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.startArButton.setOnClickListener {
            startActivity(DeepLinkHandler.buildArIntent(this, null))
        }

        routeDeepLinkIfPresent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        routeDeepLinkIfPresent(intent)
    }

    private fun routeDeepLinkIfPresent(intent: Intent?) {
        if (intent?.action != Intent.ACTION_VIEW) return

        val product = DeepLinkHandler.parseProductFromIntent(intent)
        if (product == null) {
            Toast.makeText(this, getString(R.string.error_invalid_link), Toast.LENGTH_LONG).show()
            return
        }

        startActivity(DeepLinkHandler.buildArIntent(this, product))
        finish()
    }
}
