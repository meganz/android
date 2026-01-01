package mega.privacy.android.domain.usecase

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.chat.ChatPresenceConfig
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import mega.privacy.android.domain.repository.ChatRepository
import mega.privacy.android.domain.repository.NetworkRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MonitorChatSignalPresenceUseCaseTest {
    private lateinit var underTest: MonitorChatSignalPresenceUseCase

    private val networkRepository = mock<NetworkRepository>()
    private val chatRepository = mock<ChatRepository>()

    @BeforeAll
    fun setUp() {
        underTest = MonitorChatSignalPresenceUseCase(
            networkRepository = networkRepository,
            chatRepository = chatRepository
        )
    }

    @AfterEach
    fun tearDown() {
        reset(networkRepository, chatRepository)
    }

    @Test
    fun `test that flow emits when config is not null and not pending`() = runTest {
        val signalPresenceFlow = MutableSharedFlow<Unit>()
        val config = createChatPresenceConfig(isPending = false)

        whenever(networkRepository.monitorChatSignalPresence()).thenReturn(signalPresenceFlow)
        whenever(chatRepository.getChatPresenceConfig()) doReturn config

        underTest().test {
            signalPresenceFlow.emit(Unit)
            assertThat(awaitItem()).isEqualTo(Unit)
        }
    }

    @Test
    fun `test that flow does not emit when config is null`() = runTest {
        val signalPresenceFlow = MutableSharedFlow<Unit>()

        whenever(networkRepository.monitorChatSignalPresence()).thenReturn(signalPresenceFlow)
        whenever(chatRepository.getChatPresenceConfig()) doReturn null

        underTest().test {
            signalPresenceFlow.emit(Unit)
            expectNoEvents()
        }
    }

    @Test
    fun `test that flow does not emit when config is pending`() = runTest {
        val signalPresenceFlow = MutableSharedFlow<Unit>()
        val config = createChatPresenceConfig(isPending = true)

        whenever(networkRepository.monitorChatSignalPresence()).thenReturn(signalPresenceFlow)
        whenever(chatRepository.getChatPresenceConfig()) doReturn config

        underTest().test {
            signalPresenceFlow.emit(Unit)
            expectNoEvents()
        }
    }

    @Test
    fun `test that flow emits multiple times when conditions are met`() = runTest {
        val signalPresenceFlow = MutableSharedFlow<Unit>()
        val config = createChatPresenceConfig(isPending = false)

        whenever(networkRepository.monitorChatSignalPresence()).thenReturn(signalPresenceFlow)
        whenever(chatRepository.getChatPresenceConfig()) doReturn config

        underTest().test {
            signalPresenceFlow.emit(Unit)
            assertThat(awaitItem()).isEqualTo(Unit)

            signalPresenceFlow.emit(Unit)
            assertThat(awaitItem()).isEqualTo(Unit)
        }
    }

    @Test
    fun `test that flow filters correctly when config changes from pending to not pending`() =
        runTest {
            val signalPresenceFlow = MutableSharedFlow<Unit>()
            val pendingConfig = createChatPresenceConfig(isPending = true)
            val notPendingConfig = createChatPresenceConfig(isPending = false)

            whenever(networkRepository.monitorChatSignalPresence()).thenReturn(signalPresenceFlow)

            underTest().test {
                whenever(chatRepository.getChatPresenceConfig()) doReturn pendingConfig
                signalPresenceFlow.emit(Unit)
                expectNoEvents()

                whenever(chatRepository.getChatPresenceConfig()) doReturn notPendingConfig
                signalPresenceFlow.emit(Unit)
                assertThat(awaitItem()).isEqualTo(Unit)
            }
        }

    private fun createChatPresenceConfig(
        onlineStatus: UserChatStatus = UserChatStatus.Online,
        isAutoAwayEnabled: Boolean = false,
        autoAwayTimeout: Long = 0L,
        isPersist: Boolean = false,
        isPending: Boolean = false,
        isLastGreenVisible: Boolean = false,
    ) = ChatPresenceConfig(
        onlineStatus = onlineStatus,
        isAutoAwayEnabled = isAutoAwayEnabled,
        autoAwayTimeout = autoAwayTimeout,
        isPersist = isPersist,
        isPending = isPending,
        isLastGreenVisible = isLastGreenVisible
    )
}

