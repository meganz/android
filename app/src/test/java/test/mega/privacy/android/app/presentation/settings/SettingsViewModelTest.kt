package test.mega.privacy.android.app.presentation.settings

import app.cash.turbine.Event
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.presentation.settings.SettingsViewModel
import mega.privacy.android.domain.entity.preference.StartScreen
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.exception.SettingNotFoundException
import mega.privacy.android.domain.usecase.AreChatLogsEnabled
import mega.privacy.android.domain.usecase.AreSdkLogsEnabled
import mega.privacy.android.domain.usecase.FetchMultiFactorAuthSettingUseCase
import mega.privacy.android.domain.usecase.GetAccountDetailsUseCase
import mega.privacy.android.domain.usecase.IsChatLoggedIn
import mega.privacy.android.domain.usecase.IsMultiFactorAuthAvailable
import mega.privacy.android.domain.usecase.MonitorAutoAcceptQRLinks
import mega.privacy.android.domain.usecase.MonitorMediaDiscoveryView
import mega.privacy.android.domain.usecase.MonitorPasscodeLockPreferenceUseCase
import mega.privacy.android.domain.usecase.MonitorStartScreenPreference
import mega.privacy.android.domain.usecase.RefreshPasscodeLockPreference
import mega.privacy.android.domain.usecase.SetMediaDiscoveryView
import mega.privacy.android.domain.usecase.ToggleAutoAcceptQRLinks
import mega.privacy.android.domain.usecase.camerauploads.IsCameraUploadsEnabledUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.setting.MonitorHideRecentActivityUseCase
import mega.privacy.android.domain.usecase.setting.MonitorSubFolderMediaDiscoverySettingsUseCase
import mega.privacy.android.domain.usecase.setting.SetHideRecentActivityUseCase
import mega.privacy.android.domain.usecase.setting.SetSubFolderMediaDiscoveryEnabledUseCase
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import test.mega.privacy.android.app.TEST_USER_ACCOUNT
import test.mega.privacy.android.app.extensions.asHotFlow

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SettingsViewModelTest {
    private lateinit var underTest: SettingsViewModel

    private val toggleAutoAcceptQRLinks = mock<ToggleAutoAcceptQRLinks>()
    private val fetchMultiFactorAuthSettingUseCase = mock<FetchMultiFactorAuthSettingUseCase>()
    private val isChatLoggedInValue = MutableStateFlow(true)
    private val setHideRecentActivityUseCase = mock<SetHideRecentActivityUseCase>()
    private val setMediaDiscoveryView = mock<SetMediaDiscoveryView>()
    private val getFeatureFlagValueUseCase = mock<GetFeatureFlagValueUseCase>()
    private val monitorPasscodeLockPreferenceUseCase = mock<MonitorPasscodeLockPreferenceUseCase>()
    private val refreshPasscodeLockPreference = mock<RefreshPasscodeLockPreference>()
    private val monitorAutoAcceptQRLinks = mock<MonitorAutoAcceptQRLinks>()
    private val isChatLoggedIn = mock<IsChatLoggedIn>()
    private val monitorHideRecentActivityUseCase = mock<MonitorHideRecentActivityUseCase>()
    private val monitorMediaDiscoveryView = mock<MonitorMediaDiscoveryView>()
    private val getAccountDetailsUseCase = mock<GetAccountDetailsUseCase>()
    private val monitorConnectivityUseCase = mock<MonitorConnectivityUseCase>()
    private val monitorStartScreenPreference = mock<MonitorStartScreenPreference>()
    private val isMultiFactorAuthAvailable = mock<IsMultiFactorAuthAvailable>()
    private val areSdkLogsEnabled = mock<AreSdkLogsEnabled>()
    private val areChatLogsEnabled = mock<AreChatLogsEnabled>()
    private val isCameraUploadsEnabledUseCase = mock<IsCameraUploadsEnabledUseCase>()
    private val setSubFolderMediaDiscoveryEnabledUseCase =
        mock<SetSubFolderMediaDiscoveryEnabledUseCase>()
    private val monitorSubFolderMediaDiscoverySettingsUseCase =
        mock<MonitorSubFolderMediaDiscoverySettingsUseCase>()

    @BeforeAll
    fun initialise() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @BeforeEach
    fun setUp() {
        monitorAutoAcceptQRLinks.stub {
            on { invoke() }.thenReturn(true.asHotFlow())
        }

        getFeatureFlagValueUseCase.stub {
            onBlocking { invoke(any()) }.thenReturn(false)
        }

        refreshPasscodeLockPreference.stub {
            onBlocking { invoke() }.thenReturn(false)
        }

        monitorAutoAcceptQRLinks.stub {
            on { invoke() }.thenReturn(
                emptyFlow()
            )
        }
        isChatLoggedIn.stub { on { invoke() }.thenReturn(isChatLoggedInValue) }
        monitorHideRecentActivityUseCase.stub {
            on { invoke() }.thenReturn(
                emptyFlow()
            )
        }
        monitorMediaDiscoveryView.stub {
            on { invoke() }.thenReturn(
                emptyFlow()
            )
        }

        monitorSubFolderMediaDiscoverySettingsUseCase.stub {
            on { invoke() }.thenReturn(
                emptyFlow()
            )
        }

        getAccountDetailsUseCase.stub { onBlocking { invoke(any()) }.thenReturn(TEST_USER_ACCOUNT) }

        monitorConnectivityUseCase.stub { on { invoke() }.thenReturn(MutableStateFlow(true)) }

        monitorStartScreenPreference.stub {
            on { invoke() }.thenReturn(StartScreen.Home.asHotFlow())
        }

        isMultiFactorAuthAvailable.stub { on { invoke() }.thenReturn(true) }

        areSdkLogsEnabled.stub { on { invoke() }.thenReturn(emptyFlow()) }

        areChatLogsEnabled.stub { on { invoke() }.thenReturn(emptyFlow()) }

        isCameraUploadsEnabledUseCase.stub { onBlocking { invoke() }.thenReturn(false) }

        initViewModel()
    }


    private fun initViewModel() {
        underTest = SettingsViewModel(
            getAccountDetailsUseCase = getAccountDetailsUseCase,
            canDeleteAccount = mock { on { invoke(TEST_USER_ACCOUNT) }.thenReturn(true) },
            refreshPasscodeLockPreference = refreshPasscodeLockPreference,
            areSdkLogsEnabled = areSdkLogsEnabled,
            areChatLogsEnabled = areChatLogsEnabled,
            isCameraUploadsEnabledUseCase = isCameraUploadsEnabledUseCase,
            rootNodeExistsUseCase = mock { on { runBlocking { invoke() } }.thenReturn(true) },
            isMultiFactorAuthAvailable = isMultiFactorAuthAvailable,
            monitorAutoAcceptQRLinks = monitorAutoAcceptQRLinks,
            fetchMultiFactorAuthSettingUseCase = fetchMultiFactorAuthSettingUseCase,
            startScreen = monitorStartScreenPreference,
            monitorHideRecentActivityUseCase = monitorHideRecentActivityUseCase,
            setHideRecentActivityUseCase = setHideRecentActivityUseCase,
            monitorMediaDiscoveryView = monitorMediaDiscoveryView,
            setMediaDiscoveryView = setMediaDiscoveryView,
            toggleAutoAcceptQRLinks = toggleAutoAcceptQRLinks,
            monitorConnectivityUseCase = monitorConnectivityUseCase,
            requestAccountDeletion = mock(),
            isChatLoggedIn = isChatLoggedIn,
            setSdkLogsEnabled = mock(),
            setChatLoggingEnabled = mock(),
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
            monitorPasscodeLockPreferenceUseCase = monitorPasscodeLockPreferenceUseCase,
            setSubFolderMediaDiscoveryEnabledUseCase = setSubFolderMediaDiscoveryEnabledUseCase,
            monitorSubFolderMediaDiscoverySettingsUseCase = monitorSubFolderMediaDiscoverySettingsUseCase,
        )
    }

    @AfterEach
    internal fun cleanUp() {
        Mockito.reset(
            monitorAutoAcceptQRLinks,
            getFeatureFlagValueUseCase,
            monitorHideRecentActivityUseCase,
            getAccountDetailsUseCase,
            monitorSubFolderMediaDiscoverySettingsUseCase
        )
    }

    @AfterAll
    internal fun teardown() {
        Dispatchers.resetMain()
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
        fetchMultiFactorAuthSettingUseCase.stub {
            onBlocking { invoke() }.thenReturn(false)
            monitorAutoAcceptQRLinks.stub {
                on { invoke() }.thenReturn(
                    flow {
                        emit(true)
                        emit(false)
                    }
                )
            }

            initViewModel()
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
            fetchMultiFactorAuthSettingUseCase.stub {
                onBlocking { invoke() }.thenReturn(false)
            }
        }
        monitorHideRecentActivityUseCase.stub {
            on { invoke() }.thenReturn(false.asHotFlow())
        }
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
    fun `test that an error on fetching QR setting returns false instead`() =
        runTest {
            monitorAutoAcceptQRLinks.stub {
                on { invoke() }.thenAnswer {
                    throw SettingNotFoundException(
                        -1
                    )
                }
            }

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
            fetchMultiFactorAuthSettingUseCase.stub {
                onBlocking { invoke() }.thenReturn(true)
            }
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
            fetchMultiFactorAuthSettingUseCase.stub {
                onBlocking { invoke() }.thenReturn(false)
            }

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
            val recentActivityFlow = flow {
                emit(true)
                emit(true)
                emit(false)
                emit(true)
                awaitCancellation()
            }

            monitorHideRecentActivityUseCase.stub {
                on { invoke() }.thenReturn(recentActivityFlow)
            }

            fetchMultiFactorAuthSettingUseCase.stub {
                onBlocking { invoke() }.thenReturn(false)
            }

            initViewModel()

            underTest.uiState
                .map { it.hideRecentActivityChecked }
                .distinctUntilChanged()
                .test {
                    assertThat(awaitItem()).isFalse()
                    assertThat(awaitItem()).isTrue()
                    assertThat(awaitItem()).isFalse()
                    assertThat(awaitItem()).isTrue()
                }
        }

    @Test
    fun `test that subFolderMediaDiscoveryChecked is triggered with return value of monitorSubFolderMediaDiscoverySettingsUseCase`() =
        runTest {
            val subFolderMediaDiscoveryFlow = flow {
                emit(true)
                emit(true)
                emit(false)
                emit(true)
                awaitCancellation()
            }

            monitorHideRecentActivityUseCase.stub {
                on { invoke() }.thenReturn(false.asHotFlow())
            }
            fetchMultiFactorAuthSettingUseCase.stub {
                onBlocking { invoke() }.thenReturn(false)
            }

            monitorSubFolderMediaDiscoverySettingsUseCase.stub {
                on { invoke() }.thenReturn(subFolderMediaDiscoveryFlow)
            }

            initViewModel()

            underTest.uiState
                .map { it.subFolderMediaDiscoveryChecked }
                .distinctUntilChanged()
                .test {
                    assertThat(awaitItem()).isTrue()
                    assertThat(awaitItem()).isFalse()
                    assertThat(awaitItem()).isTrue()
                }
        }

    @Test
    fun `test that mediaDiscoveryViewChecked is set with return value of monitorMediaDiscoveryView`() =
        runTest {
            monitorHideRecentActivityUseCase.stub {
                on { invoke() }.thenReturn(false.asHotFlow())
            }
            fetchMultiFactorAuthSettingUseCase.stub {
                onBlocking { invoke() }.thenReturn(false)
            }

            monitorMediaDiscoveryView.stub {
                on { invoke() }.thenReturn(
                    flow {
                        emit(0)
                        emit(1)
                        emit(2)
                    }
                )
            }

            initViewModel()

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
            verify(setHideRecentActivityUseCase).invoke(expected)
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
            onBlocking { invoke(any()) }.thenAnswer {
                throw MegaException(
                    1,
                    "It's broken"
                )
            }
        }

        underTest.uiState.test {
            assertThat(cancelAndConsumeRemainingEvents().any { it is Event.Error }).isFalse()
        }
    }


    @Test
    internal fun `test that passcodeLock is set with values returned from monitorPasscodeLockPreferenceUseCase`() =
        runTest {
            getFeatureFlagValueUseCase.stub {
                onBlocking { invoke(AppFeatures.PasscodeBackend) }.thenReturn(true)
            }

            monitorPasscodeLockPreferenceUseCase.stub {
                on { invoke() }.thenReturn(
                    flow {
                        emit(true)
                        emit(false)
                        emit(true)
                        emit(false)
                        awaitCancellation()
                    }
                )
            }

            initViewModel()

            underTest.uiState.map { it.passcodeLock }
                .distinctUntilChanged()
                .test {
                    assertThat(awaitItem()).isFalse()
                    assertThat(awaitItem()).isTrue()
                    assertThat(awaitItem()).isFalse()
                    assertThat(awaitItem()).isTrue()
                    assertThat(awaitItem()).isFalse()
                }
        }

}
