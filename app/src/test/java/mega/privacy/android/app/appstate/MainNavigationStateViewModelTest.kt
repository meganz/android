package mega.privacy.android.app.appstate

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.navigation3.runtime.NavKey
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.appstate.content.mapper.ScreenPreferenceDestinationMapper
import mega.privacy.android.app.appstate.content.navigation.MainNavigationStateViewModel
import mega.privacy.android.app.appstate.content.navigation.NavigationResultManager
import mega.privacy.android.app.appstate.content.navigation.model.MainNavState
import mega.privacy.android.app.presentation.settings.compose.home.view.SettingsHomeViewKtTest
import mega.privacy.android.domain.entity.Feature
import mega.privacy.android.domain.entity.navigation.Flagged
import mega.privacy.android.domain.entity.preference.StartScreenDestinationPreference
import mega.privacy.android.domain.usecase.featureflag.GetEnabledFlaggedItemsUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.preference.MonitorStartScreenPreferenceDestinationUseCase
import mega.privacy.android.navigation.contract.MainNavItem
import mega.privacy.android.navigation.contract.PreferredSlot
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.KStubbing
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MainNavigationStateViewModelTest {
    private lateinit var underTest: MainNavigationStateViewModel

    private val monitorConnectivityUseCase = mock<MonitorConnectivityUseCase>()
    private val getEnabledFlaggedItemsUseCase = mock<GetEnabledFlaggedItemsUseCase>()
    private val monitorStartScreenPreferenceDestinationUseCase =
        mock<MonitorStartScreenPreferenceDestinationUseCase>()
    private val screenPreferenceDestinationMapper = mock<ScreenPreferenceDestinationMapper>()
    private val defaultStartScreen = mock<NavKey>()
    private val navigationResultManager = mock<NavigationResultManager>()

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
            getEnabledFlaggedItemsUseCase,
            monitorConnectivityUseCase,
            monitorStartScreenPreferenceDestinationUseCase,
            screenPreferenceDestinationMapper,
            navigationResultManager
        )
    }

    @Test
    fun `test that initial state is loading`() = runTest {
        val mainDestinations = emptySet<@JvmSuppressWildcards MainNavItem>()
        stubAllEnabledFlaggedItems()
        initUnderTest(mainDestinations)
        assertThat(underTest.state.value).isEqualTo(MainNavState.Loading)
    }

    @Test
    fun `test that main destinations are added`() = runTest {
        stubConnectivity()
        stubAllEnabledFlaggedItems()
        val mainNavItem = mock<MainNavItem> {
            on { destination }.thenReturn(SettingsHomeViewKtTest.TestDestination)
            on { preferredSlot }.thenReturn(PreferredSlot.Ordered(1))
            on { availableOffline }.thenReturn(true)
            on { label }.thenReturn(android.R.string.ok)
            on { analyticsEventIdentifier }.thenReturn(mock())
            on { icon }.thenReturn(Icons.Default.Home)
        }

        val expected = setOf(mainNavItem)

        stubEmptyStartScreenPreference()

        initUnderTest(expected)

        underTest.state
            .filterIsInstance<MainNavState.Data>()
            .test {
                assertThat(awaitItem().mainNavItems.map { it.label }).containsExactlyElementsIn(
                    expected.map { it.label }
                )
                cancelAndIgnoreRemainingEvents()
            }
    }

    @Suppress("UNCHECKED_CAST")
    @Test
    fun `test that main nav items with disabled feature flags are not returned`() = runTest {
        stubConnectivity()
        stubEmptyStartScreenPreference()
        val expected = mock<MainNavItem> {
            on { destination }.thenReturn(SettingsHomeViewKtTest.TestDestination)
            on { preferredSlot }.thenReturn(PreferredSlot.Ordered(1))
            on { availableOffline }.thenReturn(true)
            on { label }.thenReturn(android.R.string.ok)
            on { analyticsEventIdentifier }.thenReturn(mock())
            on { icon }.thenReturn(Icons.Default.Home)
        }
        val disabledFeature = mock<Feature>()
        val notExpected =
            mock<Flagged>(extraInterfaces = arrayOf(MainNavItem::class))
        notExpected.stub {
            on { feature }.thenReturn(disabledFeature)
            with(this as KStubbing<MainNavItem>) {
                // Using KStubbing to allow for more readable stubbing
                on { destination }.thenReturn(SettingsHomeViewKtTest.TestDestination)
                on { preferredSlot }.thenReturn(PreferredSlot.Ordered(2))
                on { availableOffline }.thenReturn(true)
                on { label }.thenReturn(android.R.string.cancel)
                on { analyticsEventIdentifier }.thenReturn(mock())
                on { icon }.thenReturn(Icons.Default.Settings)
            }
        }
        val mainDestinations = setOf(expected, notExpected as MainNavItem)

        getEnabledFlaggedItemsUseCase.stub {
            onBlocking { invoke(mainDestinations) }.thenReturn(flow { emit(setOf(expected)) })
        }

        initUnderTest(mainDestinations)

        underTest.state
            .filterIsInstance<MainNavState.Data>()
            .test {
                assertThat(awaitItem().mainNavItems.map { it.label }).containsExactly(expected.label)
                cancelAndIgnoreRemainingEvents()
            }
    }

    @Suppress("UNCHECKED_CAST")
    @Test
    fun `test that main nav items with enabled feature flags are returned`() = runTest {
        stubConnectivity()
        stubEmptyStartScreenPreference()
        val expected = mock<MainNavItem> {
            on { destination }.thenReturn(SettingsHomeViewKtTest.TestDestination)
            on { preferredSlot }.thenReturn(PreferredSlot.Ordered(1))
            on { availableOffline }.thenReturn(true)
            on { label }.thenReturn(android.R.string.ok)
            on { analyticsEventIdentifier }.thenReturn(mock())
            on { icon }.thenReturn(Icons.Default.Home)
        }
        val enabledFeature = mock<Feature>()
        val alsoExpected =
            mock<Flagged>(extraInterfaces = arrayOf(MainNavItem::class))
        alsoExpected.stub {
            on { feature }.thenReturn(enabledFeature)
            with(this as KStubbing<MainNavItem>) {
                // Using KStubbing to allow for more readable stubbing
                on { destination }.thenReturn(SettingsHomeViewKtTest.TestDestination)
                on { preferredSlot }.thenReturn(PreferredSlot.Ordered(2))
                on { availableOffline }.thenReturn(true)
                on { label }.thenReturn(android.R.string.cancel)
                on { analyticsEventIdentifier }.thenReturn(mock())
                on { icon }.thenReturn(Icons.Default.Settings)
            }
        }
        val mainDestinations = setOf(expected, alsoExpected as MainNavItem)
        getEnabledFlaggedItemsUseCase.stub {
            onBlocking { invoke(mainDestinations) }.thenReturn(flow {
                emit(
                    setOf(
                        expected,
                        alsoExpected
                    )
                )
            })
        }
        initUnderTest(mainDestinations)
        underTest.state
            .filterIsInstance<MainNavState.Data>()
            .test {
                assertThat(awaitItem().mainNavItems.map { it.label }).containsExactly(
                    expected.label,
                    alsoExpected.label
                )
                cancelAndIgnoreRemainingEvents()
            }
    }

    @Test
    fun `test that items are enabled if available offline and connectivity is offline`() = runTest {
        stubEmptyStartScreenPreference()
        stubAllEnabledFlaggedItems()
        val mainNavItem = mock<MainNavItem> {
            on { destination }.thenReturn(SettingsHomeViewKtTest.TestDestination)
            on { preferredSlot }.thenReturn(PreferredSlot.Ordered(1))
            on { availableOffline }.thenReturn(true)
            on { analyticsEventIdentifier }.thenReturn(mock())
            on { label }.thenReturn(android.R.string.ok)
            on { icon }.thenReturn(Icons.Default.Home)
        }
        val expected = setOf(mainNavItem)

        stubConnectivity(connected = false)

        initUnderTest(expected)

        underTest.state
            .filterIsInstance<MainNavState.Data>()
            .test {
                assertThat(awaitItem().mainNavItems.all { it.isEnabled }).isTrue()
                cancelAndIgnoreRemainingEvents()
            }
    }

    @Test
    fun `test that items are not enabled if not available offline and connectivity is offline`() =
        runTest {
            stubEmptyStartScreenPreference()
            stubAllEnabledFlaggedItems()
            val mainNavItem = mock<MainNavItem> {
                on { destination }.thenReturn(SettingsHomeViewKtTest.TestDestination)
                on { preferredSlot }.thenReturn(PreferredSlot.Ordered(1))
                on { availableOffline }.thenReturn(false)
                on { analyticsEventIdentifier }.thenReturn(mock())
                on { label }.thenReturn(android.R.string.ok)
                on { icon }.thenReturn(Icons.Default.Home)
            }
            val expected = setOf(mainNavItem)

            stubConnectivity(connected = false)

            initUnderTest(expected)

            underTest.state
                .filterIsInstance<MainNavState.Data>()
                .test {
                    assertThat(awaitItem().mainNavItems.all { it.isEnabled }).isFalse()
                    cancelAndIgnoreRemainingEvents()
                }
        }

    @Test
    fun `test that exception in connectivity use case returns default connected state`() = runTest {
        stubEmptyStartScreenPreference()
        stubAllEnabledFlaggedItems()
        val mainNavItem = mock<MainNavItem> {
            on { destination }.thenReturn(SettingsHomeViewKtTest.TestDestination)
            on { preferredSlot }.thenReturn(PreferredSlot.Ordered(1))
            on { availableOffline }.thenReturn(false)
            on { analyticsEventIdentifier }.thenReturn(mock())
            on { label }.thenReturn(android.R.string.ok)
            on { icon }.thenReturn(Icons.Default.Home)
        }
        val expected = setOf(mainNavItem)

        monitorConnectivityUseCase.stub {
            on { invoke() }.thenReturn(
                flow {
                    throw Exception("Connectivity error")
                }
            )
        }

        initUnderTest(expected)

        underTest.state
            .filterIsInstance<MainNavState.Data>()
            .test {
                assertThat(awaitItem().mainNavItems.all { it.isEnabled }).isTrue()
                cancelAndIgnoreRemainingEvents()
            }
    }

    @Test
    fun `test that start screen destination is set if returned`() = runTest {
        stubConnectivity()
        stubAllEnabledFlaggedItems()
        val expected = SettingsHomeViewKtTest.TestDestination
        stubStartScreenPreference(mock<StartScreenDestinationPreference>())
        screenPreferenceDestinationMapper.stub {
            on { invoke(any<StartScreenDestinationPreference>()) }.thenReturn(
                expected
            )
        }
        initUnderTest(
            mainDestinations = stubDefaultMainNavigationItems()
        )

        underTest.state
            .filterIsInstance<MainNavState.Data>()
            .test {
                val item = awaitItem()
                assertThat(item.initialDestination).isEqualTo(expected)
                cancelAndIgnoreRemainingEvents()
            }
    }

    @Test
    fun `test that default start screen destination is set if no preference is returned`() =
        runTest {
            stubConnectivity()
            stubAllEnabledFlaggedItems()
            val expected = defaultStartScreen
            stubStartScreenPreference(mock<StartScreenDestinationPreference>())
            screenPreferenceDestinationMapper.stub {
                on { invoke(any<StartScreenDestinationPreference>()) }.thenReturn(
                    null
                )
            }
            initUnderTest(
                mainDestinations = stubDefaultMainNavigationItems()
            )

            underTest.state
                .filterIsInstance<MainNavState.Data>()
                .test {
                    val item = awaitItem()
                    assertThat(item.initialDestination).isEqualTo(expected)
                    cancelAndIgnoreRemainingEvents()
                }
        }


    private fun initUnderTest(
        mainDestinations: Set<MainNavItem>,
    ) {
        underTest = MainNavigationStateViewModel(
            mainDestinations = mainDestinations,
            getEnabledFlaggedItemsUseCase = getEnabledFlaggedItemsUseCase,
            monitorConnectivityUseCase = monitorConnectivityUseCase,
            monitorStartScreenPreferenceDestinationUseCase = monitorStartScreenPreferenceDestinationUseCase,
            screenPreferenceDestinationMapper = screenPreferenceDestinationMapper,
            defaultStartScreen = defaultStartScreen,
            navigationResultManager = navigationResultManager
        )
    }

    private fun stubDefaultMainNavigationItems() = setOf(mock<MainNavItem> {
        on { destination }.thenReturn(SettingsHomeViewKtTest.TestDestination)
        on { preferredSlot }.thenReturn(PreferredSlot.Ordered(1))
        on { availableOffline }.thenReturn(true)
        on { analyticsEventIdentifier }.thenReturn(mock())
        on { label }.thenReturn(android.R.string.ok)
        on { icon }.thenReturn(Icons.Default.Home)
    })

    private fun stubEmptyStartScreenPreference() {
        monitorStartScreenPreferenceDestinationUseCase.stub {
            onBlocking { invoke() }.thenReturn(
                flow {
                    emit(null)
                    awaitCancellation()
                }
            )
        }
    }

    private fun stubStartScreenPreference(destination: StartScreenDestinationPreference) {
        monitorStartScreenPreferenceDestinationUseCase.stub {
            onBlocking { invoke() }.thenReturn(
                flow {
                    emit(destination)
                    awaitCancellation()
                }
            )
        }
    }

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

    private fun stubAllEnabledFlaggedItems() {
        getEnabledFlaggedItemsUseCase.stub {
            onBlocking { invoke(any<Set<Any>>()) }.thenAnswer { flow { emit(it.arguments.first()) } }
        }
    }
}