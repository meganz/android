package mega.privacy.android.domain.entity.analytics.identifier

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class DialogDisplayedEventIdentifierTest {
    @Test
    internal fun `test that an exception is thrown if the identifier is negative`() {
        assertThrows<IllegalArgumentException> {
            DialogDisplayedEventIdentifier(
                screenName = "",
                dialogName = "",
                uniqueIdentifier = -1
            )
        }
    }

    @Test
    internal fun `test that an exception is thrown if the identifier greater than 999`() {
        assertThrows<IllegalArgumentException> {
            DialogDisplayedEventIdentifier(
                screenName = "",
                dialogName = "",
                uniqueIdentifier = 999 + 1
            )
        }
    }
}