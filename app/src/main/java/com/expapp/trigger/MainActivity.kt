package com.expapp.trigger

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.expapp.trigger.data.repository.DeviceRepository
import com.expapp.trigger.data.repository.SharedPrefsStorage
import com.expapp.trigger.network.TriggerClient
import com.expapp.trigger.ui.screens.MainScreen
import com.expapp.trigger.ui.theme.DarkBackground
import com.expapp.trigger.ui.theme.ExpAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val repository = DeviceRepository(SharedPrefsStorage(applicationContext))
        val triggerClient = TriggerClient()

        setContent {
            ExpAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkBackground
                ) {
                    MainScreen(
                        repository = repository,
                        triggerClient = triggerClient
                    )
                }
            }
        }
    }
}
