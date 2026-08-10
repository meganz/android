package mega.privacy.android.app.presentation.videosection

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.videosection.model.FavouritesPlaylistBottomSheetOption
import mega.privacy.android.app.presentation.videosection.view.playlist.FAVOURITES_PLAYLIST_DOWNLOAD_BOTTOM_SHEET_TILE_TEST_TAG
import mega.privacy.android.app.presentation.videosection.view.playlist.FAVOURITES_PLAYLIST_HIDE_BOTTOM_SHEET_TILE_TEST_TAG
import mega.privacy.android.app.presentation.videosection.view.playlist.FAVOURITES_PLAYLIST_REMOVE_FAVOURITE_BOTTOM_SHEET_TILE_TEST_TAG
import mega.privacy.android.app.presentation.videosection.view.playlist.FAVOURITES_PLAYLIST_SEND_TO_CHAT_BOTTOM_SHEET_TILE_TEST_TAG
import mega.privacy.android.app.presentation.videosection.view.playlist.FAVOURITES_PLAYLIST_SHARE_BOTTOM_SHEET_TILE_TEST_TAG
import mega.privacy.android.app.presentation.videosection.view.playlist.FAVOURITES_PLAYLIST_UNHIDE_BOTTOM_SHEET_TILE_TEST_TAG
import mega.privacy.android.app.presentation.videosection.view.playlist.FavouritesPlaylistBottomSheet
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.kotlin.verify

@OptIn(ExperimentalMaterial3Api::class)
@RunWith(AndroidJUnit4::class)
class FavouritesPlaylistBottomSheetKtTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setComposeContent(
        isHideMenuActionVisible: Boolean = false,
        isUnhideMenuActionVisible: Boolean = false,
        onBottomSheetOptionClicked: (FavouritesPlaylistBottomSheetOption) -> Unit = {},
        onDismissRequest: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            val sheetState = rememberModalBottomSheetState(
                skipPartiallyExpanded = true,
                confirmValueChange = { true }
            )

            LaunchedEffect(Unit) {
                delay(500)
                sheetState.show()
            }

            FavouritesPlaylistBottomSheet(
                sheetState = sheetState,
                isHideMenuActionVisible = isHideMenuActionVisible,
                isUnhideMenuActionVisible = isUnhideMenuActionVisible,
                onBottomSheetOptionClicked = onBottomSheetOptionClicked,
                onDismissRequest = onDismissRequest
            )
        }
    }

    @Test
    fun `test that options are displayed correctly when isHideMenuActionVisible and isUnhideMenuActionVisible are false`() =
        runTest {
            setComposeContent()
            listOf(
                FAVOURITES_PLAYLIST_DOWNLOAD_BOTTOM_SHEET_TILE_TEST_TAG,
                FAVOURITES_PLAYLIST_SHARE_BOTTOM_SHEET_TILE_TEST_TAG,
                FAVOURITES_PLAYLIST_SEND_TO_CHAT_BOTTOM_SHEET_TILE_TEST_TAG,
                FAVOURITES_PLAYLIST_REMOVE_FAVOURITE_BOTTOM_SHEET_TILE_TEST_TAG
            ).onEach {
                it.assertIsDisplayed()
            }
        }

    private fun String.assertIsDisplayed() =
        composeTestRule.onNodeWithTag(testTag = this, useUnmergedTree = true).assertIsDisplayed()

    @Test
    fun `test that options are displayed correctly when isHideMenuActionVisible is true`() =
        runTest {
            setComposeContent(isHideMenuActionVisible = true)
            listOf(
                FAVOURITES_PLAYLIST_DOWNLOAD_BOTTOM_SHEET_TILE_TEST_TAG,
                FAVOURITES_PLAYLIST_SHARE_BOTTOM_SHEET_TILE_TEST_TAG,
                FAVOURITES_PLAYLIST_SEND_TO_CHAT_BOTTOM_SHEET_TILE_TEST_TAG,
                FAVOURITES_PLAYLIST_REMOVE_FAVOURITE_BOTTOM_SHEET_TILE_TEST_TAG,
                FAVOURITES_PLAYLIST_HIDE_BOTTOM_SHEET_TILE_TEST_TAG
            ).onEach {
                it.assertIsDisplayed()
            }
            FAVOURITES_PLAYLIST_UNHIDE_BOTTOM_SHEET_TILE_TEST_TAG.assertIsNotDisplayed()
        }

    private fun String.assertIsNotDisplayed() =
        composeTestRule.onNodeWithTag(testTag = this, useUnmergedTree = true).assertIsNotDisplayed()

    @Test
    fun `test that options are displayed correctly when isUnhideMenuActionVisible is true`() =
        runTest {
            setComposeContent(isUnhideMenuActionVisible = true)
            listOf(
                FAVOURITES_PLAYLIST_DOWNLOAD_BOTTOM_SHEET_TILE_TEST_TAG,
                FAVOURITES_PLAYLIST_SHARE_BOTTOM_SHEET_TILE_TEST_TAG,
                FAVOURITES_PLAYLIST_SEND_TO_CHAT_BOTTOM_SHEET_TILE_TEST_TAG,
                FAVOURITES_PLAYLIST_REMOVE_FAVOURITE_BOTTOM_SHEET_TILE_TEST_TAG,
                FAVOURITES_PLAYLIST_UNHIDE_BOTTOM_SHEET_TILE_TEST_TAG
            ).onEach {
                it.assertIsDisplayed()
            }
            FAVOURITES_PLAYLIST_HIDE_BOTTOM_SHEET_TILE_TEST_TAG.assertIsNotDisplayed()
        }

    @Test
    fun `test that options are displayed correctly when isHideMenuActionVisible and isUnhideMenuActionVisible are true`() =
        runTest {
            setComposeContent(isHideMenuActionVisible = true, isUnhideMenuActionVisible = true)
            listOf(
                FAVOURITES_PLAYLIST_DOWNLOAD_BOTTOM_SHEET_TILE_TEST_TAG,
                FAVOURITES_PLAYLIST_SHARE_BOTTOM_SHEET_TILE_TEST_TAG,
                FAVOURITES_PLAYLIST_SEND_TO_CHAT_BOTTOM_SHEET_TILE_TEST_TAG,
                FAVOURITES_PLAYLIST_REMOVE_FAVOURITE_BOTTOM_SHEET_TILE_TEST_TAG,
                FAVOURITES_PLAYLIST_UNHIDE_BOTTOM_SHEET_TILE_TEST_TAG,
                FAVOURITES_PLAYLIST_HIDE_BOTTOM_SHEET_TILE_TEST_TAG
            ).onEach {
                it.assertIsDisplayed()
            }
        }

    @Test
    fun `test that onPlaylistBottomSheetOptionClicked is invoked with Hide option`() =
        runTest {
            val onBottomSheetOptionClicked =
                mock<(FavouritesPlaylistBottomSheetOption) -> Unit>()
            setComposeContent(
                isHideMenuActionVisible = true,
                isUnhideMenuActionVisible = true,
                onBottomSheetOptionClicked = onBottomSheetOptionClicked
            )

            val tagNode = composeTestRule.onNodeWithTag(
                testTag = FAVOURITES_PLAYLIST_HIDE_BOTTOM_SHEET_TILE_TEST_TAG,
                useUnmergedTree = true
            )
            tagNode.assertIsDisplayed()

            tagNode
                .assert(hasClickAction())
                .performSemanticsAction(SemanticsActions.OnClick)

            composeTestRule.waitForIdle()

            verify(onBottomSheetOptionClicked).invoke(FavouritesPlaylistBottomSheetOption.Hide)
        }

    @Test
    fun `test that onPlaylistBottomSheetOptionClicked is invoked with Unhide option`() =
        runTest {
            val onBottomSheetOptionClicked =
                mock<(FavouritesPlaylistBottomSheetOption) -> Unit>()
            setComposeContent(
                isHideMenuActionVisible = true,
                isUnhideMenuActionVisible = true,
                onBottomSheetOptionClicked = onBottomSheetOptionClicked
            )

            val tagNode = composeTestRule.onNodeWithTag(
                testTag = FAVOURITES_PLAYLIST_UNHIDE_BOTTOM_SHEET_TILE_TEST_TAG,
                useUnmergedTree = true
            )
            tagNode.assertIsDisplayed()

            tagNode
                .assert(hasClickAction())
                .performSemanticsAction(SemanticsActions.OnClick)

            composeTestRule.waitForIdle()

            verify(onBottomSheetOptionClicked).invoke(FavouritesPlaylistBottomSheetOption.Unhide)
        }

    @Test
    fun `test that onPlaylistBottomSheetOptionClicked is invoked with Download option`() =
        runTest {
            val onBottomSheetOptionClicked =
                mock<(FavouritesPlaylistBottomSheetOption) -> Unit>()
            setComposeContent(
                isHideMenuActionVisible = true,
                isUnhideMenuActionVisible = true,
                onBottomSheetOptionClicked = onBottomSheetOptionClicked
            )

            val tagNode = composeTestRule.onNodeWithTag(
                testTag = FAVOURITES_PLAYLIST_DOWNLOAD_BOTTOM_SHEET_TILE_TEST_TAG,
                useUnmergedTree = true
            )
            tagNode.assertIsDisplayed()

            tagNode
                .assert(hasClickAction())
                .performSemanticsAction(SemanticsActions.OnClick)

            composeTestRule.waitForIdle()

            verify(onBottomSheetOptionClicked).invoke(FavouritesPlaylistBottomSheetOption.Download)
        }

    @Test
    fun `test that onPlaylistBottomSheetOptionClicked is invoked with SendToChat option`() =
        runTest {
            val onBottomSheetOptionClicked =
                mock<(FavouritesPlaylistBottomSheetOption) -> Unit>()
            setComposeContent(
                isHideMenuActionVisible = true,
                isUnhideMenuActionVisible = true,
                onBottomSheetOptionClicked = onBottomSheetOptionClicked
            )

            val tagNode = composeTestRule.onNodeWithTag(
                testTag = FAVOURITES_PLAYLIST_SEND_TO_CHAT_BOTTOM_SHEET_TILE_TEST_TAG,
                useUnmergedTree = true
            )
            tagNode.assertIsDisplayed()

            tagNode
                .assert(hasClickAction())
                .performSemanticsAction(SemanticsActions.OnClick)

            composeTestRule.waitForIdle()

            verify(onBottomSheetOptionClicked).invoke(FavouritesPlaylistBottomSheetOption.SendToChat)
        }

    @Test
    fun `test that onPlaylistBottomSheetOptionClicked is invoked with Share option`() =
        runTest {
            val onBottomSheetOptionClicked =
                mock<(FavouritesPlaylistBottomSheetOption) -> Unit>()
            setComposeContent(
                isHideMenuActionVisible = true,
                isUnhideMenuActionVisible = true,
                onBottomSheetOptionClicked = onBottomSheetOptionClicked
            )

            val tagNode = composeTestRule.onNodeWithTag(
                testTag = FAVOURITES_PLAYLIST_SHARE_BOTTOM_SHEET_TILE_TEST_TAG,
                useUnmergedTree = true
            )
            tagNode.assertIsDisplayed()

            tagNode
                .assert(hasClickAction())
                .performSemanticsAction(SemanticsActions.OnClick)

            composeTestRule.waitForIdle()

            verify(onBottomSheetOptionClicked).invoke(FavouritesPlaylistBottomSheetOption.Share)
        }

    @Test
    fun `test that onPlaylistBottomSheetOptionClicked is invoked with RemoveFavourite option`() =
        runTest {
            val onBottomSheetOptionClicked =
                mock<(FavouritesPlaylistBottomSheetOption) -> Unit>()
            setComposeContent(
                isHideMenuActionVisible = true,
                isUnhideMenuActionVisible = true,
                onBottomSheetOptionClicked = onBottomSheetOptionClicked
            )

            val tagNode = composeTestRule.onNodeWithTag(
                testTag = FAVOURITES_PLAYLIST_REMOVE_FAVOURITE_BOTTOM_SHEET_TILE_TEST_TAG,
                useUnmergedTree = true
            )
            tagNode.assertIsDisplayed()

            tagNode
                .assert(hasClickAction())
                .performSemanticsAction(SemanticsActions.OnClick)

            composeTestRule.waitForIdle()

            verify(onBottomSheetOptionClicked).invoke(FavouritesPlaylistBottomSheetOption.RemoveFavourite)
        }
}