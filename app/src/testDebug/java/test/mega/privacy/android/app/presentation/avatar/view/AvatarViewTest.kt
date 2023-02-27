package test.mega.privacy.android.app.presentation.avatar.view

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.avatar.model.EmojiAvatarContent
import mega.privacy.android.app.presentation.avatar.model.TextAvatarContent
import mega.privacy.android.app.presentation.avatar.view.Avatar
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
class AvatarViewTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that emoji avatar is shown when there is emoji at beginning of name`() = runTest {
        composeTestRule.setContent {
            Avatar(
                modifier = Modifier,
                avatarBgColor = Color.Red.toArgb(),
                content = EmojiAvatarContent(
                    emojiContent = R.drawable.emoji_twitter_0033_fe0f_20e3
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
                modifier = Modifier,
                avatarBgColor = Color.Red.toArgb(),
                content = TextAvatarContent(
                    avatarText = "R"
                )
            )
        }
        composeTestRule.run {
            onNodeWithTag(testTag = "TextAvatar").assertIsDisplayed()
        }
    }
}
¬