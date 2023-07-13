package mega.privacy.android.feature.devicecenter.domain.exception

import mega.privacy.android.domain.exception.MegaException

/**
 * [MegaException] that indicates the type of error the occurred when setting a name to the current
 * Device
 *
 * @param errorCode The Error Code from the SDK
 * @param errorString The Error Message from the SDK
 */
sealed class SetDeviceNameException(errorCode: Int, errorString: String?) :
    MegaException(errorCode, errorString) {

    /**
     * A Device with the same name already exists
     * This is thrown when the [errorCode] is MegaError.API_EEXIST
     */
    class NameAlreadyExists(errorCode: Int, errorString: String? = null) :
        SetDeviceNameException(errorCode, errorString)

    /**
     * An unknown error occurred when renaming a Device
     * This is thrown when no matching [errorCode] is found in [SetDeviceNameException]
     */
    class Unknown(errorCode: Int, errorString: String? = null) :
        SetDeviceNameException(errorCode, errorString)
}
