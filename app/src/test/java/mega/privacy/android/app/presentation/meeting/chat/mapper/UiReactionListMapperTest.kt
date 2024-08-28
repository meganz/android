package mega.privacy.android.app.presentation.meeting.chat.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.presentation.meeting.chat.mapper.EmojiShortCodeMapper
import mega.privacy.android.app.presentation.meeting.chat.mapper.UiReactionListMapper
import mega.privacy.android.domain.entity.chat.messages.reactions.Reaction
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.mock

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UiReactionListMapperTest {

    private lateinit var underTest: UiReactionListMapper

    private val expectedShortCode = ":shortcode:"
    private val emojiShortCodeMapper = mock<EmojiShortCodeMapper>() {
        on { invoke(any()) }.thenReturn(expectedShortCode)
    }

    @BeforeAll
    fun setup() {
        underTest = UiReactionListMapper(emojiShortCodeMapper)
    }

    @Test
    fun `test that ui reaction list mapper returns correctly`() {
        val reactions = listOf(
            Reaction(
                reaction = "reaction1",
                count = 2,
                userHandles = listOf(1L, 2L),
                hasMe = true
            ), Reaction(
                reaction = "reaction2",
                count = 5,
                userHandles = listOf(1L, 2L, 3L, 4L, 5L),
                hasMe = true
            ), Reaction(
                reaction = "reaction3",
                count = 1,
                userHandles = listOf(1L),
                hasMe = false
            )
        )

        underTest(reactions).forEachIndexed { index, uiReaction ->
            val reaction = reactions[index]
            assertThat(uiReaction.reaction).isEqualTo(reaction.reaction)
            assertThat(uiReaction.count).isEqualTo(reaction.count)
            assertThat(uiReaction.shortCode).isEqualTo(expectedShortCode)
            assertThat(uiReaction.hasMe).isEqualTo(reaction.hasMe)
            val uiReactionUserList = uiReaction.userList
            uiReactionUserList.forEachIndexed { userIndex, userValue ->
                assertThat(userValue.userHandle).isEqualTo(reaction.userHandles[userIndex])
            }
        }
    }
}