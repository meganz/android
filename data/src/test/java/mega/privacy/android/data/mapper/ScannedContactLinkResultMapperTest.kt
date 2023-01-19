package mega.privacy.android.data.mapper

import com.google.common.truth.Truth
import mega.privacy.android.domain.entity.qrcode.QRCodeQueryResults
import mega.privacy.android.domain.entity.qrcode.ScannedContactLinkResult
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import org.junit.Test
import org.mockito.kotlin.mock

class ScannedContactLinkResultMapperTest {

    @Test
    fun `test that mega request and mega error returns ScannedContactLinkResult`() {
        val megaRequest = mock<MegaRequest> {
            on { email }.thenReturn("abc@gmail.com")
            on { name }.thenReturn("abc")
            on { text }.thenReturn("xyz")
            on { nodeHandle }.thenReturn(12345)
        }
        val megaError = mock<MegaError> { on { errorCode }.thenReturn(MegaError.API_OK) }

        val expectedResult = ScannedContactLinkResult(
            "abc xyz",
            "abc@gmail.com",
            12345,
            false,
            QRCodeQueryResults.CONTACT_QUERY_OK
        )

        val actualResult = toScannedContactLinkResult(megaRequest, megaError, false)

        Truth.assertThat(actualResult).isEqualTo(expectedResult)
    }
}