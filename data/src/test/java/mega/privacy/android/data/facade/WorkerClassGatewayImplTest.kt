package mega.privacy.android.data.facade

import androidx.work.CoroutineWorker
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.data.worker.ChatUploadsWorker
import mega.privacy.android.data.worker.DeleteOldestCompletedTransfersWorker
import mega.privacy.android.data.worker.DownloadsWorker
import mega.privacy.android.data.worker.OfflineSyncWorker
import mega.privacy.android.data.worker.UploadsWorker
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WorkerClassGatewayImplTest {

    private val underTest = WorkerClassGatewayImpl()

    @ParameterizedTest(name = "when get {0}")
    @MethodSource("provideParameters")
    fun `test that the correct worker class is returned`(
        actual: Class<out CoroutineWorker>,
        expected: Class<out CoroutineWorker>,
    ) {
        assertThat(actual).isEqualTo(expected)
    }

    private fun provideParameters() = Stream.of(
        Arguments.of(underTest.downloadsWorkerClass, DownloadsWorker::class.java),
        Arguments.of(underTest.chatUploadsWorkerClass, ChatUploadsWorker::class.java),
        Arguments.of(
            underTest.deleteOldestCompletedTransferWorkerClass,
            DeleteOldestCompletedTransfersWorker::class.java,
        ),
        Arguments.of(underTest.uploadsWorkerClass, UploadsWorker::class.java),
        Arguments.of(underTest.offlineSyncWorkerClass, OfflineSyncWorker::class.java),
    )
}
