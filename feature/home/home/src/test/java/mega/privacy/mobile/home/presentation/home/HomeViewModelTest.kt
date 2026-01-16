package mega.privacy.mobile.home.presentation.home

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.home.HomeWidgetConfiguration
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.home.MonitorHomeWidgetConfigurationUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.offline.HasOfflineFilesUseCase
import mega.privacy.android.feature_flags.AppFeatures
import mega.privacy.android.navigation.contract.home.HomeWidget
import mega.privacy.android.navigation.contract.home.HomeWidgetProvider
import mega.privacy.mobile.home.presentation.home.model.HomeUiState
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub

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

    @BeforeEach
    fun setUp() {
        stubFeatureFlag()
        underTest = HomeViewModel(
            widgetProviders = homeWidgetProviders,
            monitorHomeWidgetConfigurationUseCase = monitorHomeWidgetConfigurationUseCase,
            monitorConnectivityUseCase = monitorConnectivityUseCase,
            hasOfflineFilesUseCase = hasOfflineFilesUseCase,
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
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

        val dynamicWidget = stubWidget(identifier = "dynamic1", defaultOrder = 1)
        dynamicWidgetsProvider.stub {
            onBlocking { getWidgets() } doReturn setOf(
                dynamicWidget,
            )
        }

        val staticWidget = stubWidget(identifier = "static1", defaultOrder = 0)
        staticWidgetsProvider.stub {
            onBlocking { getWidgets() } doReturn setOf(
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

        val dynamicWidget = stubWidget(identifier = dynamicIdentifier, defaultOrder = 1)
        dynamicWidgetsProvider.stub {
            onBlocking { getWidgets() } doReturn setOf(
                dynamicWidget,
            )
        }

        val staticWidget = stubWidget(identifier = staticIdentifier, defaultOrder = 0)
        staticWidgetsProvider.stub {
            onBlocking { getWidgets() } doReturn setOf(
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

        val dynamicWidget = stubWidget(identifier = dynamicIdentifier, defaultOrder = 1)
        dynamicWidgetsProvider.stub {
            onBlocking { getWidgets() } doReturn setOf(
                dynamicWidget,
            )
        }

        val staticWidget = stubWidget(identifier = staticIdentifier, defaultOrder = 0)
        staticWidgetsProvider.stub {
            onBlocking { getWidgets() } doReturn setOf(
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
    fun `test that offline state is returned when disconnected and has no offline files`() = runTest {
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
    fun `test that offline state defaults to false when hasOfflineFilesUseCase throws exception`() = runTest {
        stubConnectivity(connected = false)
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
            onBlocking { invoke() } doReturn hasOfflineFiles
        }
    }

    private fun stubWidgetProviders() {
        dynamicWidgetsProvider.stub {
            onBlocking { getWidgets() } doReturn emptySet()
        }
        staticWidgetsProvider.stub {
            onBlocking { getWidgets() } doReturn emptySet()
        }
    }

    private fun stubWidget(
        identifier: String,
        defaultOrder: Int,
    ): HomeWidget {
        return mock<HomeWidget> {
            on { this.identifier } doReturn identifier
            on { this.defaultOrder } doReturn defaultOrder
        }
    }

    private fun stubFeatureFlag(enabled: Boolean = false) {
        getFeatureFlagValueUseCase.stub {
            onBlocking { invoke(AppFeatures.SearchRevamp) } doReturn enabled
        }
    }
}