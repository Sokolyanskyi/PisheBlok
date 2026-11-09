package com.pisheblok.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.pisheblok.app.ui.nav.RootApp
import com.pisheblok.app.ui.theme.PisheBlokTheme

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PisheBlokTheme {
                RootApp()
            }
        }
    }
}
