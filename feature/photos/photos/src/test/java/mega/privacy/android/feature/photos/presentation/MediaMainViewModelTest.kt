package mega.privacy.android.feature.photos.presentation

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.advanceUntilIdle
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.thumbnailpreview.DownloadThumbnailUseCase
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
internal class MediaMainViewModelTest {
    private lateinit var underTest: MediaMainViewModel

    private val getFeatureFlagValueUseCase = mock<GetFeatureFlagValueUseCase>()
    private val downloadThumbnailUseCase = mock<DownloadThumbnailUseCase>()

    private fun initTestClass() {
        underTest = MediaMainViewModel(
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
            downloadThumbnailUseCase = downloadThumbnailUseCase,
        )
    }

    @Test
    fun `test that initial state is correct`() = runTest {
        initTestClass()

        underTest.uiState.test {
            val initialState = awaitItem()
            assertThat(initialState.isTimelineRevampEnabled).isNull()
            assertThat(initialState.isTimelinePinchToZoomEnabled).isNull()
        }
    }

    @Test
    fun `test that isTimelinePinchToZoomEnabled is true when the feature flag is enabled`() =
        runTest {
            whenever(getFeatureFlagValueUseCase(ApiFeatures.TimelinePinchToZoom)) doReturn true
            initTestClass()

            advanceUntilIdle()

            assertThat(underTest.uiState.value.isTimelinePinchToZoomEnabled).isTrue()
        }

    @Test
    fun `test that isTimelinePinchToZoomEnabled is false when the feature flag is disabled`() =
        runTest {
            whenever(getFeatureFlagValueUseCase(ApiFeatures.TimelinePinchToZoom)) doReturn false
            initTestClass()

            advanceUntilIdle()

            assertThat(underTest.uiState.value.isTimelinePinchToZoomEnabled).isFalse()
        }

    @Test
    fun `test that prefetchThumbnail downloads the thumbnail for the tapped node`() =
        runTest {
            initTestClass()

            underTest.prefetchThumbnail(nodeId = 42L)

            advanceUntilIdle()

            verify(downloadThumbnailUseCase).invoke(42L)
        }
}
