package com.mettyoung.fitbro

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.mettyoung.fitbro.auth.OAuthCallbackHandler

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        handleOAuthCallback(intent)
        setContent {
            App()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleOAuthCallback(intent)
    }

    private fun handleOAuthCallback(intent: Intent) {
        val data = intent.data ?: return
        if (data.scheme == "com.mettyoung.fitbro" && data.host == "oauth" && data.path == "/callback") {
            val code = data.getQueryParameter("code") ?: return
            val state = data.getQueryParameter("state") ?: return
            OAuthCallbackHandler.deliver(code, state)
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
