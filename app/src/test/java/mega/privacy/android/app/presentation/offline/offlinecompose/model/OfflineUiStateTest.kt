package mega.privacy.android.app.presentation.offline.offlinecompose.model

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.offline.OfflineFileInformation
import mega.privacy.android.domain.entity.offline.OfflineFolderInfo
import mega.privacy.android.domain.entity.offline.OtherOfflineNodeInformation
import org.junit.jupiter.api.Test

class OfflineUiStateTest {
    @Test
    fun `test that selectedOfflineNodes returns the correct nodes`() {
        val nodeInfo1 = OtherOfflineNodeInformation(
            id = 11,
            handle = "123",
            parentId = 678,
            path = "path",
            name = "test",
            isFolder = false,
            lastModifiedTime = 123L
        )

        val offlineNodeUIItem =
            OfflineNodeUIItem(
                OfflineFileInformation(
                    nodeInfo = nodeInfo1,
                    totalSize = 1L,
                    folderInfo = OfflineFolderInfo(1, 1),
                    fileTypeInfo = null,
                    thumbnail = "test"
                ),
                isSelected = true
            )

        val nodeInfo2 = OtherOfflineNodeInformation(
            id = 12,
            handle = "345",
            parentId = 124,
            path = "path2",
            name = "test2",
            isFolder = false,
            lastModifiedTime = 123L
        )

        val offlineNodeUIItem2 =
            OfflineNodeUIItem(
                OfflineFileInformation(
                    nodeInfo = nodeInfo2,
                    totalSize = 1L,
                    folderInfo = OfflineFolderInfo(1, 1),
                    fileTypeInfo = null,
                    thumbnail = "test"
                ),
                isSelected = false
            )
        val underTest = OfflineUiState(
            offlineNodes = listOf(offlineNodeUIItem, offlineNodeUIItem2),
            selectedNodeHandles = listOf(123L)
        )

        assertThat(underTest.selectedOfflineNodes).containsExactly(offlineNodeUIItem.offlineNode)
    }

    @Test
    fun `test that actualTitle returns title if it is not null`() {
        val title = "Title"
        val underTest = OfflineUiState(title = title)

        assertThat(underTest.actualTitle).isEqualTo(title)
    }

    @Test
    fun `test that actualTitle returns default title if title is null`() {
        val defaultTitle = "Title"
        val underTest = OfflineUiState(title = null, defaultTitle = defaultTitle)

        assertThat(underTest.actualTitle).isEqualTo(defaultTitle)
    }

    @Test
    fun `test that actualSubTitle returns default title if title is not null`() {
        val title = "Title"
        val defaultTitle = "Default Title"
        val underTest = OfflineUiState(title = title, defaultTitle = defaultTitle)

        assertThat(underTest.actualSubtitle).isEqualTo(defaultTitle)
    }

    @Test
    fun `test that actualSubTitle returns null if title is null`() {
        val defaultTitle = "Title"
        val underTest = OfflineUiState(title = null, defaultTitle = defaultTitle)

        assertThat(underTest.actualSubtitle).isNull()
    }
}