package mega.privacy.android.data.mapper

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class AccountTransferDetailMapperTest {

    private val fileSizeMapper = mock<FileSizeMapper>()

    private lateinit var underTest: AccountTransferDetailMapper

    @BeforeEach
    fun setUp() {
        underTest = AccountTransferDetailMapper(fileSizeMapper = fileSizeMapper)
    }

    @AfterEach
    fun tearDown() {
        reset(fileSizeMapper)
    }

    @Test
    fun `test that totalTransfer and usedTransfer are passed through unchanged`() {
        whenever(fileSizeMapper(100L)).thenReturn(100.0)
        whenever(fileSizeMapper(50L)).thenReturn(50.0)

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
    fun `test that usedTransferPercentage is computed from the mapped used and total values`() {
        whenever(fileSizeMapper(100L)).thenReturn(50.0)
        whenever(fileSizeMapper(200L)).thenReturn(100.0)

        val result = underTest(totalTransfer = 200L, usedTransfer = 100L)

        assertThat(result.usedTransferPercentage).isEqualTo(50)
    }

    @Test
    fun `test that usedTransferPercentage is 100 when mapped used equals mapped total`() {
        whenever(fileSizeMapper(500L)).thenReturn(5.0)

        val result = underTest(totalTransfer = 500L, usedTransfer = 500L)

        assertThat(result.usedTransferPercentage).isEqualTo(100)
    }

    @Test
    fun `test that usedTransferPercentage is 0 when mapped used is 0 and totalTransfer is positive`() {
        whenever(fileSizeMapper(0L)).thenReturn(0.0)
        whenever(fileSizeMapper(100L)).thenReturn(100.0)

        val result = underTest(totalTransfer = 100L, usedTransfer = 0L)

        assertThat(result.usedTransferPercentage).isEqualTo(0)
    }

    @Test
    fun `test that usedTransferPercentage truncates fractional results down`() {
        whenever(fileSizeMapper(45L)).thenReturn(45.0)
        whenever(fileSizeMapper(1000L)).thenReturn(1000.0)

        val result = underTest(totalTransfer = 1000L, usedTransfer = 45L)

        assertThat(result.usedTransferPercentage).isEqualTo(4)
    }
}
