package mega.privacy.android.feature.sync.data

import nz.mega.sdk.MegaSync
import nz.mega.sdk.MegaSyncList

/**
 * Extension to bring the forEach method into MegaSyncList
 * Note that MegaSyncList does not extend from List and hence does not have built-in foreach method
 */
internal fun MegaSyncList.forEach(action: (MegaSync) -> Unit) {
    (0 until size()).forEach { index ->
        action(get(index))
    }
}