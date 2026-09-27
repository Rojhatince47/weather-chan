package io.github.rojhatince47.weatherchan.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import io.github.rojhatince47.weatherchan.R
import io.github.rojhatince47.weatherchan.core.ui.theme.HavaSpacing
import io.github.rojhatince47.weatherchan.core.ui.theme.HavaTheme

/**
 * Ana ekran başlığı. Düz bir primaryContainer yüzeyi üzerinde büyük başlık ve
 * açıklama taşır; ikon içermez (tema simgesi kontrol satırındadır).
 *
 * Degrade kullanılmaz: bu uygulamada akılda kalan tek öğe Faz 3'teki hava durumu
 * kartıdır, başlık onun önüne geçmemelidir. Yüzeyi düz bırakmak tipografinin
 * işini yapmasını sağlar.
 */
@Composable
fun HomeHeader(title: String, description: String, modifier: Modifier = Modifier) {
    val shape = MaterialTheme.shapes.extraLarge
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = HavaSpacing.lg)
            .clip(shape)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(HavaSpacing.xl),
        verticalArrangement = Arrangement.spacedBy(HavaSpacing.xs)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeHeaderShortTextPreview() {
    HavaTheme {
        HomeHeader(
            title = stringResource(R.string.home_title),
            description = stringResource(R.string.home_description)
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

@Preview(showBackground = true, widthDp = 600)
@Composable
private fun HomeHeaderWidePreview() {
    HavaTheme {
        HomeHeader(
            title = stringResource(R.string.home_title),
            description = stringResource(R.string.home_description)
        )
    }
}
