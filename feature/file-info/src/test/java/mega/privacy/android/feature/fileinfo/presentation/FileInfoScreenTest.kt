package mega.privacy.android.feature.fileinfo.presentation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.feature.fileinfo.presentation.model.FileInfoUiState
import mega.privacy.android.icon.pack.R as iconPackR
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FileInfoScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val fileState = FileInfoUiState(
        isLoading = false,
        title = "Presentation.pdf",
        isFile = true,
        iconRes = iconPackR.drawable.ic_pdf_medium_solid,
        fileTypeExtension = "pdf",
        sizeInBytes = 10L * 1024 * 1024,
        creationTime = 1_749_000_000L,
        modificationTime = 1_749_500_000L,
    )

    private val folderState = FileInfoUiState(
        isLoading = false,
        title = "New folder",
        isFile = false,
        iconRes = iconPackR.drawable.ic_folder_medium_solid,
        fileTypeExtension = null,
        sizeInBytes = 0L,
        creationTime = 1_749_000_000L,
        modificationTime = null,
    )

    @Test
    fun `test that the header, name and type-size subtitle are displayed for a file`() {
        setContent(uiState = fileState)

        composeRule.onNodeWithTag(FILE_INFO_HEADER_TAG).assertIsDisplayed()
        composeRule.onNodeWithText("Presentation.pdf").assertIsDisplayed()
        composeRule.onNodeWithTag(FILE_INFO_SUBTITLE_TAG).assertTextContains("PDF", substring = true)
    }

    @Test
    fun `test that the added and last modified rows are displayed for a file`() {
        setContent(uiState = fileState)

        composeRule.onNodeWithTag(FILE_INFO_ADDED_TAG).assertExists()
        composeRule.onNodeWithTag(FILE_INFO_LAST_MODIFIED_TAG).assertExists()
    }

    @Test
    fun `test that the type shows Folder for a folder`() {
        setContent(uiState = folderState)

        composeRule.onNodeWithText("New folder").assertIsDisplayed()
        composeRule.onNodeWithTag(FILE_INFO_SUBTITLE_TAG).assertTextContains("Folder", substring = true)
    }

    @Test
    fun `test that a folder shows the added row but not the last modified row`() {
        setContent(uiState = folderState)

        composeRule.onNodeWithTag(FILE_INFO_ADDED_TAG).assertExists()
        composeRule.onNodeWithTag(FILE_INFO_LAST_MODIFIED_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that the loading placeholder is displayed while loading`() {
        setContent(uiState = FileInfoUiState(isLoading = true))

        composeRule.onNodeWithTag(FILE_INFO_LOADING_TAG).assertIsDisplayed()
    }

    private fun setContent(
        uiState: FileInfoUiState,
        onBack: () -> Unit = {},
    ) {
        composeRule.setContent {
            AndroidThemeForPreviews {
                FileInfoScreen(
                    uiState = uiState,
                    onBack = onBack,
                )
            }
        }
    }
}
