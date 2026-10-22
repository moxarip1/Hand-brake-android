package com.example

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.ui.HandBrakeApp
import com.example.ui.theme.HandBrakeMobileTheme
import com.example.viewmodel.HandBrakeViewModel

class MainActivity : ComponentActivity() {

    private val handBrakeViewModel: HandBrakeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handle video shared from gallery/other apps via ACTION_SEND
        handleIncomingVideoIntent(intent)

        setContent {
            HandBrakeMobileTheme(darkTheme = true) {
                HandBrakeApp(viewModel = handBrakeViewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingVideoIntent(intent)
    }

    private fun handleIncomingVideoIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type?.startsWith("video/") == true) {
            val sharedUri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(Intent.EXTRA_STREAM)
            }
            if (sharedUri != null) {
                handBrakeViewModel.loadVideoFromUri(sharedUri)
            }
        }
    }
}
