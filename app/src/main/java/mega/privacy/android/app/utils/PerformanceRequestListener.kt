package mega.privacy.android.app.utils

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

    override fun onRequestSuccess(
        request: ImageRequest,
        requestId: String,
        isPrefetch: Boolean,
    ) {
        trace?.stop()
        trace = null
    }

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

    override fun onRequestCancellation(
        requestId: String,
    ) {
        trace?.incrementMetric("cancel", 1)
        trace?.stop()
        trace = null
    }

    override fun onProducerStart(
        requestId: String,
        producerName: String,
    ) {
    }

    override fun onProducerEvent(
        requestId: String,
        producerName: String,
        eventName: String,
    ) {
    }

    override fun onProducerFinishWithSuccess(
        requestId: String,
        producerName: String,
        extraMap: Map<String, String>?,
    ) {
    }

    override fun onProducerFinishWithFailure(
        requestId: String,
        producerName: String,
        t: Throwable,
        extraMap: Map<String, String>?,
    ) {
    }

    override fun onProducerFinishWithCancellation(
        requestId: String,
        producerName: String,
        extraMap: Map<String, String>?,
    ) {
    }

    override fun onUltimateProducerReached(
        requestId: String,
        producerName: String,
        successful: Boolean,
    ) {
    }

    override fun requiresExtraMap(
        requestId: String,
    ): Boolean =
        false

    private fun File?.getSizeInBytes(): String =
        try {
            requireNotNull(this)
            require(isFile)
            Files.size(this.toPath()).toString()
        } catch (ignore: Exception) {
            "0"
        }
}
