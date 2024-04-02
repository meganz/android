package mega.privacy.android.app.camera

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.camera.preview.navigateToPhotoPreview
import mega.privacy.android.app.camera.preview.navigateToVideoPreview
import mega.privacy.android.app.camera.preview.photoPreviewScreen
import mega.privacy.android.app.camera.preview.videoPreviewScreen
import mega.privacy.android.shared.theme.MegaAppTheme

@AndroidEntryPoint
internal class CameraActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()

            // force dark theme for camera
            MegaAppTheme(isDark = true) {
                NavHost(navController = navController, startDestination = CAMERA_CAPTURE_ROUTE) {
                    cameraCaptureScreen(
                        onFinish = ::setResult,
                        onOpenVideoPreview = { uri ->
                            navController.navigateToVideoPreview(uri)
                        },
                        onOpenPhotoPreview = { uri ->
                            navController.navigateToPhotoPreview(uri)
                        }
                    )
                    videoPreviewScreen(
                        onBackPressed = {
                            navController.popBackStack()
                        },
                        onSendVideo = ::setResult
                    )
                    photoPreviewScreen(
                        onBackPressed = {
                            navController.popBackStack()
                        },
                        onSendPhoto = ::setResult
                    )
                }
            }
        }
    }

    private fun setResult(uri: Uri?) {
        uri?.let {
            setResult(RESULT_OK, Intent().apply { putExtra(EXTRA_URI, uri) })
        }
        finish()
    }

    companion object {
        const val EXTRA_URI = "extra_uri"
    }
}