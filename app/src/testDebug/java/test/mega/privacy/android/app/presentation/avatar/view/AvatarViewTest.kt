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
import mega.privacy.android.app.presentation.avatar.model.PhotoAvatarContent
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
                content = EmojiAvatarContent(
                    emojiContent = R.drawable.emoji_twitter_0033_fe0f_20e3,
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
                modifier = Modifier,
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
                modifier = Modifier,
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
