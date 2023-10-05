package mega.privacy.android.app.presentation.photos.timeline

import android.content.DialogInterface
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewModelScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.photos.PhotosFragment
import mega.privacy.android.app.presentation.photos.timeline.model.CameraUploadsStatus
import mega.privacy.android.app.presentation.photos.timeline.view.CameraUploadsStatusCompleted
import mega.privacy.android.app.presentation.photos.timeline.view.CameraUploadsStatusSync
import mega.privacy.android.app.presentation.photos.timeline.view.CameraUploadsStatusUploading
import mega.privacy.android.app.presentation.photos.timeline.view.CameraUploadsStatusWarning
import mega.privacy.android.app.presentation.photos.timeline.viewmodel.TimelineViewModel


var testJob: Job? = null

fun TimelineViewModel.mockCUWithNoInternet() {
    testJob?.cancel()
    testJob = viewModelScope.launch {
        _state.update {
            it.copy(cameraUploadsStatus = CameraUploadsStatus.None)
        }
        delay(2000)
        _state.update {
            it.copy(
                cameraUploadsStatus = CameraUploadsStatus.Sync,
                cameraUploadsTotalFiles = 10000,
            )
        }
        delay(3000L)
        _state.update {
            it.copy(cameraUploadsStatus = CameraUploadsStatus.Uploading)
        }

        (0..100).forEach { number ->
            val cameraUploadsProgress = number / 100f
            val pending = (100 - number) * 100
            _state.update {
                it.copy(
                    cameraUploadsProgress = cameraUploadsProgress,
                    cameraUploadsPending = pending
                )
            }
            delay(200)
            if (number == 80) {
                _state.update {
                    it.copy(
                        cameraUploadsStatus = CameraUploadsStatus.Warning
                    )
                }
                delay(5000)
                _state.update {
                    it.copy(
                        cameraUploadsStatus = CameraUploadsStatus.Uploading
                    )
                }
            }
        }
        _state.update {
            it.copy(cameraUploadsStatus = CameraUploadsStatus.Completed)
        }
    }
}

fun TimelineViewModel.setCUStatusFabs(showCUStatusFabs: Boolean) {
    _state.update {
        it.copy(
            showCUStatusFabs = showCUStatusFabs
        )
    }
}

fun TimelineViewModel.mockCUAndComplete() {
    testJob?.cancel()
    testJob = viewModelScope.launch {
        _state.update {
            it.copy(
                cameraUploadsStatus = CameraUploadsStatus.None,
            )
        }
        delay(2000)
        _state.update {
            it.copy(
                cameraUploadsStatus = CameraUploadsStatus.Sync,
                cameraUploadsTotalFiles = 10000,
            )
        }
        delay(3000L)
        _state.update {
            it.copy(cameraUploadsStatus = CameraUploadsStatus.Uploading)
        }

        (0..100).forEach { number ->
            val cameraUploadsProgress = number / 100f
            val pending = (100 - number) * 100
            _state.update {
                it.copy(
                    cameraUploadsProgress = cameraUploadsProgress,
                    cameraUploadsPending = pending
                )
            }
            delay(200)
        }
        _state.update {
            it.copy(cameraUploadsStatus = CameraUploadsStatus.Completed)
        }
    }
}

fun TimelineViewModel.mockCUAndCompleteWithSingleFile() {
    testJob?.cancel()
    testJob = viewModelScope.launch {
        _state.update {
            it.copy(
                cameraUploadsStatus = CameraUploadsStatus.None,
            )
        }
        delay(2000)
        _state.update {
            it.copy(
                cameraUploadsStatus = CameraUploadsStatus.Sync,
                cameraUploadsTotalFiles = 1,
            )
        }
        delay(3000L)
        _state.update {
            it.copy(cameraUploadsStatus = CameraUploadsStatus.Uploading)
        }

        (0..100).forEach { number ->
            val cameraUploadsProgress = number / 100f
            val pending = 1
            _state.update {
                it.copy(
                    cameraUploadsProgress = cameraUploadsProgress,
                    cameraUploadsPending = pending
                )
            }
            delay(200)
        }
        _state.update {
            it.copy(cameraUploadsStatus = CameraUploadsStatus.Completed)
        }
    }
}

fun PhotosFragment.handleCUIconsOnAppBar(show: Boolean) {
    this.menu?.findItem(R.id.action_cu_status_sync)?.isVisible = show
    this.menu?.findItem(R.id.action_cu_status_uploading)?.isVisible = show
    this.menu?.findItem(R.id.action_cu_status_warning)?.isVisible = show
    this.menu?.findItem(R.id.action_cu_status_complete)?.isVisible = show
}

fun showCUStatusTestDialog(
    viewModel: TimelineViewModel,
    fragment: PhotosFragment,
    items: List<String> = listOf(
        "Start CU with internet block and reconnect",
        "Start CU and complete",
        "Single File for single string",
        "CU status icons on AppBar",
        "CU status Fabs"
    ),
    checkedItem: Int = 0,
    onDismissListener: (DialogInterface) -> Unit = {},
) {
    val sortDialog: AlertDialog
    val dialogBuilder = MaterialAlertDialogBuilder(fragment.requireContext())

    dialogBuilder.setSingleChoiceItems(items.toTypedArray(), checkedItem) { dialog, i ->
        resetUI(fragment, viewModel)
        when (i) {
            0 -> {
                viewModel.mockCUWithNoInternet()
            }

            1 -> {
                viewModel.mockCUAndComplete()
            }

            2 -> {
                viewModel.mockCUAndCompleteWithSingleFile()
            }

            3 -> {
                fragment.handleCUIconsOnAppBar(true)
            }

            4 -> {
                viewModel.setCUStatusFabs(true)
            }
        }
        dialog.dismiss()
    }

    dialogBuilder.setNegativeButton(fragment.requireContext().getString(R.string.general_cancel)) {
            dialog: DialogInterface,
            _,
        ->
        dialog.dismiss()
    }

    sortDialog = dialogBuilder.create()
    sortDialog.setTitle("CUStatusTestDialog")
    sortDialog.show()
    sortDialog.setOnDismissListener {
        onDismissListener(it)
    }
}

fun resetUI(fragment: PhotosFragment, timelineViewModel: TimelineViewModel) {
    fragment.handleCUIconsOnAppBar(false)
    timelineViewModel.setCUStatusFabs(false)
}

@Composable
fun CUStatusFabs() {
    Row(
    ) {
        CameraUploadsStatusSync()
        CameraUploadsStatusUploading(
            progress = 0.4f,
        )
        CameraUploadsStatusCompleted()
        CameraUploadsStatusWarning(
            progress = 0.8f
        )
    }
}