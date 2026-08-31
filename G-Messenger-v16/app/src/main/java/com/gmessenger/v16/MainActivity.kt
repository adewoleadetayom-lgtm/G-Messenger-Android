package com.gmessenger.v16

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gmessenger.v16.ui.GMessengerApp
import com.gmessenger.v16.ui.GMessengerViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { GMessengerApp(viewModel<GMessengerViewModel>()) }
    }
}
