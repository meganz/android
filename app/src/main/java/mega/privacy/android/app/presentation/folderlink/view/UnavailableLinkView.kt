package mega.privacy.android.app.presentation.folderlink.view

import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.app.R
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.shared.original.core.ui.controls.lists.BulletListView
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.android.shared.original.core.ui.theme.extensions.h6Medium
import mega.privacy.android.shared.original.core.ui.theme.extensions.subtitle1medium
import mega.privacy.android.shared.resources.R as sharedR


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
            style = MaterialTheme.typography.h6Medium,
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
            style = MaterialTheme.typography.subtitle1medium,
        )

        Spacer(modifier = Modifier.height(16.dp))

        BulletListView(
            items = bulletPoints.map { stringResource(it) },
            textStyle = MaterialTheme.typography.body1,
            textColor = TextColor.Secondary,
            spacing = 16.dp,
        )

        if (isInLandscapeMode) {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}


@CombinedThemePreviews
@Composable
private fun UnavailableFolderLinkViewPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        UnavailableLinkView(
            title = sharedR.string.folder_link_unavailable_title,
            subtitle = sharedR.string.general_link_unavailable_subtitle,
            bulletPoints = listOf(
                sharedR.string.folder_link_unavailable_deleted,
                sharedR.string.folder_link_unavailable_disabled,
                sharedR.string.general_link_unavailable_invalid_url,
                R.string.folder_link_unavaible_ToS_violation
            )
        )
    }
}