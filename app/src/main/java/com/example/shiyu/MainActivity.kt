package com.example.shiyu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.shiyu.ui.navigation.NavGraph
import com.example.shiyu.ui.theme.ShiYuTheme
import com.example.shiyu.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ShiYuTheme {
                NavGraph(viewModel = mainViewModel)
            }
        }
    }
}
