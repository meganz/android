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
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import mega.privacy.android.feature.sync.R
import mega.privacy.android.feature.sync.navigation.syncNewFolderRoute
import mega.privacy.android.shared.original.core.ui.controls.buttons.OutlinedWithoutBackgroundMegaButton
import mega.privacy.android.shared.original.core.ui.controls.buttons.RaisedDefaultMegaButton
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.preview.BooleanProvider
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.android.shared.original.core.ui.theme.extensions.h6Medium
import mega.privacy.android.shared.original.core.ui.theme.values.TextColor
import mega.privacy.android.shared.resources.R as sharedR
import androidx.compose.foundation.layout.size

/**
 * Composable function to show the bottom sheet for Sync Promotion
 *
 * @param isFreeAccount Indicates if account is Free or not
 * @param upgradeAccountClicked Callback called when upgrade button is tapped
 * @param modifier [Modifier]
 * @param hideSheet Callback called to hide the bottom sheet
 */
@Composable
fun SyncPromotionBottomSheet(
    isFreeAccount: Boolean,
    upgradeAccountClicked: () -> Unit,
    modifier: Modifier = Modifier,
    hideSheet: () -> Unit = {},
) {
    val context = LocalContext.current
    Column(
        modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(R.drawable.no_syncs_placeholder),
            contentDescription = "Sync Promotion bottom sheet image",
            modifier = Modifier
                .padding(top = 28.dp)
                .size(190.dp)
                .testTag(SYNC_PROMOTION_BOTTOM_SHEET_IMAGE_TEST_TAG),
        )
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
        MegaText(
            text = stringResource(sharedR.string.sync_promotion_bottom_sheet_body_message),
            textColor = TextColor.Secondary,
            modifier = Modifier.padding(top = 8.dp),
            style = MaterialTheme.typography.subtitle2,
            textAlign = TextAlign.Center,
        )
        RaisedDefaultMegaButton(
            text = if (isFreeAccount) {
                stringResource(sharedR.string.sync_promotion_bottom_sheet_primary_button_text_free)
            } else {
                stringResource(sharedR.string.sync_promotion_bottom_sheet_primary_button_text_pro)
            },
            onClick = {
                if (isFreeAccount) {
                    upgradeAccountClicked()
                } else {
                    context.startActivity(Intent(Intent.ACTION_VIEW).apply {
                        data = ADD_NEW_SYNC_URI.toUri()
                    })
                }
                hideSheet()
            },
            modifier = Modifier
                .padding(top = 24.dp)
                .fillMaxWidth(),
        )
        OutlinedWithoutBackgroundMegaButton(
            text = stringResource(sharedR.string.sync_promotion_bottom_sheet_secondary_button_text),
            onClick = {
                context.startActivity(Intent(Intent.ACTION_VIEW).apply {
                    data = LEARN_MORE_URI.toUri()
                })
                hideSheet()
            },
            rounded = false,
            modifier = Modifier
                .padding(top = 16.dp, bottom = 28.dp)
                .fillMaxWidth(),
        )
    }
}

@CombinedThemePreviews
@Composable
private fun UpgradeProPlanBottomSheetPreview(
    @PreviewParameter(BooleanProvider::class) isFreeAccount: Boolean,
) {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        SyncPromotionBottomSheet(
            isFreeAccount = isFreeAccount,
            upgradeAccountClicked = {}
        )
    }
}

private const val ADD_NEW_SYNC_URI = "https://mega.nz/$syncNewFolderRoute"
private const val LEARN_MORE_URI = "https://mega.io/syncing"

internal const val SYNC_PROMOTION_BOTTOM_SHEET_IMAGE_TEST_TAG = "sync_promotion_bottom_sheet:image"