package com.tamapoke.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.tamapoke.app.TamaPokeApp
import com.tamapoke.app.ui.main.MainScreen
import com.tamapoke.app.ui.main.MainViewModel
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels {
        MainViewModel.Factory((application as TamaPokeApp).repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val lifecycleOwner = LocalLifecycleOwner.current

            // Visible per-minute decay while the app is in the foreground, matching the
            // original device's always-on "1 game-minute = 1 real-minute" feel. Correctness
            // doesn't depend on this timer firing exactly - catchUp() on resume is the source
            // of truth - this is purely so stat bars visibly move while you're looking at them.
            LaunchedEffect(Unit) {
                lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                    while (true) {
                        delay(60_000)
                        viewModel.liveTick()
                    }
                }
            }

            MainScreen(viewModel)
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }
}
