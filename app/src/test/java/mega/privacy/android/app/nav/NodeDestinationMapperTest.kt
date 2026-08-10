package mega.privacy.android.app.nav

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.core.nodecomponents.mapper.NodeDestinationMapper
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeLocation
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.usecase.GetDeviceCurrentTimeUseCase
import mega.privacy.android.navigation.destination.CloudDriveNavKey
import mega.privacy.android.navigation.destination.DriveSyncNavKey
import mega.privacy.android.navigation.destination.HomeScreensNavKey
import mega.privacy.android.navigation.destination.RubbishBinNavKey
import mega.privacy.android.navigation.destination.SharesNavKey
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class NodeDestinationMapperTest {
    private lateinit var underTest: NodeDestinationMapper

    private val nodeId = NodeId(100L)
    private val node = mock<TypedFileNode> {
        on { id } doReturn nodeId
    }
    private val now = 234541346L
    private val getDeviceCurrentTimeUseCase: GetDeviceCurrentTimeUseCase = mock {
        on { invoke() } doReturn now
    }

    @BeforeAll
    fun setup() {
        underTest = NodeDestinationMapper(getDeviceCurrentTimeUseCase)
    }

    @ParameterizedTest
    @EnumSource(NodeSourceType::class, names = ["CLOUD_DRIVE", "RUBBISH_BIN", "INCOMING_SHARES"])
    fun `test that if node is in root node, ancestor nav keys are not added`(
        nodeSourceType: NodeSourceType,
    ) {
        val nodeLocation = NodeLocation(
            node = node,
            ancestorIds = emptyList(),
            nodeSourceType = nodeSourceType
        )
        val expected = listOf(
            when (nodeSourceType) {
                NodeSourceType.CLOUD_DRIVE -> HomeScreensNavKey(
                    root = DriveSyncNavKey(highlightedNodeHandle = nodeId.longValue),
                    destinations = null,
                    timestamp = now
                )

                NodeSourceType.RUBBISH_BIN -> RubbishBinNavKey(highlightedNodeHandle = nodeId.longValue)
                else -> SharesNavKey
            }
        )

        assertThat(underTest.invoke(nodeLocation)).isEqualTo(expected)
    }

    @ParameterizedTest
    @EnumSource(NodeSourceType::class, names = ["CLOUD_DRIVE", "RUBBISH_BIN", "INCOMING_SHARES"])
    fun `test that if node is NOT in root node, ancestor nav keys are also added`(
        nodeSourceType: NodeSourceType,
    ) {
        val ancestorIds = listOf(NodeId(200L), NodeId(300L))
        val nodeLocation = NodeLocation(
            node = node,
            ancestorIds = ancestorIds,
            nodeSourceType = nodeSourceType
        )
        val childDestinations = buildList {
            addAll(
                ancestorIds
                    .mapIndexed { index, parentId ->
                        CloudDriveNavKey(
                            nodeHandle = parentId.longValue,
                            highlightedNodeHandle = if (index == 0) node.id.longValue else null,
                            nodeSourceType = nodeSourceType,
                        )
                    }
            )
        }.reversed()
        val expected = buildList {
            when (nodeSourceType) {
                NodeSourceType.CLOUD_DRIVE -> {
                    add(
                        HomeScreensNavKey(
                            root = DriveSyncNavKey(),
                            destinations = childDestinations,
                            timestamp = now
                        )
                    )
                }

                NodeSourceType.RUBBISH_BIN -> {
                    add(RubbishBinNavKey())
                    addAll(childDestinations)
                }

                else -> {
                    add(SharesNavKey)
                    addAll(childDestinations)
                }
            }
        }

        assertThat(underTest.invoke(nodeLocation)).isEqualTo(expected)
    }
}