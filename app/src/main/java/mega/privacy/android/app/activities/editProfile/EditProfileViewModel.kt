package mega.privacy.android.app.activities.editProfile

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.hilt.lifecycle.ViewModelInject
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.arch.BaseRxViewModel
import mega.privacy.android.app.di.MegaApi
import mega.privacy.android.app.lollipop.MyAccountInfo
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.CHOOSE_PICTURE_PROFILE_CODE
import mega.privacy.android.app.utils.Constants.TAKE_PICTURE_PROFILE_CODE
import mega.privacy.android.app.utils.Util.checkTakePicture
import nz.mega.sdk.MegaApiAndroid

class EditProfileViewModel @ViewModelInject constructor(
    @MegaApi private val megaApi: MegaApiAndroid
) : BaseRxViewModel() {

    private var accountInfo: MyAccountInfo = MegaApplication.getInstance().myAccountInfo

    fun getName(): String? = accountInfo.fullName

    fun getEmail(): String = megaApi.myEmail

    fun capturePhoto(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val hasStoragePermission: Boolean = checkSelfPermission(
                activity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
            val hasCameraPermission: Boolean = checkSelfPermission(
                activity,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasStoragePermission && !hasCameraPermission) {
                ActivityCompat.requestPermissions(
                    activity, arrayOf(
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.CAMERA
                    ),
                    Constants.REQUEST_WRITE_STORAGE
                )

                return
            } else if (!hasStoragePermission) {
                ActivityCompat.requestPermissions(
                    activity, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    Constants.REQUEST_WRITE_STORAGE
                )

                return
            } else if (!hasCameraPermission) {
                ActivityCompat.requestPermissions(
                    activity, arrayOf(Manifest.permission.CAMERA),
                    Constants.REQUEST_CAMERA
                )

                return
            }
        }

        checkTakePicture(activity, TAKE_PICTURE_PROFILE_CODE)
    }

    fun launchChoosePhotoIntent(activity: Activity) {
        val intent = Intent()
        intent.action = Intent.ACTION_OPEN_DOCUMENT
        intent.action = Intent.ACTION_GET_CONTENT
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        intent.type = "image/*";
        activity.startActivityForResult(Intent.createChooser(intent, null), CHOOSE_PICTURE_PROFILE_CODE)
    }
}