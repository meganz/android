package test.mega.privacy.android.app.domain.usecase

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.domain.entity.UserAccount
import mega.privacy.android.app.domain.entity.user.UserId
import mega.privacy.android.app.domain.usecase.CanDeleteAccount
import mega.privacy.android.app.domain.usecase.DefaultCanDeleteAccount
import org.junit.Before
import org.junit.Test

class DefaultCanDeleteAccountTest {
    lateinit var underTest: CanDeleteAccount

    @Before
    fun setUp() {
        underTest = DefaultCanDeleteAccount()
    }

    @Test
    fun `test that non business accounts can be deleted`() {

        val isBusinessAccount = false
        val isMasterBusinessAccount = false
        assertThat(
                underTest(
                        userAccount(isBusinessAccount, isMasterBusinessAccount)
                )
        ).isTrue()
    }

    @Test
    fun `test that business accounts can not be deleted`() {
        val isBusinessAccount = true
        val isMasterBusinessAccount = false
        assertThat(
                underTest(
                        userAccount(isBusinessAccount, isMasterBusinessAccount)
                )
        ).isFalse()
    }

    @Test
    fun `test that master business accounts can be deleted`() {
        val isBusinessAccount = true
        val isMasterBusinessAccount = true
        assertThat(
                underTest(
                        userAccount(isBusinessAccount, isMasterBusinessAccount)
                )
        ).isTrue()
    }

    private fun userAccount(isBusinessAccount: Boolean, isMasterBusinessAccount: Boolean) =
            UserAccount(
                    userId = UserId(1L),
                    email = "",
                    isBusinessAccount = isBusinessAccount,
                    isMasterBusinessAccount = isMasterBusinessAccount,
                    accountTypeIdentifier = 0,
                    accountTypeString = "",
            )
}

