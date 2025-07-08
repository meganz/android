package mega.privacy.android.app.presentation.videosection.view.playlist

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.videosection.model.FavouritesPlaylistBottomSheetOption
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.shared.original.core.ui.controls.lists.MenuActionListTile
import mega.privacy.android.shared.original.core.ui.controls.sheets.BottomSheet
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.resources.R as sharedR

@Composable
internal fun FavouritesPlaylistBottomSheet(
    modalSheetState: ModalBottomSheetState,
    coroutineScope: CoroutineScope,
    isHideMenuActionVisible: Boolean,
    isUnhideMenuActionVisible: Boolean,
    onBottomSheetOptionClicked: (FavouritesPlaylistBottomSheetOption) -> Unit,
) {
    BottomSheet(modalSheetState = modalSheetState,
        sheetBody = {
            FavouritesPlaylistBottomSheetBody(
                isHideMenuActionVisible,
                isUnhideMenuActionVisible,
                onOptionClicked = { option ->
                    coroutineScope.launch { modalSheetState.hide() }
                    onBottomSheetOptionClicked(option)
                }
            )
        }
    )
}

@Composable
internal fun FavouritesPlaylistBottomSheetBody(
    isHideMenuActionVisible: Boolean,
    isUnhideMenuActionVisible: Boolean,
    onOptionClicked: (FavouritesPlaylistBottomSheetOption) -> Unit,
) {
    Column {
        getBottomSheetActions()
            .filter { info ->
                when (info.action) {
                    FavouritesPlaylistBottomSheetOption.Hide -> isHideMenuActionVisible
                    FavouritesPlaylistBottomSheetOption.Unhide -> isUnhideMenuActionVisible
                    else -> true
                }
            }
            .forEach { info ->
                BottomSheetItem(
                    info = info,
                    onClick = onOptionClicked
                )
            }
    }
}

/**
 * Bottom sheet action info
 *
 * @property text option text
 * @property icon option icon
 * @property action option action
 * @property testTag option test tage
 */
private data class BottomSheetActionInfo(
    @StringRes val text: Int,
    @DrawableRes val icon: Int,
    val action: FavouritesPlaylistBottomSheetOption,
    val testTag: String,
)

private fun getBottomSheetActions() =
    listOf(
        BottomSheetActionInfo(
            text = R.string.general_save_to_device,
            icon = iconPackR.drawable.ic_download_medium_thin_outline,
            action = FavouritesPlaylistBottomSheetOption.Download,
            testTag = FAVOURITES_PLAYLIST_DOWNLOAD_BOTTOM_SHEET_TILE_TEST_TAG,
        ),
        BottomSheetActionInfo(
            text = R.string.context_send_file_to_chat,
            icon = iconPackR.drawable.ic_message_arrow_up_medium_thin_outline,
            action = FavouritesPlaylistBottomSheetOption.SendToChat,
            testTag = FAVOURITES_PLAYLIST_SEND_TO_CHAT_BOTTOM_SHEET_TILE_TEST_TAG,
        ),
        BottomSheetActionInfo(
            text = R.string.general_share,
            icon = iconPackR.drawable.ic_share_network_medium_thin_outline,
            action = FavouritesPlaylistBottomSheetOption.Share,
            testTag = FAVOURITES_PLAYLIST_SHARE_BOTTOM_SHEET_TILE_TEST_TAG,
        ),
        BottomSheetActionInfo(
            text = sharedR.string.favourites_playlist_bottom_sheet_option_remove_favourite,
            icon = iconPackR.drawable.ic_bottom_sheet_option_remove_favourite,
            action = FavouritesPlaylistBottomSheetOption.RemoveFavourite,
            testTag = FAVOURITES_PLAYLIST_REMOVE_FAVOURITE_BOTTOM_SHEET_TILE_TEST_TAG,
        ),
        BottomSheetActionInfo(
            text = R.string.general_hide_node,
            icon = iconPackR.drawable.ic_bottom_sheet_option_hide,
            action = FavouritesPlaylistBottomSheetOption.Hide,
            testTag = FAVOURITES_PLAYLIST_HIDE_BOTTOM_SHEET_TILE_TEST_TAG,
        ),
        BottomSheetActionInfo(
            text = R.string.general_unhide_node,
            icon = iconPackR.drawable.ic_bottom_sheet_option_unhide,
            action = FavouritesPlaylistBottomSheetOption.Unhide,
            testTag = FAVOURITES_PLAYLIST_UNHIDE_BOTTOM_SHEET_TILE_TEST_TAG,
        ),
    )

@Composable
private fun BottomSheetItem(
    info: BottomSheetActionInfo,
    onClick: (FavouritesPlaylistBottomSheetOption) -> Unit,
) {
    MenuActionListTile(
        text = stringResource(id = info.text),
        icon = painterResource(id = info.icon),
        dividerType = null,
        modifier = Modifier.testTag(info.testTag),
        onActionClicked = { onClick(info.action) }
    )
}

@CombinedThemePreviews
@Composable
private fun FavouritesPlaylistBottomSheetBodyPreview() {
    FavouritesPlaylistBottomSheetBody(
        isHideMenuActionVisible = true,
        isUnhideMenuActionVisible = true,
        onOptionClicked = {}
    )
}

/**
 * Test tag for hide bottom Sheet tile
 */
const val FAVOURITES_PLAYLIST_HIDE_BOTTOM_SHEET_TILE_TEST_TAG =
    "favourites_playlist:bottom_sheet_tile_hide"

/**
 * Test tag for unhide bottom Sheet tile
 */
const val FAVOURITES_PLAYLIST_UNHIDE_BOTTOM_SHEET_TILE_TEST_TAG =
    "favourites_playlist:bottom_sheet_tile_unhide"

/**
 * Test tag for download bottom Sheet tile
 */
const val FAVOURITES_PLAYLIST_DOWNLOAD_BOTTOM_SHEET_TILE_TEST_TAG =
    "favourites_playlist:bottom_sheet_tile_download"

/**
 * Test tag for share bottom Sheet tile
 */
const val FAVOURITES_PLAYLIST_SHARE_BOTTOM_SHEET_TILE_TEST_TAG =
    "favourites_playlist:bottom_sheet_tile_share"

/**
 * Test tag for send to chat bottom Sheet tile
 */
const val FAVOURITES_PLAYLIST_SEND_TO_CHAT_BOTTOM_SHEET_TILE_TEST_TAG =
    "favourites_playlist:bottom_sheet_tile_send_to_chat"

/**
 * Test tag for remove favourite bottom Sheet tile
 */
const val FAVOURITES_PLAYLIST_REMOVE_FAVOURITE_BOTTOM_SHEET_TILE_TEST_TAG =
    "favourites_playlist:bottom_sheet_tile_remove_favourite"

