package mega.privacy.android.core.coroutine

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LogTypeNameTest {

    private data class Sensitive(val link: String)

    @Test
    fun `test that logTypeName returns the class name without the field values`() {
        val actual = Sensitive(link = "https://mega.nz/file/HANDLE#KEY").logTypeName()

        assertThat(actual).isEqualTo("Sensitive")
    }

    @Test
    fun `test that logTypeName returns null literal when the value is null`() {
        val value: Sensitive? = null

        assertThat(value.logTypeName()).isEqualTo("null")
    }

    @Test
    fun `test that logTypeName returns Anonymous when the class has no simple name`() {
        val value = object {}

        assertThat(value.logTypeName()).isEqualTo("Anonymous")
    }
}
