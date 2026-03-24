package mega.privacy.android.feature.clouddrive.presentation.folderlink.view

import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.icon.pack.R as iconPackR

@Composable
internal fun UnavailableLinkView(
    @StringRes title: Int,
    @StringRes subtitle: Int,
    @StringRes bulletPoints: List<Int>,
    modifier: Modifier = Modifier,
) {
    val isInLandscapeMode =
        LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(
                rememberScrollState()
            )
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(id = iconPackR.drawable.ic_alert_triangle_color),
            contentDescription = "Error",
            modifier = Modifier
                .size(120.dp)
                .align(Alignment.CenterHorizontally),
        )

        Spacer(modifier = Modifier.height(16.dp))

        MegaText(
            text = stringResource(title),
            textColor = TextColor.Primary,
            textAlign = TextAlign.Center,
            style = AppTheme.typography.titleLarge,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp)
        )

        Spacer(
            modifier = Modifier
                .height(if (isInLandscapeMode) 36.dp else 56.dp)
        )

        MegaText(
            text = stringResource(subtitle),
            textColor = TextColor.Primary,
            style = AppTheme.typography.titleMedium,
        )

        Spacer(modifier = Modifier.height(16.dp))

        BulletList(
            items = bulletPoints.map { stringResource(it) },
        )

        if (isInLandscapeMode) {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun BulletList(
    items: List<String>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        items.forEach { point ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.Top,
            ) {
                MegaText(
                    text = "•",
                    textColor = TextColor.Secondary,
                    style = AppTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Medium,
                    ),
                    modifier = Modifier.padding(end = 8.dp)
                )
                MegaText(
                    text = point,
                    textColor = TextColor.Secondary,
                    style = AppTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}
