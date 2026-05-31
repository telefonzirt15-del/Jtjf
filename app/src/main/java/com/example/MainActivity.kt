package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Surface
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.AuthMainApp
import com.example.ui.AuthViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enabling EdgeToEdge display support
        enableEdgeToEdge()
        
        setContent {
            MyApplicationTheme {
                Surface {
                    val viewModel: AuthViewModel = viewModel()
                    AuthMainApp(viewModel = viewModel)
                }
            }
        }
    }
}
