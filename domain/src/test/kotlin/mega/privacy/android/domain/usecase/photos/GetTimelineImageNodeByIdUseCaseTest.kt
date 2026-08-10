package mega.privacy.android.domain.usecase.photos

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.ImageNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.PhotosRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetTimelineImageNodeByIdUseCaseTest {

    private lateinit var underTest: GetTimelineImageNodeByIdUseCase

    private val photosRepository = mock<PhotosRepository>()

    @BeforeEach
    fun setUp() {
        underTest = GetTimelineImageNodeByIdUseCase(photosRepository = photosRepository)
    }

    @AfterEach
    fun tearDown() {
        reset(photosRepository)
    }

    @Test
    fun `test that invoke returns the image node resolved by the repository`() = runTest {
        val nodeId = NodeId(7L)
        val imageNode = mock<ImageNode>()
        whenever(photosRepository.getImageNode(nodeId)) doReturn imageNode

        val actual = underTest(nodeId)

        assertThat(actual).isEqualTo(imageNode)
        verify(photosRepository).getImageNode(nodeId)
    }

    @Test
    fun `test that invoke returns null when the repository has no node for the id`() = runTest {
        val nodeId = NodeId(7L)
        whenever(photosRepository.getImageNode(nodeId)) doReturn null

        val actual = underTest(nodeId)

        assertThat(actual).isNull()
    }
}
