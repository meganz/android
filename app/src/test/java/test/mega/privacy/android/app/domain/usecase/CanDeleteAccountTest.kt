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

class CanDeleteAccountTest{
    lateinit var underTest: CanDeleteAccount

    private val accountRepository = mock<AccountRepository>()

    @Before
    fun setUp() {
        underTest = DefaultCanDeleteAccount(accountRepository)
    }

    @Test
    fun `test that non business accounts can be deleted`() {
        whenever(accountRepository.getUserAccount()).thenReturn(
            UserAccount(
                email = "",
                isBusinessAccount = false,
                isMasterBusinessAccount = false,
                accountTypeIdentifier = 0
            )
        )

        assertThat(underTest()).isTrue()
    }

    @Test
    fun `test that business accounts can not be deleted`() {
        whenever(accountRepository.getUserAccount()).thenReturn(
            UserAccount(
                email = "",
                isBusinessAccount = true,
                isMasterBusinessAccount = false,
                accountTypeIdentifier = 0
            )
        )

        assertThat(underTest()).isFalse()
    }

    @Test
    fun `test that master business accounts can be deleted`() {
        whenever(accountRepository.getUserAccount()).thenReturn(
            UserAccount(
                email = "",
                isBusinessAccount = true,
                isMasterBusinessAccount = true,
                accountTypeIdentifier = 0
            )
        )

        assertThat(underTest()).isTrue()
    }
}

