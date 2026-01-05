package mega.privacy.android.feature.photos.presentation.playlists.view

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.delay
import mega.android.core.ui.model.menu.MenuActionWithIcon
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@OptIn(ExperimentalMaterial3Api::class)
@Config(sdk = [34])
@RunWith(AndroidJUnit4::class)
class VideoPlaylistBottomSheetTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val testActions = listOf(
        VideoPlaylistsTrashMenuAction(),
        VideoPlaylistRenameMenuAction()
    )

    private fun setComposeContent(
        actions: List<MenuActionWithIcon> = testActions,
        onActionClicked: (MenuActionWithIcon) -> Unit = {},
        onDismissRequest: () -> Unit = {},
        modifier: Modifier = Modifier,
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

            VideoPlaylistBottomSheet(
                actions = actions,
                sheetState = sheetState,
                onActionClicked = onActionClicked,
                onDismissRequest = onDismissRequest,
                modifier = modifier
            )
        }
    }

    @Test
    fun `test that actions are displayed in the bottom sheet`() {
        setComposeContent()

        testActions.forEach { action ->
            composeTestRule
                .onNodeWithTag(action.testTag)
                .assertExists()
                .assertIsDisplayed()
        }
    }
}