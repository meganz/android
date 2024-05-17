package mega.privacy.android.shared.original.core.ui.controls.chat.messages

import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChatRickLinkMessageTest {

    @get:Rule
    var composeRule = createComposeRule()

    private val title = "Title"
    private val contentTitle = "Content title"
    private val contentDescription = "Content description"
    private val url = "https://mega.io"
    private val host = "mega.io"

    @Test
    fun `test that title show correctly`() {
        initComposeRuleContent()
        composeRule.onNodeWithText(title).assertExists()
    }

    @Test
    fun `test that content title show correctly`() {
        initComposeRuleContent()
        composeRule.onNodeWithText(contentTitle).assertExists()
    }

    @Test
    fun `test that content description show correctly`() {
        initComposeRuleContent()
        composeRule.onNodeWithText(contentDescription).assertExists()
    }

    @Test
    fun `test that url show correctly`() {
        initComposeRuleContent()
        composeRule.onNodeWithText(url).assertExists()
    }

    @Test
    fun `test that host show correctly`() {
        initComposeRuleContent()
        composeRule.onNodeWithText(host).assertExists()
    }

    private fun initComposeRuleContent() {
        composeRule.setContent {
            ChatRichLinkMessage(
                isMine = true,
                title = title,
                contentTitle = contentTitle,
                contentDescription = contentDescription,
                content = url,
                links = emptyList(),
                isEdited = false,
                host = host,
                image = null,
                icon = null,
                modifier = Modifier,
            )
        }
    }
}