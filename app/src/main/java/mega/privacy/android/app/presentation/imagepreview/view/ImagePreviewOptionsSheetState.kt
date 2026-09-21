package mega.privacy.android.app.presentation.imagepreview.view

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first

/**
 * State holder for the visibility of the image preview options sheet.
 *
 * @property isShown True while the options sheet should be on screen
 */
@Stable
internal class ImagePreviewOptionsSheetState {
    var isShown: Boolean by mutableStateOf(false)
        private set

    fun show() {
        isShown = true
    }

    fun hide() {
        isShown = false
    }
}

/**
 * Returns an [ImagePreviewOptionsSheetState] that is retained across configuration changes and
 * while the hosting navigation entry stays on the back stack, including while it is covered by
 * the passcode lock. It is not written to saved instance state, so it follows the lifetime of the
 * view model whose state the sheet acts on and a process death starts with the sheet closed.
 */
@Composable
internal fun rememberImagePreviewOptionsSheetState(): ImagePreviewOptionsSheetState =
    retain { ImagePreviewOptionsSheetState() }

/**
 * Keeps [modalSheetState] in step with the retained [optionsSheetState], and clears
 * [ImagePreviewOptionsSheetState.isShown] once the sheet has come to rest hidden so that asking
 * for the sheet again always reopens it.
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun SyncImagePreviewOptionsSheet(
    optionsSheetState: ImagePreviewOptionsSheetState,
    modalSheetState: ModalBottomSheetState,
) {
    LaunchedEffect(optionsSheetState, modalSheetState) {
        snapshotFlow { optionsSheetState.isShown }.collectLatest { isShown ->
            if (isShown) {
                try {
                    modalSheetState.show()
                } catch (_: CancellationException) {
                    // The sheet animates under a mutex that the scrim and drag gestures can take
                    // over, cancelling show(). Only that interruption is absorbed here; cancellation
                    // of this collector is rethrown so it still unwinds normally.
                    currentCoroutineContext().ensureActive()
                }
                awaitSheetSettledHidden(modalSheetState)
                optionsSheetState.hide()
            } else {
                modalSheetState.hide()
            }
        }
    }
}

/**
 * Suspends until [modalSheetState] is hidden and is not on its way to being shown. Waiting on the
 * target as well as the current value keeps a sheet that has not finished opening, or that the user
 * has dragged back up, from reading as closed.
 */
@OptIn(ExperimentalMaterialApi::class)
private suspend fun awaitSheetSettledHidden(modalSheetState: ModalBottomSheetState) {
    snapshotFlow {
        !modalSheetState.isVisible &&
                modalSheetState.targetValue == ModalBottomSheetValue.Hidden
    }.first { it }
}
