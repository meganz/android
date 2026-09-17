package mega.privacy.android.core.passcode.biometric

import android.content.Context
import androidx.biometric.BiometricManager
import timber.log.Timber

/**
 * Queries whether this device can offer biometric unlock, logging the raw status so that
 * device specific refusals are diagnosable from a bug report.
 *
 * @param context used to reach the platform biometric service
 * @return true when a biometric prompt should be offered
 */
fun hasBiometricCapability(context: Context): Boolean {
    val status = BiometricManager.from(context)
        .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)
    Timber.d("Biometric capability status: ${biometricStatusName(status)}")
    return isBiometricStatusCapable(status)
}

/**
 * Decides whether a [BiometricManager] status code means the user may be able to authenticate.
 *
 * [BiometricManager.BIOMETRIC_STATUS_UNKNOWN] is documented as "unable to determine whether the
 * user can authenticate", with callers expected to attempt the prompt and let it decide. On API 28
 * the compat layer can only see fingerprint enrolment, so a device secured with a credential whose
 * owner enrolled face or iris but no fingerprint lands here; treating that as a refusal hides
 * biometric unlock entirely.
 *
 * @param status a status code returned by [BiometricManager.canAuthenticate]
 * @return true when a biometric prompt should be offered
 */
fun isBiometricStatusCapable(status: Int): Boolean =
    status == BiometricManager.BIOMETRIC_SUCCESS ||
            status == BiometricManager.BIOMETRIC_STATUS_UNKNOWN

private fun biometricStatusName(status: Int): String {
    val name = when (status) {
        BiometricManager.BIOMETRIC_SUCCESS -> "BIOMETRIC_SUCCESS"
        BiometricManager.BIOMETRIC_STATUS_UNKNOWN -> "BIOMETRIC_STATUS_UNKNOWN"
        BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> "BIOMETRIC_ERROR_UNSUPPORTED"
        BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> "BIOMETRIC_ERROR_HW_UNAVAILABLE"
        BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> "BIOMETRIC_ERROR_NONE_ENROLLED"
        BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> "BIOMETRIC_ERROR_NO_HARDWARE"
        else -> "UNRECOGNISED"
    }
    return "$name ($status)"
}
