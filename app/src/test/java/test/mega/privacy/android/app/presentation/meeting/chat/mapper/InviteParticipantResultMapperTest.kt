package test.mega.privacy.android.app.presentation.meeting.chat.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.presentation.meeting.chat.mapper.InviteParticipantResultMapper
import mega.privacy.android.app.presentation.meeting.chat.model.InviteContactToChatResult
import mega.privacy.android.domain.entity.ChatRequest
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.exception.chat.ParticipantAlreadyExistsException
import nz.mega.sdk.MegaChatError
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class InviteParticipantResultMapperTest {

    private lateinit var underTest: InviteParticipantResultMapper

    private val chatRequest: ChatRequest = mock()

    @BeforeAll
    fun setUp() {
        underTest = InviteParticipantResultMapper()
    }

    @Test
    fun `test that general error is generated when there is one error and it is not ParticipantAlreadyExistsException`() {
        val list = listOf(
            Result.success(chatRequest),
            Result.failure(
                MegaException(
                    errorCode = MegaChatError.ERROR_ACCESS,
                    errorString = "general error"
                )
            )
        )
        assertThat(underTest(list)).isInstanceOf(InviteContactToChatResult.GeneralError::class.java)
    }

    @Test
    fun `test that SomeAddedSomeNot is generated when some participants are added and some fail to be added`() {
        val list = listOf(
            Result.success(chatRequest),
            Result.success(chatRequest),
            Result.failure(
                ParticipantAlreadyExistsException()
            ), Result.failure(
                MegaException(
                    errorCode = MegaChatError.ERROR_ACCESS,
                    errorString = "access error"
                )
            )
        )
        val result = underTest(list)
        assertThat(result).isInstanceOf(InviteContactToChatResult.SomeAddedSomeNot::class.java)
        (result as InviteContactToChatResult.SomeAddedSomeNot).let {
            assertThat(it.success).isEqualTo(2)
            assertThat(it.error).isEqualTo(2)
        }
    }

    @Test
    fun `test that MultipleContactsAdded is generated when multiple contacts are added successfully`() {
        val list = List(4) { Result.success(chatRequest) }
        assertThat(underTest(list)).isInstanceOf(InviteContactToChatResult.MultipleContactsAdded::class.java)
    }

    @Test
    fun `test that OnlyOneContactAdded is generated when only one contact is added to chat`() {
        val list = listOf(
            Result.success(chatRequest)
        )
        assertThat(underTest(list)).isInstanceOf(InviteContactToChatResult.OnlyOneContactAdded::class.java)
    }

    @Test
    fun `test that AlreadyExistsError is generated when there is only one contact and it is already added in chat`() {
        val list = listOf(
            Result.success(chatRequest),
            Result.failure(
                ParticipantAlreadyExistsException()
            )
        )
        assertThat(underTest(list)).isInstanceOf(InviteContactToChatResult.AlreadyExistsError::class.java)
    }
}