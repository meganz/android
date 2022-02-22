package test.mega.privacy.android.app.domain.usecase

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.domain.entity.UserAccount
import mega.privacy.android.app.domain.repository.AccountRepository
import mega.privacy.android.app.domain.usecase.CanDeleteAccount
import mega.privacy.android.app.domain.usecase.DefaultCanDeleteAccount
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

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
                UserAccount(
                    email = "",
                    isBusinessAccount = false,
                    isMasterBusinessAccount = false,
                    accountTypeIdentifier = 0
                )
            )
        ).isTrue()
    }

    @Test
    fun `test that business accounts can not be deleted`() {
        assertThat(
            underTest(
                UserAccount(
                    email = "",
                    isBusinessAccount = true,
                    isMasterBusinessAccount = false,
                    accountTypeIdentifier = 0
                )
            )
        ).isFalse()
    }

    @Test
    fun `test that master business accounts can be deleted`() {
        assertThat(
            underTest(
                UserAccount(
                    email = "",
                    isBusinessAccount = true,
                    isMasterBusinessAccount = true,
                    accountTypeIdentifier = 0
                )
            )
        ).isTrue()
    }
}

