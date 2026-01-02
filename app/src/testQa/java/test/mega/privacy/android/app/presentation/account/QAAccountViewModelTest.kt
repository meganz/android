package test.mega.privacy.android.app.presentation.account

import app.cash.turbine.test
import de.palm.composestateevents.StateEventWithContentTriggered
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.presentation.account.QAAccountViewModel
import mega.privacy.android.app.presentation.account.model.QAAccountSwitchEvent
import mega.privacy.android.data.gateway.QAAccountCacheGateway
import mega.privacy.android.domain.entity.login.LoginStatus
import mega.privacy.android.domain.entity.user.UserCredentials
import mega.privacy.android.domain.usecase.login.ChatLogoutUseCase
import mega.privacy.android.domain.usecase.login.FastLoginUseCase
import mega.privacy.android.domain.usecase.login.GetAccountCredentialsUseCase
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class QAAccountViewModelTest {

    private lateinit var underTest: QAAccountViewModel
    private val qaAccountCacheGateway = mock<QAAccountCacheGateway>()
    private val getAccountCredentialsUseCase = mock<GetAccountCredentialsUseCase>()
    private val fastLoginUseCase = mock<FastLoginUseCase>()
    private val chatLogoutUseCase = mock<ChatLogoutUseCase>()
    private val scheduler = TestCoroutineScheduler()
    private val standardDispatcher = StandardTestDispatcher(scheduler)

    companion object {
        // Shared Test Data
        private const val TEST_EMAIL = "test@example.com"
        private const val TEST_SESSION = "test-session"
        private const val TEST_FIRST_NAME = "Test"
        private const val TEST_LAST_NAME = "User"
        private const val TEST_REMARK = "Test remark"
        private const val TEST_HANDLE = "123456789"
        private const val TEST_TIME_STAMP = 123456789L

        private fun createTestCredentials(
            email: String? = TEST_EMAIL,
            session: String? = TEST_SESSION,
            firstName: String = TEST_FIRST_NAME,
            lastName: String = TEST_LAST_NAME,
            myHandle: String = TEST_HANDLE
        ) = UserCredentials(
            email = email,
            session = session,
            firstName = firstName,
            lastName = lastName,
            myHandle = myHandle
        )
    }

    @Before
    fun setUp() = runTest {
        Dispatchers.setMain(standardDispatcher)
        whenever(qaAccountCacheGateway.getAllCachedAccounts()).thenReturn(emptyList())
        underTest = QAAccountViewModel(
            qaAccountCacheGateway = qaAccountCacheGateway,
            getAccountCredentialsUseCase = getAccountCredentialsUseCase,
            fastLoginUseCase = fastLoginUseCase,
            chatLogoutUseCase = chatLogoutUseCase
        )
        scheduler.advanceUntilIdle()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test that initial cached accounts state is empty`() = runTest {
        whenever(qaAccountCacheGateway.getAllCachedAccounts()).thenReturn(emptyList())

        underTest.uiState.test {
            scheduler.advanceUntilIdle()
            val state = awaitItem()
            assertTrue(state.cachedAccounts.isEmpty())
        }
    }

    @Test
    fun `test that initial switching account state is false`() = runTest {
        underTest.uiState.test {
            val state = awaitItem()
            assertFalse(state.isSwitchingAccount)
        }
    }

    @Test
    fun `test that initial account switch event is consumed`() = runTest {
        underTest.uiState.test {
            val state = awaitItem()
            assertFalse(state.accountSwitchEvent is StateEventWithContentTriggered)
        }
    }

    @Test
    fun `test saveCurrentAccount saves valid credentials`() = runTest {
        val credentials = createTestCredentials()
        whenever(getAccountCredentialsUseCase()).thenReturn(credentials)
        whenever(qaAccountCacheGateway.saveAccount(any())).then { }
        whenever(qaAccountCacheGateway.updateLastLoginTime(any(), any())).then { }
        whenever(qaAccountCacheGateway.getAllCachedAccounts()).thenReturn(listOf(credentials))

        val result = underTest.saveCurrentAccount()
        scheduler.advanceUntilIdle()

        assertNotNull(result)
        assertEquals(TEST_EMAIL, result.email)
        verify(qaAccountCacheGateway).saveAccount(credentials)
        verify(qaAccountCacheGateway).updateLastLoginTime(any(), any())
    }

    @Test
    fun `test saveCurrentAccount returns null when credentials are null`() = runTest {
        whenever(getAccountCredentialsUseCase()).thenReturn(null)

        val result = underTest.saveCurrentAccount()
        scheduler.advanceUntilIdle()

        assertNull(result)
        verify(qaAccountCacheGateway, times(0)).saveAccount(any())
    }

    @Test
    fun `test saveCurrentAccount returns null when email is null`() = runTest {
        val credentials = createTestCredentials(email = null)
        whenever(getAccountCredentialsUseCase()).thenReturn(credentials)

        val result = underTest.saveCurrentAccount()
        scheduler.advanceUntilIdle()

        assertNull(result)
        verify(qaAccountCacheGateway, times(0)).saveAccount(any())
    }

    @Test
    fun `test saveCurrentAccount returns null when email is blank`() = runTest {
        val credentials = createTestCredentials(email = "   ")
        whenever(getAccountCredentialsUseCase()).thenReturn(credentials)

        val result = underTest.saveCurrentAccount()
        scheduler.advanceUntilIdle()

        assertNull(result)
        verify(qaAccountCacheGateway, times(0)).saveAccount(any())
    }

    @Test
    fun `test saveCurrentAccount returns null when session is null`() = runTest {
        val credentials = createTestCredentials(session = null)
        whenever(getAccountCredentialsUseCase()).thenReturn(credentials)

        val result = underTest.saveCurrentAccount()
        scheduler.advanceUntilIdle()

        assertNull(result)
        verify(qaAccountCacheGateway, times(0)).saveAccount(any())
    }

    @Test
    fun `test saveCurrentAccount returns null when session is blank`() = runTest {
        val credentials = createTestCredentials(session = "   ")
        whenever(getAccountCredentialsUseCase()).thenReturn(credentials)

        val result = underTest.saveCurrentAccount()
        scheduler.advanceUntilIdle()

        assertNull(result)
        verify(qaAccountCacheGateway, times(0)).saveAccount(any())
    }

    @Test
    fun `test switchToAccount does nothing when session is null`() = runTest {
        val credentials = createTestCredentials(session = null)

        underTest.switchToAccount(credentials)
        scheduler.advanceUntilIdle()

        verify(chatLogoutUseCase, times(0)).invoke(any())
        verify(fastLoginUseCase, times(0)).invoke(any(), any(), any())
    }

    @Test
    fun `test switchToAccount does nothing when session is blank`() = runTest {
        val credentials = createTestCredentials(session = "   ")

        underTest.switchToAccount(credentials)
        scheduler.advanceUntilIdle()

        verify(chatLogoutUseCase, times(0)).invoke(any())
        verify(fastLoginUseCase, times(0)).invoke(any(), any(), any())
    }

    @Test
    fun `test switchToAccount sets switching state and calls logout and login`() = runTest {
        val credentials = createTestCredentials()
        whenever(chatLogoutUseCase.invoke(any())).then { }
        whenever(fastLoginUseCase.invoke(any(), any(), any())).thenReturn(
            flowOf(LoginStatus.LoginSucceed)
        )
        whenever(qaAccountCacheGateway.updateLastLoginTime(any(), any())).then { }

        underTest.switchToAccount(credentials)
        scheduler.advanceUntilIdle()

        // Then - verify switching state is false after completion
        underTest.uiState.test {
            val state = awaitItem()
            assertFalse(state.isSwitchingAccount) // Should be false after completion
        }

        // Verify use cases were called
        verify(chatLogoutUseCase).invoke(any())
        verify(fastLoginUseCase).invoke(any(), any(), any())
        verify(qaAccountCacheGateway).updateLastLoginTime(any(), any())
    }

    @Test
    fun `test switchToAccount sets success event when login succeeds`() = runTest {
        val credentials = createTestCredentials()
        whenever(chatLogoutUseCase.invoke(any())).then { }
        whenever(fastLoginUseCase.invoke(any(), any(), any())).thenReturn(
            flowOf(LoginStatus.LoginSucceed)
        )
        whenever(qaAccountCacheGateway.updateLastLoginTime(any(), any())).then { }

        underTest.switchToAccount(credentials)
        scheduler.advanceUntilIdle()

        underTest.uiState.test {
            val state = awaitItem()
            assertTrue(state.accountSwitchEvent is StateEventWithContentTriggered)
            val event = (state.accountSwitchEvent as StateEventWithContentTriggered).content
            assertIs<QAAccountSwitchEvent.Success>(event)
            assertEquals(TEST_EMAIL, (event as QAAccountSwitchEvent.Success).email)
        }
    }

    @Test
    fun `test switchToAccount handles login started status`() = runTest {
        val credentials = createTestCredentials()
        whenever(chatLogoutUseCase.invoke(any())).then { }
        whenever(fastLoginUseCase.invoke(any(), any(), any())).thenReturn(
            flowOf(LoginStatus.LoginStarted, LoginStatus.LoginSucceed)
        )
        whenever(qaAccountCacheGateway.updateLastLoginTime(any(), any())).then { }

        underTest.switchToAccount(credentials)
        scheduler.advanceUntilIdle()

        verify(fastLoginUseCase).invoke(any(), any(), any())
    }

    @Test
    fun `test removeAccount calls gateway and reloads accounts`() = runTest {
        val credentials = createTestCredentials()
        whenever(qaAccountCacheGateway.removeAccount(any())).then { }
        whenever(qaAccountCacheGateway.getAllCachedAccounts()).thenReturn(emptyList())

        underTest.removeAccount(credentials)
        scheduler.advanceUntilIdle()

        verify(qaAccountCacheGateway).removeAccount(TEST_EMAIL)
        verify(qaAccountCacheGateway, times(2)).getAllCachedAccounts() // Once in init, once after remove
    }

    @Test
    fun `test clearAllAccounts calls gateway and reloads accounts`() = runTest {
        whenever(qaAccountCacheGateway.clearAllAccounts()).then { }
        whenever(qaAccountCacheGateway.getAllCachedAccounts()).thenReturn(emptyList())

        underTest.clearAllAccounts()
        scheduler.advanceUntilIdle()

        verify(qaAccountCacheGateway).clearAllAccounts()
        verify(qaAccountCacheGateway, times(2)).getAllCachedAccounts() // Once in init, once after clear
    }

    @Test
    fun `test consumeAccountSwitchEvent resets event to consumed`() = runTest {
        val credentials = createTestCredentials()
        whenever(chatLogoutUseCase.invoke(any())).then { }
        whenever(fastLoginUseCase.invoke(any(), any(), any())).thenReturn(
            flowOf(LoginStatus.LoginSucceed)
        )
        whenever(qaAccountCacheGateway.updateLastLoginTime(any(), any())).then { }
        underTest.switchToAccount(credentials)
        scheduler.advanceUntilIdle()

        underTest.consumeAccountSwitchEvent()

        underTest.uiState.test {
            val state = awaitItem()
            assertFalse(state.accountSwitchEvent is StateEventWithContentTriggered)
        }
    }

    @Test
    fun `test getLastLoginTime returns timestamp from gateway`() = runTest {
        whenever(qaAccountCacheGateway.getLastLoginTime(TEST_EMAIL)).thenReturn(TEST_TIME_STAMP)

        val result = underTest.getLastLoginTime(TEST_EMAIL)

        assertEquals(TEST_TIME_STAMP, result)
        verify(qaAccountCacheGateway).getLastLoginTime(TEST_EMAIL)
    }

    @Test
    fun `test getLastLoginTime returns null when gateway returns null`() = runTest {
        whenever(qaAccountCacheGateway.getLastLoginTime(TEST_EMAIL)).thenReturn(null)

        val result = underTest.getLastLoginTime(TEST_EMAIL)

        assertNull(result)
    }

    @Test
    fun `test getCurrentAccountEmail returns email from credentials`() = runTest {
        val credentials = createTestCredentials()
        whenever(getAccountCredentialsUseCase()).thenReturn(credentials)

        val result = underTest.getCurrentAccountEmail()

        assertEquals(TEST_EMAIL, result)
    }

    @Test
    fun `test getCurrentAccountEmail returns null when credentials are null`() = runTest {
        whenever(getAccountCredentialsUseCase()).thenReturn(null)

        val result = underTest.getCurrentAccountEmail()

        assertNull(result)
    }

    @Test
    fun `test saveRemark calls gateway and reloads accounts`() = runTest {
        whenever(qaAccountCacheGateway.saveRemark(any(), any())).then { }
        whenever(qaAccountCacheGateway.getAllCachedAccounts()).thenReturn(emptyList())

        // When
        underTest.saveRemark(TEST_EMAIL, TEST_REMARK)
        scheduler.advanceUntilIdle()

        // Then
        verify(qaAccountCacheGateway).saveRemark(TEST_EMAIL, TEST_REMARK)
        verify(qaAccountCacheGateway, times(2)).getAllCachedAccounts() // Once in init, once after save
    }

    @Test
    fun `test getRemark returns remark from gateway`() = runTest {
        whenever(qaAccountCacheGateway.getRemark(TEST_EMAIL)).thenReturn(TEST_REMARK)

        val result = underTest.getRemark(TEST_EMAIL)

        assertEquals(TEST_REMARK, result)
        verify(qaAccountCacheGateway).getRemark(TEST_EMAIL)
    }

    @Test
    fun `test getRemark returns null when gateway returns null`() = runTest {
        whenever(qaAccountCacheGateway.getRemark(TEST_EMAIL)).thenReturn(null)

        val result = underTest.getRemark(TEST_EMAIL)

        assertNull(result)
    }

    @Test
    fun `test cachedAccounts state updates after loading`() = runTest {
        val credentials = createTestCredentials()
        whenever(qaAccountCacheGateway.getAllCachedAccounts()).thenReturn(listOf(credentials))

        val viewModel = QAAccountViewModel(
            qaAccountCacheGateway = qaAccountCacheGateway,
            getAccountCredentialsUseCase = getAccountCredentialsUseCase,
            fastLoginUseCase = fastLoginUseCase,
            chatLogoutUseCase = chatLogoutUseCase
        )
        scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals(1, state.cachedAccounts.size)
            assertEquals(TEST_EMAIL, state.cachedAccounts[0].email)
        }
    }

    @Test
    fun `test switchToAccount triggers failure event when login fails`() = runTest {
        val credentials = createTestCredentials()
        val exception = RuntimeException("Login failed")
        whenever(chatLogoutUseCase.invoke(any())).then { }
        whenever(fastLoginUseCase.invoke(any(), any(), any())).thenReturn(
            flowOf(LoginStatus.LoginStarted)
        )

        underTest.switchToAccount(credentials)
        // Simulate an exception during login
        whenever(fastLoginUseCase.invoke(any(), any(), any())).thenThrow(exception)
        scheduler.advanceUntilIdle()

        underTest.uiState.test {
            val state = awaitItem()
            if (state.accountSwitchEvent is StateEventWithContentTriggered) {
                val event = (state.accountSwitchEvent as StateEventWithContentTriggered).content
                assertIs<QAAccountSwitchEvent.Failure>(event)
            }
        }
    }

    @Test
    fun `test uiState contains all expected properties`() = runTest {
        underTest.uiState.test {
            val state = awaitItem()
            assertNotNull(state.cachedAccounts)
            assertNotNull(state.isSwitchingAccount)
            assertNotNull(state.accountSwitchEvent)
        }
    }
}
