package mega.privacy.android.app.data.extensions

import mega.privacy.android.app.utils.Constants
import nz.mega.sdk.MegaTransfer

/**
 * Checks whether a [MegaTransfer] is a background transfer.
 *
 * @return True if it is, false otherwise.
 */
fun MegaTransfer.isBackgroundTransfer(): Boolean =
    appData?.contains(Constants.APP_DATA_BACKGROUND_TRANSFER) == true

/**
 * Checks whether a [MegaTransfer] is a voice clip type transfer.
 *
 * @return True if it is, false otherwise.
 */
fun MegaTransfer.isVoiceClipTransfer(): Boolean =
    appData?.contains(Constants.APP_DATA_VOICE_CLIP) == true