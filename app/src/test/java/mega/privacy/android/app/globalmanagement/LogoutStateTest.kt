package mega.privacy.android.app.globalmanagement

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LogoutStateTest {

    @Test
    fun `test that isLoggingOut defaults to false`() {
        assertThat(LogoutState().isLoggingOut).isFalse()
    }

    @Test
    fun `test that isLoggingOut retains the assigned value`() {
        val underTest = LogoutState()

        underTest.isLoggingOut = true

        assertThat(underTest.isLoggingOut).isTrue()
    }
}
