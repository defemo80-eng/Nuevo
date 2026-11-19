package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import coil.Coil
import coil.ImageLoader
import com.example.data.sync.CatalogSyncWorker
import com.example.ui.screens.DetflixRootApp
import com.example.ui.theme.DetflixTheme
import okhttp3.OkHttpClient

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        configureDetflixImageLoader()
        CatalogSyncWorker.schedulePeriodicSync(applicationContext)
        setContent {
            DetflixTheme {
                DetflixRootApp()
            }
        }
    }

    private fun configureDetflixImageLoader() {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val req = chain.request().newBuilder()
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) DETFLIX/1.0")
                    .build()
                chain.proceed(req)
            }
            .build()
        val loader = ImageLoader.Builder(applicationContext)
            .okHttpClient(client)
            .crossfade(true)
            .build()
        Coil.setImageLoader(loader)
    }
}
