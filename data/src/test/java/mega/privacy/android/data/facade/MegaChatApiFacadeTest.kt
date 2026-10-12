package mega.privacy.android.data.facade

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import mega.privacy.android.data.model.ChatNodeHistoryUpdate
import mega.privacy.android.domain.exception.ChatRoomDoesNotExistException
import nz.mega.sdk.MegaChatApi
import nz.mega.sdk.MegaChatApiAndroid
import nz.mega.sdk.MegaChatCall
import nz.mega.sdk.MegaChatMessage
import nz.mega.sdk.MegaChatNodeHistoryListenerInterface
import nz.mega.sdk.MegaHandleList
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.timeout
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * Test class for [MegaChatApiFacade], focused on the call getters being gated on the chat
 * init state to avoid the native crash in AND-19389, on chat room open/close being
 * serialised, and on node history open/close.
 */
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class MegaChatApiFacadeTest {

    private lateinit var underTest: MegaChatApiFacade
    private val chatApi: MegaChatApiAndroid = mock()
    private val sharingScope: CoroutineScope = CoroutineScope(UnconfinedTestDispatcher())

    @BeforeAll
    fun setUp() {
        underTest = MegaChatApiFacade(
            chatApiLazy = { chatApi },
            sharingScope = sharingScope,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(chatApi)
    }

    @ParameterizedTest(name = "when init state is {0}")
    @ValueSource(
        ints = [
            MegaChatApi.INIT_ONLINE_SESSION,
            MegaChatApi.INIT_OFFLINE_SESSION,
            MegaChatApi.INIT_ANONYMOUS,
        ]
    )
    fun `test that getChatCall delegates to the sdk when chat is initialized`(initState: Int) =
        runTest {
            val chatId = 123L
            val expected = mock<MegaChatCall>()
            whenever(chatApi.initState) doReturn initState
            whenever(chatApi.getChatCall(chatId)) doReturn expected

            val result = underTest.getChatCall(chatId)

            assertThat(result).isEqualTo(expected)
            verify(chatApi).getChatCall(chatId)
        }

    @ParameterizedTest(name = "when init state is {0}")
    @ValueSource(
        ints = [
            MegaChatApi.INIT_NOT_DONE,
            MegaChatApi.INIT_ERROR,
            MegaChatApi.INIT_TERMINATED,
            MegaChatApi.INIT_NO_CACHE,
            MegaChatApi.INIT_WAITING_NEW_SESSION,
        ]
    )
    fun `test that getChatCall returns null without querying the sdk when chat is not initialized`(
        initState: Int,
    ) = runTest {
        whenever(chatApi.initState) doReturn initState

        val result = underTest.getChatCall(123L)

        assertThat(result).isNull()
        verify(chatApi, never()).getChatCall(any())
    }

    @Test
    fun `test that getChatCallByCallId delegates to the sdk when chat is initialized`() = runTest {
        val callId = 456L
        val expected = mock<MegaChatCall>()
        whenever(chatApi.initState) doReturn MegaChatApi.INIT_ONLINE_SESSION
        whenever(chatApi.getChatCallByCallId(callId)) doReturn expected

        val result = underTest.getChatCallByCallId(callId)

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `test that getChatCallByCallId returns null without querying the sdk when chat is not initialized`() =
        runTest {
            whenever(chatApi.initState) doReturn MegaChatApi.INIT_NOT_DONE

            val result = underTest.getChatCallByCallId(456L)

            assertThat(result).isNull()
            verify(chatApi, never()).getChatCallByCallId(any())
        }

    @Test
    fun `test that getChatCalls delegates to the sdk when chat is initialized`() = runTest {
        val state = MegaChatCall.CALL_STATUS_IN_PROGRESS
        val expected = mock<MegaHandleList>()
        whenever(chatApi.initState) doReturn MegaChatApi.INIT_ONLINE_SESSION
        whenever(chatApi.getChatCalls(state)) doReturn expected

        val result = underTest.getChatCalls(state)

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `test that getChatCalls returns null without querying the sdk when chat is not initialized`() =
        runTest {
            whenever(chatApi.initState) doReturn MegaChatApi.INIT_ERROR

            val result = underTest.getChatCalls(MegaChatCall.CALL_STATUS_IN_PROGRESS)

            assertThat(result).isNull()
            verify(chatApi, never()).getChatCalls(any())
        }

    @Test
    fun `test that getChatCallIds returns null without querying the sdk when chat is not initialized`() =
        runTest {
            whenever(chatApi.initState) doReturn MegaChatApi.INIT_NOT_DONE

            val result = underTest.getChatCallIds()

            assertThat(result).isNull()
            verify(chatApi, never()).chatCallsIds
        }

    @Test
    fun `test that openChatRoom does not close another chat room while a chat room is being opened`() {
        val firstChatId = 1L
        val secondChatId = 2L
        val openEntered = CountDownLatch(1)
        val releaseOpen = CountDownLatch(1)
        val secondCloseEntered = CountDownLatch(1)

        whenever(chatApi.openChatRoom(eq(firstChatId), any())) doAnswer {
            openEntered.countDown()
            releaseOpen.await()
            true
        }
        doAnswer { secondCloseEntered.countDown() }
            .whenever(chatApi).closeChatRoom(eq(secondChatId), any())

        val scope = CoroutineScope(Dispatchers.IO)
        val facade = MegaChatApiFacade(chatApiLazy = { chatApi }, sharingScope = scope)

        try {
            scope.launch { facade.openChatRoom(firstChatId).collect {} }
            assertThat(openEntered.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue()

            scope.launch { facade.openChatRoom(secondChatId).collect {} }

            assertThat(secondCloseEntered.await(BLOCKED_MILLIS, TimeUnit.MILLISECONDS)).isFalse()

            releaseOpen.countDown()
            assertThat(secondCloseEntered.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue()
        } finally {
            releaseOpen.countDown()
            scope.cancel()
        }
    }

    @Test
    fun `test that openChatRoom closes the chat room when the last subscriber goes away`() {
        val chatId = 3L
        val openEntered = CountDownLatch(1)
        val closeAfterOpen = CountDownLatch(1)

        whenever(chatApi.openChatRoom(eq(chatId), any())) doAnswer {
            openEntered.countDown()
            true
        }
        doAnswer { if (openEntered.count == 0L) closeAfterOpen.countDown() }
            .whenever(chatApi).closeChatRoom(eq(chatId), any())

        val scope = CoroutineScope(Dispatchers.IO)
        val facade = MegaChatApiFacade(chatApiLazy = { chatApi }, sharingScope = scope)

        try {
            val collectJob = scope.launch { facade.openChatRoom(chatId).collect {} }
            assertThat(openEntered.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue()

            collectJob.cancel()

            assertThat(closeAfterOpen.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue()
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun `test that monitorNodeHistory closes the node history with the opened listener when the last subscriber goes away`() {
        val chatId = 4L
        val openedListener = AtomicReference<MegaChatNodeHistoryListenerInterface>()
        val closedListener = AtomicReference<MegaChatNodeHistoryListenerInterface>()
        val closeAfterOpen = CountDownLatch(1)

        whenever(chatApi.openNodeHistory(eq(chatId), any())) doAnswer {
            openedListener.set(it.getArgument(1))
            true
        }
        whenever(chatApi.closeNodeHistory(eq(chatId), any())) doAnswer {
            if (openedListener.get() != null) {
                closedListener.set(it.getArgument(1))
                closeAfterOpen.countDown()
            }
            true
        }

        val scope = CoroutineScope(Dispatchers.IO)
        val facade = MegaChatApiFacade(chatApiLazy = { chatApi }, sharingScope = scope)

        try {
            val collectJob = scope.launch { facade.monitorNodeHistory(chatId).collect {} }
            verify(chatApi, timeout(TIMEOUT_MILLIS)).openNodeHistory(eq(chatId), any())

            collectJob.cancel()

            assertThat(closeAfterOpen.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue()
            assertThat(closedListener.get()).isSameInstanceAs(openedListener.get())
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun `test that monitorNodeHistory emits the updates delivered to the listener`() = runTest {
        val chatId = 5L
        val loaded = mock<MegaChatMessage>()
        val received = mock<MegaChatMessage>()
        whenever(chatApi.openNodeHistory(eq(chatId), any())) doAnswer {
            val listener = it.getArgument<MegaChatNodeHistoryListenerInterface>(1)
            listener.onAttachmentLoaded(chatApi, loaded)
            listener.onAttachmentLoaded(chatApi, null)
            listener.onAttachmentReceived(chatApi, received)
            listener.onAttachmentDeleted(chatApi, 10L)
            listener.onTruncate(chatApi, 11L)
            true
        }

        underTest.monitorNodeHistory(chatId).filterNot { it == ChatNodeHistoryUpdate.Opened }.test {
            assertThat(awaitItem()).isEqualTo(ChatNodeHistoryUpdate.Loaded(loaded))
            assertThat(awaitItem()).isEqualTo(ChatNodeHistoryUpdate.Loaded(null))
            assertThat(awaitItem()).isEqualTo(ChatNodeHistoryUpdate.Received(received))
            assertThat(awaitItem()).isEqualTo(ChatNodeHistoryUpdate.Deleted(10L))
            assertThat(awaitItem()).isEqualTo(ChatNodeHistoryUpdate.Truncated(11L))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that monitorNodeHistory fails with ChatRoomDoesNotExistException when the node history cannot be opened`() =
        runTest {
            val chatId = 6L
            whenever(chatApi.openNodeHistory(eq(chatId), any())) doReturn false

            underTest.monitorNodeHistory(chatId).test {
                assertThat(awaitError()).isInstanceOf(ChatRoomDoesNotExistException::class.java)
            }
        }

    @Test
    fun `test that monitorNodeHistory emits Opened when the node history opens`() = runTest {
        val chatId = 8L
        whenever(chatApi.openNodeHistory(eq(chatId), any())) doReturn true

        underTest.monitorNodeHistory(chatId).test {
            assertThat(awaitItem()).isEqualTo(ChatNodeHistoryUpdate.Opened)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that monitorNodeHistory emits Opened to a subscriber joining an already open node history`() {
        val chatId = 9L
        whenever(chatApi.openNodeHistory(eq(chatId), any())) doReturn true
        val scope = CoroutineScope(Dispatchers.IO)
        val facade = MegaChatApiFacade(chatApiLazy = { chatApi }, sharingScope = scope)

        try {
            runBlocking {
                val nodeHistory = facade.monitorNodeHistory(chatId)
                val firstSubscriber = scope.launch { nodeHistory.collect {} }
                withTimeout(TIMEOUT_MILLIS) {
                    nodeHistory.first { it == ChatNodeHistoryUpdate.Opened }
                }

                val lateOpened = withTimeout(TIMEOUT_MILLIS) { nodeHistory.first() }

                assertThat(lateOpened).isEqualTo(ChatNodeHistoryUpdate.Opened)
                verify(chatApi, times(1)).openNodeHistory(eq(chatId), any())
                firstSubscriber.cancel()
            }
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun `test that monitorNodeHistory opens the node history once for concurrent callers of the same chat`() {
        val chatId = 10L
        whenever(chatApi.openNodeHistory(eq(chatId), any())) doReturn true
        val scope = CoroutineScope(Dispatchers.IO)
        val facade = MegaChatApiFacade(chatApiLazy = { chatApi }, sharingScope = scope)

        try {
            runBlocking {
                val firstCaller = facade.monitorNodeHistory(chatId)
                val secondCaller = facade.monitorNodeHistory(chatId)
                val firstJob = scope.launch { firstCaller.collect {} }
                val secondJob = scope.launch { secondCaller.collect {} }
                withTimeout(TIMEOUT_MILLIS) { firstCaller.first { it == ChatNodeHistoryUpdate.Opened } }
                withTimeout(TIMEOUT_MILLIS) { secondCaller.first { it == ChatNodeHistoryUpdate.Opened } }

                assertThat(secondCaller).isSameInstanceAs(firstCaller)
                verify(chatApi, times(1)).openNodeHistory(eq(chatId), any())
                firstJob.cancel()
                secondJob.cancel()
            }
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun `test that loadAttachments returns the source reported by the sdk`() {
        val chatId = 7L
        val count = 20
        whenever(chatApi.loadAttachments(chatId, count)) doReturn MegaChatApi.SOURCE_REMOTE

        assertThat(underTest.loadAttachments(chatId, count)).isEqualTo(MegaChatApi.SOURCE_REMOTE)
    }

    private companion object {
        const val TIMEOUT_SECONDS = 5L
        const val BLOCKED_MILLIS = 300L
        const val TIMEOUT_MILLIS = 5_000L
    }
}
