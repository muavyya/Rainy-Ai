package com.rainy.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rainy.ai.ui.ChatScreen
import com.rainy.ai.ui.ChatViewModel
import com.rainy.ai.ui.RainyTheme
import com.rainy.ai.ui.SettingsScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RainyTheme {
                Surface(modifier = Modifier.fillMaxSize()) { RainyApp() }
            }
        }
    }
}

@Composable
fun RainyApp(vm: ChatViewModel = viewModel()) {
    var showSettings by rememberSaveable { mutableStateOf(false) }
    if (showSettings) {
        BackHandler { showSettings = false }
        SettingsScreen(vm) { showSettings = false }
    } else {
        ChatScreen(vm) { showSettings = true }
    }
}
