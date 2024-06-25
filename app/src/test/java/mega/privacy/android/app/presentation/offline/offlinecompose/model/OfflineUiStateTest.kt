package mega.privacy.android.app.presentation.offline.offlinecompose.model

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.offline.OfflineFileInformation
import mega.privacy.android.domain.entity.offline.OfflineFolderInfo
import org.junit.jupiter.api.Test

class OfflineUiStateTest {
    @Test
    fun `test that selectedOfflineNodes returns the correct nodes`() {
        val offlineNodeUIItem =
            OfflineNodeUIItem(
                OfflineFileInformation(
                    totalSize = 1L,
                    folderInfo = OfflineFolderInfo(1, 1),
                    fileTypeInfo = null,
                    thumbnail = "test",
                    id = 11,
                    handle = "123",
                    parentId = 678,
                    path = "path",
                    lastModifiedTime = 123L,
                ),
                isSelected = true
            )
        val offlineNodeUIItem2 =
            OfflineNodeUIItem(
                OfflineFileInformation(
                    totalSize = 1L,
                    folderInfo = OfflineFolderInfo(1, 1),
                    fileTypeInfo = null,
                    thumbnail = "test",
                    id = 12,
                    handle = "345",
                    parentId = 124,
                    path = "path2",
                    lastModifiedTime = 123L,
                ),
                isSelected = false
            )
        val underTest = OfflineUiState(
            offlineNodes = listOf(offlineNodeUIItem, offlineNodeUIItem2),
            selectedNodeHandles = listOf(123L)
        )

        assertThat(underTest.selectedOfflineNodes).containsExactly(offlineNodeUIItem.offlineNode)
    }
}