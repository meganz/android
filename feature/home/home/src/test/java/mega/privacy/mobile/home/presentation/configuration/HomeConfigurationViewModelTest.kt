package mega.privacy.mobile.home.presentation.configuration

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import mega.android.core.ui.model.LocalizedText
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.home.HomeWidgetConfiguration
import mega.privacy.android.domain.usecase.featureflag.GetEnabledFlaggedItemsUseCase
import mega.privacy.android.domain.usecase.home.DeleteWidgetConfigurationUseCase
import mega.privacy.android.domain.usecase.home.MonitorHomeWidgetConfigurationUseCase
import mega.privacy.android.domain.usecase.home.UpdateWidgetConfigurationsUseCase
import mega.privacy.android.navigation.contract.home.HomeWidget
import mega.privacy.android.navigation.contract.home.HomeWidgetProvider
import mega.privacy.mobile.home.presentation.configuration.mapper.WidgetConfigurationItemMapper
import mega.privacy.mobile.home.presentation.configuration.model.HomeConfigurationUiState
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify

@ExtendWith(CoroutineMainDispatcherExtension::class)
class HomeConfigurationViewModelTest {
    private lateinit var underTest: HomeConfigurationViewModel

    private val dynamicWidgetsProvider = mock<HomeWidgetProvider>()
    private val staticWidgetsProvider = mock<HomeWidgetProvider>()
    private val homeWidgetProviders = setOf(
        staticWidgetsProvider,
        dynamicWidgetsProvider,
    )
    private val monitorHomeWidgetConfigurationUseCase =
        mock<MonitorHomeWidgetConfigurationUseCase>()
    private val updateWidgetConfigurationsUseCase = mock<UpdateWidgetConfigurationsUseCase>()
    private val deleteWidgetConfigurationsUseCase = mock<DeleteWidgetConfigurationUseCase>()
    private val getEnabledFlaggedItemsUseCase = mock<GetEnabledFlaggedItemsUseCase>()

    @BeforeEach
    fun setUp() {
        underTest = HomeConfigurationViewModel(
            widgetProviders = homeWidgetProviders,
            monitorHomeWidgetConfigurationUseCase = monitorHomeWidgetConfigurationUseCase,
            widgetConfigurationItemMapper = WidgetConfigurationItemMapper(),
            updateWidgetConfigurationsUseCase = updateWidgetConfigurationsUseCase,
            deleteWidgetConfigurationUseCase = deleteWidgetConfigurationsUseCase,
            getEnabledFlaggedItemsUseCase = getEnabledFlaggedItemsUseCase,
        )
    }

    @AfterEach
    fun tearDown() {
        reset(
            dynamicWidgetsProvider,
            staticWidgetsProvider,
            monitorHomeWidgetConfigurationUseCase,
            updateWidgetConfigurationsUseCase,
            deleteWidgetConfigurationsUseCase,
            getEnabledFlaggedItemsUseCase,
        )
    }

    @Test
    fun `test that widget options are displayed even if no configurations are returned`() =
        runTest {
            monitorHomeWidgetConfigurationUseCase.stub {
                on { invoke() } doReturn flow {
                    emit(emptyList())
                    awaitCancellation()
                }
            }

            val dynamicWidget = stubWidget(identifier = "dynamic1", defaultOrder = 1)
            val dynamicWidgets = setOf(dynamicWidget)
            dynamicWidgetsProvider.stub {
                onBlocking { getWidgets() } doReturn dynamicWidgets
            }

            val staticWidget = stubWidget(identifier = "static1", defaultOrder = 0)
            val staticWidgets = setOf(staticWidget)
            staticWidgetsProvider.stub {
                onBlocking { getWidgets() } doReturn staticWidgets
            }

            stubGetEnabledFlaggedItemsUseCase(dynamicWidgets, staticWidgets)

            underTest.state.test {
                val actual = awaitItem() as HomeConfigurationUiState.Data
                assertThat(actual.widgets).hasSize(2)
            }
        }

    @Test
    fun `test that configurations override enabled state`() = runTest {
        monitorHomeWidgetConfigurationUseCase.stub {
            on { invoke() } doReturn flow {
                emit(
                    listOf(
                        HomeWidgetConfiguration(
                            widgetIdentifier = "dynamic1",
                            widgetOrder = 0,
                            enabled = false,
                        ),
                    ),
                )
                awaitCancellation()
            }
        }

        val dynamicWidget = stubWidget(identifier = "dynamic1", defaultOrder = 1)
        val dynamicWidgets = setOf(dynamicWidget)
        dynamicWidgetsProvider.stub {
            onBlocking { getWidgets() } doReturn dynamicWidgets
        }

        val staticWidget = stubWidget(identifier = "static1", defaultOrder = 0)
        val staticWidgets = setOf(staticWidget)
        staticWidgetsProvider.stub {
            onBlocking { getWidgets() } doReturn staticWidgets
        }

        stubGetEnabledFlaggedItemsUseCase(dynamicWidgets, staticWidgets)

        underTest.state.test {
            val actual = awaitItem() as HomeConfigurationUiState.Data
            assertThat(actual.widgets).hasSize(2)
            val dynamicItem =
                actual.widgets.first { it.identifier == "dynamic1" }
            assertThat(dynamicItem.enabled).isFalse()
            val staticItem =
                actual.widgets.first { it.identifier == "static1" }
            assertThat(staticItem.enabled).isTrue()
        }
    }

    @Test
    fun `test that calling update widget order calls the update configurations use case with the correct new order`() =
        runTest {
            val order1ConfigurationId = "order1"
            val order0ConfigurationId = "order0"
            val configurationList = listOf(
                HomeWidgetConfiguration(
                    widgetIdentifier = order1ConfigurationId,
                    widgetOrder = 1,
                    enabled = true,
                ),
                HomeWidgetConfiguration(
                    widgetIdentifier = order0ConfigurationId,
                    widgetOrder = 0,
                    enabled = true,
                ),
            )
            underTest.updateWidgetOrder(configurationList.map {
                WidgetConfigurationItemMapper().invoke(
                    homeWidget = stubWidget(it.widgetIdentifier, it.widgetOrder),
                    widgetConfiguration = it,
                )
            })
            val captor = argumentCaptor<List<HomeWidgetConfiguration>>()
            verify(updateWidgetConfigurationsUseCase).invoke(captor.capture())
            val actual = captor.firstValue.associateBy { it.widgetIdentifier }
            assertThat(actual[order0ConfigurationId]?.widgetOrder).isEqualTo(1)
            assertThat(actual[order1ConfigurationId]?.widgetOrder).isEqualTo(0)
        }

    @Test
    fun `test that calling update enabled state calls update configuration use case with correct values`() =
        runTest {
            underTest.updateEnabledState(
                item = WidgetConfigurationItemMapper().invoke(
                    homeWidget = stubWidget("id", 0),
                    widgetConfiguration = HomeWidgetConfiguration(
                        widgetIdentifier = "id",
                        widgetOrder = 0,
                        enabled = true,
                    ),
                ),
                enabled = false,
            )
            val captor = argumentCaptor<List<HomeWidgetConfiguration>>()
            verify(updateWidgetConfigurationsUseCase).invoke(captor.capture())
            val actual = captor.firstValue.first()
            assertThat(actual.widgetIdentifier).isEqualTo("id")
            assertThat(actual.enabled).isFalse()
        }

    @Test
    fun `test that widgets are sorted by index ascending`() = runTest {
        val secondWidget = stubWidget(identifier = "second", defaultOrder = 5)
        val thirdWidget = stubWidget(identifier = "third", defaultOrder = 6)
        val firstWidget = stubWidget(identifier = "first", defaultOrder = 4)

        monitorHomeWidgetConfigurationUseCase.stub {
            on { invoke() } doReturn flow {
                emit(
                    listOf(
                        HomeWidgetConfiguration(
                            widgetIdentifier = "second",
                            widgetOrder = 1,
                            enabled = true,
                        ),
                        HomeWidgetConfiguration(
                            widgetIdentifier = "third",
                            widgetOrder = 2,
                            enabled = true,
                        ),
                        HomeWidgetConfiguration(
                            widgetIdentifier = "first",
                            widgetOrder = 0,
                            enabled = true,
                        ),
                    ),
                )
                awaitCancellation()
            }
        }

        val dynamicWidgets = setOf(secondWidget, thirdWidget)
        dynamicWidgetsProvider.stub {
            onBlocking { getWidgets() } doReturn dynamicWidgets
        }

        val staticWidgets = setOf(firstWidget)
        staticWidgetsProvider.stub {
            onBlocking { getWidgets() } doReturn staticWidgets
        }

        stubGetEnabledFlaggedItemsUseCase(dynamicWidgets, staticWidgets)

        underTest.state.test {
            val actual = awaitItem() as HomeConfigurationUiState.Data
            assertThat(actual.widgets.map { it.identifier })
                .containsExactly("first", "second", "third")
                .inOrder()
        }
    }

    @Test
    fun `test that widgets are sorted by default order when no configuration exists`() = runTest {
        val thirdWidget = stubWidget(identifier = "third", defaultOrder = 2)
        val firstWidget = stubWidget(identifier = "first", defaultOrder = 0)
        val secondWidget = stubWidget(identifier = "second", defaultOrder = 1)

        monitorHomeWidgetConfigurationUseCase.stub {
            on { invoke() } doReturn flow {
                emit(emptyList())
                awaitCancellation()
            }
        }

        val dynamicWidgets = setOf(thirdWidget, firstWidget)
        dynamicWidgetsProvider.stub {
            onBlocking { getWidgets() } doReturn dynamicWidgets
        }

        val staticWidgets = setOf(secondWidget)
        staticWidgetsProvider.stub {
            onBlocking { getWidgets() } doReturn staticWidgets
        }

        stubGetEnabledFlaggedItemsUseCase(dynamicWidgets, staticWidgets)

        underTest.state.test {
            val actual = awaitItem() as HomeConfigurationUiState.Data
            assertThat(actual.widgets.map { it.identifier })
                .containsExactly("first", "second", "third")
                .inOrder()
        }
    }

    @Test
    fun `test that allowRemoval is false if only one item is enabled`() = runTest {
        val configurationList = listOf(
            HomeWidgetConfiguration(
                widgetIdentifier = "enabledConfigurationId",
                widgetOrder = 1,
                enabled = true,
            ),
            HomeWidgetConfiguration(
                widgetIdentifier = "disabledConfigurationId",
                widgetOrder = 0,
                enabled = false,
            ),
        )

        val homeWidgets = configurationList.map {
            stubWidget(it.widgetIdentifier, it.widgetOrder)
        }.toSet()

        dynamicWidgetsProvider.stub {
            onBlocking { getWidgets() } doReturn homeWidgets
        }

        val staticWidgets = emptySet<HomeWidget>()
        staticWidgetsProvider.stub {
            onBlocking { getWidgets() } doReturn staticWidgets
        }

        monitorHomeWidgetConfigurationUseCase.stub {
            on { invoke() } doReturn flow {
                emit(configurationList)
                awaitCancellation()
            }
        }

        stubGetEnabledFlaggedItemsUseCase(homeWidgets, staticWidgets)

        underTest.state.test {
            val actual = awaitItem() as HomeConfigurationUiState.Data
            assertThat(actual.widgets.count { it.enabled }).isEqualTo(1)
            assertThat(actual.allowRemoval).isFalse()
        }
    }


    @Test
    fun `test that reset widget state to default updates all widgets to default order and enabled`() =
        runTest {
            val firstWidget = stubWidget(identifier = "first", defaultOrder = 2)
            val secondWidget = stubWidget(identifier = "second", defaultOrder = 0)
            val thirdWidget = stubWidget(identifier = "third", defaultOrder = 1)

            dynamicWidgetsProvider.stub {
                onBlocking { getWidgets() } doReturn setOf(firstWidget, secondWidget)
            }
            staticWidgetsProvider.stub {
                onBlocking { getWidgets() } doReturn setOf(thirdWidget)
            }

            underTest.resetWidgetStateToDefault()

            val captor = argumentCaptor<List<HomeWidgetConfiguration>>()
            verify(updateWidgetConfigurationsUseCase).invoke(captor.capture())
            val actual = captor.firstValue.associateBy { it.widgetIdentifier }
            assertThat(actual).hasSize(3)
            assertThat(actual["first"]?.widgetOrder).isEqualTo(2)
            assertThat(actual["second"]?.widgetOrder).isEqualTo(0)
            assertThat(actual["third"]?.widgetOrder).isEqualTo(1)
            assertThat(actual.values.all { it.enabled }).isTrue()
        }

    private fun stubWidget(
        identifier: String,
        defaultOrder: Int,
    ): HomeWidget {
        return mock<HomeWidget> {
            on { this.identifier } doReturn identifier
            on { this.defaultOrder } doReturn defaultOrder
            on { canDelete } doReturn true
            onBlocking { getWidgetName() } doReturn LocalizedText.Literal("Test")

        }
    }

    private fun stubGetEnabledFlaggedItemsUseCase(vararg widgetSets: Set<HomeWidget>) {
        getEnabledFlaggedItemsUseCase.stub {
            widgetSets.forEach { set ->
                on { invoke(set) } doReturn flow { emit(set) }
            }
        }
    }
}