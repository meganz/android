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
import mega.privacy.android.app.presentation.advertisements.model.AdsSlotIDs
import mega.privacy.android.domain.entity.advertisements.AdDetails
import mega.privacy.android.domain.entity.advertisements.FetchAdDetailRequest
import mega.privacy.android.domain.entity.preference.StartScreen
import mega.privacy.android.domain.usecase.MonitorStartScreenPreference
import mega.privacy.android.domain.usecase.advertisements.FetchAdDetailUseCase
import mega.privacy.android.domain.usecase.advertisements.IsAccountNewUseCase
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
    private val monitorStartScreenPreference = mock<MonitorStartScreenPreference>()
    private val isAccountNewUseCase = mock<IsAccountNewUseCase>()

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
            isAccountNewUseCase,
            monitorStartScreenPreference
        )
    }

    private fun initTestClass() {
        underTest = AdsViewModel(
            fetchAdDetailUseCase = fetchAdDetailUseCase,
            isAccountNewUseCase = isAccountNewUseCase,
            monitorStartScreenPreference = monitorStartScreenPreference
        )
    }

    @AfterAll
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun provideAdReConsumptionParameters() = Stream.of(
        Arguments.of(true, false),
        Arguments.of(false, true),
    )

    @ParameterizedTest(name = "when Ad is consumed while accountIsNew is: {0},then showAdsView is {1}")
    @MethodSource("provideAdReConsumptionParameters")
    fun `test that showAdsView will be updated after Ad slot is consumed when accountIsNew is equal to the returned values`(
        input: Boolean,
        expected: Boolean,
    ) = runTest {
        initTestClass()
        whenever(isAccountNewUseCase()).thenReturn(input)
        whenever(fetchAdDetailUseCase(any())).thenReturn(fetchedAdDetail)
        underTest.enableAdsFeature()
        underTest.fetchNewAd(slotId)
        testScheduler.advanceUntilIdle()
        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.showAdsView).isTrue()
        }
        underTest.onAdConsumed()
        underTest.fetchNewAd(slotId)
        testScheduler.advanceUntilIdle()
        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.showAdsView).isEqualTo(expected)
        }
    }

    private fun provideMonitorStartScreenParameters() = Stream.of(
        Arguments.of(StartScreen.Home, AdsSlotIDs.TAB_HOME_SLOT_ID, true),
        Arguments.of(StartScreen.Photos, AdsSlotIDs.TAB_PHOTOS_SLOT_ID, true),
        Arguments.of(StartScreen.CloudDrive, AdsSlotIDs.TAB_CLOUD_SLOT_ID, true),
        Arguments.of(StartScreen.Chat, "", false),
        Arguments.of(StartScreen.SharedItems, AdsSlotIDs.SHARED_LINK_SLOT_ID, false),
    )


    @ParameterizedTest(name = "when monitorStartScreenPreference flow return start screen: {0}, emits {1}")
    @MethodSource("provideMonitorStartScreenParameters")
    fun `test showAdsView will be updated with the right value when monitorStartScreenPreference flow  return it`(
        startScreen: StartScreen,
        assignedAdSlot: String,
        expected: Boolean,
    ) =
        runTest {
            initTestClass()
            whenever(isAccountNewUseCase()).thenReturn(false)
            whenever(monitorStartScreenPreference()).thenReturn(flowOf(startScreen))
            whenever(fetchAdDetailUseCase(FetchAdDetailRequest(assignedAdSlot, null))).thenReturn(
                fetchedAdDetail
            )
            underTest.enableAdsFeature()
            underTest.getDefaultStartScreen()
            testScheduler.advanceUntilIdle()
            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.showAdsView).isEqualTo(expected)
                if (state.showAdsView)
                    assertThat(state.slotId).isEqualTo(assignedAdSlot)
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
            whenever(isAccountNewUseCase()).thenReturn(false)
            initTestClass()
            underTest.enableAdsFeature()
            testScheduler.advanceUntilIdle()
            underTest.onScreenOrientationChanged(input)
            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.showAdsView).isEqualTo(expected)
            }
        }

    @Test
    fun `test that showAdsView will be false when fetchAdDetailUseCase returns null`() =
        runTest {
            whenever(isAccountNewUseCase()).thenReturn(false)
            initTestClass()
            underTest.enableAdsFeature()
            whenever(fetchAdDetailUseCase(any())).thenReturn(null)
            underTest.fetchNewAd(slotId)
            testScheduler.advanceUntilIdle()
            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.showAdsView).isFalse()
            }
        }

    @Test
    fun `test that showAdsView will be true when fetchAdDetailUseCase is successful`() =
        runTest {
            whenever(isAccountNewUseCase()).thenReturn(false)
            initTestClass()
            underTest.enableAdsFeature()
            whenever(fetchAdDetailUseCase(FetchAdDetailRequest(slotId, null))).thenReturn(
                fetchedAdDetail
            )
            underTest.fetchNewAd(slotId)
            testScheduler.advanceUntilIdle()
            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.showAdsView).isTrue()
                assertThat(state.adsBannerUrl).isEqualTo(fetchedAdDetail.url)
            }
        }

    @Test
    fun `test that showAdsView will be false when Ads feature is not enabled`() =
        runTest {
            whenever(isAccountNewUseCase()).thenReturn(false)
            initTestClass()
            whenever(fetchAdDetailUseCase(FetchAdDetailRequest(slotId, null))).thenReturn(
                fetchedAdDetail
            )
            underTest.fetchNewAd(slotId)
            testScheduler.advanceUntilIdle()
            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.showAdsView).isFalse()
            }
        }
}