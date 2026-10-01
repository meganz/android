package mega.privacy.android.domain.usecase.login

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

/**
 * Test class for [IsUserLoggedInUseCase]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class IsUserLoggedInUseCaseTest {

    private lateinit var underTest: IsUserLoggedInUseCase

    private val getSessionUseCase = mock<GetSessionUseCase>()

    @BeforeAll
    fun setUp() {
        underTest = IsUserLoggedInUseCase(getSessionUseCase)
    }

    @BeforeEach
    fun resetMocks() {
        reset(getSessionUseCase)
    }

    @Test
    fun `test that the user is logged in`() = runTest {
        whenever(getSessionUseCase()) doReturn "sessionString"

        assertThat(underTest.invoke()).isTrue()
    }

    @Test
    fun `test that the user is not logged in`() = runTest {
        whenever(getSessionUseCase()) doReturn null

        assertThat(underTest.invoke()).isFalse()
    }
}