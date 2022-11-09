package test.mega.privacy.android.app.data.repository

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.data.repository.DefaultLoginRepository
import mega.privacy.android.data.gateway.api.MegaApiFolderGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.domain.exception.ChatLoggingOutException
import mega.privacy.android.domain.exception.ChatNotInitializedException
import nz.mega.sdk.MegaChatApi
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever


@OptIn(ExperimentalCoroutinesApi::class)
class DefaultLoginRepositoryTest {

    private lateinit var underTest: DefaultLoginRepository

    private val megaApiGateway = mock<MegaApiGateway>()
    private val megaApiFolderGateway = mock<MegaApiFolderGateway>()
    private val megaChatApiGateway = mock<MegaChatApiGateway>()


    @Before
    fun setUp() {
        underTest = DefaultLoginRepository(
            megaApiGateway = megaApiGateway,
            megaApiFolderGateway = megaApiFolderGateway,
            megaChatApiGateway = megaChatApiGateway,
            ioDispatcher = UnconfinedTestDispatcher()
        )
    }

    @Test(expected = ChatNotInitializedException::class)
    fun `test that initMega throws ChatNotInitializedException when megaChat init state is INIT_NOT_DONE and init result is INIT_ERROR`() =
        runTest {
            whenever(megaChatApiGateway.initState).thenReturn(MegaChatApi.INIT_NOT_DONE)
            whenever(megaChatApiGateway.init(any())).thenReturn(MegaChatApi.INIT_ERROR)

            underTest.initMegaChat("session_id")
        }

    @Test(expected = ChatNotInitializedException::class)
    fun `test that initMega throws ChatNotInitializedException when megaChat init state is INIT_ERROR and init result is INIT_ERROR`() =
        runTest {
            whenever(megaChatApiGateway.initState).thenReturn(MegaChatApi.INIT_ERROR)
            whenever(megaChatApiGateway.init(any())).thenReturn(MegaChatApi.INIT_ERROR)

            underTest.initMegaChat("session_id")
        }


    @Test(expected = ChatLoggingOutException::class)
    fun `test that initMega throws ChatNotInitializedException when megaChat initState is INIT_TERMINATED`() =
        runTest {
            whenever(megaChatApiGateway.initState).thenReturn(MegaChatApi.INIT_TERMINATED)
            underTest.initMegaChat("session_id")
        }

    @Test
    fun `test that MegaChatApiGateway init is invoked when megaChat initState is INIT_NOT_DONE`() =
        runTest {
            whenever(megaChatApiGateway.initState).thenReturn(MegaChatApi.INIT_NOT_DONE)
            val sessionId = "session_id"
            underTest.initMegaChat(sessionId)
            verify(megaChatApiGateway).init(sessionId)
        }

}