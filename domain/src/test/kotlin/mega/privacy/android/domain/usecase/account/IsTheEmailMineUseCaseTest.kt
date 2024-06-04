package mega.privacy.android.domain.usecase.account

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.contacts.User
import mega.privacy.android.domain.entity.user.UserVisibility
import mega.privacy.android.domain.repository.AccountRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IsTheEmailMineUseCaseTest {

    private lateinit var underTest: IsTheEmailMineUseCase

    private val accountRepository: AccountRepository = mock()

    private val email = "email@email.email"

    @BeforeEach
    fun setUp() {
        underTest = IsTheEmailMineUseCase(
            accountRepository = accountRepository
        )
    }

    @AfterEach
    fun tearDown() {
        reset(accountRepository)
    }

    @Test
    fun `test that True is returned when the given email matches the current user's email`() =
        runTest {
            val currentUser = User(
                handle = 1L,
                email = email,
                visibility = UserVisibility.Visible,
                timestamp = System.currentTimeMillis(),
                userChanges = listOf()
            )
            whenever(accountRepository.getCurrentUser()) doReturn currentUser

            val actual = underTest(email)

            assertThat(actual).isTrue()
        }

    @Test
    fun `test that False is returned when the given email does not match the current user's email`() =
        runTest {
            val currentUser = User(
                handle = 1L,
                email = "ihiy@yoyo.mama",
                visibility = UserVisibility.Visible,
                timestamp = System.currentTimeMillis(),
                userChanges = listOf()
            )
            whenever(accountRepository.getCurrentUser()) doReturn currentUser

            val actual = underTest(email)

            assertThat(actual).isFalse()
        }

    @Test
    fun `test that False is returned when the current user is NULL`() =
        runTest {
            whenever(accountRepository.getCurrentUser()) doReturn null

            val actual = underTest(email)

            assertThat(actual).isFalse()
        }
}