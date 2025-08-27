package mega.privacy.android.core.nodecomponents.dialog.rename

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import de.palm.composestateevents.triggered
import mega.privacy.android.core.nodecomponents.R
import mega.privacy.android.shared.resources.R as sharedResR
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class RenameNodeDialogM3Test {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that onDismiss is called when cancel button is clicked`() {
        val mockUiState = RenameNodeDialogState(nodeName = "test.txt")
        val mockOnLoadNodeName = mock<() -> Unit>()
        val mockOnDismiss = mock<() -> Unit>()

        composeTestRule.setContent {
            RenameNodeDialogM3View(
                uiState = mockUiState,
                onLoadNodeName = mockOnLoadNodeName,
                resetRenameValidationPassed = { },
                resetShowChangeNodeExtensionDialog = { },
                onRenameConfirmed = { },
                onDismiss = mockOnDismiss,
                onRenameNode = { }
            )
        }

        composeTestRule.onNodeWithTag(RENAME_NODE_DIALOG_TAG).assertIsDisplayed()
        verify(mockOnLoadNodeName).invoke()

        // Click cancel button
        composeTestRule.onNodeWithText(
            text = InstrumentationRegistry
                .getInstrumentation()
                .targetContext
                .getString(sharedResR.string.general_dialog_cancel_button)
        ).performClick()
        verify(mockOnDismiss).invoke()
    }

    @Test
    fun `test that onRenameConfirmed is called when rename button is clicked`() {
        val mockUiState = RenameNodeDialogState(nodeName = "test.txt")
        val mockOnLoadNodeName = mock<() -> Unit>()
        val mockOnRenameConfirmed = mock<(String) -> Unit>()

        composeTestRule.setContent {
            RenameNodeDialogM3View(
                uiState = mockUiState,
                onLoadNodeName = mockOnLoadNodeName,
                resetRenameValidationPassed = { },
                resetShowChangeNodeExtensionDialog = { },
                onRenameConfirmed = mockOnRenameConfirmed,
                onDismiss = { },
                onRenameNode = { }
            )
        }

        composeTestRule.onNodeWithTag(RENAME_NODE_DIALOG_TAG).assertIsDisplayed()
        verify(mockOnLoadNodeName).invoke()

        // Click rename button
        composeTestRule.onAllNodesWithText(
            text = InstrumentationRegistry
                .getInstrumentation()
                .targetContext
                .getString(sharedResR.string.context_rename)
        )[1].performClick()
        verify(mockOnRenameConfirmed).invoke("test.txt")
    }

    @Test
    fun `test that onDismiss and onRenameNode are called when confirmation dialog cancel is clicked`() {
        val mockUiState = RenameNodeDialogState(
            nodeName = "test.txt",
            showChangeNodeExtensionDialogEvent = triggered("test.jpg")
        )
        val mockOnLoadNodeName = mock<() -> Unit>()
        val mockResetShowChangeNodeExtensionDialog = mock<() -> Unit>()
        val mockOnDismiss = mock<() -> Unit>()

        composeTestRule.setContent {
            RenameNodeDialogM3View(
                uiState = mockUiState,
                onLoadNodeName = mockOnLoadNodeName,
                resetRenameValidationPassed = { },
                resetShowChangeNodeExtensionDialog = mockResetShowChangeNodeExtensionDialog,
                onRenameConfirmed = { },
                onDismiss = mockOnDismiss,
                onRenameNode = { }
            )
        }

        composeTestRule.onNodeWithTag(RENAME_NODE_DIALOG_CONFIRMATION_DIALOG).assertIsDisplayed()
        verify(mockOnLoadNodeName).invoke()
        verify(mockResetShowChangeNodeExtensionDialog).invoke()

        // Click cancel button on confirmation dialog
        composeTestRule.onNodeWithText(
            text = InstrumentationRegistry
                .getInstrumentation()
                .targetContext
                .getString(sharedResR.string.general_dialog_cancel_button)
        ).performClick()
        verify(mockOnDismiss).invoke()
    }

    @Test
    fun `test that onRenameNode is called when confirmation dialog confirm is clicked`() {
        val mockUiState = RenameNodeDialogState(
            nodeName = "test.txt",
            showChangeNodeExtensionDialogEvent = triggered("test.jpg")
        )
        val mockOnLoadNodeName = mock<() -> Unit>()
        val mockResetShowChangeNodeExtensionDialog = mock<() -> Unit>()
        val mockOnRenameNode = mock<(String) -> Unit>()
        val mockOnDismiss = mock<() -> Unit>()

        composeTestRule.setContent {
            RenameNodeDialogM3View(
                uiState = mockUiState,
                onLoadNodeName = mockOnLoadNodeName,
                resetRenameValidationPassed = { },
                resetShowChangeNodeExtensionDialog = mockResetShowChangeNodeExtensionDialog,
                onRenameConfirmed = { },
                onDismiss = mockOnDismiss,
                onRenameNode = mockOnRenameNode
            )
        }

        composeTestRule.onNodeWithTag(RENAME_NODE_DIALOG_CONFIRMATION_DIALOG).assertIsDisplayed()
        verify(mockOnLoadNodeName).invoke()
        verify(mockResetShowChangeNodeExtensionDialog).invoke()

        // Click confirm button on confirmation dialog
        composeTestRule.onNodeWithText(
            text = InstrumentationRegistry
                .getInstrumentation()
                .targetContext
                .getString(R.string.action_change_anyway)
        ).performClick()
        verify(mockOnRenameNode).invoke("test.jpg")
        verify(mockOnDismiss).invoke()
    }
}
