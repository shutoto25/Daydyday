package com.gmail.shu10.dev.app.feature.home

import android.Manifest
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import com.gmail.shu10.dev.app.feature.theme.DaydydayTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * 画面基盤
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DaydydayTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavHost { controller ->
                        // controllerを外部に保持
                        navController = controller
                    }
                    RequestPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        val uri = intent.data
        uri?.let {
            val date = it.getQueryParameter("date")
            if (date != null) {
                navController.navigate(AppScreen.Detail(date).route)
            }
        }
    }
}