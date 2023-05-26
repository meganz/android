package mega.privacy.android.domain.entity.analytics

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

internal class ScreenViewEventTest {

    @Test
    internal fun `test that screen event range starts at 300 000`() {
        val input = 123
        val expected = 300_000 + input
        assertThat(
            ScreenViewEvent(
                ScreenViewEventIdentifier("", input),
                ""
            ).getEventIdentifier()
        ).isEqualTo(expected)
    }
}