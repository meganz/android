package test.mega.privacy.android.app.data.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.data.mapper.toContactRequest
import nz.mega.sdk.MegaContactRequest
import org.junit.Test
import org.mockito.kotlin.mock

class ContactRequestMapperTest{

    @Test
    fun `test that request with a null target email can be mapped successfully`() {
        val megaRequest = mock<MegaContactRequest>{
            on { targetEmail }.thenReturn(null)
            on { handle }.thenReturn(12L)
            on { sourceEmail }.thenReturn(" ")
            on { sourceMessage }.thenReturn(" ")
            on { creationTime }.thenReturn(12L)
            on { modificationTime }.thenReturn(12L)
            on { status }.thenReturn(1)
            on { isOutgoing }.thenReturn(false)
            on { isAutoAccepted }.thenReturn(false)
        }

        val actual = toContactRequest(megaRequest = megaRequest)

        assertThat(actual).isNotNull()
    }
}