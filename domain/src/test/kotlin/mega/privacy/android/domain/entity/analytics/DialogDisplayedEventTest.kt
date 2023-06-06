package mega.privacy.android.domain.entity.analytics

import com.google.common.truth.Truth
import mega.privacy.android.domain.entity.analytics.identifier.DialogDisplayedEventIdentifier
import org.junit.jupiter.api.Test

internal class DialogDisplayedEventTest {
    @Test
    internal fun `test that dialog displayed event range starts at 303 000`() {
        val input = 123
        val expected = 300_000 + 3000 + input
        Truth.assertThat(
            DialogDisplayedEvent(
                DialogDisplayedEventIdentifier(
                    screenName = "",
                    dialogName = "",
                    uniqueIdentifier = input
                ),
                ""
            ).getEventIdentifier()
        ).isEqualTo(expected)
    }
}