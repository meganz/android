package mega.privacy.android.feature.mediaplayer.components

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.theme.values.TextColor

@Composable
internal fun AudioMetadataSection(
    title: String,
    artist: String?,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        MegaText(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            textColor = TextColor.Primary,
            modifier = Modifier.basicMarquee(),
        )
        if (!artist.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            MegaText(
                text = artist,
                style = MaterialTheme.typography.bodyMedium,
                textColor = TextColor.Secondary,
            )
        }
    }
}
