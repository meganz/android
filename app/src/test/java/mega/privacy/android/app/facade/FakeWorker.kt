package mega.privacy.android.app.facade

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import kotlinx.coroutines.delay

/**
 *  Fake worker simulating work to be used in tests.
 */
internal class FakeWorker(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        delay(1000)
        return Result.success()
    }
}
