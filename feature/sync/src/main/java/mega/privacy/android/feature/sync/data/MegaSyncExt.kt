package mega.privacy.android.feature.sync.data

import mega.privacy.android.feature.sync.data.mock.MegaSync
import mega.privacy.android.feature.sync.data.mock.MegaSyncList

/**
 * Extension to bring the forEach method into MegaSyncList
 * Note that MegaSyncList does not extend from List and hence does not have built-in foreach method
 */
internal fun MegaSyncList.forEach(action: (MegaSync) -> Unit) {
    (0 until size()).forEach { index ->
        action(get(index))
    }
}