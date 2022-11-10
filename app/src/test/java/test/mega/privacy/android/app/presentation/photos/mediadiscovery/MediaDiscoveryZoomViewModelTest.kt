package test.mega.privacy.android.app.presentation.photos.mediadiscovery

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.photos.mediadiscovery.MediaDiscoveryZoomViewModel
import mega.privacy.android.app.presentation.photos.model.ZoomLevel
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class MediaDiscoveryZoomViewModelTest {
    private lateinit var underTest: MediaDiscoveryZoomViewModel

    @Before
    fun setUp() {
        underTest = MediaDiscoveryZoomViewModel()
    }

    @Test
    fun `test that default zoom level is 3x grid`() = runTest {
        val expectedZoomLevel = ZoomLevel.Grid_3

        underTest.state.test {
            val actualZoomLevel = awaitItem()
            assertThat(expectedZoomLevel).isEqualTo(actualZoomLevel)
        }
    }

    @Test
    fun `test that zoom in should increase zoom level to 1x grid`() = runTest {
        val expectedZoomLevel = ZoomLevel.Grid_1

        underTest.zoomIn()

        underTest.state.test {
            val actualZoomLevel = awaitItem()
            assertThat(expectedZoomLevel).isEqualTo(actualZoomLevel)
        }
    }

    @Test
    fun `test that zoom level 1x grid is the maximum`() = runTest {
        val expectedZoomLevel = ZoomLevel.Grid_1

        underTest.zoomIn()
        underTest.zoomIn()

        underTest.state.test {
            val actualZoomLevel = awaitItem()
            assertThat(expectedZoomLevel).isEqualTo(actualZoomLevel)
        }
    }

    @Test
    fun `test that zoom out should decrease zoom level to 5x grid`() = runTest {
        val expectedZoomLevel = ZoomLevel.Grid_5

        underTest.zoomOut()

        underTest.state.test {
            val actualZoomLevel = awaitItem()
            assertThat(expectedZoomLevel).isEqualTo(actualZoomLevel)
        }
    }

    @Test
    fun `test that zoom level 5x grid is the minimum`() = runTest {
        val expectedZoomLevel = ZoomLevel.Grid_5

        underTest.zoomOut()
        underTest.zoomOut()

        underTest.state.test {
            val actualZoomLevel = awaitItem()
            assertThat(expectedZoomLevel).isEqualTo(actualZoomLevel)
        }
    }
}
