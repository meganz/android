package mega.privacy.android.feature.sync.ui.views

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import mega.privacy.android.feature.sync.R
import mega.privacy.android.shared.original.core.ui.controls.buttons.RaisedDefaultMegaButton
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.android.shared.original.core.ui.theme.extensions.h6Medium
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.shared.resources.R as sharedR
import android.content.res.Configuration
import androidx.compose.foundation.layout.size
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextDecoration
import mega.android.core.ui.model.MegaSpanStyle.LinkColorStyle
import mega.android.core.ui.theme.values.LinkColor
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.shared.original.core.ui.controls.text.MegaSpannedClickableText
import mega.privacy.android.shared.original.core.ui.model.MegaSpanStyle
import mega.privacy.android.shared.original.core.ui.model.MegaSpanStyleWithAnnotation
import mega.privacy.android.shared.original.core.ui.model.SpanIndicator
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePhoneLandscapePreviews
import mega.privacy.mobile.analytics.event.SyncPromotionBottomSheetLearnMoreButtonPressedEvent
import mega.privacy.mobile.analytics.event.SyncPromotionBottomSheetSyncFoldersButtonPressedEvent

/**
 * Composable function to show the bottom sheet for Sync Promotion
 *
 * @param onSyncFoldersClicked Callback called when "Sync folders" button is tapped
 * @param onBackUpFoldersClicked Callback called when "Back up folders" button is tapped
 * @param onLearnMoreClicked Callback called when "Learn more" button is tapped
 * @param modifier [Modifier]
 * @param hideSheet Callback called to hide the bottom sheet
 */
@Composable
fun SyncPromotionBottomSheet(
    onSyncFoldersClicked: () -> Unit,
    onBackUpFoldersClicked: () -> Unit,
    onLearnMoreClicked: () -> Unit,
    modifier: Modifier = Modifier,
    hideSheet: () -> Unit = {},
) {
    val context = LocalContext.current
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    Column(
        modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (!isLandscape) {
            Image(
                painter = painterResource(R.drawable.no_syncs_placeholder),
                contentDescription = "Sync Promotion bottom sheet image",
                modifier = Modifier
                    .padding(top = 28.dp)
                    .size(190.dp)
                    .testTag(SYNC_PROMOTION_BOTTOM_SHEET_IMAGE_TEST_TAG),
            )
        }
        MegaText(
            text = stringResource(sharedR.string.sync_promotion_bottom_sheet_whats_new_label),
            textColor = TextColor.Error,
            modifier = Modifier.padding(top = 28.dp),
            style = MaterialTheme.typography.subtitle2,
        )
        MegaText(
            text = stringResource(sharedR.string.sync_promotion_bottom_sheet_title),
            textColor = TextColor.Primary,
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.h6Medium,
        )
        MegaSpannedClickableText(
            value = stringResource(sharedR.string.sync_promotion_bottom_sheet_body_text),
            styles = mapOf(
                SpanIndicator('U') to MegaSpanStyleWithAnnotation(
                    megaSpanStyle = MegaSpanStyle(
                        spanStyle = LinkColorStyle(
                            SpanStyle(textDecoration = TextDecoration.Underline),
                            LinkColor.Primary
                        ).spanStyle,
                        color = TextColor.Info
                    ),
                    annotation = "Tap to learn more",
                )
            ),
            color = TextColor.Secondary,
            onAnnotationClick = {
                Analytics.tracker.trackEvent(SyncPromotionBottomSheetLearnMoreButtonPressedEvent)
                onLearnMoreClicked()
                context.startActivity(Intent(Intent.ACTION_VIEW).apply {
                    data = LEARN_MORE_URI.toUri()
                })
                hideSheet()
            },
            modifier = Modifier
                .padding(top = 8.dp)
                .align(Alignment.CenterHorizontally),
            baseStyle = MaterialTheme.typography.subtitle2.copy(textAlign = TextAlign.Center),
        )
        RaisedDefaultMegaButton(
            text = stringResource(sharedR.string.sync_promotion_bottom_sheet_primary_button_text_pro),
            onClick = {
                Analytics.tracker.trackEvent(SyncPromotionBottomSheetSyncFoldersButtonPressedEvent)
                onSyncFoldersClicked()
                hideSheet()
            },
            modifier = Modifier
                .padding(top = 32.dp)
                .fillMaxWidth(),
        )
        RaisedDefaultMegaButton(
            text = stringResource(sharedR.string.sync_promotion_bottom_sheet_secondary_button_text),
            onClick = {
                onBackUpFoldersClicked()
                hideSheet()
            },
            modifier = Modifier
                .padding(top = 16.dp, bottom = 28.dp)
                .fillMaxWidth(),
        )
    }
}

@CombinedThemePreviews
@CombinedThemePhoneLandscapePreviews
@Composable
private fun SyncPromotionBottomSheetPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        SyncPromotionBottomSheet(
            onSyncFoldersClicked = {},
            onBackUpFoldersClicked = {},
            onLearnMoreClicked = {},
        )
    }
}

private const val LEARN_MORE_URI = "https://mega.io/syncing"

internal const val SYNC_PROMOTION_BOTTOM_SHEET_IMAGE_TEST_TAG = "sync_promotion_bottom_sheet:image"