package mega.privacy.android.data.repository

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.gateway.AppEventGateway
import mega.privacy.android.data.gateway.api.MegaApiFolderGateway
import mega.privacy.android.data.gateway.api.MegaApiGateway
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.data.listener.OptionalMegaRequestListenerInterface
import mega.privacy.android.domain.entity.login.LoginStatus
import mega.privacy.android.domain.exception.ChatLoggingOutException
import mega.privacy.android.domain.exception.ChatNotInitializedErrorStatus
import mega.privacy.android.domain.exception.ChatNotInitializedUnknownStatus
import mega.privacy.android.domain.exception.LoginBlockedAccount
import mega.privacy.android.domain.exception.LoginLoggedOutFromOtherLocation
import mega.privacy.android.domain.exception.LoginMultiFactorAuthRequired
import mega.privacy.android.domain.exception.LoginRequireValidation
import mega.privacy.android.domain.exception.LoginTooManyAttempts
import mega.privacy.android.domain.exception.LoginUnknownStatus
import mega.privacy.android.domain.exception.LoginWrongEmailOrPassword
import nz.mega.sdk.MegaChatApi
import nz.mega.sdk.MegaError
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultLoginRepositoryTest {

    private lateinit var underTest: DefaultLoginRepository

    private val megaApiGateway = mock<MegaApiGateway>()
    private val megaApiFolderGateway = mock<MegaApiFolderGateway>()
    private val megaChatApiGateway = mock<MegaChatApiGateway>()
    private val appEventGateway = mock<AppEventGateway>()

    private val email = "test@email.com"
    private val password = "testPassword"

    @Before
    fun setUp() {
        underTest = DefaultLoginRepository(
            megaApiGateway = megaApiGateway,
            megaApiFolderGateway = megaApiFolderGateway,
            megaChatApiGateway = megaChatApiGateway,
            ioDispatcher = UnconfinedTestDispatcher(),
            appEventGateway = appEventGateway
        )
    }

    @Test(expected = ChatNotInitializedErrorStatus::class)
    fun `test that initMega throws ChatNotInitializedException when megaChat init state is INIT_NOT_DONE and init result is INIT_ERROR`() =
        runTest {
            whenever(megaChatApiGateway.initState).thenReturn(MegaChatApi.INIT_NOT_DONE)
            whenever(megaChatApiGateway.init(any())).thenReturn(MegaChatApi.INIT_ERROR)

            underTest.initMegaChat("session_id")
        }

    @Test(expected = ChatNotInitializedErrorStatus::class)
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

    @Test
    fun `test that AppEventGateway is invoked when broadcastLogout is launched`() = runTest {
        underTest.broadcastLogout()
        verify(appEventGateway).broadcastLogout()
    }

    @Test
    fun `test that AppEventGateway is invoked when monitorLogout is received`() = runTest {
        underTest.monitorLogout()
        verify(appEventGateway).monitorLogout()
    }

    @Test(expected = ChatNotInitializedErrorStatus::class)
    fun `test that initMegaChat without session throws ChatNotInitializedException ErrorStatus when megaChat init state is INIT_NOT_DONE and init result is INIT_ERROR`() =
        runTest {
            whenever(megaChatApiGateway.initState).thenReturn(MegaChatApi.INIT_NOT_DONE)
            whenever(megaChatApiGateway.init(null)).thenReturn(MegaChatApi.INIT_ERROR)

            underTest.initMegaChat()
        }

    @Test(expected = ChatNotInitializedErrorStatus::class)
    fun `test that initMegaChat without session throws ChatNotInitializedException ErrorStatus when megaChat init state is INIT_ERROR and init result is INIT_ERROR`() =
        runTest {
            whenever(megaChatApiGateway.initState).thenReturn(MegaChatApi.INIT_NOT_DONE)
            whenever(megaChatApiGateway.init(null)).thenReturn(MegaChatApi.INIT_ERROR)

            underTest.initMegaChat()
        }

    @Test(expected = ChatNotInitializedUnknownStatus::class)
    fun `test that initMegaChat without session throws ChatNotInitializedException UnknownStatus when megaChat init state is INIT_NOT_DONE and init result is INIT_NOT_DONE`() =
        runTest {
            whenever(megaChatApiGateway.initState).thenReturn(MegaChatApi.INIT_NOT_DONE)
            whenever(megaChatApiGateway.init(null)).thenReturn(MegaChatApi.INIT_NOT_DONE)

            underTest.initMegaChat()
        }

    @Test
    fun `test that initMegaChat without session finish with success when megaChat initState is INIT_NOT_DONE and init result is INIT_WAITING_NEW_SESSION`() =
        runTest {
            whenever(megaChatApiGateway.initState).thenReturn(MegaChatApi.INIT_NOT_DONE)
            whenever(megaChatApiGateway.init(null)).thenReturn(MegaChatApi.INIT_WAITING_NEW_SESSION)

            underTest.initMegaChat()
            verify(megaChatApiGateway).init(null)
        }

    @Test
    fun `test that login returns LoginStarted if the request starts`() = runTest {
        val listenerCaptor = argumentCaptor<OptionalMegaRequestListenerInterface>()

        underTest.login(email, password).test {
            verify(megaApiGateway).login(any(), any(), listenerCaptor.capture())
            val listener = listenerCaptor.firstValue
            listener.onRequestStart(mock(), mock())
            assertThat(awaitItem()).isEqualTo(LoginStatus.LoginStarted)
        }
    }

    @Test
    fun `test that login returns LoginSucceed if the request finishes with success`() = runTest {
        val listenerCaptor = argumentCaptor<OptionalMegaRequestListenerInterface>()
        val error = mock<MegaError> { on { errorCode }.thenReturn(MegaError.API_OK) }

        underTest.login(email, password).test {
            verify(megaApiGateway).login(any(), any(), listenerCaptor.capture())
            val listener = listenerCaptor.firstValue
            listener.onRequestFinish(mock(), mock(), error)
            val value = verify(megaApiGateway).accountAuth
            verify(megaApiFolderGateway).accountAuth = value
            assertThat(awaitItem()).isEqualTo(LoginStatus.LoginSucceed)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that login returns LoginRequire2FA if the request fails with API_EMFAREQUIRED`() =
        runTest {
            whenever(megaApiGateway.login(any(), any(), any())).thenAnswer {
                (it.arguments[2] as OptionalMegaRequestListenerInterface).onRequestFinish(
                    mock(),
                    mock(),
                    mock { on { errorCode }.thenReturn(MegaError.API_EMFAREQUIRED) }
                )
            }

            underTest.login(email, password).test {
                assertThat(awaitError()).isInstanceOf(LoginMultiFactorAuthRequired::class.java)
            }
        }

    @Test
    fun `test that login returns LoggedOutFromOtherLocation if the request fails with API_ESID`() =
        runTest {
            whenever(megaApiGateway.login(any(), any(), any())).thenAnswer {
                (it.arguments[2] as OptionalMegaRequestListenerInterface).onRequestFinish(
                    mock(),
                    mock(),
                    mock { on { errorCode }.thenReturn(MegaError.API_ESID) }
                )
            }

            underTest.login(email, password).test {
                assertThat(awaitError()).isInstanceOf(LoginLoggedOutFromOtherLocation::class.java)
            }
        }

    @Test
    fun `test that login returns WrongEmailOrPassword if the request fails with API_ENOENT`() =
        runTest {
            whenever(megaApiGateway.login(any(), any(), any())).thenAnswer {
                (it.arguments[2] as OptionalMegaRequestListenerInterface).onRequestFinish(
                    mock(),
                    mock(),
                    mock { on { errorCode }.thenReturn(MegaError.API_ENOENT) }
                )
            }

            underTest.login(email, password).test {
                assertThat(awaitError()).isInstanceOf(LoginWrongEmailOrPassword::class.java)
            }
        }

    @Test
    fun `test that login returns TooManyAttempts if the request fails with API_ETOOMANY`() =
        runTest {
            whenever(megaApiGateway.login(any(), any(), any())).thenAnswer {
                (it.arguments[2] as OptionalMegaRequestListenerInterface).onRequestFinish(
                    mock(),
                    mock(),
                    mock { on { errorCode }.thenReturn(MegaError.API_ETOOMANY) }
                )
            }

            underTest.login(email, password).test {
                assertThat(awaitError()).isInstanceOf(LoginTooManyAttempts::class.java)
            }
        }

    @Test
    fun `test that login returns RequireValidation if the request fails with API_EINCOMPLETE`() =
        runTest {
            whenever(megaApiGateway.login(any(), any(), any())).thenAnswer {
                (it.arguments[2] as OptionalMegaRequestListenerInterface).onRequestFinish(
                    mock(),
                    mock(),
                    mock { on { errorCode }.thenReturn(MegaError.API_EINCOMPLETE) }
                )
            }

            underTest.login(email, password).test {
                assertThat(awaitError()).isInstanceOf(LoginRequireValidation::class.java)
            }
        }

    @Test
    fun `test that login returns BlockedAccount if the request fails with API_EBLOCKED`() =
        runTest {
            whenever(megaApiGateway.login(any(), any(), any())).thenAnswer {
                (it.arguments[2] as OptionalMegaRequestListenerInterface).onRequestFinish(
                    mock(),
                    mock(),
                    mock { on { errorCode }.thenReturn(MegaError.API_EBLOCKED) }
                )
            }

            underTest.login(email, password).test {
                assertThat(awaitError()).isInstanceOf(LoginBlockedAccount::class.java)
            }
        }

    @Test
    fun `test that login returns Unknown if the request fails with non contemplated error`() =
        runTest {
            whenever(megaApiGateway.login(any(), any(), any())).thenAnswer {
                (it.arguments[2] as OptionalMegaRequestListenerInterface).onRequestFinish(
                    mock(),
                    mock(),
                    mock { on { errorCode }.thenReturn(MegaError.LOCAL_ENOSPC) }
                )
            }

            underTest.login(email, password).test {
                assertThat(awaitError()).isInstanceOf(LoginUnknownStatus::class.java)
            }
        }
}