package xyz.lisfox.vocaland

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import xyz.lisfox.vocaland.ui.VocaLandApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as VocaLandApplication).container
        setContent {
            VocaLandApp(container = container)
        }
    }
}
