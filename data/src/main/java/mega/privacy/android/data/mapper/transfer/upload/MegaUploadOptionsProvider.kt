package mega.privacy.android.data.mapper.transfer.upload

import nz.mega.sdk.MegaUploadOptions
import javax.inject.Inject

/**
 * Mega upload options provider
 */
class MegaUploadOptionsProvider @Inject constructor() {
    /**
     * Invoke
     *
     * @return new MegaUploadOptions
     */
    operator fun invoke(): MegaUploadOptions? = MegaUploadOptions.createInstance()
}
