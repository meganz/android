package mega.privacy.android.feature.photos.presentation

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
internal class MediaMainViewModelTest {
    private lateinit var underTest: MediaMainViewModel

    private val getFeatureFlagValueUseCase = mock<GetFeatureFlagValueUseCase>()

    @BeforeEach
    fun setUp() {
        underTest = MediaMainViewModel(getFeatureFlagValueUseCase)
    }

    @Test
    fun `test that initial state is correct`() = runTest {
        underTest.uiState.test {
            val initialState = awaitItem()
            assertThat(initialState.newAlbumDialogEvent).isEqualTo(consumed)
        }
    }

    @Test
    fun `test that showNewAlbumDialog triggers the event`() = runTest {
        underTest.uiState.test {
            awaitItem()

            underTest.showNewAlbumDialog()

            val state = awaitItem()
            assertThat(state.newAlbumDialogEvent).isEqualTo(triggered)
        }
    }

    @Test
    fun `test that resetNewAlbumDialog consumes the event`() = runTest {
        underTest.uiState.test {
            awaitItem()

            underTest.showNewAlbumDialog()
            awaitItem()

            underTest.resetNewAlbumDialog()

            val state = awaitItem()
            assertThat(state.newAlbumDialogEvent).isEqualTo(consumed)
        }
    }

    @Test
    fun `test that showNewAlbumDialog after reset triggers the event again`() = runTest {
        underTest.uiState.test {
            awaitItem()

            underTest.showNewAlbumDialog()
            awaitItem()

            underTest.resetNewAlbumDialog()
            awaitItem()

            underTest.showNewAlbumDialog()

            val state = awaitItem()
            assertThat(state.newAlbumDialogEvent).isEqualTo(triggered)
        }
    }

    @Test
    fun `test that isMediaRevampPhase2Enabled is updated correctly`(): Unit = runTest {
        whenever(getFeatureFlagValueUseCase(ApiFeatures.MediaRevampPhase2)).thenReturn(true)
        underTest = MediaMainViewModel(getFeatureFlagValueUseCase)
        advanceUntilIdle()
        underTest.uiState.test {
            assertThat(awaitItem().isMediaRevampPhase2Enabled).isTrue()
        }
    }
}
