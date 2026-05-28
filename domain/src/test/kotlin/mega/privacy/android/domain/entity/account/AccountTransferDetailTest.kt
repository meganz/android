package mega.privacy.android.domain.entity.account

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AccountTransferDetailTest {

    @Test
    fun `test that usedTransferPercentage is 0 when totalTransfer is 0`() {
        val underTest = AccountTransferDetail(totalTransfer = 0L, usedTransfer = 0L)
        assertThat(underTest.usedTransferPercentage).isEqualTo(0)
    }

    @Test
    fun `test that usedTransferPercentage truncates fractional values down to match iOS and web`() {
        val underTest = AccountTransferDetail(totalTransfer = 100L, usedTransfer = 4L)
        assertThat(underTest.usedTransferPercentage).isEqualTo(4)
    }

    @Test
    fun `test that usedTransferPercentage truncates a value that would otherwise round up`() {
        val underTest = AccountTransferDetail(totalTransfer = 1000L, usedTransfer = 45L)
        assertThat(underTest.usedTransferPercentage).isEqualTo(4)
    }

    @Test
    fun `test that usedTransferPercentage is 100 when usedTransfer equals totalTransfer`() {
        val underTest = AccountTransferDetail(totalTransfer = 100L, usedTransfer = 100L)
        assertThat(underTest.usedTransferPercentage).isEqualTo(100)
    }

    @Test
    fun `test that usedTransferPercentage stays below 100 when usedTransfer is just under totalTransfer`() {
        val underTest = AccountTransferDetail(totalTransfer = 1000L, usedTransfer = 999L)
        assertThat(underTest.usedTransferPercentage).isEqualTo(99)
    }
}
