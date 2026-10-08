package mega.privacy.android.feature.sync.domain.extension

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.produce
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.time.Duration

/** Emits the first value at once, then at most one value per [window], always the latest (leading and trailing). */
@OptIn(ExperimentalCoroutinesApi::class)
internal fun <T> Flow<T>.throttleLatest(window: Duration): Flow<T> = flow {
    coroutineScope {
        val latest = produce(capacity = Channel.CONFLATED) { collect { send(it) } }
        for (value in latest) {
            emit(value)
            delay(window)
        }
    }
}
