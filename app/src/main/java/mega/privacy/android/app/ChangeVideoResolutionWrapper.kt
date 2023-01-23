package mega.privacy.android.app

import android.content.Context
import mega.privacy.android.app.main.megachat.ChatUploadService
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Wrapper class used to change video resolution
 *
 * @property mChanger [VideoDownSampling]
 * @property queue [ConcurrentLinkedQueue]
 * @property context [Context]
 */
class ChangeVideoResolutionWrapper(
    private val mChanger: VideoDownSampling,
    private val queue: ConcurrentLinkedQueue<VideoUpload>,
    private val context: Context,
) : Runnable {

    private var mThrowable: Throwable? = null

    /**
     * Runs the thread
     */
    override fun run() {
        val video = queue.peek()
        if (video != null) {
            val newPath = video.newPath
            try {
                while (queue.isNotEmpty()) {
                    val videoToProcess = queue.poll()
                    if (videoToProcess != null) {
                        mChanger.prepareAndChangeResolution(videoToProcess)
                    }
                }
            } catch (throwable: Throwable) {
                mThrowable = throwable
                if (context is ChatUploadService) {
                    context.finishDownsampling(
                        returnedFile = newPath,
                        success = false,
                        idPendingMessage = video.pendingMessageId,
                    )
                }
            }
        }
    }

    /**
     * Checks if the Exception should be thrown or not
     */
    fun shouldThrow() {
        if (mThrowable != null) throw mThrowable as Throwable
    }
}