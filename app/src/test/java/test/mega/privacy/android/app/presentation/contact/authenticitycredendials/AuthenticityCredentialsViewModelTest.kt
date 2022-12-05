package test.mega.privacy.android.app.presentation.contact.authenticitycredendials

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.contact.authenticitycredendials.AuthenticityCredentialsViewModel
import mega.privacy.android.domain.entity.contacts.AccountCredentials
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.usecase.AreCredentialsVerified
import mega.privacy.android.domain.usecase.GetContactCredentials
import mega.privacy.android.domain.usecase.GetMyCredentials
import mega.privacy.android.domain.usecase.MonitorConnectivity
import mega.privacy.android.domain.usecase.ResetCredentials
import mega.privacy.android.domain.usecase.VerifyCredentials
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class AuthenticityCredentialsViewModelTest {

    private lateinit var underTest: AuthenticityCredentialsViewModel

    @get:Rule
    var rule: TestRule = InstantTaskExecutorRule()

    private val scheduler = TestCoroutineScheduler()
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

    private val getContactCredentials = mock<GetContactCredentials> {
        onBlocking { invoke(userEmail) }.thenReturn(contactCredentials)
    }

    private val areCredentialsVerified = mock<AreCredentialsVerified> {
        onBlocking { invoke(userEmail) }.thenReturn(false)
    }

    private val getMyCredentials = mock<GetMyCredentials> {
        onBlocking { invoke() }.thenReturn(myAccountCredentials)
    }

    private val verifyCredentials = mock<VerifyCredentials> {
        onBlocking { invoke(userEmail) }.thenReturn(Unit)
    }

    private val resetCredentials = mock<ResetCredentials> {
        onBlocking { invoke(userEmail) }.thenReturn(Unit)
    }

    private val monitorConnectivity =
        mock<MonitorConnectivity> { on { invoke() }.thenReturn(MutableStateFlow(true)) }

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher(scheduler))
        underTest = AuthenticityCredentialsViewModel(
            getContactCredentials = getContactCredentials,
            areCredentialsVerified = areCredentialsVerified,
            getMyCredentials = getMyCredentials,
            verifyCredentials = verifyCredentials,
            resetCredentials = resetCredentials,
            monitorConnectivity = monitorConnectivity,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
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
        }
    }

    @Test
    fun `test that when get my credentials is called my credentials are shown`() = runTest {
        whenever(getMyCredentials()).thenReturn(myAccountCredentials)

        underTest.state.map { it.myAccountCredentials }.distinctUntilChanged()
            .test {
                assertThat(awaitItem()).isNull()
                assertThat(awaitItem()).isEqualTo(myAccountCredentials)
            }
    }

    @Test
    fun `test that when request data contact credentials are shown`() = runTest {
        whenever(getContactCredentials(userEmail)).thenReturn(contactCredentials)

        underTest.state.map { it.contactCredentials }.distinctUntilChanged()
            .test {
                underTest.requestData(userEmail)
                assertThat(awaitItem()).isNull()
                assertThat(awaitItem()).isEqualTo(contactCredentials)
            }
    }

    @Test
    fun `test that when request data contact credentials are verified`() = runTest {
        whenever(areCredentialsVerified(userEmail)).thenReturn(true)

        underTest.state.map { it.areCredentialsVerified }.distinctUntilChanged()
            .test {
                underTest.requestData(userEmail)
                assertThat(awaitItem()).isFalse()
                assertThat(awaitItem()).isTrue()
            }
    }

    @Test
    fun `test that action clicked show error if there is no internet connection`() = runTest {
        whenever(monitorConnectivity()).thenReturn(MutableStateFlow(false))

        underTest.state.map { it.error }.distinctUntilChanged()
            .test {
                underTest.actionClicked()
                assertThat(awaitItem()).isNull()
                assertThat(awaitItem()).isEqualTo(R.string.check_internet_connection_error)
            }
    }

    @Test
    fun `test that error shown updates error`() = runTest {
        whenever(monitorConnectivity()).thenReturn(MutableStateFlow(false))

        underTest.apply {
            state.map { it.error }.distinctUntilChanged()
                .test {
                    assertThat(awaitItem()).isNull()
                    actionClicked()
                    assertThat(awaitItem()).isEqualTo(R.string.check_internet_connection_error)
                    errorShown()
                    assertThat(awaitItem()).isNull()
                }
        }
    }

    @Test
    fun `test that verify credentials is updated if finish with success`() = runTest {
        whenever(getContactCredentials(userEmail)).thenReturn(contactCredentials)
        whenever(verifyCredentials(userEmail)).thenReturn(Unit)

        underTest.state.map { it.contactCredentials }.distinctUntilChanged()
            .test {
                underTest.requestData(userEmail)
                assertThat(awaitItem()).isNull()
                assertThat(awaitItem()).isEqualTo(contactCredentials)
                underTest.state.map { it.areCredentialsVerified }.distinctUntilChanged()
                    .test {
                        assertThat(awaitItem()).isFalse()
                        underTest.actionClicked()
                        assertThat(awaitItem()).isTrue()
                    }
            }
    }

    @Test
    fun `test that error is shown if verify credentials throws an exception`() = runTest {
        whenever(getContactCredentials(userEmail)).thenReturn(contactCredentials)
        whenever(verifyCredentials(userEmail)).thenAnswer { throw exception }

        underTest.state.map { it.contactCredentials }.distinctUntilChanged()
            .test {
                underTest.requestData(userEmail)
                assertThat(awaitItem()).isNull()
                assertThat(awaitItem()).isEqualTo(contactCredentials)
                underTest.state.map { it.error }.distinctUntilChanged()
                    .test {
                        assertThat(awaitItem()).isNull()
                        underTest.actionClicked()
                        assertThat(awaitItem()).isNotNull()
                    }
            }
    }

    @Test
    fun `test that verify credentials is updated if reset finish with success`() = runTest {
        whenever(getContactCredentials(userEmail)).thenReturn(contactCredentials)
        whenever(verifyCredentials(userEmail)).thenReturn(Unit)
        whenever(resetCredentials(userEmail)).thenReturn(Unit)

        underTest.state.map { it.contactCredentials }.distinctUntilChanged()
            .test {
                underTest.requestData(userEmail)
                assertThat(awaitItem()).isNull()
                assertThat(awaitItem()).isEqualTo(contactCredentials)
                underTest.state.map { it.areCredentialsVerified }.distinctUntilChanged()
                    .test {
                        assertThat(awaitItem()).isFalse()
                        underTest.actionClicked()
                        assertThat(awaitItem()).isTrue()
                        underTest.actionClicked()
                        assertThat(awaitItem()).isFalse()
                    }
            }
    }

    @Test
    fun `test that error is shown if reset credentials throws an exception`() = runTest {
        whenever(getContactCredentials(userEmail)).thenReturn(contactCredentials)
        whenever(verifyCredentials(userEmail)).thenReturn(Unit)
        whenever(resetCredentials(userEmail)).thenAnswer { throw exception }

        underTest.state.map { it.contactCredentials }.distinctUntilChanged()
            .test {
                underTest.requestData(userEmail)
                assertThat(awaitItem()).isNull()
                assertThat(awaitItem()).isEqualTo(contactCredentials)
                underTest.state.map { it.areCredentialsVerified }.distinctUntilChanged()
                    .test {
                        assertThat(awaitItem()).isFalse()
                        underTest.actionClicked()
                        assertThat(awaitItem()).isTrue()
                        underTest.state.map { it.error }.distinctUntilChanged()
                            .test {
                                assertThat(awaitItem()).isNotNull()
                                underTest.errorShown()
                                assertThat(awaitItem()).isNull()
                                underTest.actionClicked()
                                assertThat(awaitItem()).isNotNull()
                            }
                    }
            }
    }
}