package mega.privacy.android.domain.entity.analytics

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class ScreenViewEventIdentifierTest {

    @Test
    internal fun `test that an exception is thrown if the identifier is negative`() {
        assertThrows<IllegalArgumentException> { ScreenViewEventIdentifier("", -1) }
    }

    @Test
    internal fun `test that an exception is thrown if the identifier greater than 999`() {
        assertThrows<IllegalArgumentException> { ScreenViewEventIdentifier("", 999 + 1) }
    }
}