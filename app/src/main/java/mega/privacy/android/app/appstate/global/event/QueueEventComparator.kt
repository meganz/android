package mega.privacy.android.app.appstate.global.event

import mega.privacy.android.navigation.contract.queue.NavigationQueueEvent
import mega.privacy.android.navigation.contract.queue.QueueEvent
import mega.privacy.android.navigation.contract.queue.dialog.AppDialogEvent
import javax.inject.Inject

class QueueEventComparator @Inject constructor() : Comparator<QueueEvent> {
    override fun compare(
        o1: QueueEvent?,
        o2: QueueEvent?,
    ): Int {
        return when {
            o1 == null && o2 == null -> 0
            o1 == null -> -1
            o2 == null -> 1
            o1 is NavigationQueueEvent && o2 is NavigationQueueEvent -> 0
            o1 is AppDialogEvent && o2 is AppDialogEvent -> 0
            o1 is NavigationQueueEvent && o2 is AppDialogEvent -> 1 // Navigation event supersedes dialog event
            o1 is AppDialogEvent && o2 is NavigationQueueEvent -> -1
            else -> 0
        }
    }
}