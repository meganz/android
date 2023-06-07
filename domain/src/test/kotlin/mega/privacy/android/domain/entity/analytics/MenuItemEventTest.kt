package mega.privacy.android.domain.entity.analytics

import com.google.common.truth.Truth
import mega.privacy.android.domain.entity.analytics.identifier.MenuItemEventIdentifier
import org.junit.jupiter.api.Test

internal class MenuItemEventTest{
    @Test
    internal fun `test that menu item event range starts at 305 000`() {
        val input = 123
        val expected = 300_000 + 5000 + input
        Truth.assertThat(
            MenuItemEvent(
                MenuItemEventIdentifier(
                    screenName = "",
                    menuItem = "",
                    menuType = "",
                    uniqueIdentifier = input,
                ),
                ""
            ).getEventIdentifier()
        ).isEqualTo(expected)
    }
}