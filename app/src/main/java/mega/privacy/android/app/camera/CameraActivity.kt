package mega.privacy.android.app.camera

import mega.privacy.android.shared.resources.R as sharedR
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.google.accompanist.navigation.material.rememberBottomSheetNavigator
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.camera.preview.navigateToPhotoPreview
import mega.privacy.android.app.camera.preview.navigateToVideoPreview
import mega.privacy.android.app.camera.preview.photoPreviewScreen
import mega.privacy.android.app.camera.preview.videoPreviewScreen
import mega.privacy.android.app.camera.setting.cameraSettingModal
import mega.privacy.android.app.camera.setting.navigateCameraSettingModal
import mega.privacy.android.app.presentation.extensions.parcelable
import mega.privacy.android.app.presentation.meeting.chat.view.showPermissionNotAllowedSnackbar
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme

@AndroidEntryPoint
internal class CameraActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context?) {
        delegate.localNightMode = AppCompatDelegate.MODE_NIGHT_YES
        super.attachBaseContext(newBase)
    }

    @OptIn(ExperimentalMaterialNavigationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val bottomSheetNavigator = rememberBottomSheetNavigator()
            val navController = rememberNavController(bottomSheetNavigator)
            val args = intent.parcelable<CameraArg>(EXTRA_ARGS) as CameraArg
            val scaffoldState = rememberScaffoldState()
            val coroutineScope = rememberCoroutineScope()

            // force dark theme for camera
            OriginalTempTheme(isDark = true) {
                NavHost(
                    navController = navController,
                    startDestination = CAMERA_CAPTURE_ROUTE,
                    modifier = Modifier.navigationBarsPadding(),
                ) {
                    cameraCaptureScreen(
                        scaffoldState = scaffoldState,
                        bottomSheetNavigator = bottomSheetNavigator,
                        onFinish = ::setResult,
                        onOpenVideoPreview = navController::navigateToVideoPreview,
                        onOpenPhotoPreview = navController::navigateToPhotoPreview,
                        onOpenCameraSetting = { navController.navigateCameraSettingModal() }
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

                    cameraSettingModal(
                        showPermissionDeniedSnackbar = {
                            navController.popBackStack()
                            showPermissionNotAllowedSnackbar(
                                snackBarHostState = scaffoldState.snackbarHostState,
                                coroutineScope = coroutineScope,
                                context = this@CameraActivity,
                                permissionStringId = sharedR.string.camera_settings_save_location_permission_denied
                            )
                        }
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
        const val EXTRA_ARGS = "extra_args"
    }
}