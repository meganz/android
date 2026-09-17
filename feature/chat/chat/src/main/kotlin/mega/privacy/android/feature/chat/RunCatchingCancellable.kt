package mega.privacy.android.feature.chat

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.cancellation.CancellationException

/**
 * Runs [block] and captures its failures, except when the calling coroutine has itself been
 * cancelled: that cancellation is rethrown so it keeps propagating instead of being reported to
 * the user as an ordinary failure.
 *
 * A [CancellationException] raised while the caller is still active — a timeout inside the call,
 * say — is an ordinary failure, and is captured like any other, so one chat timing out neither
 * abandons the rest of a bulk action nor leaves the user without an outcome.
 */
internal suspend inline fun <T> runCatchingCancellable(block: () -> T): Result<T> =
    runCatching(block).onFailure { currentCoroutineContext().ensureActive() }
