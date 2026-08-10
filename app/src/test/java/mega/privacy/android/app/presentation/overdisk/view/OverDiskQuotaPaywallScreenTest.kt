package mega.privacy.android.app.presentation.overdisk.view

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class OverDiskQuotaPaywallScreenTest {

    @Test
    fun `test that nested markup is flattened into non-overlapping tags`() {
        val input = "[B]You have [M]44 days[/M] left to upgrade[/B]. After that."
        val expected = "[B]You have [/B][M]44 days[/M][B] left to upgrade[/B]. After that."

        assertThat(flattenNestedMarkup(input)).isEqualTo(expected)
    }

    @Test
    fun `test that markup without the colored tag is left unchanged`() {
        val input = "[B]You must act immediately to save your data.[/B]"

        assertThat(flattenNestedMarkup(input)).isEqualTo(input)
    }

    @Test
    fun `test that text without any markup is left unchanged`() {
        val input = "Your data is currently subject to deletion."

        assertThat(flattenNestedMarkup(input)).isEqualTo(input)
    }
}
