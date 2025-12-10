package mega.privacy.android.feature.myaccount.presentation.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import mega.privacy.android.feature.myaccount.R
import java.text.DecimalFormat

/**
 * Get the size string for a given size in bytes
 *
 * @param totalBytes Size in bytes
 * @return Formatted size string (e.g., "124.47 GB")
 */
@Composable
fun getSizeString(totalBytes: Long): String {
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
