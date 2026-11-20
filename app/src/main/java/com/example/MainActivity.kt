package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.datastore.SessionState
import com.example.domain.model.TemaTerang
import com.example.presentation.navigation.AppNavigation
import com.example.presentation.viewmodel.EduPinjamViewModelFactory
import com.example.ui.theme.EduPinjamTheme

/**
 * Activity utama aplikasi EDU-PINJAM dengan edge-to-edge dan tema Sky Blue.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                scrim = android.graphics.Color.TRANSPARENT,
                darkScrim = android.graphics.Color.TRANSPARENT
            )
        )

        val app = application as EduPinjamApp
        val factory = EduPinjamViewModelFactory(app.repository)

        setContent {
            val sessionState by app.repository.sessionState.collectAsStateWithLifecycle(
                initialValue = SessionState()
            )
            val useSoftSkyTheme = sessionState.temaTerang == TemaTerang.BIRU_LANGIT_LEMBUT

            EduPinjamTheme(useSoftSkyTheme = useSoftSkyTheme) {
                AppNavigation(
                    factory = factory,
                    sessionState = sessionState
                )
            }
        }
    }
}
