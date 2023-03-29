package mega.privacy.android.data.mapper

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.mapper.recentactions.RecentActionBucketMapper
import mega.privacy.android.data.mapper.recentactions.RecentActionBucketMapperImpl
import mega.privacy.android.domain.entity.PdfFileTypeInfo
import mega.privacy.android.domain.entity.RecentActionBucketUnTyped
import nz.mega.sdk.MegaNode
import nz.mega.sdk.MegaNodeList
import nz.mega.sdk.MegaRecentActionBucket
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock

@OptIn(ExperimentalCoroutinesApi::class)
class RecentActionBucketMapperTest {
    private lateinit var underTest: RecentActionBucketMapper

    private val expectedName = "testName"
    private val expectedSize = 1000L
    private val expectedLabel = MegaNode.NODE_LBL_RED
    private val expectedId = 1L
    private val expectedParentId = 2L
    private val expectedBase64Id = "1L"
    private val expectedModificationTime = 123L

    private val megaNode = getMockNode(isFile = true)
    private val megaNodeList = mock<MegaNodeList> {
        on { size() }.thenReturn(10)
        on { get(any()) }.thenReturn(megaNode)
    }
    private val nodeMapper = mock<NodeMapper>()

    private val recentActionBucket = mock<MegaRecentActionBucket> {
        on { timestamp }.thenReturn(12L)
        on { userEmail }.thenReturn("something@email.com")
        on { parentHandle }.thenReturn(1L)
        on { isUpdate }.thenReturn(true)
        on { isMedia }.thenReturn(false)
        on { nodes }.thenReturn(megaNodeList)
    }

    @Before
    fun setUp() {
        underTest = RecentActionBucketMapperImpl(nodeMapper)
    }

    @Test
    fun `test that mapper returns correct value`() = runTest {
        val actual = underTest.invoke(
            recentActionBucket,
            thumbnailPath = { null },
            hasVersion = { false },
            numberOfChildFolders = { 0 },
            numberOfChildFiles = { 1 },
            isInRubbish = { false },
            fileTypeInfoMapper = { PdfFileTypeInfo },
            isPendingShare = { false },
        )
        assertThat(actual).isInstanceOf(RecentActionBucketUnTyped::class.java)
        assertThat(actual.timestamp).isEqualTo(recentActionBucket.timestamp)
        assertThat(actual.userEmail).isEqualTo(recentActionBucket.userEmail)
        assertThat(actual.parentHandle).isEqualTo(recentActionBucket.parentHandle)
        assertThat(actual.isUpdate).isEqualTo(recentActionBucket.isUpdate)
        assertThat(actual.isMedia).isEqualTo(recentActionBucket.isMedia)
        assertThat(actual.nodes.size).isEqualTo(megaNodeList.size())
    }

    private fun getMockNode(
        name: String = expectedName,
        size: Long = expectedSize,
        label: Int = expectedLabel,
        id: Long = expectedId,
        parentId: Long = expectedParentId,
        base64Id: String = expectedBase64Id,
        modificationTime: Long = expectedModificationTime,
        isFile: Boolean,
    ): MegaNode {
        val node = mock<MegaNode> {
            on { this.name }.thenReturn(name)
            on { this.size }.thenReturn(size)
            on { this.label }.thenReturn(label)
            on { this.handle }.thenReturn(id)
            on { this.parentHandle }.thenReturn(parentId)
            on { this.base64Handle }.thenReturn(base64Id)
            on { this.modificationTime }.thenReturn(modificationTime)
            on { this.isFile }.thenReturn(isFile)
            on { this.isFolder }.thenReturn(!isFile)
        }
        return node
    }
}
