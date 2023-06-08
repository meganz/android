package mega.privacy.android.domain.entity.analytics

import com.google.common.truth.Truth
import mega.privacy.android.domain.entity.analytics.identifier.NotificationEventIdentifier
import org.junit.jupiter.api.Test

internal class NotificationEventTest {
    @Test
    internal fun `test that notification event range starts at 306 000`() {
        val input = 123
        val expected = 300_000 + 6000 + input
        Truth.assertThat(
            NotificationEvent(
                NotificationEventIdentifier(
                    uniqueIdentifier = input,
                    name = "",
                ),
            ).getEventIdentifier()
        ).isEqualTo(expected)
    }
}