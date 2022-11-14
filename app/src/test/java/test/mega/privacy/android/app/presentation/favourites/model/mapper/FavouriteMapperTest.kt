package test.mega.privacy.android.app.presentation.favourites.model.mapper

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.favourites.facade.StringUtilWrapper
import mega.privacy.android.app.presentation.favourites.model.mapper.toFavourite
import mega.privacy.android.app.utils.MegaNodeUtil.isImage
import mega.privacy.android.app.utils.MegaNodeUtil.isVideo
import mega.privacy.android.domain.entity.PdfFileTypeInfo
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.FolderNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import mega.privacy.android.domain.entity.node.TypedNode
import nz.mega.sdk.MegaNode
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class FavouriteMapperTest {

    @Test
    fun `test that values returned when FavouriteInfo is folder`() {
        val expectedHandle = 1L
        val expectedName = "TestFolder"
        val expectedLabel = MegaNode.NODE_LBL_RED
        val expectedInfo = "Test folder info"
        val expectedIcon = R.drawable.ic_folder_incoming
        val expectedIsFavourite = false
        val expectedIsExported = false
        val expectedIsTakenDown = false
        val expectedShowLabel = true
        val expectedHasVersion = true
        val expectedIsAvailableOffline = false
        val expectedLabelColour = R.color.salmon_400_salmon_300
        val expectedIsInRubbishBin = true
        val expectedIsIncomingShare = true
        val expectedIsShared = true
        val expectedIsPendingShare = true

        val testNode = mock<MegaNode> {
            on { isInShare }.thenReturn(expectedIsShared)
        }

        val favouriteInfo = mock<TypedFolderNode> {
            on { id }.thenReturn(NodeId(expectedHandle))
            on { name }.thenReturn(expectedName)
            on { label }.thenReturn(expectedLabel)
            on { parentId }.thenReturn(NodeId(expectedHandle))
            on { base64Id }.thenReturn("")
            on { hasVersion }.thenReturn(expectedHasVersion)
            on { childFileCount }.thenReturn(0)
            on { childFolderCount }.thenReturn(0)
            on { isFavourite }.thenReturn(expectedIsFavourite)
            on { isExported }.thenReturn(expectedIsExported)
            on { isTakenDown }.thenReturn(expectedIsTakenDown)
            on { isInRubbishBin }.thenReturn(expectedIsInRubbishBin)
            on { isIncomingShare }.thenReturn(expectedIsIncomingShare)
            on { isShared }.thenReturn(expectedIsShared)
            on { isPendingShare }.thenReturn(expectedIsPendingShare)
            on { device }.thenReturn(null)
        }


        val stringUtil = mock<StringUtilWrapper> {
            on {
                getFolderInfo(any(), any())
            }.thenReturn(expectedInfo)
        }

        val actual = toFavourite(testNode, favouriteInfo, false, stringUtil)

        assertWithMessage("Name not mapped correctly").that(actual.name).isEqualTo(expectedName)
        assertWithMessage("Label not mapped correctly").that(actual.label).isEqualTo(expectedLabel)
        assertWithMessage("HasVersion not mapped correctly").that(actual.hasVersion)
            .isEqualTo(expectedHasVersion)
        assertWithMessage("ShowLabel not mapped correctly").that(actual.showLabel)
            .isEqualTo(expectedShowLabel)
        assertWithMessage("Info not mapped correctly").that(actual.info).isEqualTo(expectedInfo)
        assertWithMessage("IsAvailableOffline not mapped correctly").that(actual.isAvailableOffline)
            .isEqualTo(expectedIsAvailableOffline)
        assertWithMessage("IsExported not mapped correctly").that(actual.isExported)
            .isEqualTo(expectedIsExported)
        assertWithMessage("IsFavourite not mapped correctly").that(actual.isFavourite)
            .isEqualTo(expectedIsFavourite)
        assertWithMessage("IsTakenDown not mapped correctly").that(actual.isTakenDown)
            .isEqualTo(expectedIsTakenDown)
        assertWithMessage("LabelColour not mapped correctly").that(actual.labelColour)
            .isEqualTo(expectedLabelColour)
        assertWithMessage("Icon not mapped correctly").that(actual.icon).isEqualTo(expectedIcon)
        assertWithMessage("Node not mapped correctly").that(actual.node).isSameInstanceAs(testNode)
        assertWithMessage("Handle not mapped correctly").that(actual.handle)
            .isEqualTo(expectedHandle)
    }

    @Test
    fun `test that values returned when FavouriteInfo is file`() {
        val expectedHandle = 1L
        val expectedName = "TestFile.test"
        val expectedInfo = "Size · Modification"
        val expectedLabel = MegaNode.NODE_LBL_RED
        val expectedIcon = R.drawable.ic_generic_list
        val expectedIsFavourite = false
        val expectedIsExported = false
        val expectedIsTakenDown = false
        val expectedShowLabel = true
        val expectedHasVersion = true
        val expectedIsAvailableOffline = false
        val expectedLabelColour = R.color.salmon_400_salmon_300
        val expectedSize = 1000L
        val expectedModificationTime = 1000L

        val testNode = mock<MegaNode> {
            on { handle }.thenReturn(expectedHandle)
            on { name }.thenReturn(expectedName)
            on { label }.thenReturn(expectedLabel)
            on { isFavourite }.thenReturn(expectedIsFavourite)
            on { isExported }.thenReturn(expectedIsExported)
            on { isTakenDown }.thenReturn(expectedIsTakenDown)
            on { isTakenDown }.thenReturn(expectedIsTakenDown)
            on { isFolder }.thenReturn(false)
            on { isInShare }.thenReturn(true)
            on { size }.thenReturn(expectedSize)
            on { modificationTime }.thenReturn(expectedModificationTime)
            on { isImage() }.thenReturn(false)
            on { isVideo() }.thenReturn(false)
        }

        val favouriteInfo = mock<TypedFileNode> {
            on { id }.thenReturn(NodeId(expectedHandle))
            on { name }.thenReturn(expectedName)
            on { size }.thenReturn(expectedSize)
            on { label }.thenReturn(expectedLabel)
            on { parentId }.thenReturn(NodeId(expectedHandle))
            on { base64Id }.thenReturn("")
            on { modificationTime }.thenReturn(expectedModificationTime)
            on { hasVersion }.thenReturn(expectedHasVersion)
            on { isFavourite }.thenReturn(expectedIsFavourite)
            on { isExported }.thenReturn(expectedIsExported)
            on { isTakenDown }.thenReturn(expectedIsTakenDown)
            on { type }.thenReturn(PdfFileTypeInfo)
        }


        val stringUtil = mock<StringUtilWrapper> {
            on { getSizeString(expectedSize) }.thenReturn("Size")
            on { formatLongDateTime(expectedModificationTime) }.thenReturn("Modification")
        }

        val getFileIcon: (String) -> Int = mock()
        whenever(getFileIcon(expectedName)).thenReturn(expectedIcon)

        val actual = toFavourite(testNode, favouriteInfo, false, stringUtil, getFileIcon)

        assertThat(actual.handle).isEqualTo(expectedHandle)
        assertThat(actual.name).isEqualTo(expectedName)
        assertThat(actual.hasVersion).isEqualTo(expectedHasVersion)
        assertThat(actual.showLabel).isEqualTo(expectedShowLabel)
        assertThat(actual.info).isEqualTo(expectedInfo)
        assertThat(actual.isAvailableOffline).isEqualTo(expectedIsAvailableOffline)
        assertThat(actual.isExported).isEqualTo(expectedIsExported)
        assertThat(actual.isFavourite).isEqualTo(expectedIsFavourite)
        assertThat(actual.isTakenDown).isEqualTo(expectedIsTakenDown)
        assertThat(actual.labelColour).isEqualTo(expectedLabelColour)
        assertThat(actual.icon).isEqualTo(expectedIcon)
        assertThat(actual.size).isEqualTo(expectedSize)
        assertThat(actual.modificationTime).isEqualTo(expectedModificationTime)
        assertThat(actual.node).isSameInstanceAs(testNode)
    }

}