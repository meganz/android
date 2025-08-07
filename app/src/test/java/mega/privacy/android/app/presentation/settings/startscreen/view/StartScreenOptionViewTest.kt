package mega.privacy.android.app.presentation.settings.startscreen.view

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.icon.pack.IconPack
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class StartScreenOptionViewTest {

    @get:Rule
    var composeRule = createComposeRule()

    @Test
    fun `test that text is displayed`() {
        val expected = "Home"
        composeRule.setContent {
            StartScreenOptionView(
                icon = IconPack.Medium.Thin.Outline.Mega,
                text = expected,
                isSelected = false,
                onClick = {}
            )
        }

        composeRule.onNodeWithText(expected).assertIsDisplayed()
    }

    @Test
    fun `test that icon is displayed`() {
        val expectedResId = IconPack.Medium.Thin.Outline.Mega
        composeRule.setContent {
            StartScreenOptionView(
                icon = expectedResId,
                text = "expected",
                isSelected = false,
                onClick = {}
            )
        }

        composeRule.onNodeWithTag(expectedResId.toString(), useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun `test that checkmark is displayed if selected`() {
        composeRule.setContent {
            StartScreenOptionView(
                icon = IconPack.Medium.Thin.Outline.Mega,
                text = "expected",
                isSelected = true,
                onClick = {}
            )
        }

        composeRule.onNodeWithTag(
            IconPack.Medium.Thin.Outline.Mega.toString(),
            useUnmergedTree = true
        )
            .assertIsDisplayed()
    }

    @Test
    fun `test that checkmark is not displayed if not selected`() {
        composeRule.setContent {
            StartScreenOptionView(
                icon = IconPack.Medium.Thin.Outline.Mega,
                text = "expected",
                isSelected = false,
                onClick = {}
            )
        }

        composeRule.onNodeWithTag(IconPack.Medium.Thin.Outline.Mega.toString())
            .assertDoesNotExist()
    }

    @Test
    fun `test that on click is called when control is clicked`() {
        val onClick = mock<() -> Unit>()
        composeRule.setContent {
            StartScreenOptionView(
                icon = IconPack.Medium.Thin.Outline.Mega,
                text = "expected",
                isSelected = false,
                onClick = onClick
            )
        }

        composeRule.onRoot().performClick()

        verify(onClick).invoke()
    }
}