package mega.privacy.android.feature.clouddrive.presentation.filelink

import android.os.Build
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.feature.clouddrive.presentation.filelink.model.FileLinkContentState
import mega.privacy.android.feature.clouddrive.presentation.filelink.model.FileLinkUiState
import mega.privacy.android.feature.clouddrive.presentation.publiclink.view.DECRYPTION_KEY_DIALOG_TAG
import mega.privacy.android.icon.pack.R as iconPackR
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
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
    fun `test that loading skeleton is displayed when state is Loading`() {
        setupComposeContent(
            uiState = FileLinkUiState(contentState = FileLinkContentState.Loading)
        )

        composeRule.onNodeWithTag(FILE_LINK_LOADING_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that loading skeleton is not displayed when state is Loaded`() {
        setupComposeContent(uiState = loadedUiState(formattedDuration = null))

        composeRule.onNodeWithTag(FILE_LINK_LOADING_TAG).assertDoesNotExist()
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

    @Test
    fun `test that duration badge is displayed when state is Loaded with non-empty formattedDuration`() {
        setupComposeContent(uiState = loadedUiState(formattedDuration = "2:50"))

        composeRule.onNodeWithTag(FILE_LINK_DURATION_BADGE_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that duration badge is not displayed when state is Loaded with null formattedDuration`() {
        setupComposeContent(uiState = loadedUiState(formattedDuration = null))

        composeRule.onNodeWithTag(FILE_LINK_DURATION_BADGE_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that duration badge is not displayed when state is Loaded with empty formattedDuration`() {
        setupComposeContent(uiState = loadedUiState(formattedDuration = ""))

        composeRule.onNodeWithTag(FILE_LINK_DURATION_BADGE_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that play button is displayed when state is Loaded with isVideo true`() {
        setupComposeContent(uiState = loadedUiState(formattedDuration = "2:50", isVideo = true))

        composeRule.onNodeWithTag(FILE_LINK_PLAY_BUTTON_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that play button is not displayed when state is Loaded with isVideo false`() {
        setupComposeContent(uiState = loadedUiState(formattedDuration = "2:50", isVideo = false))

        composeRule.onNodeWithTag(FILE_LINK_PLAY_BUTTON_TAG).assertDoesNotExist()
    }

    private fun loadedUiState(
        formattedDuration: String?,
        isVideo: Boolean = false,
    ): FileLinkUiState {
        val fileNode: TypedFileNode = mock {
            on { name } doReturn "Hobbiton.mp4"
        }
        return FileLinkUiState(
            contentState = FileLinkContentState.Loaded(
                iconRes = iconPackR.drawable.ic_video_medium_solid,
                thumbnailData = null,
                formattedDuration = formattedDuration,
                isVideo = isVideo,
            ),
            fileNode = fileNode,
        )
    }

    private fun setupComposeContent(uiState: FileLinkUiState) {
        composeRule.setContent {
            AndroidThemeForPreviews {
                FileLinkContent(
                    uiState = uiState,
                    formattedFileSize = "647 MB",
                    onOpenClicked = {},
                    onAction = {},
                    onBack = {},
                )
            }
        }
    }
}
