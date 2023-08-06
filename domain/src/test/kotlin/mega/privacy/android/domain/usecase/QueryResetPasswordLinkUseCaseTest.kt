package mega.privacy.android.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.domain.entity.RegexPatternType
import mega.privacy.android.domain.exception.ResetPasswordLinkException
import mega.privacy.android.domain.repository.AccountRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class QueryResetPasswordLinkUseCaseTest {
    private lateinit var underTest: QueryResetPasswordLinkUseCase
    private val accountRepository: AccountRepository = mock()
    private val getUrlRegexPatternTypeUseCase: GetUrlRegexPatternTypeUseCase = mock()

    @BeforeAll
    internal fun init() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @BeforeEach
    internal fun setup() {
        reset(accountRepository, getUrlRegexPatternTypeUseCase)
    }

    private fun initTestClass() {
        underTest = QueryResetPasswordLinkUseCase(accountRepository, getUrlRegexPatternTypeUseCase)
    }

    @Test
    internal fun `test that use case should return correct url value when invoked and url type is RESET_PASSWORD_LINK`() =
        runTest {
            val link = "qhwuehqwuhajsdhsjakdhasjhd"
            whenever(getUrlRegexPatternTypeUseCase(link)).thenReturn(RegexPatternType.RESET_PASSWORD_LINK)
            whenever(accountRepository.queryResetPasswordLink(link)).thenReturn(link)

            initTestClass()

            assertThat(underTest(link)).isEqualTo(link)
            verify(accountRepository).queryResetPasswordLink(link)
        }

    @Test
    internal fun `test that use case should throw exception when invoked and url type is not RESET_PASSWORD_LINK`() =
        runTest {
            val link = "qhwuehqwuhajsdhsjakdhasjhd"
            whenever(getUrlRegexPatternTypeUseCase(link)).thenReturn(RegexPatternType.CHAT_LINK)

            initTestClass()

            assertThrows<ResetPasswordLinkException.LinkInvalid> {
                underTest(link)
            }
        }
}