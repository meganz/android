package mega.privacy.android.app.utils.threadpool

import android.os.Process
import timber.log.Timber
import java.util.concurrent.ThreadFactory

/**
 * Wrapper to set the priority of the calling thread w.r.t underlying Linux OS
 */
class MegaThreadFactory(val threadPriority: Int) : ThreadFactory {

    override fun newThread(runnable: Runnable?): Thread {
        val wrapperRunnable = Runnable {
            try {
                Process.setThreadPriority(threadPriority)
            } catch (t: Throwable) {
                Timber.e(t, "Error while running MegaThreadFactory")
            }
            runnable?.run()
        }
        return Thread(wrapperRunnable)
    }
}