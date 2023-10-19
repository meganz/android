package mega.privacy.android.domain.usecase.chat

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.ChatRoom
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import mega.privacy.android.domain.usecase.contact.GetUserOnlineStatusByHandleUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.util.stream.Stream

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetUserChatStatusByChatUseCaseTest {

    private lateinit var underTest: GetUserChatStatusByChatUseCase

    private val getUserOnlineStatusByHandleUseCase = mock<GetUserOnlineStatusByHandleUseCase>()

    @BeforeEach
    fun setup() {
        underTest = GetUserChatStatusByChatUseCase(getUserOnlineStatusByHandleUseCase)
    }

    @AfterEach
    fun resetMocks() {
        reset(getUserOnlineStatusByHandleUseCase)
    }

    @Test
    fun `test that get user chat status by chat use returns null if chat is a group`() =
        runTest {
            val chat = mock<ChatRoom> {
                on { isGroup }.thenReturn(true)
            }
            underTest.invoke(chat)
            Truth.assertThat(underTest.invoke(chat)).isEqualTo(null)
        }

    @ParameterizedTest(name = " if repository returns {0}")
    @MethodSource("provideTestParameters")
    fun `test that user chat status mapper returns correctly`(expectedUserChatStatus: UserChatStatus) =
        runTest {
            val chat = mock<ChatRoom> {
                on { isGroup }.thenReturn(false)
                on { peerHandlesList }.thenReturn(listOf(1, 2))
            }
            whenever(getUserOnlineStatusByHandleUseCase(any())).thenReturn(expectedUserChatStatus)
            Truth.assertThat(underTest(chat)).isEqualTo(expectedUserChatStatus)
        }

    private fun provideTestParameters(): Stream<Arguments> = Stream.of(
        Arguments.of(UserChatStatus.Offline),
        Arguments.of(UserChatStatus.Away),
        Arguments.of(UserChatStatus.Online),
        Arguments.of(UserChatStatus.Busy),
        Arguments.of(UserChatStatus.Invalid),
    )
}