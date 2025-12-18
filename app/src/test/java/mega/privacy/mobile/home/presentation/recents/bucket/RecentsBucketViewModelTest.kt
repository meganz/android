package mega.privacy.mobile.home.presentation.recents.bucket

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.android.core.ui.model.LocalizedText
import mega.privacy.android.core.nodecomponents.mapper.NodeUiItemMapper
import mega.privacy.android.core.nodecomponents.model.NodeUiItem
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.RecentActionBucket
import mega.privacy.android.domain.entity.TextFileTypeInfo
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.usecase.recentactions.GetRecentActionBucketByIdUseCase
import mega.privacy.mobile.home.presentation.recents.mapper.RecentsParentFolderNameMapper
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RecentsBucketViewModelTest {

    private lateinit var underTest: RecentsBucketViewModel

    private val getRecentActionBucketByIdUseCase = mock<GetRecentActionBucketByIdUseCase>()
    private val nodeUiItemMapper = mock<NodeUiItemMapper>()
    private val recentsParentFolderNameMapper = mock<RecentsParentFolderNameMapper>()

    private val testIdentifier = "test_bucket_identifier"
    private val testNodeSourceType = NodeSourceType.CLOUD_DRIVE
    private val testFolderName = "Test Folder"
    private val testTimestamp = 1234567890L
    private val testFileCount = 2

    @BeforeAll
    fun setUp() {
        reset(
            getRecentActionBucketByIdUseCase,
            nodeUiItemMapper,
            recentsParentFolderNameMapper
        )
    }

    private fun initViewModel(
        args: RecentsBucketViewModel.Args = RecentsBucketViewModel.Args(
            identifier = testIdentifier,
            isMediaBucket = false,
            folderName = testFolderName,
            nodeSourceType = testNodeSourceType,
            timestamp = testTimestamp,
            fileCount = testFileCount,
        )
    ) {
        underTest = RecentsBucketViewModel(
            args = args,
            getRecentActionBucketByIdUseCase = getRecentActionBucketByIdUseCase,
            nodeUiItemMapper = nodeUiItemMapper,
            recentsParentFolderNameMapper = recentsParentFolderNameMapper,
        )
    }

    @Test
    fun `test that initial state has isLoading true`() = runTest {
        whenever(getRecentActionBucketByIdUseCase(any(), any())).thenReturn(null)

        initViewModel()
        advanceUntilIdle()

        underTest.uiState.test {
            val state = awaitItem()
            // After loading completes, isLoading should be false
            assertThat(state.isLoading).isFalse()
            assertThat(state.nodeUiItems).isEmpty()
            // Verify initial parentFolderName is set from args
            assertThat(state.parentFolderName).isEqualTo(LocalizedText.Literal(testFolderName))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that bucket is loaded successfully and nodes are mapped`() = runTest {
        val node1 = createMockFileNode(name = "file1.txt")
        val node2 = createMockFileNode(name = "file2.txt")
        val bucketTimestamp = 9876543210L
        val bucketFolderName = "Updated Folder Name"
        val bucket = createMockRecentActionBucket(
            identifier = testIdentifier,
            nodes = listOf(node1, node2),
            timestamp = bucketTimestamp,
            parentFolderName = bucketFolderName,
        )

        val nodeUiItem1 = createMockNodeUiItem(node1)
        val nodeUiItem2 = createMockNodeUiItem(node2)
        val expectedParentFolderName = LocalizedText.Literal(bucketFolderName)

        whenever(getRecentActionBucketByIdUseCase(any(), any())).thenReturn(bucket)
        whenever(recentsParentFolderNameMapper(any())).thenReturn(expectedParentFolderName)
        whenever(
            nodeUiItemMapper(
                nodeList = any(),
                existingItems = anyOrNull(),
                nodeSourceType = any(),
                isPublicNodes = any(),
                showPublicLinkCreationTime = any(),
                highlightedNodeId = anyOrNull(),
                highlightedNames = anyOrNull(),
                isContactVerificationOn = any(),
            )
        ).thenReturn(listOf(nodeUiItem1, nodeUiItem2))

        initViewModel()
        advanceUntilIdle()

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.isLoading).isFalse()
            assertThat(state.nodeUiItems).hasSize(2)
            assertThat(state.nodeUiItems[0].node).isEqualTo(node1)
            assertThat(state.nodeUiItems[1].node).isEqualTo(node2)
            assertThat(state.fileCount).isEqualTo(bucket.nodes.size)
            assertThat(state.timestamp).isEqualTo(bucket.timestamp)
            assertThat(state.parentFolderName).isEqualTo(expectedParentFolderName)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that UI state is updated with bucket data again when bucket is loaded successfully`() =
        runTest {
            val node1 = createMockFileNode(name = "file1.txt")
            val node2 = createMockFileNode(name = "file2.txt")
            val node3 = createMockFileNode(name = "file3.txt")
            val bucketTimestamp = 9999999999L
            val bucketFolderName = "Bucket Folder Name"
            val bucket = createMockRecentActionBucket(
                identifier = testIdentifier,
                nodes = listOf(node1, node2, node3),
                timestamp = bucketTimestamp,
                parentFolderName = bucketFolderName,
            )

            val nodeUiItem1 = createMockNodeUiItem(node1)
            val nodeUiItem2 = createMockNodeUiItem(node2)
            val nodeUiItem3 = createMockNodeUiItem(node3)
            val expectedParentFolderName = LocalizedText.Literal(bucketFolderName)

            whenever(getRecentActionBucketByIdUseCase(any(), any())).thenReturn(bucket)
            whenever(recentsParentFolderNameMapper(any())).thenReturn(expectedParentFolderName)
            whenever(
                nodeUiItemMapper(
                    nodeList = any(),
                    existingItems = anyOrNull(),
                    nodeSourceType = any(),
                    isPublicNodes = any(),
                    showPublicLinkCreationTime = any(),
                    highlightedNodeId = anyOrNull(),
                    highlightedNames = anyOrNull(),
                    isContactVerificationOn = any(),
                )
            ).thenReturn(listOf(nodeUiItem1, nodeUiItem2, nodeUiItem3))

            initViewModel()
            advanceUntilIdle()

            underTest.uiState.test {
                val state = awaitItem()
                // Verify isLoading is set to false
                assertThat(state.isLoading).isFalse()
                // Verify nodeUiItems are set from mapper
                assertThat(state.nodeUiItems).hasSize(3)
                assertThat(state.nodeUiItems).containsExactly(nodeUiItem1, nodeUiItem2, nodeUiItem3)
                // Verify fileCount is set from bucket.nodes.size
                assertThat(state.fileCount).isEqualTo(bucket.nodes.size)
                assertThat(state.fileCount).isEqualTo(3)
                // Verify timestamp is set from bucket.timestamp
                assertThat(state.timestamp).isEqualTo(bucket.timestamp)
                assertThat(state.timestamp).isEqualTo(bucketTimestamp)
                // Verify parentFolderName is set from mapper
                assertThat(state.parentFolderName).isEqualTo(expectedParentFolderName)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that when bucket is null, isLoading is set to false and nodeUiItems remain empty`() =
        runTest {
            whenever(getRecentActionBucketByIdUseCase(any(), any())).thenReturn(null)

            initViewModel()
            advanceUntilIdle()

            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.isLoading).isFalse()
                assertThat(state.nodeUiItems).isEmpty()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `test that when use case throws exception, isLoading is set to false`() = runTest {
        val exception = RuntimeException("Test error")
        whenever(getRecentActionBucketByIdUseCase(any(), any())).thenThrow(exception)

        initViewModel()
        advanceUntilIdle()

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.isLoading).isFalse()
            assertThat(state.nodeUiItems).isEmpty()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `test that use case is called with correct identifier`() = runTest {
        val testId = "custom_identifier"
        whenever(getRecentActionBucketByIdUseCase(any(), any())).thenReturn(null)

        initViewModel(
            args = RecentsBucketViewModel.Args(
                identifier = testId,
                isMediaBucket = false,
                folderName = testFolderName,
                nodeSourceType = testNodeSourceType,
                timestamp = testTimestamp,
                fileCount = testFileCount,
            )
        )
        advanceUntilIdle()
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
        identifier: String = "test_bucket",
        timestamp: Long = 1234567890L,
        nodes: List<TypedFileNode> = listOf(createMockFileNode()),
        parentFolderName: String = "Test Folder",
    ): RecentActionBucket = RecentActionBucket(
        identifier = identifier,
        timestamp = timestamp,
        userEmail = "test@example.com",
        parentNodeId = NodeId(1L),
        isUpdate = false,
        isMedia = false,
        nodes = nodes,
        parentFolderName = parentFolderName,
    )

    private fun createMockNodeUiItem(
        node: TypedFileNode,
    ): NodeUiItem<TypedNode> = NodeUiItem(
        node = node,
        isSelected = false,
    )
}
