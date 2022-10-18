package mega.privacy.android.data.extensions

import nz.mega.sdk.MegaTransfer

const val APP_DATA_BACKGROUND_TRANSFER = "BACKGROUND_TRANSFER"

/**
 * Checks whether a [MegaTransfer] is a background transfer.
 *
 * @return True if it is, false otherwise.
 */
fun MegaTransfer.isBackgroundTransfer(): Boolean =
    appData?.contains(APP_DATA_BACKGROUND_TRANSFER) == true