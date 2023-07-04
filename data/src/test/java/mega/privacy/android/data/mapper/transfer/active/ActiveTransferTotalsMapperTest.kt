package mega.privacy.android.data.mapper.transfer.active

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.database.entity.ActiveTransferTotalsEntity
import mega.privacy.android.domain.entity.transfer.ActiveTransferTotals
import mega.privacy.android.domain.entity.transfer.TransferType
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ActiveTransferTotalsMapperTest {

    private lateinit var underTest: ActiveTransferTotalsMapper


    @BeforeAll
    fun setUp() {
        underTest = ActiveTransferTotalsMapper()
    }

    @ParameterizedTest(name = "Transfer Type {0}")
    @EnumSource(TransferType::class)
    fun `test that mapper returns entity correctly when invoke function`(transferType: TransferType) =
        runTest {
            val entity = ActiveTransferTotalsEntity(
                transferType,
                TOTAL_TRANSFERS,
                TOTAL_FINISHED_TRANSFERS,
                TOTAL_BYTES,
                TRANSFERRED_BYTES
            )
            val expected = ActiveTransferTotals(
                transferType,
                TOTAL_TRANSFERS,
                TOTAL_FINISHED_TRANSFERS,
                TOTAL_BYTES,
                TRANSFERRED_BYTES
            )
            Truth.assertThat(underTest(entity)).isEqualTo(expected)
        }

    @Test
    fun `test that mapper returns empty entity when null entity is mapped`() =
        runTest {
            val expected = ActiveTransferTotals(TransferType.NONE, 0, 0, 0, 0)
            Truth.assertThat(underTest(null)).isEqualTo(expected)
        }

    companion object {
        private const val TOTAL_TRANSFERS = 20
        private const val TOTAL_FINISHED_TRANSFERS = 10
        private const val TOTAL_BYTES = 2048L
        private const val TRANSFERRED_BYTES = 1024L
    }
}