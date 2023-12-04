package mega.privacy.android.core.ui.controls.chat.messages

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.core.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LocationMessageViewTest {
    @get:Rule
    var composeRule = createComposeRule()

    @Test
    fun `test that title show correctly`() {
        val title = "Title"
        initComposeRuleContent(
            title = title,
            latlon = "latlon",
        )
        composeRule.onNodeWithText(title).assertExists()
    }

    @Test
    fun `test that latlon show correctly`() {
        val latlon = "latlon"
        initComposeRuleContent(
            title = "Title",
            latlon = latlon,
        )
        composeRule.onNodeWithText(latlon).assertExists()
    }

    private fun initComposeRuleContent(
        title: String,
        latlon: String,
    ) {
        composeRule.setContent {
            LocationMessageView(
                title = title,
                latlon = latlon,
                map = ImageBitmap.imageResource(R.drawable.ic_folder_incoming),
                isMe = true,
                modifier = Modifier,
            )
        }
    }
}

