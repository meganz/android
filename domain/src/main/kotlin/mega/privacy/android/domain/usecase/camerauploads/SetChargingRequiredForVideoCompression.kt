package mega.privacy.android.domain.usecase.camerauploads

/**
 * Use Case that sets whether compressing videos require the device to be charged or not
 */
fun interface SetChargingRequiredForVideoCompression {

    /**
     * Invocation function
     *
     * @param chargingRequired Whether the device needs to be charged or not
     */
    suspend operator fun invoke(chargingRequired: Boolean)
}