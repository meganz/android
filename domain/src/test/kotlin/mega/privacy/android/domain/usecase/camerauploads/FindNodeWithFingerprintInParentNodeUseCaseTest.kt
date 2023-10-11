package mega.privacy.android.domain.usecase.camerauploads

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.usecase.IsNodeInRubbish
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import java.util.stream.Stream

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FindNodeWithFingerprintInParentNodeUseCaseTest {

    private lateinit var underTest: FindNodeWithFingerprintInParentNodeUseCase

    private val getNodeFromCloudUseCase = mock<GetNodeFromCloudUseCase>()
    private val isNodeInRubbishBin = mock<IsNodeInRubbish>()

    @BeforeAll
    fun setUp() {
        underTest = FindNodeWithFingerprintInParentNodeUseCase(
            getNodeFromCloudUseCase = getNodeFromCloudUseCase,
            isNodeInRubbish = isNodeInRubbishBin,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            getNodeFromCloudUseCase,
            isNodeInRubbishBin,
        )
    }

    @ParameterizedTest(name = "when isNodeInParentNode is {0} and isNodeInOtherNode is {1} and isNodeInRubbishBin is {2}, a pair with {3} and the NodeId is returned")
    @MethodSource("provideParameters")
    fun `test that the node exists in the parent folder`(
        isNodeInParentNode: Boolean,
        isNodeInOtherNode: Boolean,
        isNodeInRubbishBin: Boolean,
        expectedIsNodeExistingInParentFolder: Boolean?,
    ) = runTest {
        val fingerprint = "fingerprint"
        val generatedFingerprint = "generatedFingerprint"

        val parentNodeId = mock<NodeId> {
            on { longValue }.thenReturn(1111L)
        }

        val expectedNode =
            when {
                isNodeInParentNode -> {
                    mock<TypedFileNode> {
                        on { id }.thenReturn(NodeId(1234L))
                        on { parentId }.thenReturn(NodeId(1111L))
                    }
                }

                isNodeInOtherNode || isNodeInRubbishBin -> {
                    mock {
                        on { id }.thenReturn(NodeId(1234L))
                        on { parentId }.thenReturn(NodeId(2222L))
                    }
                }

                else -> null
            }

        whenever(getNodeFromCloudUseCase(fingerprint, generatedFingerprint, parentNodeId))
            .thenReturn(expectedNode)
        expectedNode?.let {
            whenever(isNodeInRubbishBin(it.id.longValue)).thenReturn(isNodeInRubbishBin)
        }

        assertThat(underTest(fingerprint, generatedFingerprint, parentNodeId))
            .isEqualTo(Pair(expectedIsNodeExistingInParentFolder, expectedNode?.id))
    }

    private fun provideParameters() = Stream.of(
        Arguments.of(true, false, false, true),
        Arguments.of(false, true, false, false),
        Arguments.of(false, false, true, null),
        Arguments.of(false, false, false, false),
    )
}
