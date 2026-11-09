package com.pisheblok.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import com.pisheblok.app.ui.nav.RootApp
import com.pisheblok.app.ui.theme.LocalSchemeState
import com.pisheblok.app.ui.theme.PisheBlokTheme
import com.pisheblok.app.ui.theme.ThemePrefs
import com.pisheblok.app.ui.theme.ThemeScheme

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val scheme = mutableStateOf(ThemePrefs.load(this))
        setContent {
            CompositionLocalProvider(LocalSchemeState provides scheme) {
                PisheBlokTheme(scheme = scheme.value) {
                    RootApp()
                }
            }
        }
    }
}
