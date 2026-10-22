package com.vitairos.industrialai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

import com.vitairos.industrialai.ui.navigation.AppNavHost
import com.vitairos.industrialai.ui.theme.VitairosTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VitairosTheme { AppNavHost() }
        }
    }
}
