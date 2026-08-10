package mega.privacy.android.data.facade

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import nz.mega.sdk.MegaChatApi
import nz.mega.sdk.MegaChatApiAndroid
import nz.mega.sdk.MegaChatCall
import nz.mega.sdk.MegaHandleList
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Test class for [MegaChatApiFacade], focused on the call getters being gated on the chat
 * init state to avoid the native crash in AND-19389.
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
            chatApi = chatApi,
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
}
