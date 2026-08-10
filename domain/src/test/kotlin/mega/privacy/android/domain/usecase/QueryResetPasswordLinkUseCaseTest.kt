package mega.privacy.android.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.domain.entity.resetpassword.ResetPasswordLinkInfo
import mega.privacy.android.domain.repository.AccountRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class QueryResetPasswordLinkUseCaseTest {
    private lateinit var underTest: QueryResetPasswordLinkUseCase
    private val accountRepository: AccountRepository = mock()

    @BeforeAll
    internal fun init() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @BeforeEach
    internal fun setup() {
        reset(accountRepository)
    }

    private fun initTestClass() {
        underTest = QueryResetPasswordLinkUseCase(accountRepository)
    }

    @Test
    internal fun `test that use case should return correct ResetPasswordLinkInfo when invoked and url type is RESET_PASSWORD_LINK`() =
        runTest {
            val link = "qhwuehqwuhajsdhsjakdhasjhd"
            val email = "lh@mega.co.nz"
            val flag = true
            val resetPasswordLinkInfo =
                ResetPasswordLinkInfo(email = email, isRequiredRecoveryKey = flag)
            whenever(accountRepository.queryResetPasswordLink(link)).thenReturn(
                resetPasswordLinkInfo
            )

            initTestClass()

            assertThat(underTest(link)).isEqualTo(resetPasswordLinkInfo)
            verify(accountRepository).queryResetPasswordLink(link)
        }
}