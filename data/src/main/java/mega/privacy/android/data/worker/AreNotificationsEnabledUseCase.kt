package mega.privacy.android.data.worker

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.domain.repository.EnvironmentRepository
import javax.inject.Inject


/**
 * Usecase to checks if notifications are enabled.
 */
class AreNotificationsEnabledUseCase @Inject constructor(
    private val notificationManager: NotificationManagerCompat,
    private val environmentRepository: EnvironmentRepository,
    @ApplicationContext private val context: Context,
) {
    /**
     * Checks if notifications are enabled.
     */
    suspend operator fun invoke() =
        notificationManager.areNotificationsEnabled() &&
                if (environmentRepository.getDeviceSdkVersionInt() >= Build.VERSION_CODES.TIRAMISU) {
                    checkSelfPermission()
                } else {
                    true
                }

    @SuppressLint("InlinedApi")
    private fun checkSelfPermission() = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS
    ) == PackageManager.PERMISSION_GRANTED
}