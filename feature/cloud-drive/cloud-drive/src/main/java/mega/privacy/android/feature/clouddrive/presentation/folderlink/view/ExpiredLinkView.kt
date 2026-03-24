package mega.privacy.android.feature.clouddrive.presentation.folderlink.view

import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.shared.resources.R as sharedR

@Composable
internal fun ExpiredLinkView(
    @StringRes title: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
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

        Spacer(modifier = Modifier.height(16.dp))

        MegaText(
            text = stringResource(sharedR.string.general_link_expired),
            textColor = TextColor.Primary,
            textAlign = TextAlign.Center,
            style = AppTheme.typography.bodyLarge,
            modifier = Modifier
                .fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(112.dp))
    }
}
