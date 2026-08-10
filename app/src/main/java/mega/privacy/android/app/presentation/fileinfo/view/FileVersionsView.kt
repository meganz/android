package mega.privacy.android.app.presentation.fileinfo.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.app.R
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.original.core.ui.controls.images.MegaIcon
import mega.privacy.android.shared.original.core.ui.preview.CombinedTextAndThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme

/**
 * Show a clickable button with the total amount of versions of this file
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun FileVersionsView(
    versions: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    textStartPadding: Dp = 72.dp,
) = Row(
    modifier = modifier
        .fillMaxWidth()
        .height(48.dp),
    verticalAlignment = Alignment.CenterVertically,
) {
    Box(
        modifier = Modifier
            .sizeIn(minWidth = textStartPadding)
            .padding(start = 16.dp)
    ) {
        MegaIcon(
            painter = rememberVectorPainter(IconPack.Medium.Thin.Outline.ClockRotate),
            contentDescription = "versions icon"
        )
    }
    MegaText(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(8.dp)
            .widthIn(min = 80.dp)
            .testTag(TEST_TAG_VERSIONS_BUTTON),
        text = pluralStringResource(
            id = R.plurals.number_of_versions,
            count = versions,
            versions
        ),
        style = AppTheme.typography.bodyMedium,
        textColor = TextColor.Secondary
    )
}

/**
 * Preview for [FileVersionsView]
 */
@CombinedTextAndThemePreviews
@Composable
private fun FileVersionsPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        FileVersionsView(
            versions = 5,
            onClick = {})
    }
}