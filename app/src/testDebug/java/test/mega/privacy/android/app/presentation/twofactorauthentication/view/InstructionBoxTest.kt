package test.mega.privacy.android.app.presentation.twofactorauthentication.view

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidTest
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.twofactorauthentication.view.screens.INSTRUCTION_MESSAGE_TEST_TAG
import mega.privacy.android.app.presentation.twofactorauthentication.view.screens.InstructionBox
import mega.privacy.android.app.presentation.twofactorauthentication.view.screens.QUESTION_MARK_ICON_TEST_TAG
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import test.mega.privacy.android.app.fromId
import test.mega.privacy.android.app.hasDrawable

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "fr-rFr-w1080dp-h1920dp")
class InstructionBoxTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun setupRule() {
        composeRule.setContent {
            InstructionBox(
                isDarkMode = false,
                openPlayStore = {}
            )
        }
    }

    @Test
    fun `test that Instruction title is shown`() {
        setupRule()
        composeRule.onNodeWithText(fromId(R.string.explain_qr_seed_2fa_1)).assertIsDisplayed()
    }

    @Test
    fun `test that Instruction message with hint icon is shown`() {
        setupRule()
        composeRule.onNodeWithTag(INSTRUCTION_MESSAGE_TEST_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that hint's question mark icon is shown`() {
        setupRule()
        composeRule.onNodeWithTag(QUESTION_MARK_ICON_TEST_TAG).assertExists()
    }

    @Test
    fun `test that hint's question mark icon is clickable`() {
        setupRule()
        composeRule.onNodeWithTag(QUESTION_MARK_ICON_TEST_TAG).assertHasClickAction()
    }

    @Test
    fun `test that hint's question mark icon has the right drawable`() {
        setupRule()
        composeRule.onNode(hasDrawable(R.drawable.ic_question_mark)).assertExists()
    }
}