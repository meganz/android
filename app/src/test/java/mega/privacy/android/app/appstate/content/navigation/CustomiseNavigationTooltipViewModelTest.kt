package mega.privacy.android.app.appstate.content.navigation

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.preference.MonitorCustomiseNavigationTooltipShownUseCase
import mega.privacy.android.domain.usecase.preference.SetCustomiseNavigationTooltipShownUseCase
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CustomiseNavigationTooltipViewModelTest {

    private val getFeatureFlagValueUseCase = mock<GetFeatureFlagValueUseCase>()
    private val monitorCustomiseNavigationTooltipShownUseCase =
        mock<MonitorCustomiseNavigationTooltipShownUseCase>()
    private val setCustomiseNavigationTooltipShownUseCase =
        mock<SetCustomiseNavigationTooltipShownUseCase>()

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
            monitorCustomiseNavigationTooltipShownUseCase,
            setCustomiseNavigationTooltipShownUseCase,
        )
    }

    private fun buildUnderTest() = CustomiseNavigationTooltipViewModel(
        getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
        monitorCustomiseNavigationTooltipShownUseCase = monitorCustomiseNavigationTooltipShownUseCase,
        setCustomiseNavigationTooltipShownUseCase = setCustomiseNavigationTooltipShownUseCase,
    )

    private fun stubDependencies(
        isCustomisationEnabled: Boolean = true,
        isTooltipShown: Boolean = false,
    ) {
        getFeatureFlagValueUseCase.stub {
            onBlocking { invoke(ApiFeatures.CustomisableBottomNavigation) }
                .thenReturn(isCustomisationEnabled)
        }
        monitorCustomiseNavigationTooltipShownUseCase.stub {
            on { invoke() }.thenReturn(flowOf(isTooltipShown))
        }
    }

    @Test
    fun `test that uiState emits true when flag is enabled and tooltip has not been shown`() =
        runTest {
            stubDependencies(isCustomisationEnabled = true, isTooltipShown = false)

            buildUnderTest().uiState.test {
                assertThat(expectMostRecentItem()).isTrue()
            }
        }

    @Test
    fun `test that uiState emits false when flag is disabled`() = runTest {
        stubDependencies(isCustomisationEnabled = false, isTooltipShown = false)

        buildUnderTest().uiState.test {
            assertThat(expectMostRecentItem()).isFalse()
        }
    }

    @Test
    fun `test that uiState emits false when tooltip has already been shown`() = runTest {
        stubDependencies(isCustomisationEnabled = true, isTooltipShown = true)

        buildUnderTest().uiState.test {
            assertThat(expectMostRecentItem()).isFalse()
        }
    }

    @Test
    fun `test that uiState stays true when shown preference updates after display`() = runTest {
        getFeatureFlagValueUseCase.stub {
            onBlocking { invoke(ApiFeatures.CustomisableBottomNavigation) }.thenReturn(true)
        }
        monitorCustomiseNavigationTooltipShownUseCase.stub {
            on { invoke() }.thenReturn(flowOf(false, true))
        }

        buildUnderTest().uiState.test {
            assertThat(expectMostRecentItem()).isTrue()
        }
    }

    @Test
    fun `test that init does not call setCustomiseNavigationTooltipShownUseCase`() = runTest {
        stubDependencies()

        buildUnderTest().uiState.test {
            expectMostRecentItem()
        }

        verifyNoInteractions(setCustomiseNavigationTooltipShownUseCase)
    }

    @Test
    fun `test that onTooltipDisplayed calls setCustomiseNavigationTooltipShownUseCase`() =
        runTest {
            stubDependencies()

            buildUnderTest().onTooltipDisplayed()

            verify(setCustomiseNavigationTooltipShownUseCase).invoke()
        }

    @Test
    fun `test that onTooltipDismissed hides the tooltip`() = runTest {
        stubDependencies(isCustomisationEnabled = true, isTooltipShown = false)
        val underTest = buildUnderTest()

        underTest.uiState.test {
            assertThat(expectMostRecentItem()).isTrue()
            underTest.onTooltipDismissed()
            assertThat(awaitItem()).isFalse()
        }
    }
}
