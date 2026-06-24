package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.ui.theme.MyApplicationTheme

import android.content.res.Configuration
import java.util.Locale

class MainActivity : ComponentActivity() {
  override fun attachBaseContext(newBase: android.content.Context) {
      val lang = LanguageManager.getLanguage(newBase)
      val locale = Locale(lang)
      Locale.setDefault(locale)
      val config = Configuration(newBase.resources.configuration)
      config.setLocale(locale)
      super.attachBaseContext(newBase.createConfigurationContext(config))
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    LanguageManager.init(this)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            AppNavigation()
        }
      }
    }
  }
}
