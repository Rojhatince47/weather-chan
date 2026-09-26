package io.github.rojhatince47.weatherchan

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import io.github.rojhatince47.weatherchan.core.ui.component.HomeHeader
import io.github.rojhatince47.weatherchan.core.ui.theme.HavaTheme

@Composable
fun MainScreen(modifier: Modifier = Modifier) {
    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        HomeHeader(
            title = stringResource(R.string.home_title),
            description = stringResource(R.string.home_description),
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MainScreenPreview() {
    HavaTheme {
        MainScreen()
    }
}
