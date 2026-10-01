package mega.privacy.android.domain.testutils

import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

fun <T> hotFlow(vararg elements: T): Flow<T> = flow {
    elements.forEach {
        emit(it)
    }
    awaitCancellation()
}
