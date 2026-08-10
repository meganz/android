package mega.privacy.mobile.home.presentation.home

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.home.HomeWidgetConfiguration
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.usecase.MonitorHomeConfigurationTooltipShownUseCase
import mega.privacy.android.domain.usecase.SetHomeConfigurationTooltipShownUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.home.MonitorHomeWidgetConfigurationUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.offline.HasOfflineFilesUseCase
import mega.privacy.android.navigation.contract.home.HomeWidget
import mega.privacy.android.navigation.contract.home.HomeWidgetOrder
import mega.privacy.android.navigation.contract.home.HomeWidgetProvider
import mega.privacy.mobile.home.presentation.home.model.HomeUiState
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.atLeast
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions

@ExtendWith(CoroutineMainDispatcherExtension::class)
class HomeViewModelTest {
    private lateinit var underTest: HomeViewModel

    private val dynamicWidgetsProvider = mock<HomeWidgetProvider>()
    private val staticWidgetsProvider = mock<HomeWidgetProvider>()
    private val homeWidgetProviders = setOf(
        staticWidgetsProvider,
        dynamicWidgetsProvider,
    )
    private val monitorHomeWidgetConfigurationUseCase =
        mock<MonitorHomeWidgetConfigurationUseCase>()
    private val monitorConnectivityUseCase = mock<MonitorConnectivityUseCase>()
    private val hasOfflineFilesUseCase = mock<HasOfflineFilesUseCase>()
    private val getFeatureFlagValueUseCase = mock<GetFeatureFlagValueUseCase>()
    private val monitorHomeConfigurationTooltipShownUseCase =
        mock<MonitorHomeConfigurationTooltipShownUseCase>()
    private val setHomeConfigurationTooltipShownUseCase =
        mock<SetHomeConfigurationTooltipShownUseCase>()

    @BeforeEach
    fun setUp() {
        stubFeatureFlag()
        stubTooltipShown(shown = true)
        underTest = HomeViewModel(
            widgetProviders = homeWidgetProviders,
            monitorHomeWidgetConfigurationUseCase = monitorHomeWidgetConfigurationUseCase,
            monitorConnectivityUseCase = monitorConnectivityUseCase,
            hasOfflineFilesUseCase = hasOfflineFilesUseCase,
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
            monitorHomeConfigurationTooltipShownUseCase = monitorHomeConfigurationTooltipShownUseCase,
            setHomeConfigurationTooltipShownUseCase = setHomeConfigurationTooltipShownUseCase,
        )
    }

    @AfterEach
    fun tearDown() {
        reset(
            dynamicWidgetsProvider,
            staticWidgetsProvider,
            monitorHomeWidgetConfigurationUseCase,
            monitorConnectivityUseCase,
            hasOfflineFilesUseCase,
            getFeatureFlagValueUseCase,
            monitorHomeConfigurationTooltipShownUseCase,
            setHomeConfigurationTooltipShownUseCase,
        )
    }

    @Test
    fun `test that widgets are displayed if no configurations exist`() = runTest {
        stubConnectivity(connected = true)
        monitorHomeWidgetConfigurationUseCase.stub {
            on { invoke() } doReturn flow {
                emit(emptyList())
                awaitCancellation()
            }
        }

        val dynamicWidget = stubWidget(identifier = "dynamic1", defaultOrder = HomeWidgetOrder.Shortcuts)
        dynamicWidgetsProvider.stub {
            on { getWidgets() } doReturn setOf(
                dynamicWidget,
            )
        }

        val staticWidget = stubWidget(identifier = "static1", defaultOrder = HomeWidgetOrder.Banner)
        staticWidgetsProvider.stub {
            on { getWidgets() } doReturn setOf(
                staticWidget,
            )
        }

        underTest.state.test {
            val actual = awaitItem() as HomeUiState.Data
            assertThat(actual.widgets).hasSize(2)
        }
    }

    @Test
    fun `test that configuration overrides displaying widgets`() = runTest {
        stubConnectivity(connected = true)
        val staticIdentifier = "static1"
        val dynamicIdentifier = "dynamic1"
        monitorHomeWidgetConfigurationUseCase.stub {
            on { invoke() } doReturn flow {
                emit(
                    listOf(
                        HomeWidgetConfiguration(
                            widgetIdentifier = dynamicIdentifier,
                            widgetOrder = 1,
                            enabled = false,
                        ),
                        HomeWidgetConfiguration(
                            widgetIdentifier = staticIdentifier,
                            widgetOrder = 1,
                            enabled = true,
                        ),
                    )
                )
                awaitCancellation()
            }
        }

        val dynamicWidget = stubWidget(identifier = dynamicIdentifier, defaultOrder = HomeWidgetOrder.Shortcuts)
        dynamicWidgetsProvider.stub {
            on { getWidgets() } doReturn setOf(
                dynamicWidget,
            )
        }

        val staticWidget = stubWidget(identifier = staticIdentifier, defaultOrder = HomeWidgetOrder.Banner)
        staticWidgetsProvider.stub {
            on { getWidgets() } doReturn setOf(
                staticWidget,
            )
        }

        underTest.state.test {
            val actual = awaitItem() as HomeUiState.Data
            assertThat(actual.widgets).hasSize(1)
        }
    }

    @Test
    fun `test that configuration items override default order`() = runTest {
        stubConnectivity(connected = true)
        val staticIdentifier = "static1"
        val dynamicIdentifier = "dynamic1"
        monitorHomeWidgetConfigurationUseCase.stub {
            on { invoke() } doReturn flow {
                emit(
                    listOf(
                        HomeWidgetConfiguration(
                            widgetIdentifier = dynamicIdentifier,
                            widgetOrder = 0,
                            enabled = true,
                        ),
                        HomeWidgetConfiguration(
                            widgetIdentifier = staticIdentifier,
                            widgetOrder = 1,
                            enabled = true,
                        ),
                    )
                )
                awaitCancellation()
            }
        }

        val dynamicWidget = stubWidget(identifier = dynamicIdentifier, defaultOrder = HomeWidgetOrder.Shortcuts)
        dynamicWidgetsProvider.stub {
            on { getWidgets() } doReturn setOf(
                dynamicWidget,
            )
        }

        val staticWidget = stubWidget(identifier = staticIdentifier, defaultOrder = HomeWidgetOrder.Banner)
        staticWidgetsProvider.stub {
            on { getWidgets() } doReturn setOf(
                staticWidget,
            )
        }

        underTest.state.test {
            val actual = awaitItem() as HomeUiState.Data
            assertThat(actual.widgets).hasSize(2)
            assertThat(actual.widgets[0].identifier).isEqualTo(dynamicIdentifier)
            assertThat(actual.widgets[1].identifier).isEqualTo(staticIdentifier)
        }
    }

    @Test
    fun `test that offline state is returned when disconnected and has offline files`() = runTest {
        stubConnectivity(connected = false)
        stubHasOfflineFiles(hasOfflineFiles = true)
        stubWidgetProviders()
        monitorHomeWidgetConfigurationUseCase.stub {
            on { invoke() } doReturn flow {
                emit(emptyList())
                awaitCancellation()
            }
        }

        underTest.state.test {
            // Find the Offline state (may skip Loading if flows emit immediately)
            val actual = awaitItem().let { state ->
                if (state is HomeUiState.Offline) {
                    state
                } else {
                    // If first item was Loading, await the next one
                    awaitItem() as HomeUiState.Offline
                }
            }
            assertThat(actual.hasOfflineFiles).isTrue()
        }
    }

    @Test
    fun `test that offline state is returned when disconnected and has no offline files`() =
        runTest {
            stubConnectivity(connected = false)
            stubHasOfflineFiles(hasOfflineFiles = false)
            stubWidgetProviders()
            monitorHomeWidgetConfigurationUseCase.stub {
                on { invoke() } doReturn flow {
                    emit(emptyList())
                    awaitCancellation()
                }
            }

            underTest.state.test {
                // Find the Offline state (may skip Loading if flows emit immediately)
                val actual = awaitItem().let { state ->
                    if (state is HomeUiState.Offline) {
                        state
                    } else {
                        // If first item was Loading, await the next one
                        awaitItem() as HomeUiState.Offline
                    }
                }
                assertThat(actual.hasOfflineFiles).isFalse()
            }
        }

    @Test
    fun `test that offline state defaults to false when hasOfflineFilesUseCase throws exception`() =
        runTest {
            stubConnectivity(connected = false)
            hasOfflineFilesUseCase.stub {
                on { invoke() }.thenThrow(RuntimeException("Test exception"))
            }
            stubWidgetProviders()
            monitorHomeWidgetConfigurationUseCase.stub {
                on { invoke() } doReturn flow {
                    emit(emptyList())
                    awaitCancellation()
                }
            }

            underTest.state.test {
                // Find the Offline state (may skip Loading if flows emit immediately)
                val actual = awaitItem().let { state ->
                    if (state is HomeUiState.Offline) {
                        state
                    } else {
                        // If first item was Loading, await the next one
                        awaitItem() as HomeUiState.Offline
                    }
                }
                assertThat(actual.hasOfflineFiles).isFalse()
            }
        }

    private fun stubConnectivity(connected: Boolean = true) {
        monitorConnectivityUseCase.stub {
            on { invoke() } doReturn flow {
                emit(connected)
                awaitCancellation()
            }
        }
    }

    private fun stubHasOfflineFiles(hasOfflineFiles: Boolean) {
        hasOfflineFilesUseCase.stub {
            on { invoke() } doReturn hasOfflineFiles
        }
    }

    private fun stubWidgetProviders() {
        dynamicWidgetsProvider.stub {
            on { getWidgets() } doReturn emptySet()
        }
        staticWidgetsProvider.stub {
            on { getWidgets() } doReturn emptySet()
        }
    }

    private fun stubWidget(
        identifier: String,
        defaultOrder: HomeWidgetOrder,
    ): HomeWidget {
        return mock<HomeWidget> {
            on { this.identifier } doReturn identifier
            on { this.defaultOrder } doReturn defaultOrder
        }
    }

    private fun stubFeatureFlag(enabled: Boolean = false) {
        getFeatureFlagValueUseCase.stub {
            on { invoke(ApiFeatures.HomeConfiguration) } doReturn enabled
        }
    }

    private fun stubTooltipShown(shown: Boolean) {
        monitorHomeConfigurationTooltipShownUseCase.stub {
            on { invoke() } doReturn flowOf(shown)
        }
    }

    @Test
    fun `test that showHomeConfigurationTooltip is true when feature flag enabled and tooltip not shown before`() =
        runTest {
            stubConnectivity(connected = true)
            stubFeatureFlag(enabled = true)
            stubTooltipShown(shown = false)
            stubWidgetProviders()
            monitorHomeWidgetConfigurationUseCase.stub {
                on { invoke() } doReturn flow {
                    emit(emptyList())
                    awaitCancellation()
                }
            }
            underTest = HomeViewModel(
                widgetProviders = homeWidgetProviders,
                monitorHomeWidgetConfigurationUseCase = monitorHomeWidgetConfigurationUseCase,
                monitorConnectivityUseCase = monitorConnectivityUseCase,
                hasOfflineFilesUseCase = hasOfflineFilesUseCase,
                getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
                monitorHomeConfigurationTooltipShownUseCase = monitorHomeConfigurationTooltipShownUseCase,
                setHomeConfigurationTooltipShownUseCase = setHomeConfigurationTooltipShownUseCase,
            )

            underTest.state.test {
                val actual = awaitItem() as HomeUiState.Data
                assertThat(actual.showHomeConfigurationTooltip).isTrue()
            }
        }

    @Test
    fun `test that showHomeConfigurationTooltip is false when feature flag enabled but tooltip already shown`() =
        runTest {
            stubConnectivity(connected = true)
            stubFeatureFlag(enabled = true)
            stubTooltipShown(shown = true)
            stubWidgetProviders()
            monitorHomeWidgetConfigurationUseCase.stub {
                on { invoke() } doReturn flow {
                    emit(emptyList())
                    awaitCancellation()
                }
            }
            underTest = HomeViewModel(
                widgetProviders = homeWidgetProviders,
                monitorHomeWidgetConfigurationUseCase = monitorHomeWidgetConfigurationUseCase,
                monitorConnectivityUseCase = monitorConnectivityUseCase,
                hasOfflineFilesUseCase = hasOfflineFilesUseCase,
                getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
                monitorHomeConfigurationTooltipShownUseCase = monitorHomeConfigurationTooltipShownUseCase,
                setHomeConfigurationTooltipShownUseCase = setHomeConfigurationTooltipShownUseCase,
            )

            underTest.state.test {
                val actual = awaitItem() as HomeUiState.Data
                assertThat(actual.showHomeConfigurationTooltip).isFalse()
            }
        }

    @Test
    fun `test that showHomeConfigurationTooltip is false when feature flag disabled even if tooltip not shown before`() =
        runTest {
            stubConnectivity(connected = true)
            stubFeatureFlag(enabled = false)
            stubTooltipShown(shown = false)
            stubWidgetProviders()
            monitorHomeWidgetConfigurationUseCase.stub {
                on { invoke() } doReturn flow {
                    emit(emptyList())
                    awaitCancellation()
                }
            }
            underTest = HomeViewModel(
                widgetProviders = homeWidgetProviders,
                monitorHomeWidgetConfigurationUseCase = monitorHomeWidgetConfigurationUseCase,
                monitorConnectivityUseCase = monitorConnectivityUseCase,
                hasOfflineFilesUseCase = hasOfflineFilesUseCase,
                getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
                monitorHomeConfigurationTooltipShownUseCase = monitorHomeConfigurationTooltipShownUseCase,
                setHomeConfigurationTooltipShownUseCase = setHomeConfigurationTooltipShownUseCase,
            )

            underTest.state.test {
                val actual = awaitItem() as HomeUiState.Data
                assertThat(actual.showHomeConfigurationTooltip).isFalse()
            }
        }

    @Test
    fun `test that onHomeConfigurationTooltipDismissed hides the tooltip and calls SetHomeConfigurationTooltipShownUseCase`() =
        runTest {
            stubConnectivity(connected = true)
            stubFeatureFlag(enabled = true)
            // Simulate DataStore reactivity: the monitor flow re-emits whenever the
            // "shown" preference is written. The fake set use-case flips the same flow,
            // so dismissing through the ViewModel naturally drives the state to hidden.
            val tooltipShownFlow = MutableStateFlow(false)
            monitorHomeConfigurationTooltipShownUseCase.stub {
                on { invoke() } doReturn tooltipShownFlow
            }
            setHomeConfigurationTooltipShownUseCase.stub {
                on { invoke() } doAnswer {
                    tooltipShownFlow.value = true
                    Unit
                }
            }
            stubWidgetProviders()
            monitorHomeWidgetConfigurationUseCase.stub {
                on { invoke() } doReturn flow {
                    emit(emptyList())
                    awaitCancellation()
                }
            }
            underTest = HomeViewModel(
                widgetProviders = homeWidgetProviders,
                monitorHomeWidgetConfigurationUseCase = monitorHomeWidgetConfigurationUseCase,
                monitorConnectivityUseCase = monitorConnectivityUseCase,
                hasOfflineFilesUseCase = hasOfflineFilesUseCase,
                getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
                monitorHomeConfigurationTooltipShownUseCase = monitorHomeConfigurationTooltipShownUseCase,
                setHomeConfigurationTooltipShownUseCase = setHomeConfigurationTooltipShownUseCase,
            )

            underTest.state.test {
                assertThat((awaitItem() as HomeUiState.Data).showHomeConfigurationTooltip).isTrue()
                underTest.onHomeConfigurationTooltipDismissed()
                assertThat((awaitItem() as HomeUiState.Data).showHomeConfigurationTooltip).isFalse()
            }
            verify(setHomeConfigurationTooltipShownUseCase).invoke()
        }

    @Test
    fun `test that init does not call SetHomeConfigurationTooltipShownUseCase`() = runTest {
        stubConnectivity(connected = true)
        stubFeatureFlag(enabled = true)
        stubTooltipShown(shown = false)
        stubWidgetProviders()
        monitorHomeWidgetConfigurationUseCase.stub {
            on { invoke() } doReturn flow {
                emit(emptyList())
                awaitCancellation()
            }
        }
        underTest = HomeViewModel(
            widgetProviders = homeWidgetProviders,
            monitorHomeWidgetConfigurationUseCase = monitorHomeWidgetConfigurationUseCase,
            monitorConnectivityUseCase = monitorConnectivityUseCase,
            hasOfflineFilesUseCase = hasOfflineFilesUseCase,
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
            monitorHomeConfigurationTooltipShownUseCase = monitorHomeConfigurationTooltipShownUseCase,
            setHomeConfigurationTooltipShownUseCase = setHomeConfigurationTooltipShownUseCase,
        )

        verifyNoInteractions(setHomeConfigurationTooltipShownUseCase)
    }

    @Test
    fun `test that isHomeCustomizationEnabled is re-evaluated when connectivity is restored`() =
        runTest {
            val connectivityFlow = MutableStateFlow(true)
            monitorConnectivityUseCase.stub {
                on { invoke() } doReturn connectivityFlow
            }
            var currentFlagValue = false
            getFeatureFlagValueUseCase.stub {
                on { invoke(ApiFeatures.HomeConfiguration) } doAnswer { currentFlagValue }
            }
            stubHasOfflineFiles(hasOfflineFiles = false)
            stubTooltipShown(shown = true)
            stubWidgetProviders()
            monitorHomeWidgetConfigurationUseCase.stub {
                on { invoke() } doReturn flow {
                    emit(emptyList())
                    awaitCancellation()
                }
            }
            underTest = HomeViewModel(
                widgetProviders = homeWidgetProviders,
                monitorHomeWidgetConfigurationUseCase = monitorHomeWidgetConfigurationUseCase,
                monitorConnectivityUseCase = monitorConnectivityUseCase,
                hasOfflineFilesUseCase = hasOfflineFilesUseCase,
                getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
                monitorHomeConfigurationTooltipShownUseCase = monitorHomeConfigurationTooltipShownUseCase,
                setHomeConfigurationTooltipShownUseCase = setHomeConfigurationTooltipShownUseCase,
            )

            underTest.state.test {
                // Initial state: online with flag = false
                var item: HomeUiState = awaitItem()
                while (item is HomeUiState.Loading) {
                    item = awaitItem()
                }
                assertThat(item).isInstanceOf(HomeUiState.Data::class.java)
                assertThat((item as HomeUiState.Data).isHomeCustomizationEnabled).isFalse()

                // Drop connectivity
                currentFlagValue = true
                connectivityFlow.update { false }
                val offline = awaitItem()
                assertThat(offline).isInstanceOf(HomeUiState.Offline::class.java)
                assertThat((offline as HomeUiState.Offline).hasOfflineFiles).isFalse()

                // Restore connectivity — skip one stale Data if present, then assert re-resolved flag
                connectivityFlow.update { true }
                var afterReconnect = awaitItem()
                assertThat(afterReconnect).isInstanceOf(HomeUiState.Data::class.java)
                if (!(afterReconnect as HomeUiState.Data).isHomeCustomizationEnabled) {
                    afterReconnect = awaitItem()
                    assertThat(afterReconnect).isInstanceOf(HomeUiState.Data::class.java)
                }
                assertThat((afterReconnect as HomeUiState.Data).isHomeCustomizationEnabled).isTrue()

                cancelAndIgnoreRemainingEvents()
            }

            verify(getFeatureFlagValueUseCase, atLeast(2)).invoke(ApiFeatures.HomeConfiguration)
        }
}