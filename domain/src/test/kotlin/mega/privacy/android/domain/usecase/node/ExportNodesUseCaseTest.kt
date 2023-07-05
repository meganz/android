package mega.privacy.android.domain.usecase.node

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ExportNodesUseCaseTest {
    private lateinit var underTest: ExportNodesUseCase

    private val exportNodeUseCase: ExportNodeUseCase = mock()

    @BeforeAll
    fun setUp() {
        underTest = ExportNodesUseCase(
            exportNodeUseCase = exportNodeUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(exportNodeUseCase)
    }


    @Test
    fun `test that list of nodes is successfully Exported`() =
        runTest {
            val nodeHandle1 = 1L
            val link1 = "result1"
            val nodeHandle2 = 2L
            val link2 = "result2"
            whenever(exportNodeUseCase(NodeId(nodeHandle1))).thenReturn(link1)
            whenever(exportNodeUseCase(NodeId(nodeHandle2))).thenReturn(link2)
            val expected = mapOf(nodeHandle1 to link1, nodeHandle2 to link2)
            val actual = underTest.invoke(listOf(nodeHandle1, nodeHandle2))
            assertThat(actual).isEqualTo(expected)
        }


    @Test
    fun `test that result of ExportNodes only contains node entries of successfully exported nodes`() =
        runTest {
            val nodeHandle1 = 1L
            val link1 = "result1"
            val nodeHandle2 = 2L
            whenever(exportNodeUseCase(NodeId(nodeHandle1))).thenReturn(link1)
            whenever(exportNodeUseCase(NodeId(nodeHandle2))).thenAnswer {
                throw IllegalArgumentException("Node not found")
            }
            val expected = mapOf(nodeHandle1 to link1)
            val actual = underTest.invoke(listOf(nodeHandle1, nodeHandle2))
            assertThat(actual).isEqualTo(expected)
        }


    @Test
    fun `test that result of ExportNodes is empty when none of nodes are exported`() =
        runTest {
            val nodeHandle1 = 1L
            val nodeHandle2 = 2L
            whenever(exportNodeUseCase(NodeId(nodeHandle1))).thenAnswer {
                throw IllegalArgumentException("Node not found")
            }
            whenever(exportNodeUseCase(NodeId(nodeHandle2))).thenAnswer {
                throw IllegalArgumentException("Node not found")
            }
            val actual = underTest.invoke(listOf(nodeHandle1, nodeHandle2))
            assertThat(actual).isEmpty()
        }
}