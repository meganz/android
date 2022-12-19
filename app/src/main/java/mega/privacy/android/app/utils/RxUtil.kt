package mega.privacy.android.app.utils

import io.reactivex.rxjava3.core.CompletableEmitter
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.FlowableEmitter
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.core.SingleEmitter
import io.reactivex.rxjava3.functions.Action
import io.reactivex.rxjava3.functions.Consumer
import timber.log.Timber
import java.util.concurrent.TimeUnit

object RxUtil {
    @JvmField
    val IGNORE = Action {}

    @JvmStatic
    fun logErr(context: String): Consumer<in Throwable> {
        return Consumer { throwable: Throwable? ->
            Timber.e(throwable, "$context onError")
        }
    }

    /**
     * Took from https://stackoverflow.com/a/56479387/5004910
     */
    fun <T : Any> Flowable<T>.debounceImmediate(timeout: Long, unit: TimeUnit): Flowable<T> =
        publish { it.take(1).concatWith(it.debounce(timeout, unit)) }

    /**
     * Retrieve a Single source synchronously ignoring any errors
     * @return  Result from Single source or null if there was any errors
     */
    fun <T : Any> Single<T>.blockingGetOrNull(): T? =
        try {
            blockingGet()
        } catch (ignore: Exception) {
            null
        }

    /**
     * Attempts to emit the specified value if the downstream
     * hasn't cancelled the sequence or is otherwise terminated.
     *
     * @param value the value to signal
     */
    fun <T : Any> FlowableEmitter<T>?.tryOnNext(value: T) {
        if (this != null && !isCancelled) onNext(value)
    }

    /**
     * Attempts to signal the completion if the downstream
     * isn't disposed or is otherwise terminated.
     */
    fun <T : Any> FlowableEmitter<T>?.tryOnComplete() {
        if (this != null && !isCancelled) onComplete()
    }

    /**
     * Attempts to emit the specified value if the downstream
     * isn't disposed or is otherwise terminated.
     *
     * @param value the value to signal
     */
    fun <T : Any> SingleEmitter<T>?.tryOnSuccess(value: T) {
        if (this != null && !isDisposed) onSuccess(value)
    }

    /**
     * Attempts to signal the completion if the downstream
     * isn't disposed or is otherwise terminated.
     */
    fun CompletableEmitter?.tryOnComplete() {
        if (this != null && !isDisposed) onComplete()
    }
}
