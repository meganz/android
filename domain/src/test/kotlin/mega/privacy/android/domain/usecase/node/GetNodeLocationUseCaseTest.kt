package mega.privacy.android.domain.usecase.node

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeLocation
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedFileNode
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetNodeLocationUseCaseTest {

    private lateinit var underTest: GetNodeLocationUseCase

    private val isNodeInCloudDriveUseCase = mock<IsNodeInCloudDriveUseCase>()
    private val isNodeInRubbishBinUseCase = mock<IsNodeInRubbishBinUseCase>()
    private val getAncestorsIdsUseCase = mock<GetAncestorsIdsUseCase>()

    private val nodeId = NodeId(100L)
    private val node = mock<TypedFileNode> {
        on { id } doReturn nodeId
    }

    @BeforeAll
    fun setup() {
        underTest = GetNodeLocationUseCase(
            isNodeInCloudDriveUseCase = isNodeInCloudDriveUseCase,
            isNodeInRubbishBinUseCase = isNodeInRubbishBinUseCase,
            getAncestorsIdsUseCase = getAncestorsIdsUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            isNodeInCloudDriveUseCase,
            isNodeInRubbishBinUseCase,
            getAncestorsIdsUseCase,
        )
    }

    @ParameterizedTest
    @EnumSource(NodeSourceType::class, names = ["CLOUD_DRIVE", "RUBBISH_BIN", "INCOMING_SHARES"])
    fun `test that correct node location is returned`(
        nodeSourceType: NodeSourceType,
    ) = runTest {
        val ancestorIds = listOf(NodeId(200L), NodeId(300L), NodeId(400L))
        val expected = NodeLocation(
            node = node,
            nodeSourceType = nodeSourceType,
            ancestorIds = ancestorIds.dropLast(if (nodeSourceType == NodeSourceType.INCOMING_SHARES) 0 else 1),
        )

        when (nodeSourceType) {
            NodeSourceType.CLOUD_DRIVE -> {
                whenever(isNodeInCloudDriveUseCase(nodeId.longValue)) doReturn true
                whenever(getAncestorsIdsUseCase(node)) doReturn ancestorIds
            }

            NodeSourceType.RUBBISH_BIN -> {
                whenever(isNodeInCloudDriveUseCase(nodeId.longValue)) doReturn false
                whenever(isNodeInRubbishBinUseCase(nodeId)) doReturn true
                whenever(getAncestorsIdsUseCase(node)) doReturn ancestorIds
            }

            else -> {
                whenever(isNodeInCloudDriveUseCase(nodeId.longValue)) doReturn false
                whenever(isNodeInRubbishBinUseCase(nodeId)) doReturn false
                whenever(getAncestorsIdsUseCase(node)) doReturn ancestorIds
            }
        }

        assertThat(underTest(node)).isEqualTo(expected)
    }
}