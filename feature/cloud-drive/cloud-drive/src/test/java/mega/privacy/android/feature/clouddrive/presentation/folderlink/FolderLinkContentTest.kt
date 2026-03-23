package mega.privacy.android.feature.clouddrive.presentation.folderlink

import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.feature.clouddrive.presentation.clouddrive.view.EMPTY_IMAGE_TAG
import mega.privacy.android.feature.clouddrive.presentation.folderlink.model.FolderLinkContentState
import mega.privacy.android.feature.clouddrive.presentation.folderlink.model.FolderLinkUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class FolderLinkContentTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `test that loading skeleton is displayed when state is Loading`() {
        setupComposeContent(
            uiState = FolderLinkUiState(
                contentState = FolderLinkContentState.Loading,
            )
        )

        composeRule.mainClock.advanceTimeBy(300)
        composeRule.onNodeWithTag(FOLDER_LINK_LOADING_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that decryption key required text is displayed`() {
        setupComposeContent(
            uiState = FolderLinkUiState(
                contentState = FolderLinkContentState.DecryptionKeyRequired(
                    url = "https://mega.nz/folder/abc",
                    isKeyIncorrect = false,
                )
            )
        )

        composeRule.onNodeWithTag(FOLDER_LINK_DECRYPTION_KEY_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that invalid decryption key text is displayed when key is incorrect`() {
        setupComposeContent(
            uiState = FolderLinkUiState(
                contentState = FolderLinkContentState.DecryptionKeyRequired(
                    url = "https://mega.nz/folder/abc",
                    isKeyIncorrect = true,
                )
            )
        )

        composeRule.onNodeWithTag(FOLDER_LINK_DECRYPTION_KEY_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that expired text is displayed when state is Expired`() {
        setupComposeContent(
            uiState = FolderLinkUiState(
                contentState = FolderLinkContentState.Expired,
            )
        )

        composeRule.onNodeWithTag(FOLDER_LINK_EXPIRED_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that unavailable text is displayed when state is Unavailable`() {
        setupComposeContent(
            uiState = FolderLinkUiState(
                contentState = FolderLinkContentState.Unavailable,
            )
        )

        composeRule.onNodeWithTag(FOLDER_LINK_UNAVAILABLE_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that empty view is displayed when Loaded with no items`() {
        setupComposeContent(
            uiState = FolderLinkUiState(
                contentState = FolderLinkContentState.Loaded,
                items = emptyList(),
            )
        )

        composeRule.onNodeWithTag(EMPTY_IMAGE_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that loading tag is not displayed when state is Expired`() {
        setupComposeContent(
            uiState = FolderLinkUiState(
                contentState = FolderLinkContentState.Expired,
            )
        )

        composeRule.onNodeWithTag(FOLDER_LINK_LOADING_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that expired tag is not displayed when state is Loading`() {
        setupComposeContent(
            uiState = FolderLinkUiState(
                contentState = FolderLinkContentState.Loading,
            )
        )

        composeRule.onNodeWithTag(FOLDER_LINK_EXPIRED_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that unavailable tag is not displayed when state is Loading`() {
        setupComposeContent(
            uiState = FolderLinkUiState(
                contentState = FolderLinkContentState.Loading,
            )
        )

        composeRule.onNodeWithTag(FOLDER_LINK_UNAVAILABLE_TAG).assertDoesNotExist()
    }

    private fun setupComposeContent(
        uiState: FolderLinkUiState = FolderLinkUiState(),
    ) {
        composeRule.setContent {
            AndroidThemeForPreviews {
                FolderLinkContent(
                    uiState = uiState,
                    isListView = true,
                    spanCount = 2,
                    onNavigate = {},
                    onAction = {},
                )
            }
        }
    }
}
