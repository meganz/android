package mega.privacy.android.shared.contact.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.contacts.ContactItem
import mega.privacy.android.domain.entity.contacts.ContactPermission
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.shared.contact.model.ContactItemUiState
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ContactPermissionUiStateMapperTest {

    private lateinit var underTest: ContactPermissionUiStateMapper

    private val contactItemUiStateMapper = mock<ContactItemUiStateMapper>()

    @BeforeEach
    fun setUp() {
        whenever(contactItemUiStateMapper(any())).thenReturn(mock())
        underTest = ContactPermissionUiStateMapper(
            contactMapper = contactItemUiStateMapper,
        )
    }

    @AfterEach
    fun tearDown() {
        reset(contactItemUiStateMapper)
    }

    @Test
    fun `test that contact item ui state is mapped from contact item ui state mapper`() {
        val contactItem = stubContactItem()
        val expectedUiState = mock<ContactItemUiState>()
        whenever(contactItemUiStateMapper(contactItem)).thenReturn(expectedUiState)

        val result = underTest(
            ContactPermission(
                contactItem = contactItem,
                accessPermission = AccessPermission.READ,
            )
        )

        assertThat(result.contactItemUiState).isEqualTo(expectedUiState)
    }

    @Test
    fun `test that contact item ui state mapper is called with the contact item`() {
        val contactItem = stubContactItem()

        underTest(
            ContactPermission(
                contactItem = contactItem,
                accessPermission = AccessPermission.READ,
            )
        )

        verify(contactItemUiStateMapper).invoke(contactItem)
    }

    @ParameterizedTest
    @EnumSource(AccessPermission::class)
    fun `test that permission is set from access permission`(permission: AccessPermission) {
        val result = underTest(
            ContactPermission(
                contactItem = stubContactItem(),
                accessPermission = permission,
            )
        )

        assertThat(result.permission).isEqualTo(permission)
    }

    @Test
    fun `test that email is set from contact item email`() {
        val expectedEmail = "test@example.com"
        val contactItem = stubContactItem(email = expectedEmail)

        val result = underTest(
            ContactPermission(
                contactItem = contactItem,
                accessPermission = AccessPermission.READ,
            )
        )

        assertThat(result.email).isEqualTo(expectedEmail)
    }

    private fun stubContactItem(email: String = "test@example.com"): ContactItem =
        mock { on { this.email } doReturn email }
}
