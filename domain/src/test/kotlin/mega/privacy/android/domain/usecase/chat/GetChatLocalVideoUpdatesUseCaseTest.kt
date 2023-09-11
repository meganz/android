package mega.privacy.android.domain.usecase.chat

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.ChatVideoUpdate
import mega.privacy.android.domain.repository.CallRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GetChatLocalVideoUpdatesUseCaseTest {
    private val callRepository: CallRepository = mock()

    private lateinit var underTest: GetChatLocalVideoUpdatesUseCase

    @BeforeEach
    fun setup() {
        underTest = GetChatLocalVideoUpdatesUseCase(callRepository)
    }

    @Test
    fun `test that getChatLocalVideoUpdates is called when invoke is called`() = runTest {
        val chatVideoUpdate = ChatVideoUpdate(1920, 1080, ByteArray(0))
        whenever(callRepository.getChatLocalVideoUpdates(1L)).thenReturn(flowOf(chatVideoUpdate))

        underTest.invoke(1L)

        verify(callRepository).getChatLocalVideoUpdates(1L)
        verifyNoMoreInteractions(callRepository)
    }
}
