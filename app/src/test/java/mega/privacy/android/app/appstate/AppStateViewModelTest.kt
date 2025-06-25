package mega.privacy.android.app.appstate

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.appstate.model.AppState
import mega.privacy.android.app.presentation.settings.compose.home.view.SettingsHomeViewKtTest
import mega.privacy.android.domain.entity.Feature
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.navigation.Flagged
import mega.privacy.android.domain.usecase.MonitorThemeModeUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.navigation.GetStartScreenPreferenceDestinationUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.navigation.contract.FeatureDestination
import mega.privacy.android.navigation.contract.MainNavItem
import mega.privacy.android.navigation.contract.PreferredSlot
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.KStubbing
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AppStateViewModelTest {
    private lateinit var underTest: AppStateViewModel
    private val getFeatureFlagValueUseCase = mock<GetFeatureFlagValueUseCase>()
    private val monitorThemeModeUseCase = mock<MonitorThemeModeUseCase>()
    private val monitorConnectivityUseCase = mock<MonitorConnectivityUseCase>()

    private val getStartScreenPreferenceDestinationUseCase =
        mock<GetStartScreenPreferenceDestinationUseCase>()

    @BeforeAll
    fun initialisation() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterAll
    fun cleanUp() {
        Dispatchers.resetMain()
    }

    @BeforeEach
    fun setUp() {
        reset(
            getFeatureFlagValueUseCase,
            getStartScreenPreferenceDestinationUseCase,
            monitorThemeModeUseCase,
            monitorConnectivityUseCase
        )
    }

    @Test
    fun `test that initial state is loading`() = runTest {
        val mainDestinations = emptySet<@JvmSuppressWildcards MainNavItem>()
        val featureDestinations = emptySet<@JvmSuppressWildcards FeatureDestination>()
        initUnderTest(mainDestinations, featureDestinations)
        assertThat(underTest.state.value).isEqualTo(AppState.Loading)
    }

    @Test
    fun `test that main destinations are added`() = runTest {
        stubConnectivity()
        val mainNavItem = mock<MainNavItem> {
            on { destination }.thenReturn(SettingsHomeViewKtTest.TestDestination)
            on { preferredSlot }.thenReturn(PreferredSlot.Ordered(1))
            on { availableOffline }.thenReturn(true)
        }

        val expected = setOf(mainNavItem)

        stubDefaultStartScreenPreference()
        stubDefaultThemeMode()

        val featureDestinations = emptySet<@JvmSuppressWildcards FeatureDestination>()
        initUnderTest(expected, featureDestinations)

        underTest.state
            .filterIsInstance<AppState.Data>()
            .test {
                assertThat(awaitItem().mainNavItems.map { it.navItem }).containsExactlyElementsIn(
                    expected
                )
                cancelAndIgnoreRemainingEvents()
            }
    }

    @Suppress("UNCHECKED_CAST")
    @Test
    fun `test that main nav items with disabled feature flags are not returned`() = runTest {
        stubConnectivity()
        stubDefaultStartScreenPreference()
        stubDefaultThemeMode()
        val expected = mock<MainNavItem> {
            on { destination }.thenReturn(SettingsHomeViewKtTest.TestDestination)
            on { preferredSlot }.thenReturn(PreferredSlot.Ordered(1))
            on { availableOffline }.thenReturn(true)
        }
        val disabledFeature = mock<Feature>()
        getFeatureFlagValueUseCase.stub {
            onBlocking { invoke(disabledFeature) }.thenReturn(false)
        }
        val notExpected =
            mock<Flagged>(extraInterfaces = arrayOf(MainNavItem::class))
        notExpected.stub {
            on { feature }.thenReturn(disabledFeature)
            (this as? KStubbing<MainNavItem>)?.on { destination }
                ?.thenReturn(SettingsHomeViewKtTest.TestDestination)
            (this as? KStubbing<MainNavItem>)?.on { preferredSlot }
                ?.thenReturn(PreferredSlot.Ordered(2))
        }
        val mainDestinations = setOf(expected, notExpected as MainNavItem)
        val featureDestinations = emptySet<@JvmSuppressWildcards FeatureDestination>()
        initUnderTest(mainDestinations, featureDestinations)

        underTest.state
            .filterIsInstance<AppState.Data>()
            .test {
                assertThat(awaitItem().mainNavItems.map { it.navItem }).containsExactly(expected)
                cancelAndIgnoreRemainingEvents()
            }
    }

    @Suppress("UNCHECKED_CAST")
    @Test
    fun `test that main nav items with enabled feature flags are returned`() = runTest {
        stubConnectivity()
        stubDefaultStartScreenPreference()
        stubDefaultThemeMode()
        val expected = mock<MainNavItem> {
            on { destination }.thenReturn(SettingsHomeViewKtTest.TestDestination)
            on { preferredSlot }.thenReturn(PreferredSlot.Ordered(1))
            on { availableOffline }.thenReturn(true)
        }
        val enabledFeature = mock<Feature>()
        getFeatureFlagValueUseCase.stub {
            onBlocking { invoke(enabledFeature) }.thenReturn(true)
        }
        val alsoExpected =
            mock<Flagged>(extraInterfaces = arrayOf(MainNavItem::class))
        alsoExpected.stub {
            on { feature }.thenReturn(enabledFeature)
            (this as? KStubbing<MainNavItem>)?.on { destination }
                ?.thenReturn(SettingsHomeViewKtTest.TestDestination)
            (this as? KStubbing<MainNavItem>)?.on { preferredSlot }
                ?.thenReturn(PreferredSlot.Ordered(2))
            (this as? KStubbing<MainNavItem>)?.on { availableOffline }
                ?.thenReturn(true)
        }
        val mainDestinations = setOf(expected, alsoExpected as MainNavItem)
        val featureDestinations = emptySet<@JvmSuppressWildcards FeatureDestination>()
        initUnderTest(mainDestinations, featureDestinations)
        underTest.state
            .filterIsInstance<AppState.Data>()
            .test {
                assertThat(awaitItem().mainNavItems.map { it.navItem }).containsExactly(
                    expected,
                    alsoExpected
                )
                cancelAndIgnoreRemainingEvents()
            }
    }

    @Test
    fun `test that feature destinations are added`() = runTest {
        stubConnectivity()
        val expected = setOf(mock<FeatureDestination>())

        stubDefaultStartScreenPreference()
        stubDefaultThemeMode()

        val mainDestinations = stubDefaultMainNavigationItems()
        initUnderTest(mainDestinations, expected)

        underTest.state
            .filterIsInstance<AppState.Data>()
            .test {
                assertThat(awaitItem().featureDestinations).isEqualTo(expected)
                cancelAndIgnoreRemainingEvents()
            }
    }

    @Test
    fun `test that featureDestination items with disabled feature flags are not returned`() =
        runTest {
            stubConnectivity()
            stubDefaultStartScreenPreference()
            stubDefaultThemeMode()
            val expected = mock<FeatureDestination>()
            val disabledFeature = mock<Feature>()
            getFeatureFlagValueUseCase.stub {
                onBlocking { invoke(disabledFeature) }.thenReturn(false)
            }
            val notExpected =
                mock<Flagged>(extraInterfaces = arrayOf(FeatureDestination::class))
            notExpected.stub {
                on { feature }.thenReturn(disabledFeature)
            }
            val mainDestinations = stubDefaultMainNavigationItems()
            val featureDestinations = setOf(expected, notExpected as FeatureDestination)
            initUnderTest(mainDestinations, featureDestinations)

            underTest.state
                .filterIsInstance<AppState.Data>()
                .test {
                    assertThat(awaitItem().featureDestinations).containsExactly(expected)
                    cancelAndIgnoreRemainingEvents()
                }
        }

    @Test
    fun `test that featureDestination items with enabled feature flags are returned`() = runTest {
        stubConnectivity()
        stubDefaultStartScreenPreference()
        stubDefaultThemeMode()
        val expected = mock<FeatureDestination>()
        val enabledFeature = mock<Feature>()
        getFeatureFlagValueUseCase.stub {
            onBlocking { invoke(enabledFeature) }.thenReturn(true)
        }
        val alsoExpected =
            mock<Flagged>(extraInterfaces = arrayOf(FeatureDestination::class))
        alsoExpected.stub {
            on { feature }.thenReturn(enabledFeature)
        }
        val mainDestinations = stubDefaultMainNavigationItems()
        val featureDestinations = setOf(expected, alsoExpected as FeatureDestination)
        initUnderTest(mainDestinations, featureDestinations)
        underTest.state
            .filterIsInstance<AppState.Data>()
            .test {
                assertThat(awaitItem().featureDestinations).containsExactly(expected, alsoExpected)
                cancelAndIgnoreRemainingEvents()
            }
    }

    @Test
    fun `test that theme mode values are emitted`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher())
        stubConnectivity()
        stubDefaultStartScreenPreference()
        val expected = listOf(
            ThemeMode.Light,
            ThemeMode.Dark,
            ThemeMode.System
        )
        val themeModeFlow = MutableStateFlow(expected.first())
        monitorThemeModeUseCase.stub {
            on { invoke() }.thenReturn(
                themeModeFlow
            )
        }
        initUnderTest(
            mainDestinations = stubDefaultMainNavigationItems(),
            featureDestinations = emptySet(),
        )

        underTest.state
            .filterIsInstance<AppState.Data>()
            .test {
                advanceUntilIdle()
                expected.forEachIndexed { index, expectedThemeMode ->
                    themeModeFlow.emit(expected[index])
                    assertThat(awaitItem().themeMode).isEqualTo(expectedThemeMode)
                }
            }

        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @Test
    fun `test that items are enabled if available offline and connectivity is offline`() = runTest {
        stubDefaultStartScreenPreference()
        stubDefaultThemeMode()
        val mainNavItem = mock<MainNavItem> {
            on { destination }.thenReturn(SettingsHomeViewKtTest.TestDestination)
            on { preferredSlot }.thenReturn(PreferredSlot.Ordered(1))
            on { availableOffline }.thenReturn(true)
        }
        val expected = setOf(mainNavItem)

        stubConnectivity(connected = false)

        initUnderTest(expected, emptySet())

        underTest.state
            .filterIsInstance<AppState.Data>()
            .test {
                assertThat(awaitItem().mainNavItems.all { it.isEnabled }).isTrue()
                cancelAndIgnoreRemainingEvents()
            }
    }

    @Test
    fun `test that items are not enabled if not available offline and connectivity is offline`() =
        runTest {
            stubDefaultStartScreenPreference()
            stubDefaultThemeMode()
            val mainNavItem = mock<MainNavItem> {
                on { destination }.thenReturn(SettingsHomeViewKtTest.TestDestination)
                on { preferredSlot }.thenReturn(PreferredSlot.Ordered(1))
                on { availableOffline }.thenReturn(false)
            }
            val expected = setOf(mainNavItem)

            stubConnectivity(connected = false)

            initUnderTest(expected, emptySet())

            underTest.state
                .filterIsInstance<AppState.Data>()
                .test {
                    assertThat(awaitItem().mainNavItems.all { it.isEnabled }).isFalse()
                    cancelAndIgnoreRemainingEvents()
                }
        }

    @Test
    fun `test that exception in connectivity use case returns default connected state`() = runTest {
        stubDefaultStartScreenPreference()
        stubDefaultThemeMode()
        val mainNavItem = mock<MainNavItem> {
            on { destination }.thenReturn(SettingsHomeViewKtTest.TestDestination)
            on { preferredSlot }.thenReturn(PreferredSlot.Ordered(1))
            on { availableOffline }.thenReturn(false)
        }
        val expected = setOf(mainNavItem)

        monitorConnectivityUseCase.stub {
            on { invoke() }.thenReturn(
                flow {
                    throw Exception("Connectivity error")
                }
            )
        }

        initUnderTest(expected, emptySet())

        underTest.state
            .filterIsInstance<AppState.Data>()
            .test {
                assertThat(awaitItem().mainNavItems.all { it.isEnabled }).isTrue()
                cancelAndIgnoreRemainingEvents()
            }
    }

    private fun initUnderTest(
        mainDestinations: Set<MainNavItem>,
        featureDestinations: Set<FeatureDestination>,
    ) {
        underTest = AppStateViewModel(
            mainDestinations = mainDestinations,
            featureDestinations = featureDestinations,
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
            getStartScreenPreferenceDestinationUseCase = getStartScreenPreferenceDestinationUseCase,
            monitorThemeModeUseCase = monitorThemeModeUseCase,
            monitorConnectivityUseCase = monitorConnectivityUseCase,
        )
    }

    private fun stubDefaultStartScreenPreference() {
        getStartScreenPreferenceDestinationUseCase.stub {
            on { invoke() }.thenReturn(
                flow {
                    emit(String::class)
                    awaitCancellation()
                }
            )
        }
    }

    private fun stubDefaultThemeMode() {
        monitorThemeModeUseCase.stub {
            on { invoke() }.thenReturn(
                flow {
                    emit(ThemeMode.System)
                    awaitCancellation()
                }
            )
        }
    }

    private fun stubDefaultMainNavigationItems() = setOf(mock<MainNavItem> {
        on { destination }.thenReturn(SettingsHomeViewKtTest.TestDestination)
        on { preferredSlot }.thenReturn(PreferredSlot.Ordered(1))
        on { availableOffline }.thenReturn(true)
    })

    private fun stubConnectivity(connected: Boolean = true) {
        monitorConnectivityUseCase.stub {
            on { invoke() }.thenReturn(
                flow {
                    emit(connected)
                    awaitCancellation()
                }
            )
        }
    }
}