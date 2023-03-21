package mega.privacy.android.data.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.data.mapper.contact.MyAccountCredentialsMapper
import mega.privacy.android.domain.entity.contacts.AccountCredentials.MyAccountCredentials
import org.junit.Before
import org.junit.Test

class MyAccountCredentialsMapperTest {

    private lateinit var underTest: MyAccountCredentialsMapper

    @Before
    fun setUp() {
        underTest = MyAccountCredentialsMapper()
    }

    @Test
    fun `test that invalid credentials return null`() {
        val invalidCredentials = "ASDFARheu435evnk3c4"

        assertThat(underTest(invalidCredentials)).isNull()
    }

    @Test
    fun `test that valid credentials return MyAccountCredentials`() {
        val validCredentials = "KJ9hFK67vhj3cNCIUHAi8ccwciojiot4hVE5yab3"
        val finalCredentials = MyAccountCredentials(validCredentials.chunked(4))

        assertThat(underTest(validCredentials))
            .isEqualTo(finalCredentials)
    }
}