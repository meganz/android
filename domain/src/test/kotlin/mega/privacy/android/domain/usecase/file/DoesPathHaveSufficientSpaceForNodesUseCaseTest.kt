package mega.privacy.android.domain.usecase.file

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.Node
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DoesPathHaveSufficientSpaceForNodesUseCaseTest {
    private lateinit var underTest: DoesPathHaveSufficientSpaceForNodesUseCase

    private val totalFileSizeOfNodesUseCase = mock<TotalFileSizeOfNodesUseCase>()
    private val doesPathHaveSufficientSpaceUseCase = mock<DoesPathHaveSufficientSpaceUseCase>()
    private val nodes = mock<List<Node>>()


    @BeforeAll
    fun setup() {
        underTest = DoesPathHaveSufficientSpaceForNodesUseCase(
            totalFileSizeOfNodesUseCase,
            doesPathHaveSufficientSpaceUseCase
        )
    }

    @BeforeEach
    fun resetMocks() = reset(totalFileSizeOfNodesUseCase, doesPathHaveSufficientSpaceUseCase, nodes)

    @Test
    fun `test that totalFileSizeOfNodesUseCase is called with the list of nodes`() = runTest {
        stubTotalFileSize()
        underTest(PATH, nodes)
        verify(totalFileSizeOfNodesUseCase).invoke(nodes)
    }

    @Test
    fun `test that doesPathHaveSufficientSpaceUseCase is called with the proper path`() = runTest {
        stubTotalFileSize()
        underTest(PATH, nodes)
        verify(doesPathHaveSufficientSpaceUseCase).invoke(eq(PATH), any())
    }

    @Test
    fun `test that doesPathHaveSufficientSpaceUseCase is called with the resulting nodes size`() =
        runTest {
            stubTotalFileSize()
            underTest(PATH, nodes)
            verify(doesPathHaveSufficientSpaceUseCase).invoke(any(), eq(TOTAL_SIZE))
        }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that doesPathHaveSufficientSpaceUseCase result is returned`(expectedHaveEnoughSpace: Boolean) =
        runTest {
            stubTotalFileSize()
            whenever(doesPathHaveSufficientSpaceUseCase.invoke(PATH, TOTAL_SIZE)).thenReturn(
                expectedHaveEnoughSpace
            )
            val actual = underTest(PATH, nodes)
            Truth.assertThat(actual).isEqualTo(expectedHaveEnoughSpace)
        }

    private suspend fun stubTotalFileSize() {
        whenever(totalFileSizeOfNodesUseCase(any())).thenReturn(TOTAL_SIZE)
    }

    companion object {
        private const val PATH = "/root"
        private const val TOTAL_SIZE = 1024L
    }
}