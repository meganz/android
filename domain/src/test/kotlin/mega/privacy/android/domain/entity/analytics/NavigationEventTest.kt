package mega.privacy.android.domain.entity.analytics

import com.google.common.truth.Truth
import mega.privacy.android.domain.entity.analytics.identifier.NavigationEventIdentifier
import org.junit.jupiter.api.Test

internal class NavigationEventTest {
    @Test
    internal fun `test that navigation event range starts at 304 000`() {
        val input = 123
        val expected = 300_000 + 4000 + input
        Truth.assertThat(
            NavigationEvent(
                NavigationEventIdentifier(
                    uniqueIdentifier = input,
                    navigationElementType = "",
                    destination = "",
                ),
                ""
            ).getEventIdentifier()
        ).isEqualTo(expected)
    }
}