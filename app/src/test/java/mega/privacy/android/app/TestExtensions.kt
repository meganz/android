package mega.privacy.android.app

import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.StateEventWithContentTriggered

fun <T> StateEventWithContent<T>.triggeredContent(): T? =
    (this as? StateEventWithContentTriggered<T>)?.content
