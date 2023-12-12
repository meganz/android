package mega.privacy.android.core.ui.controls.chat.messages

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

    @Test
    fun `test that title show correctly`() {
        val title = "Title"
        initComposeRuleContent(
            isMe = true,
            title = title,
            contentTitle = "Content title",
            contentDescription = "Content description",
            url = "https://mega.io",
            host = "mega.io",
        )
        composeRule.onNodeWithText(title).assertExists()
    }

    @Test
    fun `test that content title show correctly`() {
        val contentTitle = "Content title"
        initComposeRuleContent(
            isMe = true,
            title = "Title",
            contentTitle = contentTitle,
            contentDescription = "Content description",
            url = "https://mega.io",
            host = "mega.io",
        )
        composeRule.onNodeWithText(contentTitle).assertExists()
    }

    @Test
    fun `test that content description show correctly`() {
        val contentDescription = "Content description"
        initComposeRuleContent(
            isMe = true,
            title = "Title",
            contentTitle = "Content title",
            contentDescription = contentDescription,
            url = "https://mega.io",
            host = "mega.io",
        )
        composeRule.onNodeWithText(contentDescription).assertExists()
    }

    @Test
    fun `test that url show correctly`() {
        val url = "https://mega.io"
        initComposeRuleContent(
            isMe = true,
            title = "Title",
            contentTitle = "Content title",
            contentDescription = "Content description",
            url = url,
            host = "mega.io",
        )
        composeRule.onNodeWithText(url).assertExists()
    }

    @Test
    fun `test that host show correctly`() {
        val host = "mega.io"
        initComposeRuleContent(
            isMe = true,
            title = "Title",
            contentTitle = "Content title",
            contentDescription = "Content description",
            url = "https://mega.io",
            host = host,
        )
        composeRule.onNodeWithText(host).assertExists()
    }

    private fun initComposeRuleContent(
        isMe: Boolean,
        title: String,
        contentTitle: String,
        contentDescription: String,
        url: String,
        host: String,
    ) {
        composeRule.setContent {
            ChatRichLinkMessage(
                isMe = isMe,
                title = title,
                contentTitle = contentTitle,
                contentDescription = contentDescription,
                url = url,
                host = host,
                image = null,
                icon = null,
                modifier = Modifier,
            )
        }
    }
}