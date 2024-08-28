package mega.privacy.android.app.presentation.settings.camerauploads.dialogs

import mega.privacy.android.shared.resources.R as SharedR
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.presentation.settings.camerauploads.dialogs.RELATED_NEW_LOCAL_FOLDER_WARNING_DIALOG
import mega.privacy.android.app.presentation.settings.camerauploads.dialogs.RelatedNewLocalFolderWarningDialog
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import mega.privacy.android.app.onNodeWithText

/**
 * Test class for [RelatedNewLocalFolderWarningDialog]
 */
@RunWith(AndroidJUnit4::class)
internal class RelatedNewLocalFolderWarningDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that the dialog is shown`() {
        composeTestRule.setContent {
            RelatedNewLocalFolderWarningDialog(
                onWarningAcknowledged = {},
                onWarningDismissed = {},
            )
        }

        composeTestRule.onNodeWithTag(RELATED_NEW_LOCAL_FOLDER_WARNING_DIALOG).assertIsDisplayed()
        composeTestRule.onNodeWithText(SharedR.string.settings_camera_uploads_related_new_local_folder_warning_dialog_title)
        composeTestRule.onNodeWithText(SharedR.string.settings_camera_uploads_related_new_local_folder_warning_dialog_body)
        composeTestRule.onNodeWithText(SharedR.string.settings_camera_uploads_related_new_local_folder_warning_dialog_confirm_button)
    }

    @Test
    fun `test that clicking the button invokes the on warning acknowledged lambda`() {
        val onWarningAcknowledged = mock<() -> Unit>()
        composeTestRule.setContent {
            RelatedNewLocalFolderWarningDialog(
                onWarningAcknowledged = onWarningAcknowledged,
                onWarningDismissed = {},
            )
        }

        composeTestRule.onNodeWithText(SharedR.string.settings_camera_uploads_related_new_local_folder_warning_dialog_confirm_button)
            .performClick()

        verify(onWarningAcknowledged).invoke()
    }
}