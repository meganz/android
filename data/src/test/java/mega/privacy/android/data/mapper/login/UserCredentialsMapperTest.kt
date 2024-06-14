package mega.privacy.android.data.mapper.login

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.user.UserCredentials
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * Test class for [UserCredentialsMapper]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class UserCredentialsMapperTest {
    private lateinit var underTest: UserCredentialsMapper

    @BeforeAll
    fun setUp() {
        underTest = UserCredentialsMapper()
    }

    @Test
    fun `test that the parameters are mapped into user credentials`() = runTest {
        val email = "testemail@gmail.com"
        val session = "session"
        val firstName = "First name"
        val lastName = "Last name"
        val myHandle = "My handle"

        val expected = UserCredentials(
            email = email,
            session = session,
            firstName = firstName,
            lastName = lastName,
            myHandle = myHandle,
        )
        val actual = underTest(
            email = email,
            session = session,
            firstName = firstName,
            lastName = lastName,
            myHandle = myHandle,
        )

        assertThat(actual).isEqualTo(expected)
    }
}