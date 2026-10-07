package mega.privacy.mobile.home.presentation.home

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.home.HomeWidgetConfiguration
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.usecase.MonitorHomeConfigurationTooltipShownUseCase
import mega.privacy.android.domain.usecase.SetHomeConfigurationTooltipShownUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.home.MonitorHomeWidgetConfigurationUseCase
import mega.privacy.android.domain.usecase.network.MonitorOfflineModeUseCase
import mega.privacy.android.domain.usecase.offline.HasOfflineFilesUseCase
import mega.privacy.android.navigation.contract.home.HomeWidget
import mega.privacy.android.navigation.contract.home.HomeWidgetOrder
import mega.privacy.android.navigation.contract.home.HomeWidgetProvider
import mega.privacy.mobile.home.presentation.home.model.HomeUiState
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
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
    private val monitorOfflineModeUseCase = mock<MonitorOfflineModeUseCase>()
    private val hasOfflineFilesUseCase = mock<HasOfflineFilesUseCase>()
    private val getFeatureFlagValueUseCase = mock<GetFeatureFlagValueUseCase>()
    private val monitorHomeConfigurationTooltipShownUseCase =
        mock<MonitorHomeConfigurationTooltipShownUseCase>()
    private val setHomeConfigurationTooltipShownUseCase =
        mock<SetHomeConfigurationTooltipShownUseCase>()

    @BeforeEach
    fun setUp() {
        stubTooltipShown(shown = true)
        stubOfflineMode(isOfflineMode = false)
        stubOfflineModeFlag(enabled = false)
        underTest = HomeViewModel(
            widgetProviders = homeWidgetProviders,
            monitorHomeWidgetConfigurationUseCase = monitorHomeWidgetConfigurationUseCase,
            monitorOfflineModeUseCase = monitorOfflineModeUseCase,
            hasOfflineFilesUseCase = hasOfflineFilesUseCase,
            monitorHomeConfigurationTooltipShownUseCase = monitorHomeConfigurationTooltipShownUseCase,
            setHomeConfigurationTooltipShownUseCase = setHomeConfigurationTooltipShownUseCase,
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
        )
    }

    @AfterEach
    fun tearDown() {
        reset(
            dynamicWidgetsProvider,
            staticWidgetsProvider,
            monitorHomeWidgetConfigurationUseCase,
            monitorOfflineModeUseCase,
            monitorHomeConfigurationTooltipShownUseCase,
            setHomeConfigurationTooltipShownUseCase,
            hasOfflineFilesUseCase,
            getFeatureFlagValueUseCase,
        )
    }

    @Test
    fun `test that widgets are displayed if no configurations exist`() = runTest {
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

    private fun stubOfflineMode(isOfflineMode: Boolean) {
        monitorOfflineModeUseCase.stub {
            on { invoke() } doReturn flow {
                emit(isOfflineMode)
                awaitCancellation()
            }
        }
    }

    private fun stubOfflineModeFlag(enabled: Boolean) {
        getFeatureFlagValueUseCase.stub {
            onBlocking { invoke(ApiFeatures.OfflineModePhase1) } doReturn enabled
        }
    }

    private fun stubHasOfflineFiles(hasOfflineFiles: Boolean) {
        hasOfflineFilesUseCase.stub {
            onBlocking { invoke() } doReturn hasOfflineFiles
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

    private fun stubTooltipShown(shown: Boolean) {
        monitorHomeConfigurationTooltipShownUseCase.stub {
            on { invoke() } doReturn flowOf(shown)
        }
    }

    @Test
    fun `test that showHomeConfigurationTooltip is true when tooltip not shown before`() =
        runTest {
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
                monitorOfflineModeUseCase = monitorOfflineModeUseCase,
                hasOfflineFilesUseCase = hasOfflineFilesUseCase,
                monitorHomeConfigurationTooltipShownUseCase = monitorHomeConfigurationTooltipShownUseCase,
                setHomeConfigurationTooltipShownUseCase = setHomeConfigurationTooltipShownUseCase,
                getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
            )

            underTest.state.test {
                val actual = awaitItem() as HomeUiState.Data
                assertThat(actual.showHomeConfigurationTooltip).isTrue()
            }
        }

    @Test
    fun `test that showHomeConfigurationTooltip is false when tooltip already shown`() =
        runTest {
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
                monitorOfflineModeUseCase = monitorOfflineModeUseCase,
                hasOfflineFilesUseCase = hasOfflineFilesUseCase,
                monitorHomeConfigurationTooltipShownUseCase = monitorHomeConfigurationTooltipShownUseCase,
                setHomeConfigurationTooltipShownUseCase = setHomeConfigurationTooltipShownUseCase,
                getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
            )

            underTest.state.test {
                val actual = awaitItem() as HomeUiState.Data
                assertThat(actual.showHomeConfigurationTooltip).isFalse()
            }
        }

    @Test
    fun `test that onHomeConfigurationTooltipDismissed hides the tooltip and calls SetHomeConfigurationTooltipShownUseCase`() =
        runTest {
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
                monitorOfflineModeUseCase = monitorOfflineModeUseCase,
                hasOfflineFilesUseCase = hasOfflineFilesUseCase,
                monitorHomeConfigurationTooltipShownUseCase = monitorHomeConfigurationTooltipShownUseCase,
                setHomeConfigurationTooltipShownUseCase = setHomeConfigurationTooltipShownUseCase,
                getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
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
            monitorOfflineModeUseCase = monitorOfflineModeUseCase,
            hasOfflineFilesUseCase = hasOfflineFilesUseCase,
            monitorHomeConfigurationTooltipShownUseCase = monitorHomeConfigurationTooltipShownUseCase,
            setHomeConfigurationTooltipShownUseCase = setHomeConfigurationTooltipShownUseCase,
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
        )

        verifyNoInteractions(setHomeConfigurationTooltipShownUseCase)
    }

    @Test
    fun `test that state is Data with isOfflineMode true when offline and offline mode flag is enabled`() = runTest {
        stubOfflineMode(isOfflineMode = true)
        stubOfflineModeFlag(enabled = true)
        stubWidgetProviders()
        monitorHomeWidgetConfigurationUseCase.stub {
            on { invoke() } doReturn flow {
                emit(emptyList())
                awaitCancellation()
            }
        }

        underTest.state.test {
            val actual = awaitItem() as HomeUiState.Data
            assertThat(actual.isOfflineMode).isTrue()
        }
    }

    @Test
    fun `test that showHomeConfigurationTooltip is false when offline and offline mode flag is enabled`() = runTest {
        stubOfflineMode(isOfflineMode = true)
        stubOfflineModeFlag(enabled = true)
        stubTooltipShown(shown = false)
        stubWidgetProviders()
        monitorHomeWidgetConfigurationUseCase.stub {
            on { invoke() } doReturn flow {
                emit(emptyList())
                awaitCancellation()
            }
        }

        underTest.state.test {
            val actual = awaitItem() as HomeUiState.Data
            assertThat(actual.showHomeConfigurationTooltip).isFalse()
        }
    }

    @Test
    fun `test that isOfflineMode updates when offline mode changes and offline mode flag is enabled`() = runTest {
        stubOfflineModeFlag(enabled = true)
        val offlineModeFlow = MutableStateFlow(false)
        monitorOfflineModeUseCase.stub {
            on { invoke() } doReturn offlineModeFlow
        }
        stubWidgetProviders()
        monitorHomeWidgetConfigurationUseCase.stub {
            on { invoke() } doReturn flow {
                emit(emptyList())
                awaitCancellation()
            }
        }

        underTest.state.test {
            assertThat((awaitItem() as HomeUiState.Data).isOfflineMode).isFalse()

            offlineModeFlow.value = true
            assertThat((awaitItem() as HomeUiState.Data).isOfflineMode).isTrue()

            offlineModeFlow.value = false
            assertThat((awaitItem() as HomeUiState.Data).isOfflineMode).isFalse()
        }
    }

    @Test
    fun `test that offline state is returned when offline and offline mode flag is disabled`() = runTest {
        stubOfflineMode(isOfflineMode = true)
        stubHasOfflineFiles(hasOfflineFiles = true)
        stubWidgetProviders()
        monitorHomeWidgetConfigurationUseCase.stub {
            on { invoke() } doReturn flow {
                emit(emptyList())
                awaitCancellation()
            }
        }

        underTest.state.test {
            var item: HomeUiState = awaitItem()
            while (item is HomeUiState.Loading) {
                item = awaitItem()
            }
            assertThat(item).isInstanceOf(HomeUiState.Offline::class.java)
            assertThat((item as HomeUiState.Offline).hasOfflineFiles).isTrue()
        }
    }

    @Test
    fun `test that offline state defaults hasOfflineFiles to false when hasOfflineFilesUseCase throws`() =
        runTest {
            stubOfflineMode(isOfflineMode = true)
            hasOfflineFilesUseCase.stub {
                onBlocking { invoke() }.thenThrow(RuntimeException("Test exception"))
            }
            stubWidgetProviders()
            monitorHomeWidgetConfigurationUseCase.stub {
                on { invoke() } doReturn flow {
                    emit(emptyList())
                    awaitCancellation()
                }
            }

            underTest.state.test {
                var item: HomeUiState = awaitItem()
                while (item is HomeUiState.Loading) {
                    item = awaitItem()
                }
                assertThat((item as HomeUiState.Offline).hasOfflineFiles).isFalse()
            }
        }

    @Test
    fun `test that state returns to Data when back online and offline mode flag is disabled`() =
        runTest {
            val offlineModeFlow = MutableStateFlow(false)
            monitorOfflineModeUseCase.stub {
                on { invoke() } doReturn offlineModeFlow
            }
            stubHasOfflineFiles(hasOfflineFiles = false)
            stubWidgetProviders()
            monitorHomeWidgetConfigurationUseCase.stub {
                on { invoke() } doReturn flow {
                    emit(emptyList())
                    awaitCancellation()
                }
            }

            underTest.state.test {
                var item: HomeUiState = awaitItem()
                while (item is HomeUiState.Loading) {
                    item = awaitItem()
                }
                assertThat(item).isInstanceOf(HomeUiState.Data::class.java)

                offlineModeFlow.value = true
                assertThat(awaitItem()).isInstanceOf(HomeUiState.Offline::class.java)

                offlineModeFlow.value = false
                assertThat(awaitItem()).isInstanceOf(HomeUiState.Data::class.java)
            }
        }
}
