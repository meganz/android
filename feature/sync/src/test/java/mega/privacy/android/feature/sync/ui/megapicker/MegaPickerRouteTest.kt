package mega.privacy.android.feature.sync.ui.megapicker

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.StateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class MegaPickerRouteTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val viewModel: MegaPickerViewModel = mock()
    private val state: StateFlow<MegaPickerState> = mock()

    @Test
    fun `test that empty state is displayed when the folder is empty`() {
        whenever(state.value).thenReturn(MegaPickerState(nodes = emptyList()))
        whenever(viewModel.state).thenReturn(state)
        composeTestRule.setContent {
            MegaPickerRoute(
                viewModel = viewModel,
                syncPermissionsManager = mock(),
                folderSelected = {},
                backClicked = {},
                fileTypeIconMapper = mock(),
            )
        }

        composeTestRule.onNodeWithTag(TAG_SYNC_MEGA_FOLDER_PICKER_LIST_SCREEN_NO_ITEMS)
            .assertIsDisplayed()
    }

    @Test
    fun `test that loading state is displayed at init when the nodes list is null`() {
        whenever(state.value).thenReturn(MegaPickerState())
        whenever(viewModel.state).thenReturn(state)
        composeTestRule.setContent {
            MegaPickerRoute(
                viewModel = viewModel,
                syncPermissionsManager = mock(),
                folderSelected = {},
                backClicked = {},
                fileTypeIconMapper = mock(),
            )
        }

        composeTestRule.onNodeWithTag(TAG_SYNC_MEGA_FOLDER_PICKER_LOADING_SATE)
            .assertIsDisplayed()
    }

    @Test
    fun `test that loading state is displayed when isLoading state property is set`() {
        whenever(state.value).thenReturn(MegaPickerState(isLoading = true))
        whenever(viewModel.state).thenReturn(state)
        composeTestRule.setContent {
            MegaPickerRoute(
                viewModel = viewModel,
                syncPermissionsManager = mock(),
                folderSelected = {},
                backClicked = {},
                fileTypeIconMapper = mock(),
            )
        }

        composeTestRule.onNodeWithTag(TAG_SYNC_MEGA_FOLDER_PICKER_LOADING_SATE)
            .assertIsDisplayed()
    }
}