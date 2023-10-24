package test.mega.privacy.android.app.presentation.advertisements

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.presentation.advertisements.AdsViewModel
import mega.privacy.android.domain.entity.advertisements.AdDetails
import mega.privacy.android.domain.entity.preference.StartScreen
import mega.privacy.android.domain.usecase.MonitorStartScreenPreference
import mega.privacy.android.domain.usecase.advertisements.FetchAdDetailUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.util.stream.Stream

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AdsViewModelTest {
    private lateinit var underTest: AdsViewModel
    private val fetchAdDetailUseCase = mock<FetchAdDetailUseCase>()
    private val getFeatureFlagValueUseCase = mock<GetFeatureFlagValueUseCase>()
    private val monitorStartScreenPreference = mock<MonitorStartScreenPreference>()

    private val slotId = "ANDFB"
    private val url = "https://megaad.nz/#z_xyz"
    private val fetchedAdDetail = AdDetails(slotId, url)

    @BeforeAll
    fun setup() {
        Dispatchers.setMain(StandardTestDispatcher())

    }

    @BeforeEach
    fun resetMocks() {
        reset(
            fetchAdDetailUseCase,
            getFeatureFlagValueUseCase,
            monitorStartScreenPreference
        )
    }

    private fun initTestClass() {
        underTest = AdsViewModel(
            fetchAdDetailUseCase = fetchAdDetailUseCase,
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
            monitorStartScreenPreference = monitorStartScreenPreference
        )
    }

    @AfterAll
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun provideMonitorStartScreenParameters() = Stream.of(
        Arguments.of(StartScreen.Home, true),
        Arguments.of(StartScreen.Photos, true),
        Arguments.of(StartScreen.CloudDrive, true),
        Arguments.of(StartScreen.Chat, false),
        Arguments.of(StartScreen.SharedItems, false),
    )

    @ParameterizedTest(name = "when monitorStartScreenPreference flow return start screen: {0}, emits {1}")
    @MethodSource("provideMonitorStartScreenParameters")
    fun `test showAdsView will be updated with the right value when monitorStartScreenPreference flow  return it`(
        input: StartScreen,
        expected: Boolean,
    ) =
        runTest {
            whenever(getFeatureFlagValueUseCase(any())).thenReturn(true)
            whenever(monitorStartScreenPreference()).thenReturn(flowOf(input))
            initTestClass()
            whenever(fetchAdDetailUseCase(any())).thenReturn(fetchedAdDetail)
            testScheduler.advanceUntilIdle()
            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.showAdsView).isEqualTo(expected)
            }
        }

    private fun provideOrientationParameters() = Stream.of(
        Arguments.of(true, true),
        Arguments.of(false, false),
    )

    @ParameterizedTest(name = "when screen orientation isPortrait is: {0}, showAdsView is {1}")
    @MethodSource("provideOrientationParameters")
    fun `test that showAdsView will be updated with the right value when screen orientation changes`(
        input: Boolean,
        expected: Boolean,
    ) =
        runTest {
            whenever(getFeatureFlagValueUseCase(any())).thenReturn(true)
            initTestClass()
            testScheduler.advanceUntilIdle()
            underTest.onScreenOrientationChanged(input)
            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.showAdsView).isEqualTo(expected)
            }
        }

    private fun provideFeatureFlagParameters() = Stream.of(
        Arguments.of(true, true),
        Arguments.of(false, false),
    )

    @ParameterizedTest(name = "when getFeatureFlagValueUseCase flow return: {0}, emits {1}")
    @MethodSource("provideFeatureFlagParameters")
    fun `test that showAdsView will be updated with the right value when getFeatureFlagValueUseCase return it`(
        input: Boolean,
        expected: Boolean,
    ) =
        runTest {
            whenever(getFeatureFlagValueUseCase(any())).thenReturn(input)
            whenever(monitorStartScreenPreference()).thenReturn(flowOf(StartScreen.Home))
            initTestClass()
            whenever(fetchAdDetailUseCase(any())).thenReturn(fetchedAdDetail)
            testScheduler.advanceUntilIdle()
            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.showAdsView).isEqualTo(expected)
            }
        }

    @Test
    fun `test that showAdsView will be false when fetchAdDetailUseCase returns null`() =
        runTest {
            whenever(getFeatureFlagValueUseCase(any())).thenReturn(true)
            whenever(monitorStartScreenPreference()).thenReturn(flowOf(StartScreen.Home))
            initTestClass()
            whenever(fetchAdDetailUseCase(any())).thenReturn(null)
            testScheduler.advanceUntilIdle()
            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.showAdsView).isFalse()
            }
        }

    @Test
    fun `test that showAdsView will be true when fetchAdDetailUseCase is successful`() =
        runTest {
            whenever(getFeatureFlagValueUseCase(any())).thenReturn(true)
            whenever(monitorStartScreenPreference()).thenReturn(flowOf(StartScreen.Home))
            initTestClass()
            whenever(fetchAdDetailUseCase(any())).thenReturn(fetchedAdDetail)
            testScheduler.advanceUntilIdle()
            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.showAdsView).isTrue()
                assertThat(state.adsBannerUrl).isEqualTo(fetchedAdDetail.url)

            }
        }
}