package mega.privacy.android.app.utils

import android.os.Build
import com.facebook.imagepipeline.listener.RequestListener
import com.facebook.imagepipeline.request.ImageRequest
import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.perf.metrics.Trace
import java.io.File
import java.nio.file.Files

/**
 * Custom Request Listener to measure performance on file image requests
 *
 * @property firebasePerformance    Firebase Performance instance
 * @property traceName              Performance trace name
 */
class PerformanceRequestListener(
    private val firebasePerformance: FirebasePerformance,
    private val traceName: String,
) : RequestListener {

    private var trace: Trace? = null

    /**
     * Called when request is about to be submitted to the Orchestrator's executor queue.
     *
     * @param request which triggered the event
     * @param callerContext context of the caller of the request
     * @param requestId unique id generated automatically for each request submission
     * @param isPrefetch whether the request is a prefetch or not
     */
    override fun onRequestStart(
        request: ImageRequest,
        callerContext: Any?,
        requestId: String,
        isPrefetch: Boolean,
    ) {
        trace?.stop()
        trace = firebasePerformance.newTrace(traceName).apply {
            putAttribute("id", requestId)
            putAttribute("file_size", request.sourceFile.getSizeInBytes())
            start()
        }
    }

    /**
     * Called after successful completion of the request (all producers completed successfully).
     *
     * @param request which triggered the event
     * @param requestId unique id generated automatically for each request submission
     * @param isPrefetch whether the request is a prefetch or not
     */
    override fun onRequestSuccess(
        request: ImageRequest,
        requestId: String,
        isPrefetch: Boolean,
    ) {
        trace?.stop()
        trace = null
    }

    /**
     * Called after failure to complete the request (some producer failed).
     *
     * @param request which triggered the event
     * @param requestId unique id generated automatically for each request submission
     * @param throwable cause of failure
     * @param isPrefetch whether the request is a prefetch or not
     */
    override fun onRequestFailure(
        request: ImageRequest,
        requestId: String,
        throwable: Throwable,
        isPrefetch: Boolean,
    ) {
        trace?.incrementMetric("error", 1)
        trace?.stop()
        trace = null
    }

    /**
     * Called after the request is cancelled.
     *
     * @param requestId unique id generated automatically for each request submission
     */
    override fun onRequestCancellation(
        requestId: String,
    ) {
        trace?.incrementMetric("cancel", 1)
        trace?.stop()
        trace = null
    }

    /**
     * On producer start
     *
     * @param requestId
     * @param producerName
     */
    override fun onProducerStart(
        requestId: String,
        producerName: String,
    ) {
    }

    /**
     * On producer event
     *
     * @param requestId
     * @param producerName
     * @param eventName
     */
    override fun onProducerEvent(
        requestId: String,
        producerName: String,
        eventName: String,
    ) {
    }

    /**
     * On producer finish with success
     *
     * @param requestId
     * @param producerName
     * @param extraMap
     */
    override fun onProducerFinishWithSuccess(
        requestId: String,
        producerName: String,
        extraMap: Map<String, String>?,
    ) {
    }

    /**
     * On producer finish with failure
     *
     * @param requestId
     * @param producerName
     * @param t
     * @param extraMap
     */
    override fun onProducerFinishWithFailure(
        requestId: String,
        producerName: String,
        t: Throwable,
        extraMap: Map<String, String>?,
    ) {
    }

    /**
     * On producer finish with cancellation
     *
     * @param requestId
     * @param producerName
     * @param extraMap
     */
    override fun onProducerFinishWithCancellation(
        requestId: String,
        producerName: String,
        extraMap: Map<String, String>?,
    ) {
    }

    /**
     * On ultimate producer reached
     *
     * @param requestId
     * @param producerName
     * @param successful
     */
    override fun onUltimateProducerReached(
        requestId: String,
        producerName: String,
        successful: Boolean,
    ) {
    }

    /**
     * Requires extra map
     *
     * @param requestId
     * @return
     */
    override fun requiresExtraMap(
        requestId: String,
    ): Boolean =
        false

    /**
     * Get file size in bytes as a String
     *
     * @return  File size
     */
    private fun File?.getSizeInBytes(): String =
        try {
            requireNotNull(this)
            require(isFile)
            Files.size(this.toPath()).toString()
        } catch (ignore: Exception) {
            "0"
        }
}
