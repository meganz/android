package mega.privacy.android.app.utils

import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.functions.Action
import io.reactivex.rxjava3.functions.Consumer
import java.util.concurrent.TimeUnit

object RxUtil {
    @JvmField
    val IGNORE = Action {}

    @JvmStatic
    fun logErr(context: String): Consumer<in Throwable> {
        return Consumer { throwable: Throwable? ->
            LogUtil.logError("$context onError", throwable)
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
    fun <T> Single<T>.blockingGetOrNull(): T? =
        try {
            blockingGet()
        } catch (ignore: Exception) {
            null
        }

    /**
     * Add the disposable to a HashCompositeDisposable.
     *
     * @param key           Disposable associated key
     * @param disposable    HashCompositeDisposable to add this disposable to
     * @return              this instance
     */
    fun Disposable.addTo(key: Long, disposable: HashCompositeDisposable): Disposable =
        apply { disposable.add(key, this) }
}
