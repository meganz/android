package mega.privacy.android.domain.usecase.transfer

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.TransferRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import kotlin.random.Random

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PauseTransferByTagUseCaseTest {
    private val transferRepository: TransferRepository = mock()
    private lateinit var underTest: PauseTransferByTagUseCase

    @BeforeAll
    fun setup() {
        underTest = PauseTransferByTagUseCase(transferRepository)
    }

    @BeforeEach
    fun resetMocks() {
        reset(transferRepository)
    }

    @Test
    fun `test that transferRepository call pauseTransferByTag correctly`() {
        runTest {
            val tag = Random.nextInt()
            val isPause = Random.nextBoolean()
            underTest(tag, isPause)
            verify(transferRepository).pauseTransferByTag(tag, isPause)
        }
    }
}