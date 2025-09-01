package mega.privacy.android.app.presentation.settings

import androidx.navigation3.runtime.NavKey
import app.cash.turbine.Event
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.TEST_USER_ACCOUNT
import mega.privacy.android.app.appstate.content.mapper.ScreenPreferenceDestinationMapper
import mega.privacy.android.app.extensions.asHotFlow
import mega.privacy.android.app.presentation.settings.SettingsFragment.Companion.COOKIES_URI
import mega.privacy.android.app.presentation.settings.startscreen.mapper.StartScreenSummaryMapper
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.MyAccountUpdate
import mega.privacy.android.domain.entity.preference.StartScreen
import mega.privacy.android.domain.entity.preference.StartScreenDestinationPreference
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.exception.SettingNotFoundException
import mega.privacy.android.domain.usecase.GetAccountDetailsUseCase
import mega.privacy.android.domain.usecase.GetBusinessStatusUseCase
import mega.privacy.android.domain.usecase.IsChatLoggedIn
import mega.privacy.android.domain.usecase.IsMultiFactorAuthAvailable
import mega.privacy.android.domain.usecase.MonitorMediaDiscoveryView
import mega.privacy.android.domain.usecase.MonitorPasscodeLockPreferenceUseCase
import mega.privacy.android.domain.usecase.MonitorStartScreenPreference
import mega.privacy.android.domain.usecase.SetMediaDiscoveryView
import mega.privacy.android.domain.usecase.account.IsMultiFactorAuthEnabledUseCase
import mega.privacy.android.domain.usecase.account.MonitorAccountDetailUseCase
import mega.privacy.android.domain.usecase.account.MonitorMyAccountUpdateUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsCameraUploadsEnabledUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.login.GetSessionTransferURLUseCase
import mega.privacy.android.domain.usecase.mediaplayer.audioplayer.SetAudioBackgroundPlayEnabledUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.preference.MonitorStartScreenPreferenceDestinationUseCase
import mega.privacy.android.domain.usecase.setting.MonitorContactLinksOptionUseCase
import mega.privacy.android.domain.usecase.setting.MonitorHideRecentActivityUseCase
import mega.privacy.android.domain.usecase.setting.MonitorShowHiddenItemsUseCase
import mega.privacy.android.domain.usecase.setting.MonitorSubFolderMediaDiscoverySettingsUseCase
import mega.privacy.android.domain.usecase.setting.SetHideRecentActivityUseCase
import mega.privacy.android.domain.usecase.setting.SetSubFolderMediaDiscoveryEnabledUseCase
import mega.privacy.android.domain.usecase.setting.ToggleContactLinksOptionUseCase
import mega.privacy.android.feature_flags.AppFeatures
import mega.privacy.android.navigation.contract.MainNavItem
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.stream.Stream

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SettingsViewModelTest {
    private lateinit var underTest: SettingsViewModel

    private val toggleContactLinksOptionUseCase = mock<ToggleContactLinksOptionUseCase>()
    private val isMultiFactorAuthEnabledUseCase = mock<IsMultiFactorAuthEnabledUseCase>()
    private val isChatLoggedInValue = MutableStateFlow(true)
    private val setHideRecentActivityUseCase = mock<SetHideRecentActivityUseCase>()
    private val setMediaDiscoveryView = mock<SetMediaDiscoveryView>()
    private val getFeatureFlagValueUseCase = mock<GetFeatureFlagValueUseCase>()
    private val monitorPasscodeLockPreferenceUseCase = mock<MonitorPasscodeLockPreferenceUseCase>()
    private val monitorContactLinksOptionUseCase = mock<MonitorContactLinksOptionUseCase>()
    private val isChatLoggedIn = mock<IsChatLoggedIn>()
    private val monitorHideRecentActivityUseCase = mock<MonitorHideRecentActivityUseCase>()
    private val monitorMediaDiscoveryView = mock<MonitorMediaDiscoveryView>()
    private val getAccountDetailsUseCase = mock<GetAccountDetailsUseCase>()
    private val monitorConnectivityUseCase = mock<MonitorConnectivityUseCase>()
    private val monitorStartScreenPreference = mock<MonitorStartScreenPreference>()
    private val isMultiFactorAuthAvailable = mock<IsMultiFactorAuthAvailable>()
    private val isCameraUploadsEnabledUseCase = mock<IsCameraUploadsEnabledUseCase>()
    private val setSubFolderMediaDiscoveryEnabledUseCase =
        mock<SetSubFolderMediaDiscoveryEnabledUseCase>()
    private val monitorSubFolderMediaDiscoverySettingsUseCase =
        mock<MonitorSubFolderMediaDiscoverySettingsUseCase>()
    private val getSessionTransferURLUseCase = mock<GetSessionTransferURLUseCase>()
    private val monitorShowHiddenItemsUseCase = mock<MonitorShowHiddenItemsUseCase>()
    private val monitorAccountDetailUseCase = mock<MonitorAccountDetailUseCase>()
    private val setAudioBackgroundPlayEnabledUseCase = mock<SetAudioBackgroundPlayEnabledUseCase>()
    private val getBusinessStatusUseCase = mock<GetBusinessStatusUseCase>()
    private val monitorMyAccountUpdateUseCase = mock<MonitorMyAccountUpdateUseCase>()

    private val startScreenSummaryMapper = mock<StartScreenSummaryMapper>()


    private val monitorStartScreenPreferenceDestinationUseCase =
        mock<MonitorStartScreenPreferenceDestinationUseCase>()

    private val screenPreferenceDestinationMapper = mock<ScreenPreferenceDestinationMapper>()

    private val defaultStartScreen = mock<NavKey>()

    @BeforeEach
    fun setUp() {
        monitorContactLinksOptionUseCase.stub {
            on { invoke() }.thenReturn(true.asHotFlow())
        }

        getFeatureFlagValueUseCase.stub {
            onBlocking { invoke(any()) }.thenReturn(false)
        }

        monitorContactLinksOptionUseCase.stub {
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

            startScreenSummaryMapper.stub {
                on { invoke(any<StartScreen>()) } doReturn "Start Screen"
            }
        }

        getAccountDetailsUseCase.stub { onBlocking { invoke(any()) }.thenReturn(TEST_USER_ACCOUNT) }

        monitorConnectivityUseCase.stub { on { invoke() }.thenReturn(MutableStateFlow(true)) }

        monitorStartScreenPreference.stub {
            on { invoke() }.thenReturn(StartScreen.Home.asHotFlow())
        }

        isMultiFactorAuthAvailable.stub { on { invoke() }.thenReturn(true) }

        isCameraUploadsEnabledUseCase.stub { onBlocking { invoke() }.thenReturn(false) }

        getSessionTransferURLUseCase.stub { onBlocking { invoke(any()) }.thenReturn(null) }

        monitorShowHiddenItemsUseCase.stub { onBlocking { invoke() }.thenReturn(emptyFlow()) }

        monitorAccountDetailUseCase.stub { on { invoke() }.thenReturn(emptyFlow()) }
        whenever(monitorMyAccountUpdateUseCase()).thenReturn(emptyFlow())

        whenever(monitorPasscodeLockPreferenceUseCase()).thenReturn(emptyFlow())
        isMultiFactorAuthEnabledUseCase.stub {
            onBlocking { invoke() }.thenReturn(false)
        }

    }

    private fun initViewModel(mainNavItems: Set<MainNavItem> = emptySet()) {
        underTest = SettingsViewModel(
            getAccountDetailsUseCase = getAccountDetailsUseCase,
            canDeleteAccount = mock { on { invoke(TEST_USER_ACCOUNT) }.thenReturn(true) },
            isCameraUploadsEnabledUseCase = isCameraUploadsEnabledUseCase,
            rootNodeExistsUseCase = mock { onBlocking { invoke() }.thenReturn(true) },
            isMultiFactorAuthAvailable = isMultiFactorAuthAvailable,
            monitorContactLinksOptionUseCase = monitorContactLinksOptionUseCase,
            isMultiFactorAuthEnabledUseCase = isMultiFactorAuthEnabledUseCase,
            startScreen = monitorStartScreenPreference,
            monitorHideRecentActivityUseCase = monitorHideRecentActivityUseCase,
            setHideRecentActivityUseCase = setHideRecentActivityUseCase,
            monitorMediaDiscoveryView = monitorMediaDiscoveryView,
            setMediaDiscoveryView = setMediaDiscoveryView,
            toggleContactLinksOptionUseCase = toggleContactLinksOptionUseCase,
            monitorConnectivityUseCase = monitorConnectivityUseCase,
            requestAccountDeletion = mock(),
            isChatLoggedIn = isChatLoggedIn,
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
            monitorPasscodeLockPreferenceUseCase = monitorPasscodeLockPreferenceUseCase,
            setSubFolderMediaDiscoveryEnabledUseCase = setSubFolderMediaDiscoveryEnabledUseCase,
            monitorSubFolderMediaDiscoverySettingsUseCase = monitorSubFolderMediaDiscoverySettingsUseCase,
            getSessionTransferURLUseCase = getSessionTransferURLUseCase,
            monitorShowHiddenItemsUseCase = monitorShowHiddenItemsUseCase,
            setShowHiddenItemsUseCase = mock(),
            monitorAccountDetailUseCase = monitorAccountDetailUseCase,
            setAudioBackgroundPlayEnabledUseCase = setAudioBackgroundPlayEnabledUseCase,
            getBusinessStatusUseCase = getBusinessStatusUseCase,
            monitorMyAccountUpdateUseCase = monitorMyAccountUpdateUseCase,
            startScreenSummaryMapper = startScreenSummaryMapper,
            mainDestinations = mainNavItems,
            monitorStartScreenPreferenceDestinationUseCase = monitorStartScreenPreferenceDestinationUseCase,
            screenPreferenceDestinationMapper = screenPreferenceDestinationMapper,
            defaultStartScreen = defaultStartScreen
        )
    }

    @AfterEach
    internal fun cleanUp() {
        Mockito.reset(
            monitorContactLinksOptionUseCase,
            getFeatureFlagValueUseCase,
            monitorHideRecentActivityUseCase,
            getAccountDetailsUseCase,
            monitorSubFolderMediaDiscoverySettingsUseCase,
            getSessionTransferURLUseCase,
            setAudioBackgroundPlayEnabledUseCase,
            startScreenSummaryMapper,
            monitorStartScreenPreferenceDestinationUseCase,
            screenPreferenceDestinationMapper,
            defaultStartScreen,
            isCameraUploadsEnabledUseCase,
            monitorContactLinksOptionUseCase,
            isMultiFactorAuthEnabledUseCase,
            monitorStartScreenPreference,
            monitorMediaDiscoveryView,
            toggleContactLinksOptionUseCase,
            monitorConnectivityUseCase,
            monitorPasscodeLockPreferenceUseCase,
            setSubFolderMediaDiscoveryEnabledUseCase,
            monitorShowHiddenItemsUseCase,
            monitorAccountDetailUseCase,
            getBusinessStatusUseCase,
            monitorMyAccountUpdateUseCase,
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
        whenever(monitorPasscodeLockPreferenceUseCase()).thenReturn(emptyFlow())
        isMultiFactorAuthEnabledUseCase.stub {
            onBlocking { invoke() }.thenReturn(false)
        }
        monitorContactLinksOptionUseCase.stub {
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
        isMultiFactorAuthEnabledUseCase.stub {
            onBlocking { invoke() }.thenReturn(false)
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
        initViewModel()
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
            monitorContactLinksOptionUseCase.stub {
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
            isMultiFactorAuthEnabledUseCase.stub {
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
            isMultiFactorAuthEnabledUseCase.stub {
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

            isMultiFactorAuthEnabledUseCase.stub {
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
            isMultiFactorAuthEnabledUseCase.stub {
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
            isMultiFactorAuthEnabledUseCase.stub {
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

    @ParameterizedTest(name = "when all Feature Flags are {0} and sessionTransferURL is {1} then cookiePolicyLink is: {2}")
    @MethodSource("provideCookiePolicyLinkParameters")
    fun `test that cookiePolicyLink is updated`(
        isEnabled: Boolean,
        link: String?,
        expected: String?,
    ) = runTest {
        initViewModel()
        whenever(getSessionTransferURLUseCase(any())).thenReturn(link)
        getFeatureFlagValueUseCase.stub {
            onBlocking { invoke(any()) }.thenReturn(isEnabled)
        }
        advanceUntilIdle()
        assertThat(underTest.getCookiePolicyLink()).isEqualTo(expected)
    }

    private fun provideCookiePolicyLinkParameters(): Stream<Arguments> {
        return Stream.of(
            Arguments.of(true, "https://mega.app/testcookie", "https://mega.app/testcookie"),
            Arguments.of(true, null, null),
            Arguments.of(false, null, COOKIES_URI)
        )
    }

    @Test
    fun `test that setAudioBackgroundPlayEnabledUseCase is invoked as expected`() = runTest {
        val expected = false
        underTest.toggleBackgroundPlay(expected)
        advanceUntilIdle()
        verify(setAudioBackgroundPlayEnabledUseCase).invoke(expected)
    }

    @Test
    fun `test that my account update and account details are updated`() = runTest {
        whenever(monitorMyAccountUpdateUseCase()).thenReturn(flowOf(MyAccountUpdate(MyAccountUpdate.Action.UPDATE_ACCOUNT_DETAILS)))
        initViewModel()
        advanceUntilIdle()
        verify(getAccountDetailsUseCase).invoke(true)
    }

    @Test
    fun `test that start screen summary is correct when single activity flag is false`() = runTest {
        getFeatureFlagValueUseCase.stub {
            onBlocking { invoke(AppFeatures.SingleActivity) }.thenReturn(false)
        }

        val expected = "Expected Start Screen"
        startScreenSummaryMapper.stub {
            on { invoke(any<StartScreen>()) } doReturn expected
        }

        initViewModel()

        underTest.uiState.map { it.startScreenSummary }
            .distinctUntilChanged()
            .test {
                assertThat(awaitItem()).isEmpty()
                advanceUntilIdle()
                assertThat(awaitItem()).isEqualTo(expected)
            }
    }

    @Test
    fun `test that start screen summary is correct when single activity flag is true and no value is set`() =
        runTest {
            getFeatureFlagValueUseCase.stub {
                onBlocking { invoke(AppFeatures.SingleActivity) }.thenReturn(true)
            }

            val expected = "Default Single activity Start Screen"

            monitorStartScreenPreferenceDestinationUseCase.stub {
                on { invoke() } doReturn flow {
                    emit(null)
                    awaitCancellation()
                }
            }

            val expectedMainNavItem = mock<MainNavItem> {
                on { destination } doReturn defaultStartScreen
            }

            val mainNavItems = setOf(
                mock<MainNavItem> {
                    on { destination } doReturn mock<NavKey>()
                },
                mock<MainNavItem> {
                    on { destination } doReturn mock<NavKey>()
                },
                expectedMainNavItem,
            )

            screenPreferenceDestinationMapper.stub {
                on { invoke(null) } doReturn null
            }

            startScreenSummaryMapper.stub {
                on { invoke(expectedMainNavItem) } doReturn expected
            }

            initViewModel(mainNavItems)

            underTest.uiState
//                .test { cancelAndConsumeRemainingEvents().forEach { println(it) } }
                .map { it.startScreenSummary }
                .distinctUntilChanged()
                .test {
                    assertThat(awaitItem()).isEmpty()
                    advanceUntilIdle()
                    assertThat(awaitItem()).isEqualTo(expected)
                }
        }

    @Test
    fun `test that start screen summary is correct when single activity flag is true`() = runTest {
        getFeatureFlagValueUseCase.stub {
            onBlocking { invoke(AppFeatures.SingleActivity) }.thenReturn(true)
        }

        val expected = "Expected Single activity Start Screen"

        monitorStartScreenPreferenceDestinationUseCase.stub {
            on { invoke() } doReturn flow {
                emit(mock<StartScreenDestinationPreference>())
                awaitCancellation()
            }
        }

        val selectedOption = mock<NavKey>()
        val expectedMainNavItem = mock<MainNavItem> {
            on { destination } doReturn selectedOption
        }
        val mainNavItems = setOf(
            mock<MainNavItem> {
                on { destination } doReturn mock<NavKey>()
            },
            mock<MainNavItem> {
                on { destination } doReturn mock<NavKey>()
            },
            expectedMainNavItem,
        )

        screenPreferenceDestinationMapper.stub {
            on { invoke(any()) } doReturn selectedOption
        }

        startScreenSummaryMapper.stub {
            on { invoke(expectedMainNavItem) } doReturn expected
        }

        initViewModel(mainNavItems)

        underTest.uiState.map { it.startScreenSummary }
            .distinctUntilChanged()
            .test {
                assertThat(awaitItem()).isEmpty()
                advanceUntilIdle()
                assertThat(awaitItem()).isEqualTo(expected)
            }
    }

    companion object {
        @JvmField
        @RegisterExtension
        val extension = CoroutineMainDispatcherExtension(StandardTestDispatcher())
    }
}
