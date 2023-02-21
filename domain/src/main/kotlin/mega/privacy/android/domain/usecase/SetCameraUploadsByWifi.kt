package mega.privacy.android.domain.usecase

/**
 * Use Case to set whether Camera Uploads should only run through Wi-Fi / Wi-Fi or Mobile Data
 */
fun interface SetCameraUploadsByWifi {

    /**
     * Invocation function
     *
     * @param wifiOnly If true, Camera Uploads will only run through Wi-Fi
     * If false, Camera Uploads can run through either Wi-Fi or Mobile Data
     */
    suspend operator fun invoke(wifiOnly: Boolean)
}