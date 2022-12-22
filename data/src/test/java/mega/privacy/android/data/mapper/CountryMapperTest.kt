package mega.privacy.android.data.mapper

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CountryMapperTest {
    @Test
    fun `test that country type can be mapped correctly`() {
        val name = "NewZealand"
        val code = "NZ"
        val callingCode = "+64"
        val actual = toCountry(name, code, callingCode)
        assertThat(actual.name).isEqualTo(name)
        assertThat(actual.code).isEqualTo(code)
        assertThat(actual.callingCode).isEqualTo(callingCode)
    }
}
