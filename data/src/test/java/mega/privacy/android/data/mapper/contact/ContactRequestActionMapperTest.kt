package mega.privacy.android.data.mapper.contact

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.contacts.ContactRequestAction
import nz.mega.sdk.MegaContactRequest
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory

class ContactRequestActionMapperTest {
    private val underTest = ContactRequestActionMapper()

    @TestFactory
    fun `test that contact request action is mapped correctly`() =
        HashMap<ContactRequestAction, Int>().apply {
            put(ContactRequestAction.Accept, MegaContactRequest.REPLY_ACTION_ACCEPT)
            put(ContactRequestAction.Deny, MegaContactRequest.REPLY_ACTION_DENY)
            put(ContactRequestAction.Ignore, MegaContactRequest.REPLY_ACTION_IGNORE)
            put(ContactRequestAction.Add, MegaContactRequest.INVITE_ACTION_ADD)
            put(ContactRequestAction.Delete, MegaContactRequest.INVITE_ACTION_DELETE)
            put(ContactRequestAction.Remind, MegaContactRequest.INVITE_ACTION_REMIND)
        }.map { (requestAction, requestType) ->
            DynamicTest.dynamicTest("test that $requestAction is mapped to $requestType") {
                assertThat(underTest(requestAction)).isEqualTo(requestType)
            }
        }
}
