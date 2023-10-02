package mega.privacy.android.data.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.FolderInfo
import nz.mega.sdk.MegaFolderInfo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FolderInfoMapperTest {

    private lateinit var underTest: FolderInfoMapper

    @BeforeEach
    fun setup() {
        underTest = FolderInfoMapper()
    }

    @Test
    fun `test that folder info type can be mapped correctly`() {
        val expectedResult = FolderInfo(
            currentSize = 1000L,
            numVersions = 1,
            numFiles = 2,
            numFolders = 3,
            versionsSize = 4,
            folderName = "folder_name",
        )

        val megaFolderInfo = mock<MegaFolderInfo> {
            on { numFolders }.thenReturn(expectedResult.numFolders)
            on { numVersions }.thenReturn(expectedResult.numVersions)
            on { currentSize }.thenReturn(expectedResult.currentSize)
            on { numFiles }.thenReturn(expectedResult.numFiles)
            on { versionsSize }.thenReturn(expectedResult.versionsSize)
        }

        val result = underTest(megaFolderInfo, expectedResult.folderName)
        assertThat(result).isEqualTo(expectedResult)
    }
}