package mega.privacy.android.core.sharedcomponents.extension

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class StringExtTest {

    @Test
    fun `test that truncateMiddle keeps the first and last ten characters by default when it exceeds the limit`() {
        val name = "FirstBlock" + "z".repeat(40) + "FinalBlock"

        assertThat(name.truncateMiddle()).isEqualTo("FirstBlo...nalBlock")
    }

    @Test
    fun `test that truncateMiddle honours custom maxLength and edgeLength`() {
        val name = "abcdefghij"

        assertThat(name.truncateMiddle(maxLength = 6, edgeLength = 2)).isEqualTo("ab...ij")
    }

    @Test
    fun `test that truncateMiddle returns an empty string unchanged`() {
        assertThat("".truncateMiddle()).isEmpty()
    }
}
