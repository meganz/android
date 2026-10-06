package mega.privacy.android.feature.contact.conversation.navigation

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.navigation.destination.CreateGroupChatNavKey
import mega.privacy.android.navigation.destination.NewChatNavKey
import org.junit.jupiter.api.Test

class StartConversationEntryTest {

    @Test
    fun `test that toNewChatResult carries the emails and group settings`() {
        val groupResult = CreateGroupChatNavKey.NewGroupChatResult(
            emails = listOf("alice@example.com", "bob@example.com"),
            title = "Team",
            isEkr = true,
            isChatLink = false,
            allowAddParticipants = true,
            imageUri = UriPath("content://image"),
        )

        assertThat(groupResult.toNewChatResult()).isEqualTo(
            NewChatNavKey.NewChatResult(
                emails = listOf("alice@example.com", "bob@example.com"),
                groupSettings = NewChatNavKey.NewChatResult.GroupSettings(
                    title = "Team",
                    isEkr = true,
                    isChatLink = false,
                    allowAddParticipants = true,
                    imageUri = UriPath("content://image"),
                ),
            )
        )
    }

    @Test
    fun `test that toNewChatResult keeps group settings when the group has no other participants`() {
        val groupResult = CreateGroupChatNavKey.NewGroupChatResult(
            emails = emptyList(),
            title = null,
            isEkr = false,
            isChatLink = true,
            allowAddParticipants = false,
            imageUri = null,
        )

        val actual = groupResult.toNewChatResult()

        assertThat(actual.emails).isEmpty()
        assertThat(actual.groupSettings).isNotNull()
        assertThat(actual.groupSettings?.isChatLink).isTrue()
    }
}
