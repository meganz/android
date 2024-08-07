package mega.privacy.android.domain.usecase.call

import app.cash.turbine.test
import com.google.common.truth.Truth
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.ChatVideoUpdate
import mega.privacy.android.domain.repository.CallRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GetCallRemoteVideoUpdatesUseCaseTest {
    private val callRepository: CallRepository = mock()

    private lateinit var underTest: GetCallRemoteVideoUpdatesUseCase

    @BeforeEach
    fun setup() {
        underTest = GetCallRemoteVideoUpdatesUseCase(callRepository)
    }

    @Test
    fun `test that when invoked it returns correctly`() = runTest {
        val chatId = 1L
        val clientId = 3L
        val hiRes = true
        val chatVideoUpdate = ChatVideoUpdate(1920, 1080, ByteArray(0))
        whenever(
            callRepository.getChatRemoteVideoUpdates(
                chatId = chatId,
                clientId = clientId,
                hiRes = hiRes
            )
        ).thenReturn(
            flowOf(
                chatVideoUpdate
            )
        )

        underTest(
            chatId = chatId,
            clientId = clientId,
            isHighRes = hiRes
        ).test {
            val item = awaitItem()
            Truth.assertThat(item).isEqualTo(chatVideoUpdate)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
