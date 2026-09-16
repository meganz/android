package mega.privacy.android.feature.chat.list.mapper

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.feature.chat.list.model.ChatRoomActionMessage
import mega.privacy.android.shared.resources.R as sharedR
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChatRoomActionPluralMapperTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private val underTest = ChatRoomActionPluralMapper(context)

    @Test
    fun `test that invoke returns the plural form when several chats were archived`() {
        val actual = underTest(
            ChatRoomActionMessage.Plural(
                messageId = sharedR.plurals.chat_list_chats_archived_message,
                quantity = 3,
            )
        )

        assertThat(actual).isEqualTo("3 chats were archived")
    }

    @Test
    fun `test that invoke returns the singular form when one chat was archived`() {
        val actual = underTest(
            ChatRoomActionMessage.Plural(
                messageId = sharedR.plurals.chat_list_chats_archived_message,
                quantity = 1,
            )
        )

        assertThat(actual).isEqualTo("1 chat was archived")
    }

    @Test
    fun `test that invoke returns the plural form when several chats were unarchived`() {
        val actual = underTest(
            ChatRoomActionMessage.Plural(
                messageId = sharedR.plurals.chat_list_chats_unarchived_message,
                quantity = 5,
            )
        )

        assertThat(actual).isEqualTo("5 chats were unarchived")
    }

    @Test
    fun `test that invoke returns the singular form when one chat was unarchived`() {
        val actual = underTest(
            ChatRoomActionMessage.Plural(
                messageId = sharedR.plurals.chat_list_chats_unarchived_message,
                quantity = 1,
            )
        )

        assertThat(actual).isEqualTo("1 chat was unarchived")
    }
}
