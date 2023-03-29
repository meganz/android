package test.mega.privacy.android.app.presentation.favourites.model.mapper

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.favourites.facade.StringUtilWrapper
import mega.privacy.android.app.presentation.favourites.model.mapper.toFavourite
import mega.privacy.android.app.utils.MegaNodeUtil.isImage
import mega.privacy.android.app.utils.MegaNodeUtil.isVideo
import mega.privacy.android.domain.entity.PdfFileTypeInfo
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedFolderNode
import nz.mega.sdk.MegaNode
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class FavouriteMapperTest {

    @Test
    fun `test that values returned when FavouriteInfo is folder`() {
        val expectedNodeId = NodeId(1L)
        val expectedName = "TestFolder"
        val expectedLabel = MegaNode.NODE_LBL_RED
        val expectedInfo = "Test folder info"
        val expectedIcon = R.drawable.ic_folder_incoming
        val expectedIsFavourite = false
        val expectedIsTakenDown = false
        val expectedShowLabel = true
        val expectedHasVersion = true
        val expectedIsAvailableOffline = false
        val expectedLabelColour = R.color.salmon_400_salmon_300
        val expectedIsInRubbishBin = false
        val expectedIsIncomingShare = true
        val expectedIsShared = true
        val expectedIsPendingShare = true

        val testNode = mock<MegaNode> {
            on { isInShare }.thenReturn(expectedIsShared)
        }

        val typedFolderNode = mock<TypedFolderNode> {
            on { id }.thenReturn(expectedNodeId)
            on { name }.thenReturn(expectedName)
            on { label }.thenReturn(expectedLabel)
            on { parentId }.thenReturn(expectedNodeId)
            on { base64Id }.thenReturn("")
            on { hasVersion }.thenReturn(expectedHasVersion)
            on { childFileCount }.thenReturn(0)
            on { childFolderCount }.thenReturn(0)
            on { isFavourite }.thenReturn(expectedIsFavourite)
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

        val actual = toFavourite(
            node = testNode,
            nodeInfo = typedFolderNode,
            isAvailableOffline = false,
            stringUtil = stringUtil,
            isSelected = false
        )

        assertWithMessage("ShowLabel not mapped correctly").that(actual.showLabel)
            .isEqualTo(expectedShowLabel)
        assertWithMessage("Info not mapped correctly").that(actual.info).isEqualTo(expectedInfo)
        assertWithMessage("IsAvailableOffline not mapped correctly").that(actual.isAvailableOffline)
            .isEqualTo(expectedIsAvailableOffline)
        assertWithMessage("LabelColour not mapped correctly").that(actual.labelColour)
            .isEqualTo(expectedLabelColour)
        assertWithMessage("Icon not mapped correctly").that(actual.icon).isEqualTo(expectedIcon)

        assertThat(actual.typedNode).isSameInstanceAs(typedFolderNode)
    }

    @Test
    fun `test that values returned when FavouriteInfo is file`() {
        val expectedNodeId = NodeId(1L)
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
            on { handle }.thenReturn(expectedNodeId.longValue)
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

        val typedFileNode = mock<TypedFileNode> {
            on { id }.thenReturn(expectedNodeId)
            on { name }.thenReturn(expectedName)
            on { size }.thenReturn(expectedSize)
            on { label }.thenReturn(expectedLabel)
            on { parentId }.thenReturn(expectedNodeId)
            on { base64Id }.thenReturn("")
            on { modificationTime }.thenReturn(expectedModificationTime)
            on { hasVersion }.thenReturn(expectedHasVersion)
            on { isFavourite }.thenReturn(expectedIsFavourite)
            on { isTakenDown }.thenReturn(expectedIsTakenDown)
            on { type }.thenReturn(PdfFileTypeInfo)
        }


        val stringUtil = mock<StringUtilWrapper> {
            on { getSizeString(expectedSize) }.thenReturn("Size")
            on { formatLongDateTime(expectedModificationTime) }.thenReturn("Modification")
        }

        val getFileIcon: (String) -> Int = mock()
        whenever(getFileIcon(expectedName)).thenReturn(expectedIcon)

        val actual = toFavourite(
            node = testNode,
            nodeInfo = typedFileNode,
            isAvailableOffline = false,
            stringUtil = stringUtil,
            isSelected = false,
            getFileIcon = getFileIcon,
        )

        assertThat(actual.showLabel).isEqualTo(expectedShowLabel)
        assertThat(actual.info).isEqualTo(expectedInfo)
        assertThat(actual.isAvailableOffline).isEqualTo(expectedIsAvailableOffline)
        assertThat(actual.labelColour).isEqualTo(expectedLabelColour)
        assertThat(actual.icon).isEqualTo(expectedIcon)
        assertThat(actual.typedNode).isSameInstanceAs(typedFileNode)
        assertThat(actual.node).isSameInstanceAs(testNode)
    }

}