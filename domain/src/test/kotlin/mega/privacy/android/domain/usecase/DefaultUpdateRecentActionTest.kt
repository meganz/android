package mega.privacy.android.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.RecentActionBucket
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.usecase.recentactions.GetRecentActionsUseCase
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultUpdateRecentActionTest {
    private lateinit var underTest: UpdateRecentAction
    private val getRecentActionsUseCase = mock<GetRecentActionsUseCase>()

    @Before
    fun setUp() {
        underTest = DefaultUpdateRecentAction(
            getRecentActionsUseCase = getRecentActionsUseCase,
            ioDispatcher = UnconfinedTestDispatcher(),
        )
    }

    private fun createBucket(
        isMedia: Boolean,
        isUpdate: Boolean,
        timestamp: Long,
        parentNodeId: NodeId,
        userEmail: String,
    ): RecentActionBucket =
        RecentActionBucket(
            isMedia = isMedia,
            isUpdate = isUpdate,
            timestamp = timestamp,
            parentNodeId = parentNodeId,
            userEmail = userEmail,
            nodes = emptyList(),
        )

    @Test
    fun `test that is same bucket return true if two buckets have same properties`() = runTest {
        val bucket1 = createBucket(isMedia = true, isUpdate = true, 0L, NodeId(1L), "1")
        val bucket2 = createBucket(isMedia = true, isUpdate = true, 0L, NodeId(1L), "1")
        val bucket3 = createBucket(isMedia = false, isUpdate = false, 0L, NodeId(1L), "1")
        val bucket4 = createBucket(isMedia = true, isUpdate = false, 0L, NodeId(1L), "1")
        val bucket5 = createBucket(isMedia = true, isUpdate = true, 1L, NodeId(1L), "1")
        val bucket6 = createBucket(isMedia = true, isUpdate = true, 0L, NodeId(0L), "1")
        val bucket7 = createBucket(isMedia = true, isUpdate = true, 0L, NodeId(1L), "2")

        assertThat(bucket1.identifier == bucket2.identifier).isEqualTo(true)
        assertThat(bucket1.identifier == bucket3.identifier).isEqualTo(false)
        assertThat(bucket1.identifier == bucket4.identifier).isEqualTo(false)
        assertThat(bucket1.identifier == bucket5.identifier).isEqualTo(false)
        assertThat(bucket1.identifier == bucket6.identifier).isEqualTo(false)
        assertThat(bucket1.identifier == bucket7.identifier).isEqualTo(false)
    }

    @Test
    fun `test that when updated action list contains the current action, then return this action`() =
        runTest {
            val expected = createBucket(isMedia = true, isUpdate = true, 0L, NodeId(1L), "1")
            val list = listOf(expected, expected, expected)
            whenever(getRecentActionsUseCase(any())).thenReturn(list)

            assertThat(underTest.invoke(expected, null, false)).isEqualTo(expected)
        }

    @Test
    fun `test that when updated action list does not contains the current action, then return the bucket that differs from all the items of the cached list`() =
        runTest {
            val current = createBucket(isMedia = true, isUpdate = true, 0L, NodeId(0L), "1")

            val item1 = createBucket(isMedia = true, isUpdate = true, 0L, NodeId(2L), "1")
            val item2 = createBucket(isMedia = true, isUpdate = true, 0L, NodeId(3L), "1")

            val expected = createBucket(isMedia = true, isUpdate = true, 1L, NodeId(0L), "1")

            val cachedActionList = listOf(current, item1, item2)

            val list = listOf(expected, item1, item2)
            whenever(getRecentActionsUseCase(any())).thenReturn(list)

            assertThat(underTest.invoke(current, cachedActionList, false)).isEqualTo(expected)
        }

    @Test
    fun `test that when updated actions list does not contains the current action, and no item from the updated list differs from the cached list, then return null`() =
        runTest {
            val current = createBucket(isMedia = true, isUpdate = true, 0L, NodeId(0L), "1")

            val item1 = createBucket(isMedia = true, isUpdate = true, 0L, NodeId(2L), "1")
            val item2 = createBucket(isMedia = true, isUpdate = true, 0L, NodeId(3L), "1")

            val cachedActionList = listOf(current, item1, item2)

            val list = listOf(item1, item2)
            whenever(getRecentActionsUseCase(any())).thenReturn(list)

            assertThat(underTest.invoke(current, cachedActionList, false)).isEqualTo(null)
        }
}
