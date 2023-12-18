package test.mega.privacy.android.app.presentation.contact.authenticitycredendials

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.contact.authenticitycredendials.AuthenticityCredentialsViewModel
import mega.privacy.android.domain.entity.contacts.AccountCredentials
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.usecase.GetContactCredentials
import mega.privacy.android.domain.usecase.GetMyCredentials
import mega.privacy.android.domain.usecase.ResetCredentials
import mega.privacy.android.domain.usecase.VerifyCredentials
import mega.privacy.android.domain.usecase.contact.AreCredentialsVerifiedUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import org.mockito.kotlin.wheneverBlocking

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AuthenticityCredentialsViewModelTest {

    private lateinit var underTest: AuthenticityCredentialsViewModel

    private val userEmail = "test@email.com"
    private val userCredentials = listOf(
        "AG1F",
        "U3FS",
        "KAJ7",
        "5AFS",
        "YAW1",
        "ZVVW",
        "9SD4",
        "09DR",
        "SA1S",
        "OR2S",
    )
    private val contactCredentials = AccountCredentials.ContactCredentials(
        credentials = userCredentials,
        name = "Test Name",
        email = userEmail
    )
    private val myCredentials = listOf(
        "KDF8",
        "ASDI",
        "9S32",
        "ASH1",
        "ASD0",
        "ASV1",
        "L131",
        "3AS3",
        "AS31",
        "ASDF",
    )
    private val myAccountCredentials = AccountCredentials.MyAccountCredentials(
        credentials = myCredentials
    )
    private val exception = MegaException(-5, null)

    private val getContactCredentials = mock<GetContactCredentials>()
    private val areCredentialsVerifiedUseCase = mock<AreCredentialsVerifiedUseCase>()
    private val getMyCredentials = mock<GetMyCredentials>()
    private val verifyCredentials = mock<VerifyCredentials>()
    private val resetCredentials = mock<ResetCredentials>()
    private var connectivityFlow = MutableSharedFlow<Boolean>()
    private val monitorConnectivityUseCase = mock<MonitorConnectivityUseCase>()

    @BeforeAll
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterAll
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            getContactCredentials,
            areCredentialsVerifiedUseCase,
            getMyCredentials,
            verifyCredentials,
            resetCredentials
        )
        wheneverBlocking { monitorConnectivityUseCase() }.thenReturn(connectivityFlow)
        initTestClass()
    }

    private fun initTestClass() {
        underTest = AuthenticityCredentialsViewModel(
            getContactCredentials = getContactCredentials,
            areCredentialsVerifiedUseCase = areCredentialsVerifiedUseCase,
            getMyCredentials = getMyCredentials,
            verifyCredentials = verifyCredentials,
            resetCredentials = resetCredentials,
            monitorConnectivityUseCase = monitorConnectivityUseCase,
        )
    }

    @Test
    fun `test that the initial state is returned`() = runTest {
        underTest.state.test {
            val initial = awaitItem()
            assertThat(initial.contactCredentials).isNull()
            assertThat(initial.areCredentialsVerified).isFalse()
            assertThat(initial.isVerifyingCredentials).isFalse()
            assertThat(initial.myAccountCredentials).isNull()
            assertThat(initial.error).isNull()
            assertThat(initial.showContactVerificationBanner).isFalse()
        }
    }

    @Test
    fun `test that when request data contact credentials are shown`() = runTest {
        whenever(getContactCredentials(userEmail)).thenReturn(contactCredentials)

        underTest.requestData(userEmail)
        underTest.state.map { it.contactCredentials }.drop(1).test {
            assertThat(awaitItem()).isEqualTo(contactCredentials)
        }
    }

    @Test
    fun `test that when request data contact credentials are verified`() = runTest {
        whenever(areCredentialsVerifiedUseCase(userEmail)).thenReturn(true)

        underTest.requestData(userEmail)
        underTest.state.map { it.areCredentialsVerified }.drop(1).test {
            assertThat(awaitItem()).isTrue()
        }
    }

    @Test
    fun `test that action clicked show error if there is no internet connection`() = runTest {
        testScheduler.advanceUntilIdle()
        connectivityFlow.emit(false)
        underTest.state.map { it.error }.drop(1).test {
            underTest.actionClicked()
            assertThat(awaitItem()).isEqualTo(R.string.check_internet_connection_error)
        }
    }

    @Test
    fun `test that error shown updates error`() = runTest {
        testScheduler.advanceUntilIdle()
        connectivityFlow.emit(false)
        underTest.apply {
            state.map { it.error }.drop(1).test {
                actionClicked()
                assertThat(awaitItem()).isEqualTo(R.string.check_internet_connection_error)
                errorShown()
                assertThat(awaitItem()).isNull()
            }
        }
    }

    @Test
    fun `test that error is shown if verify credentials throws an exception`() = runTest {
        whenever(verifyCredentials(userEmail)).thenAnswer { throw exception }

        underTest.state.map { it.error }.drop(1).test {
            underTest.actionClicked()
            assertThat(awaitItem()).isNotNull()
        }
    }

    @Test
    fun `test that verify credentials is updated if reset finish with success`() = runTest {
        whenever(verifyCredentials(userEmail)).thenReturn(Unit)

        underTest.state.map { it.areCredentialsVerified }.drop(1).test {
            underTest.actionClicked()
            assertThat(awaitItem()).isFalse()
        }
    }

    @Test
    fun `test that error is shown if reset credentials throws an exception`() = runTest {
        whenever(areCredentialsVerifiedUseCase(userEmail)).thenReturn(true)
        whenever(resetCredentials(userEmail)).thenAnswer { throw exception }

        underTest.requestData(userEmail)
        underTest.state.map { it.areCredentialsVerified }.drop(1).test {
            assertThat(awaitItem()).isTrue()
        }
        underTest.state.map { it.error }.drop(1).test {
            underTest.actionClicked()
            assertThat(awaitItem()).isNotNull()
            underTest.errorShown()
            assertThat(awaitItem()).isNull()
        }
    }
}
