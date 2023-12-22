package mega.privacy.android.domain.usecase.transfers.downloads

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.usecase.GetNodeByIdUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetDownloadLocationForNodeIdUseCaseTest {
    private lateinit var underTest: GetDownloadLocationForNodeIdUseCase

    private val getNodeByIdUseCase = mock<GetNodeByIdUseCase>()
    private val getDownloadLocationForNodeUseCase = mock<GetDownloadLocationForNodeUseCase>()


    @BeforeAll
    fun setup() {
        underTest = GetDownloadLocationForNodeIdUseCase(
            getNodeByIdUseCase,
            getDownloadLocationForNodeUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            getNodeByIdUseCase,
            getDownloadLocationForNodeUseCase,
        )
    }

    @Test
    fun `test that location for node is returned when node exists`() = runTest {
        val nodeId = NodeId(11L)
        val node = mock<TypedFileNode>()
        val location = "location"
        whenever(getNodeByIdUseCase(nodeId)).thenReturn(node)
        whenever(getDownloadLocationForNodeUseCase(node)).thenReturn(location)
        assertThat(underTest(nodeId)).isEqualTo(location)
    }

    @Test
    fun `test that null is returned when node does not exist`() = runTest {
        whenever(getNodeByIdUseCase(NodeId(any()))).thenReturn(null)
        assertThat(underTest(NodeId(11L))).isNull()
        verifyNoInteractions(getDownloadLocationForNodeUseCase)
    }
}
