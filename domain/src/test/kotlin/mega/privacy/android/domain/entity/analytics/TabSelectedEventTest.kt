package mega.privacy.android.domain.entity.analytics

import com.google.common.truth.Truth
import mega.privacy.android.domain.entity.analytics.identifier.TabSelectedEventIdentifier
import org.junit.jupiter.api.Test

internal class TabSelectedEventTest {

    @Test
    internal fun `test that tab selected event range starts at 301 000`() {
        val input = 123
        val expected = 300_000 + 1000 + input
        Truth.assertThat(
            TabSelectedEvent(
                TabSelectedEventIdentifier("", "", input),
                ""
            ).getEventIdentifier()
        ).isEqualTo(expected)
    }
}