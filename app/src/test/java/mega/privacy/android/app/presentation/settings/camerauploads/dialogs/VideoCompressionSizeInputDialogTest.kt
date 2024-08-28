package mega.privacy.android.app.presentation.settings.camerauploads.dialogs

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasImeAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.input.ImeAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.settings.camerauploads.dialogs.VIDEO_COMPRESSION_SIZE_INPUT_DIALOG
import mega.privacy.android.app.presentation.settings.camerauploads.dialogs.VideoCompressionSizeInputDialog
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

/**
 * Test class for [VideoCompressionSizeInputDialog]
 */
@RunWith(AndroidJUnit4::class)
internal class VideoCompressionSizeInputDialogTest {
    @get:Rule
    var composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `test that the dialog is shown in its initial state`() {
        initializeComposeContent()

        with(composeTestRule) {
            onNodeWithTag(VIDEO_COMPRESSION_SIZE_INPUT_DIALOG).assertIsDisplayed()
            onNodeWithText(activity.getString(R.string.settings_video_compression_queue_size_popup_title)).assertIsDisplayed()
            onNodeWithText(activity.getString(R.string.label_mega_byte)).assertIsDisplayed()
            onNodeWithText(activity.getString(R.string.general_ok)).assertIsDisplayed()
            onNodeWithText(activity.getString(android.R.string.cancel)).assertIsDisplayed()
            onNodeWithText(
                activity.getString(
                    R.string.settings_compression_queue_subtitle,
                    activity.getString(R.string.label_file_size_mega_byte, "100"),
                    activity.getString(R.string.label_file_size_mega_byte, "1000"),
                )
            ).assertDoesNotExist()
        }
    }

    @Test
    fun `test that clicking the keyboard ime done key without any input shows the error message`() {
        initializeComposeContent()

        with(composeTestRule) {
            onNode(hasImeAction(ImeAction.Done)).performImeAction()

            onNodeWithText(
                activity.getString(
                    R.string.settings_compression_queue_subtitle,
                    activity.getString(R.string.label_file_size_mega_byte, "100"),
                    activity.getString(R.string.label_file_size_mega_byte, "1000"),
                )
            ).assertIsDisplayed()
        }
    }

    @Test
    fun `test that clicking the keyboard ime done key with a below minimum input shows the error message`() {
        initializeComposeContent()

        with(composeTestRule) {
            onNode(hasImeAction(ImeAction.Done)).apply {
                performTextInput("50")
                performImeAction()
            }

            onNodeWithText(
                activity.getString(
                    R.string.settings_compression_queue_subtitle,
                    activity.getString(R.string.label_file_size_mega_byte, "100"),
                    activity.getString(R.string.label_file_size_mega_byte, "1000"),
                )
            ).assertIsDisplayed()
        }
    }

    @Test
    fun `test that clicking the keyboard ime done key with a beyond maximum input shows the error message`() {
        initializeComposeContent()

        with(composeTestRule) {
            onNode(hasImeAction(ImeAction.Done)).apply {
                performTextInput("2000")
                performImeAction()
            }

            onNodeWithText(
                activity.getString(
                    R.string.settings_compression_queue_subtitle,
                    activity.getString(R.string.label_file_size_mega_byte, "100"),
                    activity.getString(R.string.label_file_size_mega_byte, "1000"),
                )
            ).assertIsDisplayed()
        }
    }

    @Test
    fun `test that clicking the keyboard ime done key with a valid input invokes the on new size provided lambda`() {
        val onNewSizeProvided = mock<(Int) -> Unit>()
        composeTestRule.setContent {
            VideoCompressionSizeInputDialog(
                onNewSizeProvided = onNewSizeProvided,
                onDismiss = {},
            )
        }

        composeTestRule.onNode(hasImeAction(ImeAction.Done)).apply {
            performTextInput("500")
            performImeAction()
        }

        verify(onNewSizeProvided).invoke(any())
    }

    @Test
    fun `test that clicking the positive button without any input shows the error message`() {
        initializeComposeContent()

        with(composeTestRule) {
            onNodeWithText(activity.getString(R.string.general_ok)).performClick()

            onNodeWithText(
                activity.getString(
                    R.string.settings_compression_queue_subtitle,
                    activity.getString(R.string.label_file_size_mega_byte, "100"),
                    activity.getString(R.string.label_file_size_mega_byte, "1000"),
                )
            ).assertIsDisplayed()
        }
    }

    @Test
    fun `test that clicking the positive button with a below minimum input shows the error message`() {
        initializeComposeContent()

        with(composeTestRule) {
            onNode(hasImeAction(ImeAction.Done)).performTextInput("50")
            onNodeWithText(activity.getString(R.string.general_ok)).performClick()

            onNodeWithText(
                activity.getString(
                    R.string.settings_compression_queue_subtitle,
                    activity.getString(R.string.label_file_size_mega_byte, "100"),
                    activity.getString(R.string.label_file_size_mega_byte, "1000"),
                )
            ).assertIsDisplayed()
        }
    }

    @Test
    fun `test that clicking the positive button with a beyond maximum input shows the error message`() {
        initializeComposeContent()

        with(composeTestRule) {
            onNode(hasImeAction(ImeAction.Done)).performTextInput("2000")
            onNodeWithText(activity.getString(R.string.general_ok)).performClick()

            onNodeWithText(
                activity.getString(
                    R.string.settings_compression_queue_subtitle,
                    activity.getString(R.string.label_file_size_mega_byte, "100"),
                    activity.getString(R.string.label_file_size_mega_byte, "1000"),
                )
            ).assertIsDisplayed()
        }
    }

    @Test
    fun `test that clicking the positive button with a valid input invokes the on new size provided lambda`() {
        val onNewSizeProvided = mock<(Int) -> Unit>()
        composeTestRule.setContent {
            VideoCompressionSizeInputDialog(
                onNewSizeProvided = onNewSizeProvided,
                onDismiss = {},
            )
        }

        with(composeTestRule) {
            onNode(hasImeAction(ImeAction.Done)).performTextInput("500")
            onNodeWithText(activity.getString(R.string.general_ok)).performClick()
        }

        verify(onNewSizeProvided).invoke(any())
    }

    @Test
    fun `test that clicking the negative button invokes the on dismiss lambda`() {
        val onDismiss = mock<() -> Unit>()
        composeTestRule.setContent {
            VideoCompressionSizeInputDialog(
                onNewSizeProvided = {},
                onDismiss = onDismiss,
            )
        }

        with(composeTestRule) {
            onNodeWithText(activity.getString(android.R.string.cancel)).performClick()
        }

        verify(onDismiss).invoke()
    }

    private fun initializeComposeContent() {
        composeTestRule.setContent {
            VideoCompressionSizeInputDialog(
                onNewSizeProvided = {},
                onDismiss = {},
            )
        }
    }
}