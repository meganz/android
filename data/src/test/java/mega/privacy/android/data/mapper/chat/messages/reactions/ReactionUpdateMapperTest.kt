package mega.privacy.android.data.mapper.chat.messages.reactions

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.data.model.ChatRoomUpdate
import mega.privacy.android.domain.entity.chat.messages.reactions.ReactionUpdate
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ReactionUpdateMapperTest {

    private lateinit var underTest: ReactionUpdateMapper

    @BeforeAll
    fun setup() {
        underTest = ReactionUpdateMapper()
    }

    @Test
    fun `test that maps correctly`() {
        val onReactionUpdate = mock<ChatRoomUpdate.OnReactionUpdate> {
            on { msgId } doReturn 1L
            on { reaction } doReturn "reaction"
            on { count } doReturn 1
        }
        val reactionUpdate = with(onReactionUpdate) {
            ReactionUpdate(
                msgId,
                reaction,
                count
            )
        }
        assertThat(underTest.invoke(onReactionUpdate)).isEqualTo(reactionUpdate)
    }
}