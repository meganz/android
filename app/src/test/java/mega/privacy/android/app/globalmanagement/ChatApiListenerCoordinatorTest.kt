package mega.privacy.android.app.globalmanagement

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.listeners.GlobalChatListener
import mega.privacy.android.app.meeting.CallSoundType
import mega.privacy.android.app.usecase.call.MonitorCallSoundsUseCase
import nz.mega.sdk.MegaChatApiAndroid
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ChatApiListenerCoordinatorTest {

    private val megaChatApi = mock<MegaChatApiAndroid>()
    private val chatRequestHandler = mock<MegaChatRequestHandler>()
    private val megaChatNotificationHandler = mock<MegaChatNotificationHandler>()
    private val globalChatListener = mock<GlobalChatListener>()
    private val monitorCallSoundsUseCase = mock<MonitorCallSoundsUseCase>()

    @BeforeEach
    fun resetMocks() {
        reset(
            megaChatApi,
            chatRequestHandler,
            megaChatNotificationHandler,
            globalChatListener,
            monitorCallSoundsUseCase,
        )
        whenever(monitorCallSoundsUseCase()).thenReturn(emptyFlow())
    }

    private fun createUnderTest(applicationScope: CoroutineScope) = ChatApiListenerCoordinator(
        megaChatApi = megaChatApi,
        chatRequestHandler = chatRequestHandler,
        megaChatNotificationHandler = megaChatNotificationHandler,
        globalChatListener = globalChatListener,
        monitorCallSoundsUseCase = monitorCallSoundsUseCase,
        applicationScope = applicationScope,
    )

    @Test
    fun `test that register adds all chat listeners and starts call sounds collection`() =
        runTest {
            val underTest = createUnderTest(backgroundScope)

            underTest.register()
            runCurrent()

            verify(megaChatApi).addChatRequestListener(chatRequestHandler)
            verify(megaChatApi).addChatNotificationListener(megaChatNotificationHandler)
            verify(megaChatApi).addChatListener(globalChatListener)
            verify(megaChatApi).addChatCallListener(any())
            verify(monitorCallSoundsUseCase).invoke()
        }

    @Test
    fun `test that register adds no listeners when called a second time`() = runTest {
        val underTest = createUnderTest(backgroundScope)

        underTest.register()
        underTest.register()
        runCurrent()

        verify(megaChatApi, times(1)).addChatRequestListener(chatRequestHandler)
        verify(megaChatApi, times(1)).addChatNotificationListener(megaChatNotificationHandler)
        verify(megaChatApi, times(1)).addChatListener(globalChatListener)
        verify(megaChatApi, times(1)).addChatCallListener(any())
        verify(monitorCallSoundsUseCase, times(1)).invoke()
    }

    @Test
    fun `test that unregister removes all chat listeners and cancels call sounds collection`() =
        runTest {
            var collecting = false
            whenever(monitorCallSoundsUseCase()).thenReturn(
                flow<CallSoundType> {
                    collecting = true
                    try {
                        awaitCancellation()
                    } finally {
                        collecting = false
                    }
                }
            )
            val underTest = createUnderTest(backgroundScope)
            underTest.register()
            runCurrent()
            assertThat(collecting).isTrue()

            underTest.unregister()
            runCurrent()

            verify(megaChatApi).removeChatRequestListener(chatRequestHandler)
            verify(megaChatApi).removeChatNotificationListener(megaChatNotificationHandler)
            verify(megaChatApi).removeChatListener(globalChatListener)
            verify(megaChatApi).removeChatCallListener(any())
            assertThat(collecting).isFalse()
        }

    @Test
    fun `test that register adds listeners again when called after unregister`() = runTest {
        val underTest = createUnderTest(backgroundScope)

        underTest.register()
        underTest.unregister()
        underTest.register()
        runCurrent()

        verify(megaChatApi, times(2)).addChatRequestListener(chatRequestHandler)
        verify(megaChatApi, times(2)).addChatNotificationListener(megaChatNotificationHandler)
        verify(megaChatApi, times(2)).addChatListener(globalChatListener)
        verify(megaChatApi, times(2)).addChatCallListener(any())
    }

    @Test
    fun `test that unregister removes listeners when never registered`() = runTest {
        val underTest = createUnderTest(backgroundScope)

        underTest.unregister()
        runCurrent()

        verify(megaChatApi).removeChatRequestListener(chatRequestHandler)
        verify(megaChatApi, never()).addChatRequestListener(any())
    }
}
