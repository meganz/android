package mega.privacy.android.data.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.data.mapper.contact.ContactCredentialsMapper
import mega.privacy.android.data.mapper.contact.ContactCredentialsMapperImpl
import mega.privacy.android.domain.entity.contacts.AccountCredentials.ContactCredentials
import org.junit.Before
import org.junit.Test

class ContactCredentialsMapperTest {

    private lateinit var underTest: ContactCredentialsMapper

    private val email = "test@mega.nz"
    private val name = "test"

    @Before
    fun setUp() {
        underTest = ContactCredentialsMapperImpl()
    }

    @Test
    fun `test that invalid credentials return null`() {
        val invalidCredentials = "camneLkcL43uqkqk"

        assertThat(underTest(invalidCredentials, email, name)).isNull()
    }

    @Test
    fun `test that valid credentials return MyAccountCredentials`() {
        val validCredentials = "KJ9hFK67vhj3cNCIUHAi8ccwciojiot4hVE5yab3"
        val finalCredentials = ContactCredentials(validCredentials.chunked(4), email, name)

        assertThat(underTest(validCredentials, email, name))
            .isEqualTo(finalCredentials)
    }
}