package mega.privacy.android.app.presentation.avatar.mapper

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.sp
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.R
import mega.privacy.android.app.components.twemoji.wrapper.EmojiManagerWrapper
import mega.privacy.android.app.presentation.avatar.model.EmojiAvatarContent
import mega.privacy.android.app.presentation.avatar.model.PhotoAvatarContent
import mega.privacy.android.app.presentation.avatar.model.TextAvatarContent
import mega.privacy.android.data.wrapper.AvatarWrapper
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.io.File
import java.net.URI

@OptIn(ExperimentalCoroutinesApi::class)
class AvatarContentMapperImplTest {

    lateinit var underTest: AvatarContentMapperImpl

    private val avatarWrapper: AvatarWrapper = mock()
    private val emojiManagerWrapper: EmojiManagerWrapper = mock()
    private val context: Context = mock()
    private val localFile: File = mock()

    @Before
    fun setup() {
        underTest = AvatarContentMapperImpl(
            avatarWrapper = avatarWrapper,
            emojiManagerWrapper = emojiManagerWrapper,
            context = context,
            ioDispatcher = UnconfinedTestDispatcher()
        )
    }

    @Test
    fun `test that a text avatar content is returned when there is no emoji at begin of name and no local avatar file`() =
        runTest {
            whenever(avatarWrapper.getFirstLetter(any())).thenReturn("L")
            whenever(emojiManagerWrapper.getFirstEmoji(any())).thenReturn(null)

            val result = underTest(
                fullName = "full name",
                localFile = null,
                showBorder = true,
                backgroundColor = Color.Black.toArgb(),
                textSize = 38.sp
            )
            assertThat(result).isInstanceOf(TextAvatarContent::class.java)
        }

    @Test
    fun `test that an emoji avatar content is returned when there is emoji at begin of name and no local avatar file`() =
        runTest {
            whenever(avatarWrapper.getFirstLetter(any())).thenReturn("L")
            whenever(emojiManagerWrapper.getFirstEmoji(any())).thenReturn(R.drawable.emoji_twitter_0033_fe0f_20e3)

            val result = underTest(
                fullName = "full name",
                localFile = null,
                showBorder = true,
                backgroundColor = Color.Black.toArgb(),
                textSize = 38.sp,
            )
            assertThat(result).isInstanceOf(EmojiAvatarContent::class.java)
        }

    @Test
    fun `test that a text avatar content is returned when full name is null and no local avatar file`() =
        runTest {
            val defaultFirstName = "FirstName"
            val defaultLastName = "LastName"
            whenever(avatarWrapper.getFirstLetter("$defaultFirstName $defaultLastName")).thenReturn(
                "F"
            )
            whenever(emojiManagerWrapper.getFirstEmoji(any())).thenReturn(null)
            whenever(context.getString(R.string.first_name_text)).thenReturn(defaultFirstName)
            whenever(context.getString(R.string.lastname_text)).thenReturn(defaultLastName)

            val result = underTest(
                fullName = null,
                localFile = null,
                showBorder = true,
                backgroundColor = Color.Black.toArgb(),
                textSize = 38.sp,
            )
            assertThat(result).isInstanceOf(TextAvatarContent::class.java)
        }

    @Test
    fun `test that a photo avatar content is returned when there is local avatar file`() = runTest {
        val expectedFilePath = "file://local/avatar/file"
        whenever(localFile.exists()).thenReturn(true)
        whenever(localFile.length()).thenReturn(100)
        whenever(localFile.toURI()).thenReturn(URI(expectedFilePath))
        val result = underTest(
            fullName = "name",
            localFile = localFile,
            showBorder = true,
            backgroundColor = Color.Black.toArgb(),
            textSize = 38.sp,
        )
        assertThat(result).isInstanceOf(PhotoAvatarContent::class.java)
    }

    @Test
    fun `test that a text avatar content is returned if local avatar does not exist`() = runTest {
        whenever(localFile.exists()).thenReturn(false)
        whenever(avatarWrapper.getFirstLetter(any())).thenReturn("L")
        whenever(emojiManagerWrapper.getFirstEmoji(any())).thenReturn(null)

        val result = underTest(
            fullName = "full name",
            localFile = localFile,
            showBorder = true,
            backgroundColor = Color.Black.toArgb(),
            textSize = 38.sp,
        )
        assertThat(result).isInstanceOf(TextAvatarContent::class.java)
    }
}
