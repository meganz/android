package test.mega.privacy.android.app.data.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.data.gateway.api.MegaApiGateway
import mega.privacy.android.app.data.mapper.toFavouriteInfo
import mega.privacy.android.domain.entity.FavouriteFile
import mega.privacy.android.domain.entity.PdfFileTypeInfo
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import nz.mega.sdk.MegaNode
import org.junit.Test
import org.mockito.kotlin.mock

class FavouriteInfoMapperTest {
    private val expectedName = "testName"
    private val expectedSize = 1000L
    private val expectedLabel = MegaNode.NODE_LBL_RED
    private val expectedId = 1L
    private val expectedParentId = 2L
    private val expectedBase64Id = "1L"
    private val expectedModificationTime = 123L

    @Test
    fun `test that files are mapped if isFile is true`() {
        val megaNode = getMockNode(isFile = true)
        val actual = toFavouriteInfo(megaNode, null, false, 0, 1) { PdfFileTypeInfo }

        assertThat(actual).isInstanceOf(FavouriteFile::class.java)
    }

    @Test
    fun `test that folders are mapped if isFile is false`() {
        val megaNode = getMockNode(isFile = false)
        val actual = toFavouriteInfo(megaNode, null, false, 0, 1) { PdfFileTypeInfo }

        assertThat(actual).isInstanceOf(FavouriteFile::class.java)
    }

    @Test
    fun `test that values returned by gateway are used`() {
        val node = getMockNode(isFile = false)
        val expectedHasVersion = true
        val expectedNumChildFolders = 2
        val expectedNumChildFiles = 3
        val gateway = mock<MegaApiGateway> {
            on { hasVersion(node) }.thenReturn(expectedHasVersion)
            on { getNumChildFolders(node) }.thenReturn(expectedNumChildFolders)
            on { getNumChildFiles(node) }.thenReturn(expectedNumChildFiles)
        }

        val actual = toFavouriteInfo(node,
            null,
            gateway.hasVersion(node),
            gateway.getNumChildFolders(node),
            gateway.getNumChildFiles(node)) { VideoFileTypeInfo("", "") }

        assertThat(actual.name).isEqualTo(expectedName)
        assertThat(actual.label).isEqualTo(expectedLabel)
        assertThat(actual.hasVersion).isEqualTo(expectedHasVersion)
        assertThat(actual.id).isEqualTo(expectedId)
        assertThat(actual.parentId).isEqualTo(expectedParentId)
        assertThat(actual.base64Id).isEqualTo(expectedBase64Id)
        assertThat(actual.isFavourite).isEqualTo(node.isFavourite)
        assertThat(actual.isExported).isEqualTo(node.isExported)
        assertThat(actual.isTakenDown).isEqualTo(node.isTakenDown)
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
        }
        return node
    }
}