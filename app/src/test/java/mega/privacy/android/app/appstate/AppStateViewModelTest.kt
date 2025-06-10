package mega.privacy.android.app.appstate

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
import mega.privacy.android.app.appstate.model.AppState
import mega.privacy.android.domain.entity.Feature
import mega.privacy.android.domain.entity.navigation.Flagged
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.navigation.GetStartScreenPreferenceDestinationUseCase
import mega.privacy.android.navigation.contract.FeatureDestination
import mega.privacy.android.navigation.contract.MainNavItem
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.withSettings

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AppStateViewModelTest {
    private lateinit var underTest: AppStateViewModel
    private val getFeatureFlagValueUseCase = mock<GetFeatureFlagValueUseCase>()

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
        )
    }

    @Test
    fun `test that initial state is loading`() = runTest {
        underTest = AppStateViewModel(
            mainDestinations = emptySet(),
            featureDestinations = emptySet(),
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
            getStartScreenPreferenceDestinationUseCase = getStartScreenPreferenceDestinationUseCase,
        )
        assertThat(underTest.state.value).isEqualTo(AppState.Loading)
    }

    @Test
    fun `test that main destinations are added`() = runTest {
        val expected = setOf(mock<MainNavItem>())

        stubDefaultStartScreenPreference()

        underTest = AppStateViewModel(
            mainDestinations = expected,
            featureDestinations = emptySet(),
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
            getStartScreenPreferenceDestinationUseCase = getStartScreenPreferenceDestinationUseCase,
        )

        underTest.state
            .filterIsInstance<AppState.Data>()
            .test {
                assertThat(awaitItem().mainNavItems).isEqualTo(expected)
                cancelAndIgnoreRemainingEvents()
            }
    }

    @Test
    fun `test that main nav items with disabled feature flags are not returned`() = runTest {
        stubDefaultStartScreenPreference()
        val expected = mock<MainNavItem>()
        val disabledFeature = mock<Feature>()
        getFeatureFlagValueUseCase.stub {
            onBlocking { invoke(disabledFeature) }.thenReturn(false)
        }
        val notExpected =
            mock<Flagged>(withSettings(extraInterfaces = arrayOf(MainNavItem::class)))
        notExpected.stub {
            on { feature }.thenReturn(disabledFeature)
        }
        underTest = AppStateViewModel(
            mainDestinations = setOf(expected, notExpected as MainNavItem),
            featureDestinations = emptySet(),
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
            getStartScreenPreferenceDestinationUseCase = getStartScreenPreferenceDestinationUseCase,
        )

        underTest.state
            .filterIsInstance<AppState.Data>()
            .test {
                assertThat(awaitItem().mainNavItems).containsExactly(expected)
                cancelAndIgnoreRemainingEvents()
            }
    }

    @Test
    fun `test that main nav items with enabled feature flags are returned`() = runTest {
        stubDefaultStartScreenPreference()
        val expected = mock<MainNavItem>()
        val enabledFeature = mock<Feature>()
        getFeatureFlagValueUseCase.stub {
            onBlocking { invoke(enabledFeature) }.thenReturn(true)
        }
        val alsoExpected =
            mock<Flagged>(withSettings(extraInterfaces = arrayOf(MainNavItem::class)))
        alsoExpected.stub {
            on { feature }.thenReturn(enabledFeature)
        }
        underTest = AppStateViewModel(
            mainDestinations = setOf(expected, alsoExpected as MainNavItem),
            featureDestinations = emptySet(),
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
            getStartScreenPreferenceDestinationUseCase = getStartScreenPreferenceDestinationUseCase,
        )
        underTest.state
            .filterIsInstance<AppState.Data>()
            .test {
                assertThat(awaitItem().mainNavItems).containsExactly(expected, alsoExpected)
                cancelAndIgnoreRemainingEvents()
            }
    }

    @Test
    fun `test that feature destinations are added`() = runTest {
        val expected = setOf(mock<FeatureDestination>())

        stubDefaultStartScreenPreference()

        underTest = AppStateViewModel(
            mainDestinations = stubDefaultMainNavigationItems(),
            featureDestinations = expected,
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
            getStartScreenPreferenceDestinationUseCase = getStartScreenPreferenceDestinationUseCase,
        )

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
            stubDefaultStartScreenPreference()
            val expected = mock<FeatureDestination>()
            val disabledFeature = mock<Feature>()
            getFeatureFlagValueUseCase.stub {
                onBlocking { invoke(disabledFeature) }.thenReturn(false)
            }
            val notExpected =
                mock<Flagged>(withSettings(extraInterfaces = arrayOf(FeatureDestination::class)))
            notExpected.stub {
                on { feature }.thenReturn(disabledFeature)
            }
            underTest = AppStateViewModel(
                mainDestinations = stubDefaultMainNavigationItems(),
                featureDestinations = setOf(expected, notExpected as FeatureDestination),
                getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
                getStartScreenPreferenceDestinationUseCase = getStartScreenPreferenceDestinationUseCase,
            )

            underTest.state
                .filterIsInstance<AppState.Data>()
                .test {
                    assertThat(awaitItem().featureDestinations).containsExactly(expected)
                    cancelAndIgnoreRemainingEvents()
                }
        }

    @Test
    fun `test that featureDestination items with enabled feature flags are returned`() = runTest {
        stubDefaultStartScreenPreference()
        val expected = mock<FeatureDestination>()
        val enabledFeature = mock<Feature>()
        getFeatureFlagValueUseCase.stub {
            onBlocking { invoke(enabledFeature) }.thenReturn(true)
        }
        val alsoExpected =
            mock<Flagged>(withSettings(extraInterfaces = arrayOf(FeatureDestination::class)))
        alsoExpected.stub {
            on { feature }.thenReturn(enabledFeature)
        }
        underTest = AppStateViewModel(
            mainDestinations = stubDefaultMainNavigationItems(),
            featureDestinations = setOf(expected, alsoExpected as FeatureDestination),
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
            getStartScreenPreferenceDestinationUseCase = getStartScreenPreferenceDestinationUseCase,
        )
        underTest.state
            .filterIsInstance<AppState.Data>()
            .test {
                assertThat(awaitItem().featureDestinations).containsExactly(expected, alsoExpected)
                cancelAndIgnoreRemainingEvents()
            }
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

    private fun stubDefaultMainNavigationItems() = setOf(mock<MainNavItem>())

}