package mega.privacy.android.domain.usecase.node

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DisableExportNodesUseCaseTest {
    private lateinit var underTest: DisableExportNodesUseCase
    private val disableExportUseCase: DisableExportUseCase = mock()

    @BeforeAll
    fun setup() {
        underTest = DisableExportNodesUseCase(disableExportUseCase)
    }

    @BeforeEach
    fun resetMocks() {
        reset(disableExportUseCase)
    }

    @Test
    fun `test that when call disableExportUseCase successfully then result returns correctly`() =
        runTest {
            whenever(disableExportUseCase(NodeId(any()))).thenReturn(Unit)
            val actual = underTest(listOf(NodeId(1L)))
            Truth.assertThat(actual.errorCount).isEqualTo(0)
            Truth.assertThat(actual.successCount).isEqualTo(1)
        }

    @Test
    fun `test that when call disableExportUseCase failed then result returns correctly`() =
        runTest {
            whenever(disableExportUseCase(NodeId(any()))).thenThrow(RuntimeException::class.java)
            val actual = underTest(listOf(NodeId(1L)))
            Truth.assertThat(actual.errorCount).isEqualTo(1)
            Truth.assertThat(actual.successCount).isEqualTo(0)
        }

    @Test
    fun `test that when call disableExportUseCase failed or success then result returns correctly`() =
        runTest {
            whenever(disableExportUseCase(NodeId(1L))).thenReturn(Unit)
            whenever(disableExportUseCase(NodeId(2L))).thenThrow(RuntimeException::class.java)
            val actual = underTest(listOf(NodeId(1L), NodeId(2L)))
            Truth.assertThat(actual.errorCount).isEqualTo(1)
            Truth.assertThat(actual.successCount).isEqualTo(1)
        }
}