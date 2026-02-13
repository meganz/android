package mega.privacy.android.feature.myaccount.presentation.widget.view

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import mega.privacy.android.feature.myaccount.presentation.model.EmojiAvatarContent
import mega.privacy.android.feature.myaccount.presentation.model.PhotoAvatarContent
import mega.privacy.android.feature.myaccount.presentation.model.TextAvatarContent
import mega.privacy.android.icon.pack.R as iconPackR
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
@Ignore("The test is Flaky")
class AvatarViewTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that emoji avatar is shown when there is emoji at beginning of name`() = runTest {
        composeTestRule.setContent {
            Avatar(
                modifier = Modifier.Companion,
                content = EmojiAvatarContent(
                    emojiContent = iconPackR.drawable.emoji_twitter_1f604,
                    backgroundColor = Color.Red.toArgb(),
                )
            )
        }
        composeTestRule.run {
            onNodeWithTag(testTag = "EmojiAvatar").assertIsDisplayed()
        }
    }

    @Test
    fun `test that text avatar is shown when there is no emoji at beginning of name`() = runTest {
        composeTestRule.setContent {
            Avatar(
                modifier = Modifier.Companion,
                content = TextAvatarContent(
                    avatarText = "R",
                    backgroundColor = Color.Red.toArgb(),
                )
            )
        }
        composeTestRule.run {
            onNodeWithTag(testTag = "TextAvatar").assertIsDisplayed()
        }
    }

    @Test
    fun `test that photo avatar is shown when local photo avatar is available`() = runTest {
        composeTestRule.setContent {
            Avatar(
                modifier = Modifier.Companion,
                content = PhotoAvatarContent(
                    path = "file:/path/to/avatar/file.jpg",
                    size = 0L
                )
            )
        }
        composeTestRule.run {
            onNodeWithTag(testTag = "PhotoAvatar").assertIsDisplayed()
        }
    }
}