package test.mega.privacy.android.app.presentation.meeting.chat.mapper

import com.google.common.truth.Truth
import mega.privacy.android.app.presentation.meeting.chat.mapper.UiReactionListMapper
import mega.privacy.android.core.ui.controls.chat.messages.reaction.model.UIReaction
import mega.privacy.android.domain.entity.chat.messages.reactions.Reaction
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UiReactionListMapperTest {

    private lateinit var underTest: UiReactionListMapper

    @BeforeAll
    fun setup() {
        underTest = UiReactionListMapper()
    }

    @Test
    fun `test that ui reaction list mapper returns correctly`() {
        val reaction1 = "reaction1"
        val reaction1Count = 2
        val reaction1HasMe = true
        val reaction2 = "reaction2"
        val reaction2Count = 5
        val reaction2HasMe = true
        val reaction3 = "reaction3"
        val reaction3Count = 1
        val reaction3HasMe = false
        val reactions = listOf(
            Reaction(
                reaction = reaction1,
                count = reaction1Count,
                userHandles = listOf(1L, 2L),
                hasMe = reaction1HasMe
            ), Reaction(
                reaction = reaction2,
                count = reaction2Count,
                userHandles = listOf(1L, 2L, 3L, 4L, 5L),
                hasMe = reaction2HasMe
            ), Reaction(
                reaction = reaction3,
                count = reaction3Count,
                userHandles = listOf(1L),
                hasMe = reaction3HasMe
            )
        )
        Truth.assertThat(underTest(reactions)).containsExactly(
            UIReaction(
                reaction = reaction1,
                count = reaction1Count,
                hasMe = reaction1HasMe
            ),
            UIReaction(
                reaction = reaction2,
                count = reaction2Count,
                hasMe = reaction2HasMe
            ),
            UIReaction(
                reaction = reaction3,
                count = reaction3Count,
                hasMe = reaction3HasMe
            )
        )
    }
}