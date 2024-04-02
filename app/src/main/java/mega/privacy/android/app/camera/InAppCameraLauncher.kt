package mega.privacy.android.app.camera

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract
import mega.privacy.android.app.presentation.extensions.parcelable

internal class InAppCameraLauncher : ActivityResultContract<Unit, Uri?>() {
    override fun createIntent(context: Context, input: Unit): Intent =
        Intent(context, CameraActivity::class.java)

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? =
        intent?.parcelable(CameraActivity.EXTRA_URI)
}