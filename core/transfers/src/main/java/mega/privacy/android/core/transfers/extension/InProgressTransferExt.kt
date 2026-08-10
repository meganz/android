package mega.privacy.android.core.transfers.extension

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import mega.privacy.android.core.formatter.formatFileSize
import mega.privacy.android.domain.entity.transfer.InProgressTransfer
import mega.privacy.android.domain.entity.transfer.TransferState
import mega.privacy.android.shared.resources.R as sharedR
import java.text.DecimalFormat

/**
 * Get the speed string for a given speed as Long.
 */
@Composable
fun InProgressTransfer.getSpeedString(
    areTransfersPaused: Boolean,
    isTransferOverQuota: Boolean,
    isStorageOverQuota: Boolean,
): String {
    val df = DecimalFormat("#.##")

    return when {
        isTransferOverQuota -> {
            stringResource(sharedR.string.camera_uploads_progress_screen_label_transfer_over_quota)
        }

        isStorageOverQuota -> {
            stringResource(sharedR.string.camera_uploads_progress_screen_label_storage_over_quota)
        }

        isPaused || areTransfersPaused -> {
            stringResource(sharedR.string.camera_uploads_progress_screen_label_transfer_paused)
        }

        state == TransferState.STATE_QUEUED -> {
            stringResource(sharedR.string.camera_uploads_progress_screen_label_transfer_queued)
        }

        state == TransferState.STATE_COMPLETING -> {
            stringResource(sharedR.string.camera_uploads_progress_screen_label_transfer_completing)
        }

        state == TransferState.STATE_RETRYING -> {
            stringResource(sharedR.string.camera_uploads_progress_screen_label_transfer_retrying)
        }

        speed < KB -> {
            stringResource(sharedR.string.label_file_upload_speed_byte, speed.toString())
        }

        speed < MB -> {
            stringResource(
                sharedR.string.label_file_upload_speed_kilo_byte,
                df.format((speed / KB).toDouble())
            )
        }

        speed < GB -> {
            stringResource(
                sharedR.string.label_file_upload_speed_mega_byte,
                df.format((speed / MB).toDouble())
            )
        }

        speed < TB -> {
            stringResource(
                sharedR.string.label_file_upload_speed_giga_byte,
                df.format((speed / GB).toDouble())
            )
        }

        speed < PB -> {
            stringResource(
                sharedR.string.label_file_upload_speed_tera_byte,
                df.format((speed / TB).toDouble())
            )
        }

        speed < EB -> {
            stringResource(
                sharedR.string.label_file_upload_size_peta_byte,
                df.format((speed / PB).toDouble())
            )
        }

        else -> {
            stringResource(
                sharedR.string.label_file_upload_size_exa_byte,
                df.format((speed / EB).toDouble())
            )
        }
    }
}

@Composable
fun InProgressTransfer.getProgressSizeString(): String {
    val context = LocalContext.current
    val totalSizeString = remember(totalBytes) {
        formatFileSize(size = totalBytes, context = context)
    }
    val transferredString = remember(totalBytes, progress) {
        formatFileSize(size = (totalBytes * progress.floatValue).toLong(), context = context)
    }
    return stringResource(
        sharedR.string.transfers_completed_transfer_size_indicator,
        transferredString,
        totalSizeString
    )
}

fun InProgressTransfer.getProgressPercentString() = "${progress.intValue}%"

private const val KB = 1024f
private const val MB = KB * 1024
private const val GB = MB * 1024
private const val TB = GB * 1024
private const val PB = TB * 1024
private const val EB = PB * 1024
