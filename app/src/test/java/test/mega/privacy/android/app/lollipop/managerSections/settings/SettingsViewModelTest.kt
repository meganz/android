package test.mega.privacy.android.app.lollipop.managerSections.settings

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.domain.entity.UserAccount
import mega.privacy.android.app.domain.exception.SettingNotFoundException
import mega.privacy.android.app.domain.usecase.FetchAutoAcceptQRLinks
import mega.privacy.android.app.domain.usecase.IsChatLoggedIn
import mega.privacy.android.app.domain.usecase.ToggleAutoAcceptQRLinks
import mega.privacy.android.app.presentation.settings.SettingsViewModel
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class SettingsViewModelTest {
    private lateinit var underTest: SettingsViewModel

    private val fetchAutoAcceptQRLinks = mock<FetchAutoAcceptQRLinks>()
    private val toggleAutoAcceptQRLinks = mock<ToggleAutoAcceptQRLinks>()

    private val userAccount = UserAccount(
        email = "",
        isBusinessAccount = false,
        isMasterBusinessAccount = false,
        accountTypeIdentifier = 0
    )

    private val isChatLoggedInValue = MutableStateFlow(true)
    private val isChatLoggedIn =
        mock<IsChatLoggedIn> { on { invoke() }.thenReturn(isChatLoggedInValue) }


    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        runBlocking {
            whenever(fetchAutoAcceptQRLinks()).thenReturn(true)
        }
        underTest = SettingsViewModel(
            getAccountDetails = mock { on { invoke(any()) }.thenReturn(userAccount) },
            canDeleteAccount = mock { on { invoke(userAccount) }.thenReturn(true) },
            refreshPasscodeLockPreference = mock(),
            areSdkLogsEnabled = mock(),
            areChatLogsEnabled = mock(),
            isCameraSyncEnabled = mock(),
            rootNodeExists = mock { on { invoke() }.thenReturn(true) },
            isMultiFactorAuthAvailable = mock { on { invoke() }.thenReturn(true) },
            fetchAutoAcceptQRLinks = fetchAutoAcceptQRLinks,
            fetchMultiFactorAuthSetting = mock { on { invoke() }.thenReturn(emptyFlow()) },
            startScreen = mock { on { invoke() }.thenReturn(emptyFlow()) },
            isHideRecentActivityEnabled = mock { on { invoke() }.thenReturn(emptyFlow()) },
            toggleAutoAcceptQRLinks = toggleAutoAcceptQRLinks,
            monitorConnectivity = mock { on { invoke() }.thenReturn(flowOf(true)) },
            requestAccountDeletion = mock(),
            isChatLoggedIn = isChatLoggedIn,
            setSdkLogsEnabled = mock(),
            setChatLoggingEnabled = mock(),
        )
    }

    @Test
    fun `test initial value for auto accept is false`() = runTest {
        underTest.uiState.test {
            assertThat(awaitItem().autoAcceptChecked).isFalse()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that the subsequent value auto accept is returned from the use case`() = runTest {
        underTest.uiState
            .map { it.autoAcceptChecked }
            .distinctUntilChanged()
            .test {
                assertThat(awaitItem()).isFalse()
                assertThat(awaitItem()).isTrue()
            }

    }

    @Test
    fun `test that toggle updates the auto accept value`() = runTest {
        whenever(toggleAutoAcceptQRLinks()).thenReturn(false)

        underTest.uiState
            .map { it.autoAcceptChecked }
            .distinctUntilChanged(Boolean::equals)
            .test {
                assertThat(awaitItem()).isFalse()
                assertThat(awaitItem()).isTrue()
                underTest.toggleAutoAcceptPreference()
                assertThat(awaitItem()).isFalse()
            }

    }

    @Test
    fun `test that logging out of chat disables chat settings`() = runTest {

        underTest.uiState
            .map { it.chatEnabled }
            .distinctUntilChanged()
            .test {
                assertThat(awaitItem()).isTrue()

                isChatLoggedInValue.tryEmit(false)

                assertThat(awaitItem()).isFalse()
            }
    }

    @Test
    fun `test that chat is enabled by default`() = runTest {
        underTest.uiState
            .map { it.chatEnabled }
            .test {
                assertThat(awaitItem()).isTrue()
            }
    }

    @Test
    fun `test that camera uploads is enabled by default`() = runTest {
        underTest.uiState
            .map { it.cameraUploadEnabled }
            .test {
                assertThat(awaitItem()).isTrue()
            }
    }

    @Test
    fun `test that an error on fetching QR setting returns false instead`() = runTest {
        whenever(fetchAutoAcceptQRLinks()).thenAnswer { throw SettingNotFoundException() }

        underTest.uiState
            .map { it.autoAcceptChecked }
            .test {
                assertThat(awaitItem()).isFalse()
            }
    }
}