package mega.privacy.android.shared.original.core.ui.controls.controlssliders

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MegaSwitchTest {
    @get:Rule
    var composeRule = createComposeRule()

    @Test
    fun `test that checked icon is displayed when the switch is checked`() {
        composeRule.setContent {
            testSwitch(checked = true)
        }
        composeRule.onNodeWithTag(CHECKED_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that checked icon is not displayed when the switch is not checked`() {
        composeRule.setContent {
            testSwitch(checked = false)
        }
        composeRule.onNodeWithTag(CHECKED_TAG).assertIsNotDisplayed()
    }

    @Test
    fun `test that on checked listener returns false when the switch was checked`() {
        var checked = null as Boolean?
        composeRule.setContent {
            testSwitch(
                checked = true,
                onCheckedChange = { checked = it }
            )
        }
        composeRule.onNodeWithTag(tag).performClick()
        Truth.assertThat(checked).isFalse()
    }

    @Test
    fun `test that on checked listener returns true when the switch was not checked`() {
        var checked = null as Boolean?
        composeRule.setContent {
            testSwitch(
                checked = false,
                onCheckedChange = { checked = it }
            )
        }
        composeRule.onNodeWithTag(tag).performClick()
        Truth.assertThat(checked).isTrue()
    }

    @Test
    fun `test that switch is toggleable if it is enabled`() {
        var checked = false
        composeRule.setContent {
            testSwitch(checked = checked, onCheckedChange = { checked = it })
        }
        composeRule.onNodeWithTag(tag).performClick()
        Truth.assertThat(checked).isTrue()
    }

    @Test
    fun `test that switch is not toggleable if it is not enabled`() {
        var checked = false
        composeRule.setContent {
            testSwitch(checked = checked, onCheckedChange = { checked = it }, enabled = false)
        }
        composeRule.onNodeWithTag(tag).performClick()
        Truth.assertThat(checked).isFalse()
    }

    @Test
    fun `test that switch is clickable if it is enabled`() {
        var changed = false
        composeRule.setContent {
            testSwitch(onClickListener = { changed = true })
        }
        composeRule.onNodeWithTag(tag).performClick()
        Truth.assertThat(changed).isTrue()
    }

    @Test
    fun `test that switch is not clickable if it is not enabled`() {
        var changed = false
        composeRule.setContent {
            testSwitch(onClickListener = { changed = true }, enabled = false)
        }
        composeRule.onNodeWithTag(tag).performClick()
        Truth.assertThat(changed).isFalse()
    }

    fun `test that both listeners work together`() {
        var changed = false
        var checked = false
        composeRule.setContent {
            testSwitch(
                onClickListener = { changed = true },
                checked = checked,
                onCheckedChange = { checked = it })
        }
        composeRule.onNodeWithTag(tag).performClick()
        Truth.assertThat(changed).isTrue()
        Truth.assertThat(checked).isTrue()
    }

    @Composable
    private fun testSwitch(
        checked: Boolean = true,
        enabled: Boolean = true,
        onCheckedChange: ((Boolean) -> Unit)? = null,
        onClickListener: (() -> Unit)? = null,
    ) = MegaSwitch(
        modifier = Modifier.testTag(tag),
        checked = checked,
        enabled = enabled,
        onCheckedChange = onCheckedChange,
        onClickListener = onClickListener
    )

    private val tag = "switch"

}