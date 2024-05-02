package mega.privacy.android.domain.usecase

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.offline.OtherOfflineNodeInformation
import org.junit.jupiter.api.Test

class SortOfflineInfoUseCaseTest {
    private val underTest = SortOfflineInfoUseCase()

    @Test
    fun `test that providing unsorted list returns sorted list`() {
        val item1 = OtherOfflineNodeInformation(
            id = 1,
            path = "",
            name = "ROhit.txt",
            isFolder = false,
            handle = "1",
            lastModifiedTime = 1L,
            parentId = 1
        )
        val item2 = OtherOfflineNodeInformation(
            id = 1,
            path = "",
            name = "sample.txt",
            isFolder = false,
            handle = "1",
            lastModifiedTime = 1L,
            parentId = 1
        )
        val item3 = OtherOfflineNodeInformation(
            id = 1,
            path = "",
            name = "New Folder",
            isFolder = true,
            handle = "1",
            lastModifiedTime = 1L,
            parentId = 1
        )
        val item4 = OtherOfflineNodeInformation(
            id = 1,
            path = "",
            name = "mega.pdf",
            isFolder = false,
            handle = "1",
            lastModifiedTime = 1L,
            parentId = 1
        )
        val item5 = OtherOfflineNodeInformation(
            id = 1,
            path = "",
            name = "abc",
            isFolder = true,
            handle = "1",
            lastModifiedTime = 1L,
            parentId = 1
        )
        val item6 = OtherOfflineNodeInformation(
            id = 1,
            path = "",
            name = "123",
            isFolder = true,
            handle = "1",
            lastModifiedTime = 1L,
            parentId = 1
        )
        val item7 = OtherOfflineNodeInformation(
            id = 1,
            path = "",
            name = "213.txt",
            isFolder = false,
            handle = "1",
            lastModifiedTime = 1L,
            parentId = 1
        )
        val item8 = OtherOfflineNodeInformation(
            id = 1,
            path = "",
            name = "zzz.txt",
            isFolder = false,
            handle = "1",
            lastModifiedTime = 1L,
            parentId = 1
        )
        val item9 = OtherOfflineNodeInformation(
            id = 1,
            path = "",
            name = "zzzz",
            isFolder = true,
            handle = "1",
            lastModifiedTime = 1L,
            parentId = 1
        )
        val list = mutableListOf(item1, item2, item3, item4, item5, item6, item7, item8, item9)
        val resultList = underTest(list)
        assertThat(resultList.first().name).isEqualTo("123")
        assertThat(resultList.first().isFolder).isTrue()

        assertThat(resultList.last().name).isEqualTo("zzz.txt")
        assertThat(resultList.last().isFolder).isFalse()
    }
}