package mega.privacy.android.data.mapper.transfer

import com.google.common.truth.Truth
import mega.privacy.android.domain.entity.transfer.TransferData
import nz.mega.sdk.MegaTransferData
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock

internal class TransferDataMapperTest {

    @Test
    fun `test that transfer data mapper returns correctly`() {
        val downloads = 3
        val firstDownloadTag = 1
        val secondDownloadTag = 2
        val thirdDownloadTag = 3
        val uploads = 1
        val firstUploadTag = 4

        val transferData = mock<MegaTransferData> {
            on { numDownloads }.thenReturn(downloads)
            on { numUploads }.thenReturn(uploads)
            on { getDownloadTag(0) }.thenReturn(firstDownloadTag)
            on { getDownloadTag(1) }.thenReturn(secondDownloadTag)
            on { getDownloadTag(2) }.thenReturn(thirdDownloadTag)
            on { getUploadTag(0) }.thenReturn(firstUploadTag)
        }
        val expected = TransferData(
            numDownloads = downloads,
            numUploads = uploads,
            downloadTags = listOf(firstDownloadTag, secondDownloadTag, thirdDownloadTag),
            uploadTags = listOf(firstUploadTag)
        )

        Truth.assertThat(TransferDataMapper().invoke(transferData)).isEqualTo(expected)
    }
}