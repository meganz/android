package mega.privacy.android.domain.exception.security

/**
 * No passcode set exception
 */
class NoPasscodeSetException : RuntimeException("Attempting to check passcode with none set")