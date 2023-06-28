package mega.privacy.android.data.mapper.transfer.active

import com.google.common.truth.Truth
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.data.database.entity.ActiveTransferEntity
import mega.privacy.android.domain.entity.transfer.ActiveTransfer
import mega.privacy.android.domain.entity.transfer.TransferType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ActiveTransferEntityMapperTest {
    private lateinit var underTest: ActiveTransferEntityMapper


    @BeforeAll
    fun setUp() {
        underTest = ActiveTransferEntityMapper()
    }

    @ParameterizedTest(name = "Transfer Type {0}")
    @EnumSource(TransferType::class)
    fun `test that mapper returns model correctly when invoke function`(transferType: TransferType) =
        runTest {
            val model = ActiveTransfer(TAG, transferType, TOTAL, TRANSFERRED, true)
            val expected = ActiveTransferEntity(TAG, transferType, TOTAL, TRANSFERRED, true)
            Truth.assertThat(underTest(model)).isEqualTo(expected)
        }

    companion object {
        private const val TAG = 2
        private const val TOTAL = 1024L
        private const val TRANSFERRED = 1024L
    }
}