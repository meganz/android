package mega.privacy.mobile.home.presentation.home

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.home.HomeWidgetConfiguration
import mega.privacy.android.domain.usecase.home.MonitorHomeWidgetConfigurationUseCase
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

    @BeforeEach
    fun setUp() {
        underTest = HomeViewModel(
            widgetProviders = homeWidgetProviders,
            monitorHomeWidgetConfigurationUseCase = monitorHomeWidgetConfigurationUseCase,
        )
    }

    @AfterEach
    fun tearDown() {
        reset(
            dynamicWidgetsProvider,
            staticWidgetsProvider,
            monitorHomeWidgetConfigurationUseCase,
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

    private fun stubWidget(
        identifier: String,
        defaultOrder: Int,
    ): HomeWidget {
        return mock<HomeWidget> {
            on { this.identifier } doReturn identifier
            on { this.defaultOrder } doReturn defaultOrder
        }
    }
}