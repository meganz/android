package mega.privacy.android.feature.sync.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import mega.privacy.android.feature.sync.ui.views.ApplyToAllDialog
import mega.privacy.android.feature.sync.ui.views.TEST_TAG_APPLY_TO_ALL_DIALOG_CHECKBOX

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "fr-rFr-w1080dp-h1920dp")
class ApplyToAllDialogTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

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
} 