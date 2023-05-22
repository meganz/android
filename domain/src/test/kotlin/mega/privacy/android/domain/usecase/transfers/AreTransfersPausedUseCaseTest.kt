package mega.privacy.android.domain.usecase.transfers

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.TransferRepository
import mega.privacy.android.domain.usecase.transfer.AreTransfersPausedUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

/**
 * Test class for [AreTransfersPausedUseCaseTest]
 */
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AreTransfersPausedUseCaseTest {

    private lateinit var underTest: AreTransfersPausedUseCase

    private val transferRepository = mock<TransferRepository>()

    @BeforeAll
    fun setUp() {
        underTest = AreTransfersPausedUseCase(
            transferRepository = transferRepository,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(transferRepository)
    }

    @ParameterizedTest(name = "with {0}")
    @ValueSource(booleans = [true, false])
    fun `test that correct value is returned when invoked`(expected: Boolean) =
        runTest {
            whenever(transferRepository.areTransfersPaused()).thenReturn(expected)
            val actual = underTest()
            Truth.assertThat(actual).isEqualTo(expected)
        }
}
