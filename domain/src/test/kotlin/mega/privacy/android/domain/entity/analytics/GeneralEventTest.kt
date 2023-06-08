package mega.privacy.android.domain.entity.analytics

import com.google.common.truth.Truth
import mega.privacy.android.domain.entity.analytics.identifier.GeneralEventIdentifier
import org.junit.jupiter.api.Test

internal class GeneralEventTest {
    @Test
    internal fun `test that general event range starts at 307 000`() {
        val input = 123
        val expected = 300_000 + 7000 + input
        Truth.assertThat(
            GeneralEvent(
                GeneralEventIdentifier(
                    uniqueIdentifier = input,
                    name = "",
                    info = "",
                ),
                ""
            ).getEventIdentifier()
        ).isEqualTo(expected)
    }
}