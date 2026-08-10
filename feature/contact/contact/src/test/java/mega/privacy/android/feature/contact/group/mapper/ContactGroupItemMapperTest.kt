package mega.privacy.android.feature.contact.group.mapper

import androidx.compose.ui.graphics.Color
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.chat.ChatAvatarItem
import mega.privacy.android.domain.entity.contacts.group.ContactGroup
import mega.privacy.android.shared.contact.mapper.ChatAvatarItemMapper
import mega.privacy.android.shared.contact.model.AvatarData
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class ContactGroupItemMapperTest {

    private lateinit var underTest: ContactGroupItemMapper

    private val avatarItemMapper = mock<ChatAvatarItemMapper>()

    @BeforeEach
    fun setUp() {
        underTest = ContactGroupItemMapper(
            avatarItemMapper = avatarItemMapper,
        )
    }

    @AfterEach
    fun tearDown() {
        reset(avatarItemMapper)
    }

    @Test
    fun `test that chatId is mapped from group chatId`() {
        val result = underTest(createContactGroup(chatId = 123L))

        assertThat(result.chatId).isEqualTo(123L)
    }

    @Test
    fun `test that name is mapped from group title`() {
        val result = underTest(createContactGroup(title = "My Group"))

        assertThat(result.name).isEqualTo("My Group")
    }

    @Test
    fun `test that isPrivate is true when group is not public`() {
        val result = underTest(createContactGroup(isPublic = false))

        assertThat(result.isPrivate).isTrue()
    }

    @Test
    fun `test that isPrivate is false when group is public`() {
        val result = underTest(createContactGroup(isPublic = true))

        assertThat(result.isPrivate).isFalse()
    }

    @Test
    fun `test that avatarData is empty when group avatar list is empty`() {
        val result = underTest(createContactGroup(avatar = emptyList()))

        assertThat(result.avatarData).isEmpty()
    }

    @Test
    fun `test that each avatar is mapped by avatar item mapper`() {
        val avatars = listOf(
            ChatAvatarItem(placeholderText = "A"),
            ChatAvatarItem(placeholderText = "B"),
        )
        val mappedA = AvatarData.Initials(initials = "A", avatarColor = Color.Black)
        val mappedB = AvatarData.Initials(initials = "B", avatarColor = Color.Black)
        whenever(avatarItemMapper(avatars[0])).thenReturn(mappedA)
        whenever(avatarItemMapper(avatars[1])).thenReturn(mappedB)

        val result = underTest(createContactGroup(avatar = avatars))

        assertThat(result.avatarData).containsExactly(mappedA, mappedB).inOrder()
    }

    @Test
    fun `test that avatar item mapper is called for each avatar`() {
        val avatars = listOf(
            ChatAvatarItem(placeholderText = "A"),
            ChatAvatarItem(placeholderText = "B"),
        )
        whenever(avatarItemMapper(any())).thenReturn(
            AvatarData.Initials(initials = "", avatarColor = Color.Black)
        )

        underTest(createContactGroup(avatar = avatars))

        verify(avatarItemMapper).invoke(avatars[0])
        verify(avatarItemMapper).invoke(avatars[1])
    }

    private fun createContactGroup(
        chatId: Long = 1L,
        title: String = "Group",
        avatar: List<ChatAvatarItem> = emptyList(),
        isPublic: Boolean = true,
    ) = ContactGroup(
        chatId = chatId,
        title = title,
        avatar = avatar,
        isPublic = isPublic,
    )
}
