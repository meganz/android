package test.mega.privacy.android.app.exportRK

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidTest
import mega.privacy.android.app.R
import mega.privacy.android.app.exportRK.model.RecoveryKeyUIState
import mega.privacy.android.app.exportRK.view.COLUMN_TEST_TAG
import mega.privacy.android.app.exportRK.view.ExportRecoveryKeyView
import mega.privacy.android.app.exportRK.view.ROW_TEST_TAG
import mega.privacy.android.app.exportRK.view.SNACKBAR_TEST_TAG
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import test.mega.privacy.android.app.onNodeWithText

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ExportRecoveryKeyComposeViewTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that all compose view child are visible with correct value on load`() {
        setComposeContent()

        with(composeTestRule) {
            onNodeWithText(R.string.backup_title).assertIsDisplayed()
            onNodeWithText(R.string.backup_subtitle).assertIsDisplayed()
            onNodeWithText(R.string.backup_first_paragraph).assertIsDisplayed()
            onNodeWithText(R.string.backup_second_paragraph).assertIsDisplayed()
            onNodeWithText(R.string.backup_third_paragraph).assertIsDisplayed()
            onNodeWithText(R.string.backup_action).assertIsDisplayed()
            onNodeWithText(R.string.context_option_print).assertIsDisplayed()
            onNodeWithText(R.string.context_copy).assertIsDisplayed()
            onNodeWithText(R.string.save_action).assertIsDisplayed()
        }
    }

    @Test
    fun `test that button group layout should be horizontal button group has enough space`() {
        setComposeContent()

        verifyActionGroupOrientation(
            verticalColumnMatcher = { assertDoesNotExist() },
            horizontalRowMatcher = { assertExists() }
        )
    }

    @Test
    @Config(qualifiers = "fr-rFr-w320dp-h471dp")
    fun `test that button group layout should be vertical when any button is overflowing`() {
        setComposeContent(uiState = RecoveryKeyUIState(isActionGroupVertical = true))

        verifyActionGroupOrientation(
            verticalColumnMatcher = { assertExists() },
            horizontalRowMatcher = { assertDoesNotExist() }
        )
    }

    @Test
    fun `test that snackbar should show if message is not empty`() {
        setComposeContent(uiState = RecoveryKeyUIState(snackBarMessage = "Message"))

        composeTestRule.onNodeWithTag(SNACKBAR_TEST_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that snackbar not show when message empty`() {
        setComposeContent(uiState = RecoveryKeyUIState(snackBarMessage = null))

        composeTestRule.onNodeWithTag(SNACKBAR_TEST_TAG).assertDoesNotExist()
    }

    private fun setComposeContent(uiState: RecoveryKeyUIState = RecoveryKeyUIState()) {
        composeTestRule.setContent {
            ExportRecoveryKeyView(
                uiState = uiState,
                onSnackBarShown = {},
                onButtonOverflow = {},
                onClickPrint = {},
                onClickCopy = {},
                onClickSave = {}
            )
        }
    }

    private fun verifyActionGroupOrientation(
        verticalColumnMatcher: SemanticsNodeInteraction.() -> Unit,
        horizontalRowMatcher: SemanticsNodeInteraction.() -> Unit,
    ) {
        composeTestRule.onNodeWithTag(COLUMN_TEST_TAG).let(verticalColumnMatcher)
        composeTestRule.onNodeWithTag(ROW_TEST_TAG).let(horizontalRowMatcher)
    }
}