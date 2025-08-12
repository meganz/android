package mega.privacy.android.feature.sync.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth
import mega.privacy.android.feature.sync.ui.views.ApplyToAllDialog
import mega.privacy.android.feature.sync.ui.views.TEST_TAG_APPLY_TO_ALL_DIALOG_CHECKBOX
import mega.privacy.android.feature.sync.ui.views.TEST_TAG_APPLY_TO_ALL_DIALOG_CHECKBOX_ROW
import mega.privacy.android.feature.sync.ui.views.TEST_TAG_APPLY_TO_ALL_DIALOG_CHECKBOX_TEXT
import mega.privacy.android.feature.sync.ui.views.TEST_TAG_APPLY_TO_ALL_DIALOG_DESCRIPTION
import mega.privacy.android.shared.resources.R as sharedR
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "fr-rFr-w1080dp-h1920dp")
internal class ApplyToAllDialogTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `test that dialog displays with correct title and description`() {
        val title = "Choose the local file?"
        val description = "The local file will be moved to the .rubbish folder."

        composeTestRule.setContent {
            ApplyToAllDialog(
                title = title,
                description = description,
                onApplyToCurrent = {},
                onApplyToAll = {},
                onCancel = {},
                actionButtonStringRes = sharedR.string.general_dialog_choose_button
            )
        }

        composeTestRule.onNodeWithText(title).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TEST_TAG_APPLY_TO_ALL_DIALOG_DESCRIPTION)
            .assertIsDisplayed()
    }

    @Test
    fun `test that checkbox is displayed`() {
        composeTestRule.setContent {
            ApplyToAllDialog(
                title = "Choose the local file?",
                description = "The local file will be moved to the .rubbish folder.",
                onApplyToCurrent = {},
                onApplyToAll = {},
                onCancel = {},
                actionButtonStringRes = sharedR.string.general_dialog_choose_button
            )
        }

        composeTestRule.onNodeWithTag(TEST_TAG_APPLY_TO_ALL_DIALOG_CHECKBOX)
            .assertIsDisplayed()
    }

    @Test
    fun `test that checkbox text is displayed`() {
        composeTestRule.setContent {
            ApplyToAllDialog(
                title = "Choose the local file?",
                description = "The local file will be moved to the .rubbish folder.",
                onApplyToCurrent = {},
                onApplyToAll = {},
                onCancel = {},
                actionButtonStringRes = sharedR.string.general_dialog_choose_button
            )
        }

        composeTestRule.onNodeWithTag(TEST_TAG_APPLY_TO_ALL_DIALOG_CHECKBOX_TEXT)
            .assertIsDisplayed()
    }

    @Test
    fun `test that choose button is displayed`() {
        composeTestRule.setContent {
            ApplyToAllDialog(
                title = "Choose the local file?",
                description = "The local file will be moved to the .rubbish folder.",
                onApplyToCurrent = {},
                onApplyToAll = {},
                onCancel = {},
                actionButtonStringRes = sharedR.string.general_dialog_choose_button
            )
        }

        composeTestRule.onNodeWithText(composeTestRule.activity.getString(sharedR.string.general_dialog_choose_button))
            .assertIsDisplayed()
    }

    @Test
    fun `test that cancel button is displayed`() {
        composeTestRule.setContent {
            ApplyToAllDialog(
                title = "Choose the local file?",
                description = "The local file will be moved to the .rubbish folder.",
                onApplyToCurrent = {},
                onApplyToAll = {},
                onCancel = {},
                actionButtonStringRes = sharedR.string.general_dialog_choose_button
            )
        }

        composeTestRule.onNodeWithText(composeTestRule.activity.getString(mega.privacy.android.core.R.string.general_cancel))
            .assertIsDisplayed()
    }

    @Test
    fun `test onApplyToCurrent callback when checkbox is unchecked`() {
        var applyToCurrentCalled = false
        var applyToAllCalled = false

        composeTestRule.setContent {
            ApplyToAllDialog(
                title = "Choose the local file?",
                description = "The local file will be moved to the .rubbish folder.",
                onApplyToCurrent = { applyToCurrentCalled = true },
                onApplyToAll = { applyToAllCalled = true },
                onCancel = {},
                actionButtonStringRes = sharedR.string.general_dialog_choose_button
            )
        }

        // Click the choose button without checking the checkbox
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(sharedR.string.general_dialog_choose_button))
            .performClick()

        Truth.assertThat(applyToCurrentCalled).isTrue()
        Truth.assertThat(applyToAllCalled).isFalse()
    }

    @Test
    fun `test onApplyToAll callback when checkbox is checked`() {
        var applyToCurrentCalled = false
        var applyToAllCalled = false

        composeTestRule.setContent {
            ApplyToAllDialog(
                title = "Choose the local file?",
                description = "The local file will be moved to the .rubbish folder.",
                onApplyToCurrent = { applyToCurrentCalled = true },
                onApplyToAll = { applyToAllCalled = true },
                onCancel = {},
                actionButtonStringRes = sharedR.string.general_dialog_choose_button
            )
        }

        // Check the checkbox first
        composeTestRule.onNodeWithTag(TEST_TAG_APPLY_TO_ALL_DIALOG_CHECKBOX).performClick()

        // Then click the choose button
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(sharedR.string.general_dialog_choose_button))
            .performClick()

        Truth.assertThat(applyToCurrentCalled)
            .isFalse()
        Truth.assertThat(applyToAllCalled).isTrue()
    }

    @Test
    fun `test onCancel callback when cancel button is clicked`() {
        var cancelCalled = false

        composeTestRule.setContent {
            ApplyToAllDialog(
                title = "Choose the local file?",
                description = "The local file will be moved to the .rubbish folder.",
                onApplyToCurrent = {},
                onApplyToAll = {},
                onCancel = { cancelCalled = true },
                actionButtonStringRes = sharedR.string.general_dialog_choose_button
            )
        }

        composeTestRule.onNodeWithText(composeTestRule.activity.getString(mega.privacy.android.core.R.string.general_cancel))
            .performClick()

        Truth.assertThat(cancelCalled).isTrue()
    }

    @Test
    fun `test that shouldShowApplyToAllOption set to false makes checkbox invisible`() {
        composeTestRule.setContent {
            ApplyToAllDialog(
                title = "Choose the local file?",
                description = "The local file will be moved to the .rubbish folder.",
                onApplyToCurrent = {},
                onApplyToAll = {},
                onCancel = {},
                shouldShowApplyToAllOption = false,
                actionButtonStringRes = sharedR.string.general_dialog_choose_button
            )
        }

        composeTestRule.onNodeWithTag(TEST_TAG_APPLY_TO_ALL_DIALOG_CHECKBOX)
            .assertDoesNotExist()
        composeTestRule.onNodeWithTag(TEST_TAG_APPLY_TO_ALL_DIALOG_CHECKBOX_TEXT)
            .assertDoesNotExist()
        composeTestRule.onNodeWithTag(TEST_TAG_APPLY_TO_ALL_DIALOG_CHECKBOX_ROW)
            .assertDoesNotExist()
    }

    @Test
    fun `test onDismiss callback is triggered correctly`() {
        var dismissCalled = false

        composeTestRule.setContent {
            ApplyToAllDialog(
                title = "Choose the local file?",
                description = "The local file will be moved to the .rubbish folder.",
                onApplyToCurrent = {},
                onApplyToAll = {},
                onCancel = { dismissCalled = true },
                actionButtonStringRes = sharedR.string.general_dialog_choose_button
            )
        }
        // Click the cancel button to trigger the dismiss callback
        composeTestRule.onNodeWithText(composeTestRule.activity.getString(mega.privacy.android.core.R.string.general_cancel))
            .performClick()

        Truth.assertThat(dismissCalled).isTrue()
    }

    @Test
    fun `test that rename button is displayed for rename action`() {
        composeTestRule.setContent {
            ApplyToAllDialog(
                title = "Rename all items?",
                description = "All conflicting items will be renamed.",
                onApplyToCurrent = {},
                onApplyToAll = {},
                onCancel = {},
                actionButtonStringRes = sharedR.string.sync_apply_all_dialog_rename_button
            )
        }

        composeTestRule.onNodeWithText(composeTestRule.activity.getString(sharedR.string.sync_apply_all_dialog_rename_button))
            .assertIsDisplayed()
    }

    @Test
    fun `test that merge button is displayed for merge action`() {
        composeTestRule.setContent {
            ApplyToAllDialog(
                title = "Merge folders?",
                description = "The folders will be merged.",
                onApplyToCurrent = {},
                onApplyToAll = {},
                onCancel = {},
                actionButtonStringRes = sharedR.string.sync_apply_all_dialog_merge_button
            )
        }

        composeTestRule.onNodeWithText(composeTestRule.activity.getString(sharedR.string.sync_apply_all_dialog_merge_button))
            .assertIsDisplayed()
    }

    @Test
    fun `test that choose button is displayed for default actions`() {
        composeTestRule.setContent {
            ApplyToAllDialog(
                title = "Choose the local file?",
                description = "The local file will be chosen.",
                onApplyToCurrent = {},
                onApplyToAll = {},
                onCancel = {},
                actionButtonStringRes = sharedR.string.general_dialog_choose_button
            )
        }

        composeTestRule.onNodeWithText(composeTestRule.activity.getString(sharedR.string.general_dialog_choose_button))
            .assertIsDisplayed()
    }
} 
