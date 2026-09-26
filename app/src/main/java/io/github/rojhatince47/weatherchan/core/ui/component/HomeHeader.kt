package io.github.rojhatince47.weatherchan.core.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.rojhatince47.weatherchan.core.ui.theme.HavaTheme

@Composable
fun HomeHeader(title: String, description: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeHeaderShortTextPreview() {
    HavaTheme {
        HomeHeader(
            title = "Weather-chan",
            description = "Şehirlerin havasını sevimli takip et"
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeHeaderLongTextPreview() {
    HavaTheme {
        HomeHeader(
            title = "Çok uzun bir başlık örneği için preview",
            description = "Bu açıklama metni, uzun metinlerin bile okunabilir ve düzenli kalmasını sağlamak için kullanılır."
        )
    }
}

@Preview(showBackground = true, widthDp = 320, fontScale = 1.5f)
@Composable
private fun HomeHeaderNarrowScreenLargeFontPreview() {
    HavaTheme {
        HomeHeader(
            title = "Çok uzun bir başlık örneği için preview",
            description = "Bu açıklama metni, uzun metinlerin bile okunabilir ve düzenli kalmasını sağlamak için kullanılır."
        )
    }
}
