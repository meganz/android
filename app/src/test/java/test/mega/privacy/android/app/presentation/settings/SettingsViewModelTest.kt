package test.mega.privacy.android.app.presentation.settings

import app.cash.turbine.Event
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.presentation.settings.SettingsViewModel
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.exception.SettingNotFoundException
import mega.privacy.android.domain.usecase.FetchMultiFactorAuthSettingUseCase
import mega.privacy.android.domain.usecase.GetAccountDetailsUseCase
import mega.privacy.android.domain.usecase.IsChatLoggedIn
import mega.privacy.android.domain.usecase.MonitorAutoAcceptQRLinks
import mega.privacy.android.domain.usecase.MonitorHideRecentActivity
import mega.privacy.android.domain.usecase.MonitorMediaDiscoveryView
import mega.privacy.android.domain.usecase.SetHideRecentActivity
import mega.privacy.android.domain.usecase.SetMediaDiscoveryView
import mega.privacy.android.domain.usecase.ToggleAutoAcceptQRLinks
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import test.mega.privacy.android.app.TEST_USER_ACCOUNT

@ExperimentalCoroutinesApi
class SettingsViewModelTest {
    private lateinit var underTest: SettingsViewModel

    private val monitorAutoAcceptQRLinks = mock<MonitorAutoAcceptQRLinks> {
        on { invoke() }.thenReturn(
            emptyFlow()
        )
    }
    private val toggleAutoAcceptQRLinks = mock<ToggleAutoAcceptQRLinks>()
    private val fetchMultiFactorAuthSettingUseCase = mock<FetchMultiFactorAuthSettingUseCase>()
    private val isChatLoggedInValue = MutableStateFlow(true)
    private val isChatLoggedIn =
        mock<IsChatLoggedIn> { on { invoke() }.thenReturn(isChatLoggedInValue) }
    private val monitorHideRecentActivity = mock<MonitorHideRecentActivity> {
        on { invoke() }.thenReturn(
            emptyFlow()
        )
    }
    private val setHideRecentActivity = mock<SetHideRecentActivity>()
    private val monitorMediaDiscoveryView = mock<MonitorMediaDiscoveryView> {
        on { invoke() }.thenReturn(
            emptyFlow()
        )
    }
    private val setMediaDiscoveryView = mock<SetMediaDiscoveryView>()

    private val getAccountDetailsUseCase =
        mock<GetAccountDetailsUseCase> { onBlocking { invoke(any()) }.thenReturn(TEST_USER_ACCOUNT) }

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
        whenever(monitorAutoAcceptQRLinks()).thenReturn(flowOf(true))
        underTest = SettingsViewModel(
            getAccountDetailsUseCase = getAccountDetailsUseCase,
            canDeleteAccount = mock { on { invoke(TEST_USER_ACCOUNT) }.thenReturn(true) },
            refreshPasscodeLockPreference = mock(),
            areSdkLogsEnabled = mock { on { invoke() }.thenReturn(emptyFlow()) },
            areChatLogsEnabled = mock { on { invoke() }.thenReturn(emptyFlow()) },
            isCameraUploadsEnabledUseCase = mock { onBlocking { invoke() }.thenReturn(false) },
            rootNodeExistsUseCase = mock { on { runBlocking { invoke() } }.thenReturn(true) },
            isMultiFactorAuthAvailable = mock { on { invoke() }.thenReturn(true) },
            monitorAutoAcceptQRLinks = monitorAutoAcceptQRLinks,
            fetchMultiFactorAuthSettingUseCase = fetchMultiFactorAuthSettingUseCase,
            startScreen = mock { on { invoke() }.thenReturn(emptyFlow()) },
            monitorHideRecentActivity = monitorHideRecentActivity,
            setHideRecentActivity = setHideRecentActivity,
            monitorMediaDiscoveryView = monitorMediaDiscoveryView,
            setMediaDiscoveryView = setMediaDiscoveryView,
            toggleAutoAcceptQRLinks = toggleAutoAcceptQRLinks,
            monitorConnectivityUseCase = mock { on { invoke() }.thenReturn(MutableStateFlow(true)) },
            requestAccountDeletion = mock(),
            isChatLoggedIn = isChatLoggedIn,
            setSdkLogsEnabled = mock(),
            setChatLoggingEnabled = mock(),
            putStringPreference = mock(),
            putStringSetPreference = mock(),
            putIntPreference = mock(),
            putLongPreference = mock(),
            putFloatPreference = mock(),
            putBooleanPreference = mock(),
            getStringPreference = mock(),
            getStringSetPreference = mock(),
            getIntPreference = mock(),
            getLongPreference = mock(),
            getFloatPreference = mock(),
            getBooleanPreference = mock(),
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
        whenever(fetchMultiFactorAuthSettingUseCase()).thenReturn(false)
        whenever(monitorAutoAcceptQRLinks()).thenReturn(
            flow {
                emit(true)
                emit(false)
            }
        )

        underTest.uiState
            .map { it.autoAcceptChecked }
            .distinctUntilChanged()
            .test {
                assertThat(awaitItem()).isFalse()
                assertThat(awaitItem()).isTrue()
                assertThat(awaitItem()).isFalse()
            }
    }

    @Test
    fun `test that logging out of chat disables chat settings`() = runTest {
        whenever(fetchMultiFactorAuthSettingUseCase()).thenReturn(false)
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
            .map { it.cameraUploadsEnabled }
            .test {
                assertThat(awaitItem()).isTrue()
            }
    }

    @Test
    fun `test that an error on fetching QR setting returns false instead`() = runTest {
        whenever(monitorAutoAcceptQRLinks()).thenAnswer { throw SettingNotFoundException(-1) }

        underTest.uiState
            .map { it.autoAcceptChecked }
            .test {
                assertThat(awaitItem()).isFalse()
            }
    }

    @Test
    fun `test that multi factor is disabled by default`() = runTest {
        underTest.uiState
            .map { it.multiFactorAuthChecked }
            .test {
                assertThat(awaitItem()).isFalse()
            }
    }

    @Test
    fun `test that multi factor is enabled when fetching multi factor enabled returns true`() =
        runTest {
            whenever(fetchMultiFactorAuthSettingUseCase()).thenReturn(true)
            underTest.refreshMultiFactorAuthSetting()
            underTest.uiState
                .map { it.multiFactorAuthChecked }
                .distinctUntilChanged()
                .test {
                    assertThat(awaitItem()).isFalse()
                    assertThat(awaitItem()).isTrue()
                }
        }

    @Test
    fun `test that multi factor is disabled when fetching multi factor enabled returns false`() =
        runTest {
            whenever(fetchMultiFactorAuthSettingUseCase()).thenReturn(false)
            underTest.refreshMultiFactorAuthSetting()
            underTest.uiState
                .map { it.multiFactorAuthChecked }
                .distinctUntilChanged()
                .test {
                    assertThat(awaitItem()).isFalse()
                }
        }

    @Test
    fun `test that hideRecentActivityChecked is set with return value of monitorHideRecentActivity`() =
        runTest {
            whenever(fetchMultiFactorAuthSettingUseCase()).thenReturn(false)
            whenever(monitorHideRecentActivity()).thenReturn(
                flow {
                    emit(true)
                    emit(false)
                }
            )

            underTest.uiState
                .map { it.hideRecentActivityChecked }
                .distinctUntilChanged()
                .test {
                    assertThat(awaitItem()).isFalse()
                    assertThat(awaitItem()).isTrue()
                    assertThat(awaitItem()).isFalse()
                }
        }

    @Test
    fun `test that mediaDiscoveryViewChecked is set with return value of monitorMediaDiscoveryView`() =
        runTest {
            whenever(fetchMultiFactorAuthSettingUseCase()).thenReturn(false)
            whenever(monitorMediaDiscoveryView()).thenReturn(
                flow {
                    emit(0)
                    emit(1)
                    emit(2)
                }
            )

            underTest.uiState
                .map { it.mediaDiscoveryViewState }
                .distinctUntilChanged()
                .test {
                    assertThat(awaitItem()).isEqualTo(0)
                    assertThat(awaitItem()).isEqualTo(1)
                    assertThat(awaitItem()).isEqualTo(2)
                }
        }

    @Test
    fun `test that hideRecentActivity will call setHideRecentActivity use case`() =
        runTest {
            val expected = false
            underTest.hideRecentActivity(expected)
            advanceUntilIdle()
            verify(setHideRecentActivity).invoke(expected)
        }

    @Test
    fun `test that mediaDiscoveryView will call setMediaDiscoveryView use case`() =
        runTest {
            val expected = 0
            underTest.mediaDiscoveryView(expected)
            advanceUntilIdle()
            verify(setMediaDiscoveryView).invoke(expected)
        }

    @Test
    fun `test that an exception from get account is not propagated`() = runTest {
        getAccountDetailsUseCase.stub {
            onBlocking { invoke(any()) }.thenAnswer { throw MegaException(1, "It's broken") }
        }

        underTest.uiState.test {
            assertThat(cancelAndConsumeRemainingEvents().any { it is Event.Error }).isFalse()
        }
    }
}
