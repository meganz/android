package mega.privacy.android.core.ui.controls.dialogs

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.core.R
import mega.privacy.android.core.ui.controls.dialogs.internal.CANCEL_TAG
import mega.privacy.android.core.ui.controls.dialogs.internal.CONFIRM_TAG
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

/**
 * Test class for [ConfirmationDialogWithIllustration]
 */
@RunWith(AndroidJUnit4::class)
internal class ConfirmationDialogWithIllustrationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val titleText = "title"
    private val illustrationId = R.drawable.ic_sync
    private val bodyText = "Body"
    private val confirmText = "Confirm"
    private val cancelText = "Cancel"

    private fun createConfirmationDialog(
        cancelButtonText: String? = cancelText,
        onConfirmButtonClicked: () -> Unit = {},
        onCancelButtonClicked: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            ConfirmationDialogWithIllustration(
                title = titleText,
                illustrationId = illustrationId,
                body = bodyText,
                confirmButtonText = confirmText,
                cancelButtonText = cancelButtonText,
                onDismiss = { },
                onConfirm = onConfirmButtonClicked,
                onCancel = onCancelButtonClicked,
            )
        }
    }

    @Test
    fun `test that the title text is set`() {
        createConfirmationDialog()
        composeTestRule.onNodeWithTag(CONFIRMATION_DIALOG_WITH_ILLUSTRATION_TITLE).assertTextEquals(
            titleText
        )
    }

    @Test
    fun `test that the illustration is set`() {
        createConfirmationDialog()
        val hasDrawable =
            SemanticsMatcher.expectValue(DrawableResId, illustrationId)
        composeTestRule.onNodeWithTag(CONFIRMATION_DIALOG_WITH_ILLUSTRATION_IMAGE)
            .assertIsDisplayed()
        composeTestRule.onNode(hasDrawable).assertIsDisplayed()
    }

    @Test
    fun `test that the body text is set`() {
        createConfirmationDialog()
        composeTestRule.onNodeWithTag(CONFIRMATION_DIALOG_WITH_ILLUSTRATION_BODY)
            .assertTextEquals(bodyText)
    }

    @Test
    fun `test that the confirm button text is set`() {
        createConfirmationDialog()
        composeTestRule.onNodeWithTag(CONFIRM_TAG).assertTextEquals(confirmText)
    }

    @Test
    fun `test that the cancel button text is set`() {
        createConfirmationDialog()
        composeTestRule.onNodeWithTag(CANCEL_TAG).assertTextEquals(cancelText)
    }

    @Test
    fun `test that the cancel button is hidden when no cancel button text is provided`() {
        createConfirmationDialog(cancelButtonText = null)
        composeTestRule.onNodeWithTag(CANCEL_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that the confirm action is triggered when clicking the confirm button`() {
        val confirmEvent = mock<() -> Unit>()
        createConfirmationDialog(onConfirmButtonClicked = confirmEvent)
        composeTestRule.onNodeWithTag(CONFIRM_TAG).performClick()
        verify(confirmEvent).invoke()
    }

    @Test
    fun `test that the cancel action is triggered when clicking the cancel button`() {
        val cancelEvent = mock<() -> Unit>()
        createConfirmationDialog(onCancelButtonClicked = cancelEvent)
        composeTestRule.onNodeWithTag(CANCEL_TAG).performClick()
        verify(cancelEvent).invoke()
    }
}