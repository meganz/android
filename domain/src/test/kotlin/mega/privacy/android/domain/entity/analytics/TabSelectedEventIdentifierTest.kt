package mega.privacy.android.domain.entity.analytics

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class TabSelectedEventIdentifierTest {

    @Test
    internal fun `test that an exception is thrown if the identifier is negative`() {
        assertThrows<IllegalArgumentException> {
            TabSelectedEventIdentifier(
                screenName = "",
                tabName = "",
                uniqueIdentifier = -1
            )
        }
    }

    @Test
    internal fun `test that an exception is thrown if the identifier greater than 999`() {
        assertThrows<IllegalArgumentException> {
            TabSelectedEventIdentifier(
                screenName = "",
                tabName = "",
                uniqueIdentifier = 999 + 1
            )
        }
    }
}