package mega.privacy.android.feature.photos.presentation

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.advanceUntilIdle
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.thumbnailpreview.DownloadPreviewUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
internal class MediaMainViewModelTest {
    private lateinit var underTest: MediaMainViewModel

    private val getFeatureFlagValueUseCase = mock<GetFeatureFlagValueUseCase>()
    private val downloadPreviewUseCase = mock<DownloadPreviewUseCase>()

    @BeforeEach
    fun setUp() {
        underTest = MediaMainViewModel(
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
            downloadPreviewUseCase = downloadPreviewUseCase,
        )
    }

    @Test
    fun `test that initial state is correct`() = runTest {
        underTest.uiState.test {
            val initialState = awaitItem()
            assertThat(initialState.isTimelineRevampEnabled).isNull()
        }
    }

    @Test
    fun `test that prefetchPreview downloads the preview for the tapped node`() = runTest {
        underTest.prefetchPreview(nodeId = 42L)

        advanceUntilIdle()

        verify(downloadPreviewUseCase).invoke(42L)
    }
}
