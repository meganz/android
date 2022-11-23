package mega.privacy.android.data.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.contacts.AccountCredentials.MyAccountCredentials
import org.junit.Test

class MyAccountCredentialsMapperTest {

    @Test
    fun `test that invalid credentials return null`() {
        val invalidCredentials = "ASDFARheu435evnk3c4"

        assertThat(toMyAccountCredentials(invalidCredentials)).isNull()
    }

    @Test
    fun `test that valid credentials return MyAccountCredentials`() {
        val validCredentials = "KJ9hFK67vhj3cNCIUHAi8ccwciojiot4hVE5yab3"
        val finalCredentials = MyAccountCredentials(validCredentials.chunked(4))

        assertThat(toMyAccountCredentials(validCredentials))
            .isEqualTo(finalCredentials)
    }
}