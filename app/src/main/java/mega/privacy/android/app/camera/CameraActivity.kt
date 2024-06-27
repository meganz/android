package mega.privacy.android.app.camera

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.camera.preview.navigateToPhotoPreview
import mega.privacy.android.app.camera.preview.navigateToVideoPreview
import mega.privacy.android.app.camera.preview.photoPreviewScreen
import mega.privacy.android.app.camera.preview.videoPreviewScreen
import mega.privacy.android.app.presentation.extensions.parcelable
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme

@AndroidEntryPoint
internal class CameraActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        delegate.localNightMode = AppCompatDelegate.MODE_NIGHT_YES
        setContent {
            val navController = rememberNavController()
            val args = intent.parcelable<CameraArg>(EXTRA_ARGS) as CameraArg

            // force dark theme for camera
            OriginalTempTheme(isDark = true) {
                NavHost(navController = navController, startDestination = CAMERA_CAPTURE_ROUTE) {
                    cameraCaptureScreen(
                        onFinish = ::setResult,
                        onOpenVideoPreview = navController::navigateToVideoPreview,
                        onOpenPhotoPreview = navController::navigateToPhotoPreview
                    )
                    videoPreviewScreen(
                        title = args.title,
                        onBackPressed = navController::popBackStack,
                        onSendVideo = ::setResult
                    )
                    photoPreviewScreen(
                        title = args.title,
                        onBackPressed = navController::popBackStack,
                        onSendPhoto = ::setResult
                    )
                }
            }
        }
    }

    private fun setResult(uri: Uri?) {
        uri?.let {
            contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            setResult(RESULT_OK, Intent().apply { putExtra(EXTRA_URI, uri) })
        }
        finish()
    }

    companion object {
        const val EXTRA_URI = "extra_uri"
        const val EXTRA_ARGS = "extra_args"
    }
}