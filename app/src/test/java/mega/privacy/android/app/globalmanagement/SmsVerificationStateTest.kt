package mega.privacy.android.app.globalmanagement

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SmsVerificationStateTest {

    @Test
    fun `test that isVerificationShown defaults to false`() {
        assertThat(SmsVerificationState().isVerificationShown).isFalse()
    }

    @Test
    fun `test that isVerificationShown retains the assigned value`() {
        val underTest = SmsVerificationState()

        underTest.isVerificationShown = true

        assertThat(underTest.isVerificationShown).isTrue()
    }
}
