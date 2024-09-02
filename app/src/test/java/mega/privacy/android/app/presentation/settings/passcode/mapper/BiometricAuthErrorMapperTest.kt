package mega.privacy.android.app.presentation.settings.passcode.mapper

import androidx.biometric.BiometricPrompt
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.presentation.settings.passcode.biometric.BiometricAuthError
import org.junit.jupiter.api.Test

class BiometricAuthErrorMapperTest {
    private val underTest: BiometricAuthErrorMapper = BiometricAuthErrorMapper()

    @Test
    fun `test that user cancellation errors return the correct error`() {
        assertThat(
            underTest(
                errorCode = BiometricPrompt.ERROR_USER_CANCELED,
                message = ""
            )
        ).isEqualTo(
            BiometricAuthError.UserDeclined
        )

        assertThat(
            underTest(
                errorCode = BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                message = ""
            )
        ).isEqualTo(
            BiometricAuthError.UserDeclined
        )
    }

    @Test
    fun `test that non user errors return message`() {
        val expected = "Expected"
        assertThat(
            underTest(
                errorCode = 12345,
                message = expected
            ).reason
        ).isEqualTo(expected)
    }
}