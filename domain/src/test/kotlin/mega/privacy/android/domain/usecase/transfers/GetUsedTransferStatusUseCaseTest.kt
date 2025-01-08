package mega.privacy.android.domain.usecase.transfers

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.transfer.UsedTransferStatus
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetUsedTransferStatusUseCaseTest {

    private val underTest = GetUsedTransferStatusUseCase()

    @Test
    fun `test that status is NoTransferProblems when used transfer percentage is between 0 and 80`() {
        val usedTransferPercentage = 50
        val result = underTest(usedTransferPercentage)
        assertThat(result).isEqualTo(UsedTransferStatus.NoTransferProblems)
    }

    @Test
    fun `test that status is AlmostFull when used transfer percentage is between 81 and 99`() {
        val usedTransferPercentage = 85
        val result = underTest(usedTransferPercentage)
        assertThat(result).isEqualTo(UsedTransferStatus.AlmostFull)
    }

    @Test
    fun `test that status is Full when used transfer percentage is 100 or more`() {
        val usedTransferPercentage = 100
        val result = underTest(usedTransferPercentage)
        assertThat(result).isEqualTo(UsedTransferStatus.Full)
    }
}