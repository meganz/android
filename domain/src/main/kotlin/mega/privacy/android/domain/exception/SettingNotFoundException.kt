package mega.privacy.android.domain.exception

/**
 * Setting not found exception
 *
 *
 * @param errorCode
 * @param errorString
 */
class SettingNotFoundException(errorCode: Int, errorString: String? = null) :
    MegaException(errorCode, errorString)
