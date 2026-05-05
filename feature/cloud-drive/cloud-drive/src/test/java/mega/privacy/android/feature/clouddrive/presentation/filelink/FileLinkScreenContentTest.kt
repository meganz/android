package mega.privacy.android.feature.clouddrive.presentation.filelink

import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.feature.clouddrive.presentation.filelink.model.FileLinkContentState
import mega.privacy.android.feature.clouddrive.presentation.filelink.model.FileLinkUiState
import mega.privacy.android.feature.clouddrive.presentation.publiclink.view.DECRYPTION_KEY_DIALOG_TAG
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [Build.VERSION_CODES.Q])
class FileLinkScreenContentTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `test that decryption key dialog is displayed when state is DecryptionKeyRequired`() {
        setupComposeContent(
            uiState = FileLinkUiState(
                contentState = FileLinkContentState.DecryptionKeyRequired(
                    url = "https://mega.nz/file/abc",
                    isKeyIncorrect = false,
                )
            )
        )

        composeRule.onNodeWithTag(DECRYPTION_KEY_DIALOG_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that decryption key dialog is displayed when key is incorrect`() {
        setupComposeContent(
            uiState = FileLinkUiState(
                contentState = FileLinkContentState.DecryptionKeyRequired(
                    url = "https://mega.nz/file/abc",
                    isKeyIncorrect = true,
                )
            )
        )

        composeRule.onNodeWithTag(DECRYPTION_KEY_DIALOG_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that expired view is displayed when state is Expired`() {
        setupComposeContent(
            uiState = FileLinkUiState(contentState = FileLinkContentState.Expired)
        )

        composeRule.onNodeWithTag(FILE_LINK_EXPIRED_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that unavailable view is displayed when state is Unavailable`() {
        setupComposeContent(
            uiState = FileLinkUiState(contentState = FileLinkContentState.Unavailable)
        )

        composeRule.onNodeWithTag(FILE_LINK_UNAVAILABLE_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that expired tag is not displayed when state is Loading`() {
        setupComposeContent(
            uiState = FileLinkUiState(contentState = FileLinkContentState.Loading)
        )

        composeRule.onNodeWithTag(FILE_LINK_EXPIRED_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that unavailable tag is not displayed when state is Loading`() {
        setupComposeContent(
            uiState = FileLinkUiState(contentState = FileLinkContentState.Loading)
        )

        composeRule.onNodeWithTag(FILE_LINK_UNAVAILABLE_TAG).assertDoesNotExist()
    }

    private fun setupComposeContent(uiState: FileLinkUiState) {
        composeRule.setContent {
            AndroidThemeForPreviews {
                FileLinkContent(
                    uiState = uiState,
                    formattedFileSize = "",
                    onOpenClicked = {},
                    onAction = {},
                    onBack = {},
                )
            }
        }
    }
}
