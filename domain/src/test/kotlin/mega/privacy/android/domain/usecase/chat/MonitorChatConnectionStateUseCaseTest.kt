package mega.privacy.android.domain.usecase.chat

import app.cash.turbine.test
import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.ChatConnectionState
import mega.privacy.android.domain.entity.chat.ChatConnectionStatus
import mega.privacy.android.domain.repository.ContactsRepository
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class MonitorChatConnectionStateUseCaseTest {

    private val contactsRepository = mock<ContactsRepository>()
    private val underTest = MonitorChatConnectionStateUseCase(contactsRepository)

    private val chatId = 123456L
    private val sampleData = ChatConnectionState(chatId, ChatConnectionStatus.Online)


    @Test
    fun `test that monitor chat connection state returns flow of chat connection state`() =
        runTest {
            whenever(contactsRepository.monitorChatConnectionStateUpdates()).thenReturn(
                flowOf(sampleData)
            )
            underTest().test {
                val actual = awaitItem()
                awaitComplete()
                Truth.assertThat(actual.chatId).isEqualTo(chatId)
            }
        }
}