package mega.privacy.android.domain.usecase.camerauploads

/**
 * Use Case that checks whether compressing videos require the device to be charged or not
 */
fun interface IsChargingRequiredForVideoCompression {

    /**
     * Invocation function
     *
     * @return true if the device needs to be charged to compress videos, and false if otherwise
     */
    suspend operator fun invoke(): Boolean
}