package mega.privacy.android.shared.original.core.ui.controls.buttons

import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MegaFloatingActionButtonTest {
    @get:Rule
    var composeRule = createComposeRule()

    @Test
    fun `test that fab content is shown correctly`() {
        val iconTag = "fab_icon"
        composeRule.setContent {
            MegaFloatingActionButton(
                onClick = {}) {
                TestIcon(Modifier.testTag(iconTag))
            }
        }
        composeRule.onNodeWithTag(iconTag, useUnmergedTree = true).assertExists()
    }

    @Test
    fun `test that fab is clickable if it is enabled`() {
        var clicked = false
        val tag = "fab"
        composeRule.setContent {
            MegaFloatingActionButton(
                modifier = Modifier.testTag(tag),
                onClick = { clicked = true },
                enabled = true,
            ) {
                TestIcon()
            }
        }
        composeRule.onNodeWithTag("fab").performClick()
        assertThat(clicked).isTrue()
    }

    @Test
    fun `test that fab is not clickable if it is not enabled`() {
        var clicked = false
        val tag = "fab"
        composeRule.setContent {
            MegaFloatingActionButton(
                modifier = Modifier.testTag(tag),
                onClick = { clicked = true },
                enabled = false,
            ) {
                TestIcon()
            }
        }
        composeRule.onNodeWithTag("fab").performClick()
        assertThat(clicked).isFalse()
    }

    @Test
    fun `test that big fab has correct size`() {
        testFabHasCorrectSize(FloatingActionButtonStyle.Big)
    }

    @Test
    fun `test that medium fab has correct size`() {
        testFabHasCorrectSize(FloatingActionButtonStyle.Medium)
    }

    @Test
    fun `test that small fab has correct size`() {
        testFabHasCorrectSize(FloatingActionButtonStyle.Small)
    }

    @Test
    fun `test that small without elevation fab has correct size`() {
        testFabHasCorrectSize(FloatingActionButtonStyle.SmallWithoutElevation)
    }

    private fun testFabHasCorrectSize(style: FloatingActionButtonStyle) {
        val tag = "fab"
        composeRule.setContent {
            MegaFloatingActionButton(
                modifier = Modifier.testTag(tag),
                style = style,
                onClick = {}) {
                TestIcon()
            }
        }
        composeRule.onNodeWithTag(tag).assertHeightIsEqualTo(style.size)
        composeRule.onNodeWithTag(tag).assertWidthIsEqualTo(style.size)
    }

    @Composable
    private fun TestIcon(modifier: Modifier = Modifier) = Icon(
        modifier = modifier.size(16.dp),
        imageVector = Icons.Default.Add,
        contentDescription = "",
    )

}