package mega.privacy.android.domain.usecase.photos

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.ImageNode
import mega.privacy.android.domain.repository.PhotosRepository
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class MonitorTimelineNodesUseCaseTest {
    private lateinit var underTest: MonitorTimelineNodesUseCase

    private val photosRepository = mock<PhotosRepository>()

    @Before
    fun setUp() {
        underTest = MonitorTimelineNodesUseCase(
            photosRepository = photosRepository,
        )
    }

    @Test
    fun `test that timeline image nodes are returned from monitorTimelineImageNodes`() = runTest {
        val expected = listOf(mock<ImageNode>())
        whenever(photosRepository.monitorTimelineImageNodes()).thenReturn(flowOf(expected))

        underTest().test {
            assertThat(awaitItem()).isEqualTo(expected)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that the heavy monitorImageNodes pipeline is not used`() = runTest {
        whenever(photosRepository.monitorTimelineImageNodes()).thenReturn(flowOf(emptyList()))

        underTest()

        verify(photosRepository).monitorTimelineImageNodes()
    }
}
