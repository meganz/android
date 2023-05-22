package test.mega.privacy.android.app.upgradeAccount.model.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.R
import mega.privacy.android.app.upgradeAccount.model.FormattedSize
import mega.privacy.android.app.upgradeAccount.model.mapper.FormattedSizeMapper
import org.junit.Test

class FormattedSizeMapperTest {
    private val underTest = FormattedSizeMapper()

    @Test
    fun `test that mapper returns correct formatted GB size and GB string id`() {
        val expectedResult = FormattedSize(R.string.label_file_size_giga_byte, "400")
        assertThat(underTest(400)).isEqualTo(expectedResult)
    }

    @Test
    fun `test that mapper returns correct formatted TB size and TB string id`() {
        val expectedResult = FormattedSize(R.string.label_file_size_tera_byte, "1")
        assertThat(underTest(1024)).isEqualTo(expectedResult)
    }
}