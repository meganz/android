package mega.privacy.android.app.presentation.extensions.transfers

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import mega.privacy.android.app.R
import mega.privacy.android.domain.entity.transfer.InProgressTransfer
import mega.privacy.android.domain.entity.transfer.TransferState
import mega.privacy.android.shared.resources.R as sharedR
import java.text.DecimalFormat

/**
 * Get the speed string for a given speed as Long.
 */
@Composable
internal fun InProgressTransfer.getSpeedString(
    areTransfersPaused: Boolean,
): String {
    val df = DecimalFormat("#.##")

    return when {
        isPaused || areTransfersPaused -> {
            stringResource(R.string.transfer_paused)
        }

        state == TransferState.STATE_QUEUED -> {
            stringResource(R.string.transfer_queued)
        }

        state == TransferState.STATE_COMPLETING -> {
            stringResource(R.string.transfer_completing)
        }

        state == TransferState.STATE_RETRYING -> {
            stringResource(R.string.transfer_retrying)
        }

        speed < KB -> {
            stringResource(R.string.label_file_speed_byte, speed.toString())
        }

        speed < MB -> {
            stringResource(R.string.label_file_speed_kilo_byte, df.format((speed / KB).toDouble()))
        }

        speed < GB -> {
            stringResource(R.string.label_file_speed_mega_byte, df.format((speed / MB).toDouble()))
        }

        speed < TB -> {
            stringResource(R.string.label_file_speed_giga_byte, df.format((speed / GB).toDouble()))
        }

        speed < PB -> {
            stringResource(R.string.label_file_speed_tera_byte, df.format((speed / TB).toDouble()))
        }

        speed < EB -> {
            stringResource(R.string.label_file_size_peta_byte, df.format((speed / PB).toDouble()))
        }

        else -> {
            stringResource(R.string.label_file_size_exa_byte, df.format((speed / EB).toDouble()))
        }
    }
}

@Composable
internal fun InProgressTransfer.getProgressSizeString(): String {
    val totalSizeString = getSizeString(this.totalBytes)
    val transferredString = getSizeString((totalBytes * progress.floatValue).toLong())
    return stringResource(
        sharedR.string.transfers_completed_transfer_size_indicator,
        transferredString,
        totalSizeString
    )
}

internal fun InProgressTransfer.getProgressPercentString() = "${progress.intValue}%"

/**
 * Get the size string for a given size as Long.
 */
@Composable
internal fun getSizeString(totalBytes: Long): String {
    val df = DecimalFormat("#.##")

    return when {
        totalBytes < KB -> {
            stringResource(R.string.label_file_size_byte, totalBytes.toString())
        }

        totalBytes < MB -> {
            stringResource(
                R.string.label_file_size_kilo_byte,
                df.format((totalBytes / KB).toDouble())
            )
        }

        totalBytes < GB -> {
            stringResource(
                R.string.label_file_size_mega_byte,
                df.format((totalBytes / MB).toDouble())
            )
        }

        totalBytes < TB -> {
            stringResource(
                R.string.label_file_size_giga_byte,
                df.format((totalBytes / GB).toDouble())
            )
        }

        totalBytes < PB -> {
            stringResource(
                R.string.label_file_size_tera_byte,
                df.format((totalBytes / TB).toDouble())
            )
        }

        totalBytes < EB -> {
            stringResource(
                R.string.label_file_size_peta_byte,
                df.format((totalBytes / PB).toDouble())
            )
        }

        else -> {
            stringResource(
                R.string.label_file_size_exa_byte,
                df.format((totalBytes / EB).toDouble())
            )
        }
    }
}

private const val KB = 1024f
private const val MB = KB * 1024
private const val GB = MB * 1024
private const val TB = GB * 1024
private const val PB = TB * 1024
private const val EB = PB * 1024