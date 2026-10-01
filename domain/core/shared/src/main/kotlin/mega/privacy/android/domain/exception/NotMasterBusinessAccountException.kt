package mega.privacy.android.domain.exception

/**
 * Not master business account exception
 *
 *
 * @param errorCode
 * @param errorString
 */
class NotMasterBusinessAccountException(errorCode: Int, errorString: String? = null) :
    MegaException(errorCode, errorString)
