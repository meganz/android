package mega.privacy.android.data.facade

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.READ_MEDIA_AUDIO
import android.Manifest.permission.READ_MEDIA_IMAGES
import android.Manifest.permission.READ_MEDIA_VIDEO
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.data.gateway.PermissionGateway
import javax.inject.Inject

/**
 * Permission Gateway implementation
 */
internal class PermissionFacade @Inject constructor(
    @ApplicationContext private val context: Context,
) : PermissionGateway {

    /**
     * Check permissions whether are granted
     * @param permissions one or more permission strings
     * @return whether permissions has been granted
     */
    override fun hasPermissions(vararg permissions: String): Boolean {
        for (permission in permissions) {
            // In Android 11+ WRITE_EXTERNAL_STORAGE doesn't grant any addition access so can assume it has been granted
            if ((Build.VERSION.SDK_INT < Build.VERSION_CODES.R || permission != WRITE_EXTERNAL_STORAGE) &&
                ContextCompat.checkSelfPermission(
                    context,
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
        }
        return true
    }

    /**
     * Get read permission regarding image based on sdk version
     *
     * @return read image permission based on sdk version
     */
    override fun getImagePermissionByVersion() =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getReadImagePermission()
        } else {
            getReadExternalStoragePermission()
        }

    /**
     * Get read permission regarding video based on sdk version
     *
     * @return read video permission based on sdk version
     */
    override fun getVideoPermissionByVersion() =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getReadVideoPermission()
        } else {
            getReadExternalStoragePermission()
        }

    /**
     * Get read permission regarding audio based on sdk version
     *
     * @return read audio permission based on sdk version
     */
    override fun getAudioPermissionByVersion() =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getReadAudioPermission()
        } else {
            getReadExternalStoragePermission()
        }

    /**
     * Get READ_MEDIA_IMAGES
     *
     * @return READ_MEDIA_IMAGES
     */
    @RequiresApi(33)
    private fun getReadImagePermission() = READ_MEDIA_IMAGES

    /**
     * Get READ_EXTERNAL_STORAGE
     *
     * @return READ_EXTERNAL_STORAGE
     */
    fun getReadExternalStoragePermission() = READ_EXTERNAL_STORAGE

    /**
     * Get READ_MEDIA_VIDEO
     *
     * @return READ_MEDIA_VIDEO
     */
    @RequiresApi(33)
    private fun getReadVideoPermission() = READ_MEDIA_VIDEO

    /**
     * Get READ_MEDIA_AUDIO
     *
     * @return READ_MEDIA_AUDIO
     */
    @RequiresApi(33)
    private fun getReadAudioPermission() = READ_MEDIA_AUDIO
}
