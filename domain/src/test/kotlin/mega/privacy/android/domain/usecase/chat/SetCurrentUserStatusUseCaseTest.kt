package mega.privacy.android.domain.usecase.chat

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import mega.privacy.android.domain.repository.ChatParticipantsRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SetCurrentUserStatusUseCaseTest {
    private val chatParticipantsRepository: ChatParticipantsRepository = mock()

    private lateinit var underTest: SetCurrentUserStatusUseCase

    @BeforeAll
    fun setup() {
        underTest = SetCurrentUserStatusUseCase(chatParticipantsRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(chatParticipantsRepository)
    }

    @ParameterizedTest(name = "test that ChatParticipantsRepository invoke {0} when UseCase invoke {0}")
    @EnumSource(UserChatStatus::class)
    fun `test that setOnlineStatus invoke correctly`(status: UserChatStatus) = runTest {
        underTest(status)
        verify(chatParticipantsRepository).setOnlineStatus(status)
    }
}