package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.data.sync.CatalogSyncWorker
import com.example.ui.screens.DetflixRootApp
import com.example.ui.theme.DetflixTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        CatalogSyncWorker.schedulePeriodicSync(applicationContext)
        setContent {
            DetflixTheme {
                DetflixRootApp()
            }
        }
    }
}
