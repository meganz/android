package mega.privacy.android.shared.contact.mapper


import androidx.compose.ui.graphics.Color
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.chat.ChatAvatarItem
import mega.privacy.android.shared.contact.model.AvatarData
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ChatAvatarItemMapperTest {

    private lateinit var underTest: ChatAvatarItemMapper

    @BeforeEach
    fun setUp() {
        underTest = ChatAvatarItemMapper()
    }

    @Test
    fun `test that avatar is Image when uri is non-null`() {
        val chatAvatarItem = ChatAvatarItem(uri = "/data/user/0/avatar.jpg")

        val result = underTest(chatAvatarItem)

        assertThat(result).isInstanceOf(AvatarData.Image::class.java)
        assertThat((result as AvatarData.Image).file.path)
            .isEqualTo("/data/user/0/avatar.jpg")
    }

    @Test
    fun `test that avatar is Initials when uri is null`() {
        val chatAvatarItem = ChatAvatarItem(
            placeholderText = "A",
            uri = null,
            color = 0xFF2E7D32.toInt(),
        )

        val result = underTest(chatAvatarItem)

        assertThat(result).isInstanceOf(AvatarData.Initials::class.java)
    }

    @Test
    fun `test that initials avatar uses placeholder text`() {
        val chatAvatarItem = ChatAvatarItem(placeholderText = "A", uri = null)

        val result = underTest(chatAvatarItem) as AvatarData.Initials

        assertThat(result.initials).isEqualTo("A")
    }

    @Test
    fun `test that initials avatar uses empty string when placeholder text is null`() {
        val chatAvatarItem = ChatAvatarItem(placeholderText = null, uri = null)

        val result = underTest(chatAvatarItem) as AvatarData.Initials

        assertThat(result.initials).isEmpty()
    }

    @Test
    fun `test that initials avatar uses color when non-null`() {
        val chatAvatarItem = ChatAvatarItem(
            placeholderText = "A",
            uri = null,
            color = 0xFF2E7D32.toInt(),
        )

        val result = underTest(chatAvatarItem) as AvatarData.Initials

        assertThat(result.avatarColor).isEqualTo(Color(0xFF2E7D32.toInt()))
    }

    @Test
    fun `test that initials avatar falls back to black when color is null`() {
        val chatAvatarItem = ChatAvatarItem(
            placeholderText = "A",
            uri = null,
            color = null,
        )

        val result = underTest(chatAvatarItem) as AvatarData.Initials

        assertThat(result.avatarColor).isEqualTo(Color.Black)
    }
}
