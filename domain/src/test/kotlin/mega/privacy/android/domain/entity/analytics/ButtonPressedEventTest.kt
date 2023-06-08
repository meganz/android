package mega.privacy.android.domain.entity.analytics

import com.google.common.truth.Truth
import mega.privacy.android.domain.entity.analytics.identifier.ButtonPressedEventIdentifier
import org.junit.jupiter.api.Test

internal class ButtonPressedEventTest {
    @Test
    internal fun `test that button press event range starts at 302 000`() {
        val input = 123
        val expected = 300_000 + 2000 + input
        Truth.assertThat(
            ButtonPressedEvent(
                ButtonPressedEventIdentifier(
                    screenName = "",
                    dialogName = "",
                    uniqueIdentifier = input,
                    buttonName = "",
                ),
                ""
            ).getEventIdentifier()
        ).isEqualTo(expected)
    }
}