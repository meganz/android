package mega.privacy.android.domain.usecase.file

import com.google.common.truth.Truth
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.uri.UriPath
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DoesUriPathHaveSufficientSpaceForNodesUseCaseTest {
    private lateinit var underTest: DoesUriPathHaveSufficientSpaceForNodesUseCase

    private val totalFileSizeOfNodesUseCase = mock<TotalFileSizeOfNodesUseCase>()
    private val doesUriPathHaveSufficientSpaceUseCase =
        mock<DoesUriPathHaveSufficientSpaceUseCase>()
    private val nodes = mock<List<TypedNode>>()

    @BeforeAll
    fun setup() {
        underTest = DoesUriPathHaveSufficientSpaceForNodesUseCase(
            totalFileSizeOfNodesUseCase,
            doesUriPathHaveSufficientSpaceUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() =
        reset(totalFileSizeOfNodesUseCase, doesUriPathHaveSufficientSpaceUseCase, nodes)

    @Test
    fun `test that totalFileSizeOfNodesUseCase is called with the list of nodes`() = runTest {
        stubTotalFileSize()
        underTest(uriPath, nodes)
        verify(totalFileSizeOfNodesUseCase).invoke(nodes)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test that doesPathHaveSufficientSpaceUseCase result is returned`(expectedHaveEnoughSpace: Boolean) =
        runTest {
            stubTotalFileSize()
            whenever(
                doesUriPathHaveSufficientSpaceUseCase.invoke(
                    UriPath(PATH),
                    TOTAL_SIZE
                )
            ).thenReturn(
                expectedHaveEnoughSpace
            )
            val actual = underTest(uriPath, nodes)
            Truth.assertThat(actual).isEqualTo(expectedHaveEnoughSpace)
        }

    private suspend fun stubTotalFileSize() {
        whenever(totalFileSizeOfNodesUseCase(any())).thenReturn(TOTAL_SIZE)
    }

    companion object {
        private const val PATH = "/root"
        private val uriPath = UriPath(PATH)
        private const val TOTAL_SIZE = 1024L
    }
}