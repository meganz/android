package mega.privacy.android.app.presentation.transfers.view.dialog

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.onNodeWithText
import mega.privacy.android.shared.resources.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class NotEnoughSpaceForUploadDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val onUpgrade = mock<() -> Unit>()
    private val onCancel = mock<() -> Unit>()
    private val onLearnMore = mock<() -> Unit>()

    @Test
    fun `test that dialog shows correctly`() {
        initComposeRule()

        with(composeTestRule) {
            onNodeWithTag(TEST_TAG_NOT_ENOUGH_SPACE_FOR_UPLOAD_DIALOG).assertIsDisplayed()
            onNodeWithText(R.string.dialog_not_enough_space_to_upload_title).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_NOT_ENOUGH_SPACE_FOR_UPLOAD_DIALOG_CONTENT).assertIsDisplayed()
            onNodeWithText(R.string.general_upgrade_button).assertIsDisplayed()
            onNodeWithText(R.string.general_dialog_cancel_button).assertIsDisplayed()
        }
    }

    @Test
    fun `test that onCancel is invoked if negative button is pressed`() {
        initComposeRule()

        composeTestRule.onNodeWithText(R.string.general_dialog_cancel_button).performClick()

        verify(onCancel).invoke()
    }

    @Test
    fun `test that onCancel and onUpgrade are invoked if positive button is pressed`() {
        initComposeRule()

        composeTestRule.onNodeWithText(R.string.general_upgrade_button).performClick()

        verify(onUpgrade).invoke()
        verify(onCancel).invoke()
    }

    private fun initComposeRule() {
        composeTestRule.setContent {
            NotEnoughSpaceForUploadDialog(
                onUpgrade = onUpgrade,
                onCancel = onCancel,
                onLearnMore = onLearnMore,
            )
        }
    }
}