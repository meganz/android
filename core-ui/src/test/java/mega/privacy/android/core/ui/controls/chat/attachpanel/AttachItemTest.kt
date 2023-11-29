package mega.privacy.android.core.ui.controls.chat.attachpanel

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.core.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@RunWith(AndroidJUnit4::class)
class AttachItemTest {

    @get:Rule
    var composeRule = createComposeRule()

    private val iconId = R.drawable.ic_menu
    private val itemName = "Item"
    private val itemTag = "Tag"

    @Test
    fun `test attach item shows correctly`() {
        initComposeRule()
        composeRule.onNodeWithTag(itemTag, true).assertIsDisplayed()
        composeRule.onNodeWithText(itemName).assertIsDisplayed()
        composeRule.onNodeWithTag(TEST_TAG_ATTACH_ITEM_ICON, true).assertIsDisplayed()
    }

    @Test
    fun `test attach item click is invoked`() {
        val onItemClick = mock<() -> Unit>()
        initComposeRule(onItemClick)
        composeRule.onNodeWithTag(itemTag).apply {
            assertIsDisplayed()
            performClick()
        }
        verify(onItemClick).invoke()
    }

    private fun initComposeRule(onItemClick: () -> Unit = {}) {
        composeRule.setContent {
            AttachItem(
                iconId = iconId,
                itemName = itemName,
                onItemClick = onItemClick,
                modifier = Modifier.testTag(itemTag)
            )
        }
    }
}