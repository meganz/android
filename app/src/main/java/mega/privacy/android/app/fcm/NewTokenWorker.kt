package mega.privacy.android.app.fcm

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

/**
 * Worker class to manage device token updates.
 */
class NewTokenWorker(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    override fun doWork(): Result {
        TODO("Not yet implemented")
    }

    companion object {
        const val NEW_TOKEN = "NEW_TOKE"
    }
}