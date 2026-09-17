package mega.privacy.android.core.passcode.biometric

import androidx.biometric.BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE
import androidx.biometric.BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED
import androidx.biometric.BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE
import androidx.biometric.BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED
import androidx.biometric.BiometricManager.BIOMETRIC_STATUS_UNKNOWN
import androidx.biometric.BiometricManager.BIOMETRIC_SUCCESS
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

internal class BiometricCapabilityTest {

    @Test
    fun `test that isBiometricStatusCapable returns true when status is BIOMETRIC_SUCCESS`() {
        assertThat(BIOMETRIC_SUCCESS).isEqualTo(0)

        assertThat(isBiometricStatusCapable(BIOMETRIC_SUCCESS)).isTrue()
    }

    @Test
    fun `test that isBiometricStatusCapable returns true when status is BIOMETRIC_STATUS_UNKNOWN`() {
        assertThat(BIOMETRIC_STATUS_UNKNOWN).isEqualTo(-1)

        assertThat(isBiometricStatusCapable(BIOMETRIC_STATUS_UNKNOWN)).isTrue()
    }

    @Test
    fun `test that isBiometricStatusCapable returns false when status is BIOMETRIC_ERROR_UNSUPPORTED`() {
        assertThat(BIOMETRIC_ERROR_UNSUPPORTED).isEqualTo(-2)

        assertThat(isBiometricStatusCapable(BIOMETRIC_ERROR_UNSUPPORTED)).isFalse()
    }

    @Test
    fun `test that isBiometricStatusCapable returns false when status is BIOMETRIC_ERROR_HW_UNAVAILABLE`() {
        assertThat(BIOMETRIC_ERROR_HW_UNAVAILABLE).isEqualTo(1)

        assertThat(isBiometricStatusCapable(BIOMETRIC_ERROR_HW_UNAVAILABLE)).isFalse()
    }

    @Test
    fun `test that isBiometricStatusCapable returns false when status is BIOMETRIC_ERROR_NONE_ENROLLED`() {
        assertThat(BIOMETRIC_ERROR_NONE_ENROLLED).isEqualTo(11)

        assertThat(isBiometricStatusCapable(BIOMETRIC_ERROR_NONE_ENROLLED)).isFalse()
    }

    @Test
    fun `test that isBiometricStatusCapable returns false when status is BIOMETRIC_ERROR_NO_HARDWARE`() {
        assertThat(BIOMETRIC_ERROR_NO_HARDWARE).isEqualTo(12)

        assertThat(isBiometricStatusCapable(BIOMETRIC_ERROR_NO_HARDWARE)).isFalse()
    }

    @ParameterizedTest(name = "status {0}")
    @ValueSource(ints = [Int.MIN_VALUE, -3, 2, 13, Int.MAX_VALUE])
    fun `test that isBiometricStatusCapable returns false when status is unrecognised`(status: Int) {
        assertThat(isBiometricStatusCapable(status)).isFalse()
    }
}
