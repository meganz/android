package test.mega.privacy.android.app.upgradeAccount.model.mapper

import mega.privacy.android.app.R
import mega.privacy.android.app.upgradeAccount.model.mapper.toFormattedSizeGBBased
import org.junit.Assert.*
import org.junit.Test

class FormattedSizeGBBasedMapperTest {
    @Test
    fun `test that mapper returns correct formatted GB size and GB string id`() {
        val expectedResult = Pair(R.string.label_file_size_giga_byte, "400")
        assertEquals(expectedResult, toFormattedSizeGBBased(400))
    }

    @Test
    fun `test that mapper returns correct formatted TB size and TB string id`() {
        val expectedResult = Pair(R.string.label_file_size_tera_byte, "1")
        assertEquals(expectedResult, toFormattedSizeGBBased(1024))
    }
}