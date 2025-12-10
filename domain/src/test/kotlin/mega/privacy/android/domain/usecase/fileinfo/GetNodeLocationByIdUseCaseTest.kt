package mega.privacy.android.domain.usecase.fileinfo

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.NodeLocation
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.repository.NodeRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetNodeLocationByIdUseCaseTest {
    private lateinit var underTest: GetNodeLocationByIdUseCase

    private val nodeRepository: NodeRepository = mock()

    private val nodeId = NodeId(100L)
    private val otherNodeId = NodeId(200L)

    @BeforeEach
    fun setUp() {
        underTest = GetNodeLocationByIdUseCase(
            nodeRepository = nodeRepository,
        )
        resetMocks()
    }

    private fun resetMocks() {
        reset(nodeRepository)
    }

    @ParameterizedTest
    @MethodSource("provideNodeLocations")
    fun `test that returns correct location from repository`(
        location: NodeLocation,
    ) = runTest {
        whenever(nodeRepository.getNodeLocationById(nodeId)).thenReturn(location)

        val result = underTest(nodeId)

        assertThat(result).isEqualTo(location)
        verify(nodeRepository).getNodeLocationById(nodeId)
        verifyNoMoreInteractions(nodeRepository)
    }

    @Test
    fun `test that returns CloudDrive when repository returns null`() = runTest {
        whenever(nodeRepository.getNodeLocationById(nodeId)).thenReturn(null)

        val result = underTest(nodeId)

        assertThat(result).isEqualTo(NodeLocation.CloudDrive)
        verify(nodeRepository).getNodeLocationById(nodeId)
        verifyNoMoreInteractions(nodeRepository)
    }

    @Test
    fun `test that uses correct node id`() = runTest {
        val expectedLocation = NodeLocation.RubbishBin
        whenever(nodeRepository.getNodeLocationById(nodeId)).thenReturn(expectedLocation)

        val result = underTest(nodeId)

        assertThat(result).isEqualTo(expectedLocation)
        verify(nodeRepository).getNodeLocationById(nodeId)
    }

    @Test
    fun `test that works with different node ids`() = runTest {
        val expectedLocation1 = NodeLocation.CloudDriveRoot
        val expectedLocation2 = NodeLocation.IncomingShares
        whenever(nodeRepository.getNodeLocationById(nodeId)).thenReturn(expectedLocation1)
        whenever(nodeRepository.getNodeLocationById(otherNodeId)).thenReturn(expectedLocation2)

        val result1 = underTest(nodeId)
        val result2 = underTest(otherNodeId)

        assertThat(result1).isEqualTo(expectedLocation1)
        assertThat(result2).isEqualTo(expectedLocation2)
        verify(nodeRepository).getNodeLocationById(nodeId)
        verify(nodeRepository).getNodeLocationById(otherNodeId)
    }

    private fun provideNodeLocations() = Stream.of(
        Arguments.of(NodeLocation.CloudDriveRoot),
        Arguments.of(NodeLocation.CloudDrive),
        Arguments.of(NodeLocation.RubbishBin),
        Arguments.of(NodeLocation.IncomingSharesRoot),
        Arguments.of(NodeLocation.IncomingShares),
    )
}
