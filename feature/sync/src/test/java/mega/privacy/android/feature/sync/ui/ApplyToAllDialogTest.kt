package mega.privacy.android.feature.sync.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.feature.sync.ui.views.ApplyToAllDialog
import mega.privacy.android.feature.sync.ui.views.TEST_TAG_APPLY_TO_ALL_DIALOG_CHECKBOX
import mega.privacy.android.feature.sync.ui.views.TEST_TAG_APPLY_TO_ALL_DIALOG_CHECKBOX_TEXT
import mega.privacy.android.feature.sync.ui.views.TEST_TAG_APPLY_TO_ALL_DIALOG_DESCRIPTION
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "fr-rFr-w1080dp-h1920dp")
class ApplyToAllDialogTest {

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
            )
        }

        composeTestRule.onNodeWithText("Choose").assertIsDisplayed()
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
            )
        }

        composeTestRule.onNodeWithText("Cancel").assertIsDisplayed()
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
            )
        }

        // Click the choose button without checking the checkbox
        composeTestRule.onNodeWithText("Choose").performClick()

        assert(applyToCurrentCalled) { "onApplyToCurrent should be called when checkbox is unchecked" }
        assert(!applyToAllCalled) { "onApplyToAll should not be called when checkbox is unchecked" }
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
            )
        }

        // Check the checkbox first
        composeTestRule.onNodeWithTag(TEST_TAG_APPLY_TO_ALL_DIALOG_CHECKBOX).performClick()

        // Then click the choose button
        composeTestRule.onNodeWithText("Choose").performClick()

        assert(!applyToCurrentCalled) { "onApplyToCurrent should not be called when checkbox is checked" }
        assert(applyToAllCalled) { "onApplyToAll should be called when checkbox is checked" }
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
            )
        }

        composeTestRule.onNodeWithText("Cancel").performClick()

        assert(cancelCalled) { "onCancel should be called when cancel button is clicked" }
    }
} 
