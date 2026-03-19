package mega.privacy.android.feature.photos.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import mega.android.core.ui.components.button.PrimaryFilledButton
import mega.android.core.ui.components.button.TextOnlyButtonM3
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.tokens.theme.DSTokens
import mega.privacy.android.icon.pack.R
import mega.privacy.android.shared.resources.R as sharedR

/**
 * The bottom buttons in Select video to playlist screen
 */
@Composable
fun SelectVideosBottomView(
    isAddedButtonEnabled: Boolean,
    onCancelClicked: () -> Unit,
    onAddClicked: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(DSTokens.colors.background.surface1)
            .padding(vertical = DSTokens.spacings.s2, horizontal = DSTokens.spacings.s4)
            .testTag(SELECT_VIDEOS_BOTTOM_VIEW_ROW_TEST_TAG),
        horizontalArrangement = Arrangement.spacedBy(DSTokens.spacings.s4, Alignment.End)
    ) {
        TextOnlyButtonM3(
            modifier = Modifier.testTag(SELECT_VIDEOS_BOTTOM_VIEW_CANCEL_BUTTON_TEST_TAG),
            onClick = onCancelClicked,
            text = stringResource(sharedR.string.general_dialog_cancel_button)
        )

        PrimaryFilledButton(
            modifier = Modifier.testTag(SELECT_VIDEOS_BOTTOM_VIEW_ADD_BUTTON_TEST_TAG),
            text = stringResource(sharedR.string.video_to_playlist_add_button),
            enabled = isAddedButtonEnabled,
            onClick = onAddClicked
        )
    }
}

@CombinedThemePreviews
@Composable
private fun SelectVideosBottomViewPreview() {
    AndroidThemeForPreviews {
        LazyColumn {
            listOf(false, true).forEach { isSelected ->
                item {
                    SelectVideoListItem(
                        title = "Long title Long title Long title Long title Long title Long title",
                        subtitle = "Simple sub title",
                        icon = R.drawable.ic_folder_outgoing_medium_solid,
                        isSelected = isSelected,
                        onItemClicked = { },
                        isAvailableSelected = isSelected,
                        isEnabled = !isSelected
                    )
                }
            }
        }
    }
}

const val SELECT_VIDEOS_BOTTOM_VIEW_ROW_TEST_TAG = "select_videos_bottom_view:view_row"

const val SELECT_VIDEOS_BOTTOM_VIEW_CANCEL_BUTTON_TEST_TAG =
    "select_videos_bottom_view:button_cancel"

const val SELECT_VIDEOS_BOTTOM_VIEW_ADD_BUTTON_TEST_TAG =
    "select_videos_bottom_view:button_add"