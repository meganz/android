package mega.privacy.android.data.mapper.login

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.account.AccountSession
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

/**
 * Test class for [AccountSessionMapper]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AccountSessionMapperTest {
    private lateinit var underTest: AccountSessionMapper

    @BeforeAll
    fun setUp() {
        underTest = AccountSessionMapper()
    }

    @Test
    fun `test that the parameters are mapped into an account session`() = runTest {
        val email = "test@gmail.com"
        val session = "Session"
        val myHandle = 123456L

        val expected = AccountSession(email = email, session = session, myHandle = myHandle)
        val actual = underTest(email = email, session = session, myHandle = myHandle)

        assertThat(actual).isEqualTo(expected)
    }
}