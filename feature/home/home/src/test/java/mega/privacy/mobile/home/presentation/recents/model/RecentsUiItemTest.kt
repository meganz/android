package mega.privacy.mobile.home.presentation.recents.model

import com.google.common.truth.Truth.assertThat
import mega.android.core.ui.model.LocalizedText
import mega.privacy.android.domain.entity.RecentActionBucket
import mega.privacy.android.domain.entity.RecentActionsSharesType
import mega.privacy.android.domain.entity.TextFileTypeInfo
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.icon.pack.R as IconPackR
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RecentsUiItemTest {

    @ParameterizedTest(name = "firstNode returns expected node when bucket has {0} node(s)")
    @MethodSource("provideFirstNodeTestCases")
    fun `test that firstNode returns correct node based on bucket nodes`(
        nodeCount: Int,
        nodes: List<TypedFileNode>,
        expectedNode: TypedFileNode?,
    ) {
        val bucket = createMockRecentActionBucket(nodes = nodes)
        val item = createRecentsUiItem(bucket = bucket)

        assertThat(item.firstNode).isEqualTo(expectedNode)
    }

    @ParameterizedTest(name = "nodeSourceType returns {1} when parentFolderSharesType is {0}")
    @MethodSource("provideNodeSourceTypeTestCases")
    fun `test that nodeSourceType returns correct type based on parentFolderSharesType`(
        sharesType: RecentActionsSharesType,
        expectedSourceType: NodeSourceType,
    ) {
        val bucket = createMockRecentActionBucket(
            parentFolderSharesType = sharesType
        )
        val item = createRecentsUiItem(bucket = bucket)

        assertThat(item.nodeSourceType).isEqualTo(expectedSourceType)
    }

    @ParameterizedTest(name = "key returns {3} when isSingleNode is {0}")
    @MethodSource("provideKeyTestCases")
    fun `test that key returns correct value based on isSingleNode and firstNode`(
        isSingleNode: Boolean,
        identifier: String,
        nodes: List<TypedFileNode>,
        expectedKey: String,
    ) {
        val bucket = createMockRecentActionBucket(
            identifier = identifier,
            nodes = nodes
        )
        val item = createRecentsUiItem(
            bucket = bucket,
            isSingleNode = isSingleNode
        )

        assertThat(item.key).isEqualTo(expectedKey)
    }

    companion object {
        @JvmStatic
        fun provideFirstNodeTestCases(): Stream<Arguments> {
            val node1 = createMockFileNodeStatic(name = "file1.txt")
            val node2 = createMockFileNodeStatic(name = "file2.txt")
            val singleNode = createMockFileNodeStatic(name = "single.txt")

            return Stream.of(
                Arguments.of(0, emptyList<TypedFileNode>(), null),
                Arguments.of(1, listOf(singleNode), singleNode),
                Arguments.of(2, listOf(node1, node2), node1),
            )
        }

        @JvmStatic
        fun provideNodeSourceTypeTestCases(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(
                    RecentActionsSharesType.INCOMING_SHARES,
                    NodeSourceType.INCOMING_SHARES
                ),
                Arguments.of(
                    RecentActionsSharesType.NONE,
                    NodeSourceType.CLOUD_DRIVE
                ),
                Arguments.of(
                    RecentActionsSharesType.OUTGOING_SHARES,
                    NodeSourceType.CLOUD_DRIVE
                ),
                Arguments.of(
                    RecentActionsSharesType.PENDING_OUTGOING_SHARES,
                    NodeSourceType.CLOUD_DRIVE
                ),
            )
        }

        @JvmStatic
        fun provideKeyTestCases(): Stream<Arguments> {
            val nodeWithId123 = createMockFileNodeStatic(name = "file1.txt", nodeId = 123L)
            val nodeWithId456 = createMockFileNodeStatic(name = "file2.txt", nodeId = 456L)

            return Stream.of(
                Arguments.of(
                    false,
                    "bucket-identifier-1",
                    listOf(nodeWithId123),
                    "bucket-identifier-1"
                ),
                Arguments.of(
                    false,
                    "bucket-identifier-3",
                    listOf(nodeWithId123, nodeWithId456),
                    "bucket-identifier-3"
                )
            )
        }

        private fun createMockFileNodeStatic(
            name: String = "testFile.txt",
            nodeId: Long = 1L,
        ): TypedFileNode = mock {
            on { it.name }.thenReturn(name)
            on { it.id }.thenReturn(NodeId(nodeId))
            val fileTypeInfo = TextFileTypeInfo("text/plain", "txt")
            on { it.type }.thenReturn(fileTypeInfo)
        }
    }

    private fun createMockFileNode(
        name: String = "testFile.txt",
    ): TypedFileNode = mock {
        on { it.name }.thenReturn(name)
        on { it.id }.thenReturn(NodeId(1L))
        val fileTypeInfo = TextFileTypeInfo("text/plain", "txt")
        on { it.type }.thenReturn(fileTypeInfo)
    }

    private fun createMockRecentActionBucket(
        identifier: String = "test-identifier",
        nodes: List<TypedFileNode> = listOf(createMockFileNode()),
        parentFolderSharesType: RecentActionsSharesType = RecentActionsSharesType.NONE,
    ): RecentActionBucket = mock {
        on { it.identifier }.thenReturn(identifier)
        on { it.nodes }.thenReturn(nodes)
        on { it.timestamp }.thenReturn(1234567890L)
        on { it.dateTimestamp }.thenReturn(1234567890L)
        on { it.userEmail }.thenReturn("test@example.com")
        on { it.parentNodeId }.thenReturn(NodeId(1L))
        on { it.isUpdate }.thenReturn(false)
        on { it.isMedia }.thenReturn(false)
        on { it.parentFolderSharesType }.thenReturn(parentFolderSharesType)
    }

    private fun createRecentsUiItem(
        bucket: RecentActionBucket,
        isSingleNode: Boolean = true,
    ): RecentsUiItem {
        return RecentsUiItem(
            title = RecentActionTitleText.SingleNode("test.txt"),
            icon = IconPackR.drawable.ic_generic_medium_solid,
            shareIcon = null,
            parentFolderName = LocalizedText.Literal("Test Folder"),
            isMediaBucket = false,
            isUpdate = false,
            updatedByText = null,
            userName = null,
            isFavourite = false,
            nodeLabel = null,
            bucket = bucket,
            isSingleNode = isSingleNode,
            isSensitive = false
        )
    }
}

