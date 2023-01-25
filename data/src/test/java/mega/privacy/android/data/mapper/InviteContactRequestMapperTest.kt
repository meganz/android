package mega.privacy.android.data.mapper

import com.google.common.truth.Truth
import mega.privacy.android.domain.entity.contacts.InviteContactRequest
import nz.mega.sdk.MegaError
import org.junit.Test
import org.mockito.kotlin.mock

class InviteContactRequestMapperTest {

    @Test
    fun `test that mega error API_OK returns Sent enum`() {
        val megaError = mock<MegaError> { on { errorCode }.thenReturn(MegaError.API_OK) }
        val expectedResult = InviteContactRequest.Sent
        Truth.assertThat(toInviteContactRequest(megaError)).isEqualTo(expectedResult)
    }

    @Test
    fun `test that mega error API_EEXIST returns AlreadyContact enum`() {
        val megaError = mock<MegaError> { on { errorCode }.thenReturn(MegaError.API_EEXIST) }
        val expectedResult = InviteContactRequest.AlreadyContact
        Truth.assertThat(toInviteContactRequest(megaError)).isEqualTo(expectedResult)
    }

    @Test
    fun `test that mega error API_EARGS returns InvalidEmail enum`() {
        val megaError = mock<MegaError> { on { errorCode }.thenReturn(MegaError.API_EARGS) }
        val expectedResult = InviteContactRequest.InvalidEmail
        Truth.assertThat(toInviteContactRequest(megaError)).isEqualTo(expectedResult)
    }

    @Test
    fun `test that mega error API_EACCESS returns InvalidStatus enum`() {
        val megaError = mock<MegaError> { on { errorCode }.thenReturn(MegaError.API_EACCESS) }
        val expectedResult = InviteContactRequest.InvalidStatus
        Truth.assertThat(toInviteContactRequest(megaError)).isEqualTo(expectedResult)
    }
}