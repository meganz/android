package mega.privacy.android.app.presentation.extensions.transfers

import android.content.Context
import mega.privacy.android.app.R
import mega.privacy.android.domain.entity.transfer.InProgressTransfer
import mega.privacy.android.domain.entity.transfer.TransferState
import java.text.DecimalFormat

/**
 * Get the speed string for a given speed as Long.
 */
internal fun InProgressTransfer.getSpeedString(
    context: Context,
    areTransfersPaused: Boolean,
): String = with(context) {
    val df = DecimalFormat("#.##")

    return when {
        isPaused || areTransfersPaused -> {
            getString(R.string.transfer_paused)
        }

        state == TransferState.STATE_QUEUED -> {
            getString(R.string.transfer_queued)
        }

        state == TransferState.STATE_COMPLETING -> {
            getString(R.string.transfer_completing)
        }

        state == TransferState.STATE_RETRYING -> {
            getString(R.string.transfer_retrying)
        }

        speed < KB -> {
            getString(R.string.label_file_speed_byte, speed.toString())
        }

        speed < MB -> {
            getString(R.string.label_file_speed_kilo_byte, df.format((speed / KB).toDouble()))
        }

        speed < GB -> {
            getString(R.string.label_file_speed_mega_byte, df.format((speed / MB).toDouble()))
        }

        speed < TB -> {
            getString(R.string.label_file_speed_giga_byte, df.format((speed / GB).toDouble()))
        }

        speed < PB -> {
            getString(R.string.label_file_speed_tera_byte, df.format((speed / TB).toDouble()))
        }

        speed < EB -> {
            getString(R.string.label_file_size_peta_byte, df.format((speed / PB).toDouble()))
        }

        else -> {
            getString(R.string.label_file_size_exa_byte, df.format((speed / EB).toDouble()))
        }
    }
}

internal fun InProgressTransfer.getProgressString(context: Context, isOverQuota: Boolean): String =
    with(context) {
        val isDownload = this@getProgressString is InProgressTransfer.Download
        val totalSizeString = getTotalSizeString(context)
        val progressString = getString(
            R.string.progress_size_indicator,
            progress.intValue,
            totalSizeString
        )

        return when {
            isOverQuota && isDownload -> {
                "$progressString ${getString(R.string.label_transfer_over_quota)}"
            }

            isOverQuota -> {
                "$progressString ${getString(R.string.label_storage_over_quota)}"
            }

            else -> {
                progressString
            }
        }
    }

/**
 * Get the size string for a given size as Long.
 */
internal fun InProgressTransfer.getTotalSizeString(context: Context): String = with(context) {
    val df = DecimalFormat("#.##")

    return when {
        totalBytes < KB -> {
            getString(R.string.label_file_size_byte, totalBytes.toString())
        }

        totalBytes < MB -> {
            getString(R.string.label_file_size_kilo_byte, df.format((totalBytes / KB).toDouble()))
        }

        totalBytes < GB -> {
            getString(R.string.label_file_size_mega_byte, df.format((totalBytes / MB).toDouble()))
        }

        totalBytes < TB -> {
            getString(R.string.label_file_size_giga_byte, df.format((totalBytes / GB).toDouble()))
        }

        totalBytes < PB -> {
            getString(R.string.label_file_size_tera_byte, df.format((totalBytes / TB).toDouble()))
        }

        totalBytes < EB -> {
            getString(R.string.label_file_size_peta_byte, df.format((totalBytes / PB).toDouble()))
        }

        else -> {
            getString(R.string.label_file_size_exa_byte, df.format((totalBytes / EB).toDouble()))
        }
    }
}

private const val KB = 1024f
private const val MB = KB * 1024
private const val GB = MB * 1024
private const val TB = GB * 1024
private const val PB = TB * 1024
private const val EB = PB * 1024