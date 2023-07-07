package mega.privacy.android.domain.usecase.chat

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.contacts.UserStatus
import mega.privacy.android.domain.repository.ChatParticipantsRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GetCurrentUserStatusUseCaseTest {
    private val chatParticipantsRepository: ChatParticipantsRepository = mock()

    private lateinit var underTest: GetCurrentUserStatusUseCase

    @BeforeAll
    fun setup() {
        underTest = GetCurrentUserStatusUseCase(chatParticipantsRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(chatParticipantsRepository)
    }

    @ParameterizedTest(name = "test that UseCase returns correctly when repository returns {0}")
    @EnumSource(UserStatus::class)
    fun `test that getCurrentStatus returns correctly`(status: UserStatus) = runTest {
        whenever(chatParticipantsRepository.getCurrentStatus()).thenReturn(status)
        Truth.assertThat(underTest()).isEqualTo(status)
    }
}