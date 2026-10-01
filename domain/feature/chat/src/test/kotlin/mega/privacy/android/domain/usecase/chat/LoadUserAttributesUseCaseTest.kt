package mega.privacy.android.domain.usecase.chat

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.ChatParticipantsRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LoadUserAttributesUseCaseTest {

    private lateinit var underTest: LoadUserAttributesUseCase

    private val chatParticipantsRepository = mock<ChatParticipantsRepository>()

    @BeforeAll
    fun setup() {
        underTest = LoadUserAttributesUseCase(chatParticipantsRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(chatParticipantsRepository)
    }

    @Test
    fun `test that use case invokes chat participants repository`() = runTest {
        val chatId = 123L
        val userHandles = listOf(1L, 2L, 3L)
        whenever(chatParticipantsRepository.loadUserAttributes(chatId, userHandles))
            .thenReturn(Unit)
        underTest.invoke(chatId, userHandles)
        verify(chatParticipantsRepository).loadUserAttributes(chatId, userHandles)
    }
}