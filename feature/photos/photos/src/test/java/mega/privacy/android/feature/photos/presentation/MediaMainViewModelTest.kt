package mega.privacy.android.feature.photos.presentation

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
internal class MediaMainViewModelTest {
    private lateinit var underTest: MediaMainViewModel

    @BeforeEach
    fun setUp() {
        underTest = MediaMainViewModel()
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
}
