package mega.privacy.android.shared.nodes.dialog.newfile

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.exception.EmptyNodeNameException
import mega.privacy.android.domain.exception.InvalidNodeExtensionException
import mega.privacy.android.domain.exception.NodeNameAlreadyExistsException
import mega.privacy.android.domain.exception.NodeNameException
import mega.privacy.android.shared.nodes.R as NodesR
import mega.privacy.android.shared.nodes.dialog.newfile.NewTextFileNodeDialogUiState.Companion.DEFAULT_TEXT_FILE_EXTENSION
import mega.privacy.android.shared.resources.R as sharedR
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class NewTextFileNodeDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val title: String
        get() = context.getString(sharedR.string.general_new_text_file)

    private fun defaultState(
        parentNodeId: NodeId = NodeId(123L),
        fileName: String = DEFAULT_TEXT_FILE_EXTENSION,
        fileNameException: NodeNameException? = null,
        validationSuccessEvent: StateEventWithContent<String> = consumed(),
    ) = NewTextFileNodeDialogUiState.Data(
        parentNodeId = parentNodeId,
        fileName = fileName,
        fileNameException = fileNameException,
        validationSuccessEvent = validationSuccessEvent,
    )

    @Test
    fun `test that dialog is displayed with default file name`() {
        composeTestRule.setContent {
            NewTextFileNodeDialog(
                uiState = defaultState(),
                title = title,
                onConfirm = {},
                onFileNameChanged = {},
                validateFileName = {},
                onValidationSuccessEventConsumed = {},
            )
        }

        composeTestRule.onNodeWithTag(NEW_TEXT_FILE_NODE_DIALOG_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithText(DEFAULT_TEXT_FILE_EXTENSION).assertIsDisplayed()
    }

    @Test
    fun `test that title and buttons are displayed`() {
        composeTestRule.setContent {
            NewTextFileNodeDialog(
                uiState = defaultState(),
                title = title,
                onConfirm = {},
                onFileNameChanged = {},
                validateFileName = {},
                onValidationSuccessEventConsumed = {},
            )
        }

        composeTestRule.onNodeWithText(title).assertIsDisplayed()
        composeTestRule.onNodeWithText(
            context.getString(sharedR.string.general_create_label)
        ).assertIsDisplayed()
        composeTestRule.onNodeWithText(
            context.getString(sharedR.string.general_dialog_cancel_button)
        ).assertIsDisplayed()
    }

    @Test
    fun `test that onDismiss is called when cancel button is clicked`() {
        val onDismiss = mock<() -> Unit>()

        composeTestRule.setContent {
            NewTextFileNodeDialog(
                uiState = defaultState(),
                title = title,
                onConfirm = {},
                onFileNameChanged = {},
                validateFileName = {},
                onValidationSuccessEventConsumed = {},
                onDismiss = onDismiss,
            )
        }

        composeTestRule.onNodeWithText(
            context.getString(sharedR.string.general_dialog_cancel_button)
        ).performClick()

        verify(onDismiss).invoke()
    }

    @Test
    fun `test that onFileNameChanged is called when text is edited`() {
        val onFileNameChanged = mock<(String) -> Unit>()
        val fileName = "hello$DEFAULT_TEXT_FILE_EXTENSION"

        composeTestRule.setContent {
            NewTextFileNodeDialog(
                uiState = defaultState(),
                title = title,
                onConfirm = {},
                onFileNameChanged = onFileNameChanged,
                validateFileName = {},
                onValidationSuccessEventConsumed = {},
            )
        }

        composeTestRule.onNodeWithText(DEFAULT_TEXT_FILE_EXTENSION)
            .performTextReplacement(fileName)
        composeTestRule.waitForIdle()

        verify(onFileNameChanged).invoke(fileName)
    }

    @Test
    fun `test that validateFileName is called when create button is clicked`() {
        val validateFileName = mock<() -> Unit>()

        composeTestRule.setContent {
            NewTextFileNodeDialog(
                uiState = defaultState(),
                title = title,
                onConfirm = {},
                onFileNameChanged = {},
                validateFileName = validateFileName,
                onValidationSuccessEventConsumed = {},
            )
        }

        composeTestRule.onNodeWithText(
            context.getString(sharedR.string.general_create_label)
        ).performClick()
        composeTestRule.waitForIdle()

        verify(validateFileName).invoke()
    }

    @Test
    fun `test that onConfirm is called when validation success event is triggered`() {
        val onConfirm = mock<(String) -> Unit>()
        val fileName = "hello$DEFAULT_TEXT_FILE_EXTENSION"

        composeTestRule.setContent {
            NewTextFileNodeDialog(
                uiState = defaultState().copy(validationSuccessEvent = triggered(fileName)),
                title = title,
                onConfirm = onConfirm,
                onFileNameChanged = {},
                validateFileName = {},
                onValidationSuccessEventConsumed = {},
            )
        }
        composeTestRule.waitForIdle()

        verify(onConfirm).invoke(fileName)
    }

    @Test
    fun `test that empty name error is displayed when state has EmptyNodeNameException`() {
        composeTestRule.setContent {
            NewTextFileNodeDialog(
                uiState = defaultState().copy(fileNameException = EmptyNodeNameException()),
                title = title,
                onConfirm = {},
                onFileNameChanged = {},
                validateFileName = {},
                onValidationSuccessEventConsumed = {},
            )
        }

        composeTestRule.onNodeWithText(
            context.getString(sharedR.string.general_invalid_string)
        ).assertIsDisplayed()
    }

    @Test
    fun `test that name already exists error is displayed when state has NodeNameAlreadyExistsException`() {
        composeTestRule.setContent {
            NewTextFileNodeDialog(
                uiState = defaultState().copy(fileNameException = NodeNameAlreadyExistsException()),
                title = title,
                onConfirm = {},
                onFileNameChanged = {},
                validateFileName = {},
                onValidationSuccessEventConsumed = {},
            )
        }

        composeTestRule.onNodeWithText(
            context.getString(NodesR.string.same_file_name_warning)
        ).assertIsDisplayed()
    }

    @Test
    fun `test that invalid extension error is displayed when state has InvalidNodeExtensionException`() {
        composeTestRule.setContent {
            NewTextFileNodeDialog(
                uiState = defaultState().copy(fileNameException = InvalidNodeExtensionException()),
                title = title,
                onConfirm = {},
                onFileNameChanged = {},
                validateFileName = {},
                onValidationSuccessEventConsumed = {},
            )
        }

        composeTestRule.onNodeWithText(
            context.getString(sharedR.string.new_text_file_invalid_extension_error_message)
        ).assertIsDisplayed()
    }
}
