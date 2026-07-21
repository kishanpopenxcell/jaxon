package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.ui.JaxonApp
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.JaxonViewModel
import com.example.ui.viewmodel.JaxonViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Fetch manual dependencies from AppContainer
        val appContainer = (application as JaxonApplication).container
        val factory = JaxonViewModelFactory(
            app = application,
            repository = appContainer.jaxonRepository,
            speechManager = appContainer.speechManager,
            ttsManager = appContainer.ttsManager,
            intentParser = appContainer.intentParser,
            actionExecutor = appContainer.actionExecutor
        )
        val viewModel: JaxonViewModel by viewModels { factory }

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    JaxonApp(viewModel = viewModel)
                }
            }
        }
    }
}

