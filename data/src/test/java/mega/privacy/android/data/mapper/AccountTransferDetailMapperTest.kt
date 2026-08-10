package mega.privacy.android.data.mapper

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AccountTransferDetailMapperTest {

    private lateinit var underTest: AccountTransferDetailMapper

    @BeforeEach
    fun setUp() {
        underTest = AccountTransferDetailMapper()
    }

    @Test
    fun `test that totalTransfer and usedTransfer are passed through unchanged`() {
        val result = underTest(totalTransfer = 100L, usedTransfer = 50L)

        assertThat(result.totalTransfer).isEqualTo(100L)
        assertThat(result.usedTransfer).isEqualTo(50L)
    }

    @Test
    fun `test that usedTransferPercentage is 0 when totalTransfer is 0`() {
        val result = underTest(totalTransfer = 0L, usedTransfer = 0L)

        assertThat(result.usedTransferPercentage).isEqualTo(0)
    }

    @Test
    fun `test that usedTransferPercentage is 0 when totalTransfer is negative`() {
        val result = underTest(totalTransfer = -1L, usedTransfer = 50L)

        assertThat(result.usedTransferPercentage).isEqualTo(0)
    }

    @Test
    fun `test that usedTransferPercentage is computed from the raw used and total values`() {
        val result = underTest(totalTransfer = 200L, usedTransfer = 100L)

        assertThat(result.usedTransferPercentage).isEqualTo(50)
    }

    @Test
    fun `test that usedTransferPercentage is correct when used and total span different size units`() {
        // 512 MB used out of 2 GB total -> 25%
        val usedTransfer = 512L * 1024 * 1024
        val totalTransfer = 2L * 1024 * 1024 * 1024

        val result = underTest(totalTransfer = totalTransfer, usedTransfer = usedTransfer)

        assertThat(result.usedTransferPercentage).isEqualTo(25)
    }

    @Test
    fun `test that usedTransferPercentage is 100 when used equals total`() {
        val result = underTest(totalTransfer = 500L, usedTransfer = 500L)

        assertThat(result.usedTransferPercentage).isEqualTo(100)
    }

    @Test
    fun `test that usedTransferPercentage is 0 when used is 0 and totalTransfer is positive`() {
        val result = underTest(totalTransfer = 100L, usedTransfer = 0L)

        assertThat(result.usedTransferPercentage).isEqualTo(0)
    }

    @Test
    fun `test that usedTransferPercentage is floored when used is genuinely below total`() {
        // 9996 / 10000 = 99.96% -> 99, consistent with other clients
        val result = underTest(totalTransfer = 10000L, usedTransfer = 9996L)

        assertThat(result.usedTransferPercentage).isEqualTo(99)
    }

    @Test
    fun `test that usedTransferPercentage is 100 when a large quota is essentially full`() {
        // 240 TB quota with usedTransfer one byte short: the Float precision step at this
        // magnitude is larger than the gap, so it must read as 100% (matches other clients).
        val totalTransfer = 240L * 1024 * 1024 * 1024 * 1024
        val usedTransfer = totalTransfer - 1

        val result = underTest(totalTransfer = totalTransfer, usedTransfer = usedTransfer)

        assertThat(result.usedTransferPercentage).isEqualTo(100)
    }
}
