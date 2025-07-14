package mega.privacy.android.app.presentation.settings.camerauploads.dialogs

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.R
import mega.privacy.android.app.onNodeWithText
import mega.privacy.android.shared.resources.R as sharedR
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions

@RunWith(AndroidJUnit4::class)
class CancelAllTransfersDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val onDisable = mock<() -> Unit>()
    private val onDismiss = mock<() -> Unit>()

    @Test
    fun `test that dialog shows correctly`() {
        initComposeTestRule()

        with(composeTestRule) {
            onNodeWithTag(TEST_TAG_DISABLE_CU_DIALOG).assertIsDisplayed()
            onNodeWithText(sharedR.string.settings_camera_uploads_disable_warning).assertIsDisplayed()
            onNodeWithText(R.string.verify_2fa_subtitle_diable_2fa).assertIsDisplayed()
            onNodeWithText(sharedR.string.general_dialog_cancel_button).assertIsDisplayed()
        }
    }

    @Test
    fun `test that clicking on disable button calls onDisable and onDismiss`() {
        initComposeTestRule()

        composeTestRule.onNodeWithText(R.string.verify_2fa_subtitle_diable_2fa).performClick()

        verify(onDisable).invoke()
        verify(onDismiss).invoke()
    }

    @Test
    fun `test that clicking on cancel button calls onDismiss but not onDisable`() {
        initComposeTestRule()

        composeTestRule.onNodeWithText(sharedR.string.general_dialog_cancel_button).performClick()

        verify(onDismiss).invoke()
        verifyNoInteractions(onDisable)
    }

    private fun initComposeTestRule() {
        composeTestRule.setContent {
            DisableCameraUploadsDialog(
                onDisable = onDisable,
                onDismiss = onDismiss
            )
        }
    }
}