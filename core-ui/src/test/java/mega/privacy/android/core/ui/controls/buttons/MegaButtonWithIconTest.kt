package mega.privacy.android.core.ui.controls.buttons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock

@RunWith(AndroidJUnit4::class)
class MegaButtonWithIconTest {
    @get:Rule
    var composeRule = createComposeRule()

    private var iconColor: Color = Color.Black

    @Test
    fun `test that button is enabled`() {
        composeRule.setContent {
            MegaButtonWithIcon(
                iconColor = iconColor,
                enabled = true,
                onClick = {})
        }
        composeRule.onNodeWithTag(TEST_TAG_MEGA_BUTTON_WITH_ICON, useUnmergedTree = true)
            .assertIsEnabled()
    }

    @Test
    fun `test that button is disabled`() {
        composeRule.setContent {
            MegaButtonWithIcon(
                iconColor = iconColor,
                enabled = false,
                onClick = {})
        }
        composeRule.onNodeWithTag(TEST_TAG_MEGA_BUTTON_WITH_ICON, useUnmergedTree = true)
            .assertIsNotEnabled()
    }

    @Test
    fun `test that button is clicked`() {
        val onClick = mock<() -> Unit>()

        composeRule.setContent {
            MegaButtonWithIcon(
                iconColor = iconColor,
                enabled = true,
                onClick = onClick
            )
        }
        composeRule.onNodeWithTag(TEST_TAG_MEGA_BUTTON_WITH_ICON, useUnmergedTree = true)
            .performClick()
    }
}