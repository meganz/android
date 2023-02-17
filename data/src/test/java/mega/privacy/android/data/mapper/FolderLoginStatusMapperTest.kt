package mega.privacy.android.data.mapper

import com.google.common.truth.Truth
import mega.privacy.android.domain.entity.folderlink.FolderLoginStatus
import nz.mega.sdk.MegaError
import org.junit.Test
import org.mockito.kotlin.mock

class FolderLoginStatusMapperTest {

    @Test
    fun `test that mega error returns FolderLoginStatus`() {
        val megaError = mock<MegaError> { on { errorCode }.thenReturn(MegaError.API_OK) }
        val expectedResult = FolderLoginStatus.SUCCESS

        val actualResult = toFolderLoginStatus(megaError)
        Truth.assertThat(actualResult).isEqualTo(expectedResult)
    }
}