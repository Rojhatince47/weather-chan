package io.github.rojhatince47.weatherchan

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import io.github.rojhatince47.weatherchan.core.navigation.HavaNavHost

@Composable
fun HavaApp(modifier: Modifier = Modifier) {
    val navController = rememberNavController()

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        HavaNavHost(
            navController = navController,
            modifier = Modifier.padding(innerPadding)
        )
    }
}
