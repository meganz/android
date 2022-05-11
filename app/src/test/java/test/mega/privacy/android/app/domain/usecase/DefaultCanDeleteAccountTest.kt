package test.mega.privacy.android.app.domain.usecase

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.domain.entity.UserAccount
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

        assertThat(
            underTest(
                userAccount(
                    isBusinessAccount = false,
                    isMasterBusinessAccount = false,
                )
            )
        ).isTrue()
    }

    @Test
    fun `test that business accounts can not be deleted`() {
        assertThat(
            underTest(
                userAccount(
                    isBusinessAccount = true,
                    isMasterBusinessAccount = false
                )
            )
        ).isFalse()
    }

    @Test
    fun `test that master business accounts can be deleted`() {
        assertThat(
            underTest(
                userAccount(
                    isBusinessAccount = true,
                    isMasterBusinessAccount = true
                )
            )
        ).isTrue()
    }

    private fun userAccount(
        isBusinessAccount: Boolean = false,
        isMasterBusinessAccount: Boolean = false
    ) = UserAccount(
        userId = null,
        email = "",
        isBusinessAccount = isBusinessAccount,
        isMasterBusinessAccount = isMasterBusinessAccount,
        accountTypeIdentifier = 0
    )
}

