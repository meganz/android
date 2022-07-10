package test.mega.privacy.android.app.data.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.favourites.facade.StringUtilWrapper
import mega.privacy.android.app.presentation.mapper.toFavourite
import mega.privacy.android.app.utils.MegaNodeUtil.isImage
import mega.privacy.android.app.utils.MegaNodeUtil.isVideo
import mega.privacy.android.domain.entity.FavouriteInfo
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
        val expectedSize = 10L
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
        val expectedModificationTime = 1000L

        val testNode = mock<MegaNode> {
            on { handle }.thenReturn(expectedHandle)
            on { name }.thenReturn(expectedName)
            on { size }.thenReturn(expectedSize)
            on { label }.thenReturn(expectedLabel)
            on { isFavourite }.thenReturn(expectedIsFavourite)
            on { isExported }.thenReturn(expectedIsExported)
            on { isTakenDown }.thenReturn(expectedIsTakenDown)
            on { isTakenDown }.thenReturn(expectedIsTakenDown)
            on { modificationTime }.thenReturn(expectedModificationTime)
            on { isFolder }.thenReturn(true)
            on { isInShare }.thenReturn(true)
            on { isImage() }.thenReturn(false)
            on { isVideo() }.thenReturn(false)
        }

        val favouriteInfo = FavouriteInfo(
            id = expectedHandle,
            name = expectedName,
            size = expectedSize,
            label = expectedLabel,
            parentId = expectedHandle,
            base64Id = "",
            modificationTime = expectedModificationTime,
            hasVersion = expectedHasVersion,
            numChildFiles = 0,
            numChildFolders = 0,
            isImage = false,
            isVideo = false,
            isFolder = true,
            isFavourite = expectedIsFavourite,
            isExported = expectedIsExported,
            isTakenDown = expectedIsTakenDown,
        )


        val stringUtil = mock<StringUtilWrapper> {
            on {
                getFolderInfo(any(), any())
            }.thenReturn(expectedInfo)
        }

        val actual = toFavourite(testNode, favouriteInfo, false, stringUtil)

        assertThat(actual.handle).isEqualTo(expectedHandle)
        assertThat(actual.name).isEqualTo(expectedName)
        assertThat(actual.size).isEqualTo(expectedSize)
        assertThat(actual.label).isEqualTo(expectedLabel)
        assertThat(actual.modificationTime).isEqualTo(expectedModificationTime)
        assertThat(actual.hasVersion).isEqualTo(expectedHasVersion)
        assertThat(actual.showLabel).isEqualTo(expectedShowLabel)
        assertThat(actual.info).isEqualTo(expectedInfo)
        assertThat(actual.isAvailableOffline).isEqualTo(expectedIsAvailableOffline)
        assertThat(actual.isExported).isEqualTo(expectedIsExported)
        assertThat(actual.isFavourite).isEqualTo(expectedIsFavourite)
        assertThat(actual.isTakenDown).isEqualTo(expectedIsTakenDown)
        assertThat(actual.labelColour).isEqualTo(expectedLabelColour)
        assertThat(actual.icon).isEqualTo(expectedIcon)
        assertThat(actual.node).isSameInstanceAs(testNode)
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

        val favouriteInfo = FavouriteInfo(
            id = expectedHandle,
            name = expectedName,
            size = expectedSize,
            label = expectedLabel,
            parentId = expectedHandle,
            base64Id = "",
            modificationTime = expectedModificationTime,
            hasVersion = expectedHasVersion,
            numChildFiles = 0,
            numChildFolders = 0,
            isImage = false,
            isVideo = false,
            isFolder = false,
            isFavourite = expectedIsFavourite,
            isExported = expectedIsExported,
            isTakenDown = expectedIsTakenDown,
        )


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