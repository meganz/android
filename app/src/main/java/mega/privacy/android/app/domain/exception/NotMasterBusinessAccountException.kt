package mega.privacy.android.app.domain.exception

/**
 * Not master business account exception
 *
 *
 * @param errorCode
 * @param errorString
 */
class NotMasterBusinessAccountException(errorCode: Int? = null, errorString: String? = null) : MegaException(errorCode, errorString)
