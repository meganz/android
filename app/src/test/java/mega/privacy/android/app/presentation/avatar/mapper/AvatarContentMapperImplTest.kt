package mega.privacy.android.app.presentation.avatar.mapper

import android.content.Context
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.R
import mega.privacy.android.app.components.twemoji.wrapper.EmojiManagerWrapper
import mega.privacy.android.app.presentation.avatar.model.EmojiAvatarContent
import mega.privacy.android.app.presentation.avatar.model.TextAvatarContent
import mega.privacy.android.data.wrapper.AvatarWrapper
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class AvatarContentMapperImplTest {

    lateinit var underTest: AvatarContentMapperImpl

    private val avatarWrapper: AvatarWrapper = mock()
    private val emojiManagerWrapper: EmojiManagerWrapper = mock()
    private val context: Context = mock()

    @Before
    fun setup() {
        underTest = AvatarContentMapperImpl(
            avatarWrapper = avatarWrapper,
            emojiManagerWrapper = emojiManagerWrapper,
            context = context,
        )
    }

    @Test
    fun `test that a text avatar content is returned when there is no emoji at begin of name`() =
        runTest {
            whenever(avatarWrapper.getFirstLetter(any())).thenReturn("L")
            whenever(emojiManagerWrapper.getFirstEmoji(any())).thenReturn(null)

            val result = underTest("full name")
            assertThat(result).isInstanceOf(TextAvatarContent::class.java)
        }

    @Test
    fun `test that a emoji avatar content is returned when there is emoji at begin of name`() =
        runTest {
            whenever(avatarWrapper.getFirstLetter(any())).thenReturn("L")
            whenever(emojiManagerWrapper.getFirstEmoji(any())).thenReturn(R.drawable.emoji_twitter_0033_fe0f_20e3)

            val result = underTest("full name")
            assertThat(result).isInstanceOf(EmojiAvatarContent::class.java)
        }

    @Test
    fun `test that a text avatar content is returned when full name is null`() = runTest {
        val defaultFirstName = "FirstName"
        val defaultLastName = "LastName"
        whenever(avatarWrapper.getFirstLetter("$defaultFirstName $defaultLastName")).thenReturn("F")
        whenever(emojiManagerWrapper.getFirstEmoji(any())).thenReturn(null)
        whenever(context.getString(R.string.first_name_text)).thenReturn(defaultFirstName)
        whenever(context.getString(R.string.lastname_text)).thenReturn(defaultLastName)

        val result = underTest(null)
        assertThat(result).isInstanceOf(TextAvatarContent::class.java)
    }
}
