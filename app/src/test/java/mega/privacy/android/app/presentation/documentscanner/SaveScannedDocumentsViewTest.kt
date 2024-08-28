package mega.privacy.android.app.presentation.documentscanner

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.presentation.documentscanner.SAVE_SCANNED_DOCUMENTS_TOOLBAR
import mega.privacy.android.app.presentation.documentscanner.SaveScannedDocumentsView
import mega.privacy.android.app.presentation.documentscanner.model.SaveScannedDocumentsUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Test class for [SaveScannedDocumentsView]
 */
@RunWith(AndroidJUnit4::class)
internal class SaveScannedDocumentsViewTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that the ui components are displayed`() {
        composeTestRule.setContent {
            SaveScannedDocumentsView(
                uiState = SaveScannedDocumentsUiState(
                    filename = "PDF"
                )
            )
        }

        composeTestRule.onNodeWithTag(SAVE_SCANNED_DOCUMENTS_TOOLBAR).assertIsDisplayed()
    }
}