package mega.privacy.android.app.presentation.settings.camerauploads.navigation.pickers

import android.content.Context
import android.content.Intent
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.ActivityResult
import mega.privacy.android.app.main.FileExplorerActivity
import mega.privacy.android.app.main.FileStorageActivity

/**
 * Selects a new Camera Uploads Local Primary Folder by starting [FileStorageActivity] and
 * retrieving the new Local Folder path afterwards
 *
 * @param context The [Context] to create a [FileStorageActivity] [Intent]
 * @param launcher The Launcher to start [FileStorageActivity]
 */
internal fun openCameraUploadsLocalFolderPicker(
    context: Context,
    launcher: ManagedActivityResultLauncher<Intent, ActivityResult>,
) {
    Intent(context, FileStorageActivity::class.java).also {
        it.action = FileStorageActivity.Mode.PICK_FOLDER.action
        it.putExtra(
            FileStorageActivity.PICK_FOLDER_TYPE,
            FileStorageActivity.PickFolderType.CU_FOLDER.folderType
        )
        launcher.launch(it)
    }
}

/**
 * Selects a new Camera Uploads Folder Node by starting [FileExplorerActivity] and retrieving the
 * new Folder Node handle afterwards
 *
 * @param context The [Context] to create a [FileExplorerActivity] [Intent]
 * @param launcher The Launcher to start [FileExplorerActivity]
 */
internal fun openCameraUploadsFolderNodePicker(
    context: Context,
    launcher: ManagedActivityResultLauncher<Intent, ActivityResult>,
) {
    Intent(context, FileExplorerActivity::class.java).also {
        it.action = FileExplorerActivity.ACTION_CHOOSE_MEGA_FOLDER_SYNC
        launcher.launch(it)
    }
}