package mega.privacy.android.app.listeners

import com.facebook.imagepipeline.listener.RequestListener
import com.facebook.imagepipeline.request.ImageRequest

/**
 * RequestListener with optional callbacks.
 */
class OptionalRequestListener(
    private val onProducerStart: ((String?, String?) -> Unit)? = null,
    private val onProducerEvent: ((String?, String?, String?) -> Unit)? = null,
    private val onProducerFinishWithSuccess: ((String?, String?, MutableMap<String, String>?) -> Unit)? = null,
    private val onProducerFinishWithFailure: ((String?, String?, Throwable?, MutableMap<String, String>?) -> Unit)? = null,
    private val onProducerFinishWithCancellation: ((String?, String?, MutableMap<String, String>?) -> Unit)? = null,
    private val onUltimateProducerReached: ((String?, String?, Boolean) -> Unit)? = null,
    private val requiresExtraMap: Boolean = false,
    private val onRequestStart: ((ImageRequest?, Any?, String?, Boolean) -> Unit)? = null,
    private val onRequestSuccess: ((ImageRequest?, String?, Boolean) -> Unit)? = null,
    private val onRequestFailure: ((ImageRequest?, String?, Throwable?, Boolean) -> Unit)? = null,
    private val onRequestCancellation: ((String?) -> Unit)? = null
) : RequestListener {

    override fun onProducerStart(requestId: String?, producerName: String?) {
        onProducerStart?.invoke(requestId, producerName)
    }

    override fun onProducerEvent(requestId: String?, producerName: String?, eventName: String?) {
        onProducerEvent?.invoke(requestId, producerName, eventName)
    }

    override fun onProducerFinishWithSuccess(
        requestId: String?,
        producerName: String?,
        extraMap: MutableMap<String, String>?
    ) {
        onProducerFinishWithSuccess?.invoke(requestId, producerName, extraMap)
    }

    override fun onProducerFinishWithFailure(
        requestId: String?,
        producerName: String?,
        t: Throwable?,
        extraMap: MutableMap<String, String>?
    ) {
        onProducerFinishWithFailure?.invoke(requestId, producerName, t, extraMap)
    }

    override fun onProducerFinishWithCancellation(
        requestId: String?,
        producerName: String?,
        extraMap: MutableMap<String, String>?
    ) {
        onProducerFinishWithCancellation?.invoke(requestId, producerName, extraMap)
    }

    override fun onUltimateProducerReached(
        requestId: String?,
        producerName: String?,
        successful: Boolean
    ) {
        onUltimateProducerReached?.invoke(requestId, producerName, successful)
    }

    override fun requiresExtraMap(requestId: String?): Boolean =
        requiresExtraMap

    override fun onRequestStart(
        request: ImageRequest?,
        callerContext: Any?,
        requestId: String?,
        isPrefetch: Boolean
    ) {
        onRequestStart?.invoke(request, callerContext, requestId, isPrefetch)
    }

    override fun onRequestSuccess(request: ImageRequest?, requestId: String?, isPrefetch: Boolean) {
        onRequestSuccess?.invoke(request, requestId, isPrefetch)
    }

    override fun onRequestFailure(
        request: ImageRequest?,
        requestId: String?,
        throwable: Throwable?,
        isPrefetch: Boolean
    ) {
        onRequestFailure?.invoke(request, requestId, throwable, isPrefetch)
    }

    override fun onRequestCancellation(requestId: String?) {
        onRequestCancellation?.invoke(requestId)
    }
}