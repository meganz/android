package mega.privacy.android.data.repository

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.CancelTokenRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DefaultCancelTokenRepositoryTest {

    private lateinit var underTest: CancelTokenRepository

    private val cancelTokenProvider = mock<CancelTokenProvider>()


    @BeforeAll
    fun setup() {
        underTest = DefaultCancelTokenRepository(
            cancelTokenProvider = cancelTokenProvider,
            ioDispatcher = UnconfinedTestDispatcher(),
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(cancelTokenProvider)
    }

    @Test
    fun `test that provider's cancel token method is called when cancel token is called`() =
        runTest {
            underTest.cancelCurrentToken()
            verify(cancelTokenProvider).cancelCurrentToken()
        }

    @Test
    fun `test that provider's invalidate token method is called when invalidate token is called`() =
        runTest {
            underTest.invalidateCurrentToken()
            verify(cancelTokenProvider).invalidateCurrentToken()
        }
}