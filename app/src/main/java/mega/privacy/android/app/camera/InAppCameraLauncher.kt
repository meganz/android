package mega.privacy.android.app.camera

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract
import mega.privacy.android.app.presentation.extensions.parcelable

internal class InAppCameraLauncher : ActivityResultContract<CameraArg, Uri?>() {
    override fun createIntent(context: Context, input: CameraArg): Intent =
        Intent(context, CameraActivity::class.java).apply {
            putExtra(CameraActivity.EXTRA_ARGS, input)
        }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? =
        intent?.parcelable(CameraActivity.EXTRA_URI)
}