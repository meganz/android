package mega.privacy.android.domain.usecase.canceltoken

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.CancelTokenRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class InvalidateCancelTokenUseCaseTest {

    private lateinit var underTest: InvalidateCancelTokenUseCase

    private val cancelTokenRepository = mock<CancelTokenRepository>()


    @BeforeAll
    fun setup() {
        underTest = InvalidateCancelTokenUseCase(
            cancelTokenRepository = cancelTokenRepository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(cancelTokenRepository)
    }

    @Test
    fun `test that repository's cancel token method is called use case is invoked`() =
        runTest {
            underTest()
            verify(cancelTokenRepository).invalidateCurrentToken()
        }
}