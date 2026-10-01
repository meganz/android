package mega.privacy.android.domain.exception.security

/**
 * No passcode type set exception
 */
class NoPasscodeTypeSetException :
    RuntimeException("Attempting to check passcode type with none set")
