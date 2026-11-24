package com.mymovie.log

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.mymovie.log.presentation.navigation.AppNavHost
import com.mymovie.log.presentation.ui.theme.MyMovieLogTheme
import com.mymovie.log.util.AppLogger
import dagger.hilt.android.AndroidEntryPoint
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.handleDeeplinks
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var supabaseClient: SupabaseClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppLogger.d("APP_INIT", "MainActivity onCreate | coldStart=${savedInstanceState == null}")
        enableEdgeToEdge()
        // deep link on cold start
        handleAuthDeeplink(intent)
        setContent {
            MyMovieLogTheme {
                AppNavHost()
            }
        }
    }

    // deep link while running
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleAuthDeeplink(intent)
    }

    private fun handleAuthDeeplink(intent: Intent?) {
        intent ?: return
        val uri = intent.data ?: return
        if (uri.scheme == "mymovie" && uri.host == "login-callback") {
            AppLogger.i("APP_INIT", "OAuth deeplink received: $uri")
            lifecycleScope.launch {
                supabaseClient.handleDeeplinks(intent)
            }
        }
    }
}
