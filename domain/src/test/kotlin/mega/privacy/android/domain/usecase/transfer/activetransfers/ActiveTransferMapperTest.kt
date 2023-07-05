package mega.privacy.android.domain.usecase.transfer.activetransfers

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.transfer.ActiveTransfer
import mega.privacy.android.domain.entity.transfer.ActiveTransferMapper
import mega.privacy.android.domain.entity.transfer.Transfer
import mega.privacy.android.domain.entity.transfer.TransferType
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.mock

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ActiveTransferMapperTest {

    private lateinit var underTest: ActiveTransferMapper

    @BeforeAll
    fun setup() {
        underTest = ActiveTransferMapper()
    }

    @ParameterizedTest
    @EnumSource(TransferType::class)
    fun `test that active transfer mapper returns correctly`(
        transferType: TransferType,
    ) = runTest {
        val expected = ActiveTransfer(
            tag = TAG,
            transferType = transferType,
            totalBytes = TOTAL_BYTES,
            transferredBytes = TRANSFERRED_BYTES,
            isFinished = IS_FINISHED,
        )
        val original = mock<Transfer> {
            on { tag }.thenReturn(TAG)
            on { type }.thenReturn(transferType)
            on { totalBytes }.thenReturn(TOTAL_BYTES)
            on { transferredBytes }.thenReturn(TRANSFERRED_BYTES)
            on { isFinished }.thenReturn(IS_FINISHED)
        }
        val actual = underTest(original)
        Truth.assertThat(actual).isEqualTo(expected)
    }

    companion object {
        const val TAG = 11
        const val TOTAL_BYTES = 1133L
        const val TRANSFERRED_BYTES = 110L
        const val IS_FINISHED = false
    }
}