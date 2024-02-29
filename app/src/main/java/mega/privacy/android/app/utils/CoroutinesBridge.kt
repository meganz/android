package mega.privacy.android.app.utils

import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext

fun <T> onResult(
    scope: CoroutineScope,
    callback: (T?) -> Unit,
) = object : Continuation<T?> {

    override val context: CoroutineContext = scope.coroutineContext

    override fun resumeWith(result: Result<T?>) =
        callback(result.getOrNull())
}