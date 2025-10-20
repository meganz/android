package mega.privacy.android.app.appstate

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.appstate.content.AppContentStateViewModel
import mega.privacy.android.app.appstate.content.model.AppContentState
import mega.privacy.android.domain.entity.Feature
import mega.privacy.android.domain.entity.navigation.Flagged
import mega.privacy.android.domain.usecase.RootNodeExistsUseCase
import mega.privacy.android.domain.usecase.featureflag.GetEnabledFlaggedItemsUseCase
import mega.privacy.android.domain.usecase.login.MonitorFetchNodesFinishUseCase
import mega.privacy.android.domain.usecase.preference.MonitorStartScreenPreferenceDestinationUseCase
import mega.privacy.android.navigation.contract.FeatureDestination
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AppContentStateViewModelTest {
    private lateinit var underTest: AppContentStateViewModel
    private val getEnabledFlaggedItemsUseCase = mock<GetEnabledFlaggedItemsUseCase>()

    private val monitorStartScreenPreferenceDestinationUseCase =
        mock<MonitorStartScreenPreferenceDestinationUseCase>()

    private val monitorFetchNodesFinishUseCase: MonitorFetchNodesFinishUseCase = mock()

    private val rootNodeExistsUseCase: RootNodeExistsUseCase = mock()

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
            monitorStartScreenPreferenceDestinationUseCase,
            rootNodeExistsUseCase,
        )
    }

    @Test
    fun `test that initial state is loading`() = runTest {
        val featureDestinations = emptySet<@JvmSuppressWildcards FeatureDestination>()
        initUnderTest(featureDestinations)
        assertThat(underTest.state.value).isEqualTo(AppContentState.Loading)
    }


    @Test
    fun `test that feature destinations are added`() = runTest {
        val expected = setOf(mock<FeatureDestination>())

        getEnabledFlaggedItemsUseCase.stub {
            onBlocking { invoke(expected) }.thenReturn(flow { emit(expected) })
        }

        stubMonitoFetchNodes()

        initUnderTest(expected)

        underTest.state
            .filterIsInstance<AppContentState.Data>()
            .test {
                assertThat(awaitItem().featureDestinations).isEqualTo(expected)
                cancelAndIgnoreRemainingEvents()
            }
    }

    @Test
    fun `test that featureDestination items with disabled feature flags are not returned`() =
        runTest {
            val expected = mock<FeatureDestination>()
            val disabledFeature = mock<Feature>()
            val notExpected =
                mock<Flagged>(extraInterfaces = arrayOf(FeatureDestination::class))
            notExpected.stub {
                on { feature }.thenReturn(disabledFeature)
            }
            val featureDestinations = setOf(expected, notExpected as FeatureDestination)
            getEnabledFlaggedItemsUseCase.stub {
                onBlocking { invoke(featureDestinations) }.thenReturn(flow { emit(setOf(expected)) })
            }

            stubMonitoFetchNodes()

            initUnderTest(featureDestinations)

            underTest.state
                .filterIsInstance<AppContentState.Data>()
                .test {
                    assertThat(awaitItem().featureDestinations).containsExactly(expected)
                    cancelAndIgnoreRemainingEvents()
                }
        }

    @Test
    fun `test that featureDestination items with enabled feature flags are returned`() = runTest {
        val expected = mock<FeatureDestination>()
        val enabledFeature = mock<Feature>()
        val alsoExpected =
            mock<Flagged>(extraInterfaces = arrayOf(FeatureDestination::class))
        alsoExpected.stub {
            on { feature }.thenReturn(enabledFeature)
        }
        val featureDestinations = setOf(expected, alsoExpected as FeatureDestination)
        getEnabledFlaggedItemsUseCase.stub {
            onBlocking { invoke(featureDestinations) }.thenReturn(flow {
                emit(
                    setOf(
                        expected,
                        alsoExpected
                    )
                )
            })
        }
        stubMonitoFetchNodes()
        initUnderTest(featureDestinations)
        underTest.state
            .filterIsInstance<AppContentState.Data>()
            .test {
                assertThat(awaitItem().featureDestinations).containsExactly(expected, alsoExpected)
                cancelAndIgnoreRemainingEvents()
            }
    }

    private fun stubAllEnabledFlaggedItems() {
        getEnabledFlaggedItemsUseCase.stub {
            onBlocking { invoke(any<Set<Any>>()) }.thenAnswer { flow { emit(it.arguments.first()) } }
        }
    }

    private fun stubMonitoFetchNodes(
        flow: Flow<Boolean> = emptyFlow(),
        initialValue: Boolean = true,
    ) {
        monitorFetchNodesFinishUseCase.stub {
            on { invoke() }.thenReturn(
                flow
            )
        }
        rootNodeExistsUseCase.stub {
            onBlocking { invoke() } doReturn initialValue
        }
    }


    private fun initUnderTest(
        featureDestinations: Set<FeatureDestination>,
    ) {
        underTest = AppContentStateViewModel(
            featureDestinations = featureDestinations,
            getEnabledFlaggedItemsUseCase = getEnabledFlaggedItemsUseCase,
            monitorFetchNodesFinishUseCase = monitorFetchNodesFinishUseCase,
            rootNodeExistsUseCase = rootNodeExistsUseCase,
            appDialogDestinations = emptySet(),
        )
    }
}
