package mega.privacy.android.core.test.extension

import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow

fun <T> T.asHotFlow() = flow {
    emit(this@asHotFlow)
    awaitCancellation()
}
