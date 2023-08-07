package test.mega.privacy.android.app.extensions

import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.flow

internal fun <T> T.asHotFlow() = flow {
    emit(this@asHotFlow)
    awaitCancellation()
}