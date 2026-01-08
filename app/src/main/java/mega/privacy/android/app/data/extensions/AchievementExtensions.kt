package mega.privacy.android.app.data.extensions

import android.content.Context
import androidx.annotation.StringRes
import mega.privacy.android.app.R
import mega.privacy.android.core.formatter.formatFileSize
import java.text.DecimalFormat
import java.util.concurrent.TimeUnit

/**
 * Kilobyte unit of Bytes
 */
const val kilobyte = 1024f

/**
 * Megabyte unit of Bytes
 */
const val megabyte = kilobyte * 1024

/**
 * Gigabyte unit of Bytes
 */
const val gigabyte = megabyte * 1024

/**
 * Terabyte unit of Bytes
 */
const val terabyte = gigabyte * 1024

/**
 * Petabyte unit of Bytes
 */
const val petabyte = terabyte * 1024

/**
 * Exabyte unit of Bytes
 */
const val exabyte = petabyte * 1024

/**
 * Gets the corresponding storage string depending on the long received
 *
 * @return [String] with the storage unit
 */
fun Long.toStorageString(context: Context): String {
    val formatter = DecimalFormat("#.##")

    return when {
        this < kilobyte -> {
            context.getString(R.string.account_achievements_storage_quota_byte, this.toString())
        }

        this < megabyte -> {
            context.getString(
                R.string.account_achievements_storage_quota_kilo_byte,
                formatter.format((this / kilobyte))
            )
        }

        this < gigabyte -> {
            context.getString(
                R.string.account_achievements_storage_quota_mega_byte,
                formatter.format((this / megabyte))
            )
        }

        this < terabyte -> {
            context.getString(
                R.string.account_achievements_storage_quota_giga_byte,
                formatter.format((this / gigabyte))
            )
        }

        this < petabyte -> {
            context.getString(
                R.string.account_achievements_storage_quota_tera_byte,
                formatter.format((this / terabyte))
            )
        }

        this < exabyte -> {
            context.getString(
                R.string.account_achievements_storage_quota_peta_byte,
                formatter.format((this / petabyte))
            )
        }

        else -> {
            context.getString(
                R.string.account_achievements_storage_quota_exa_byte,
                formatter.format((this / exabyte))
            )
        }
    }
}

/**
 * Gets a string from bytes in readable unit
 *
 * @return [String] unit string from bytes
 *
 * @deprecated Use [mega.privacy.android.app.presentation.mapper.file.FileSizeStringMapper] instead
 */
fun Long.toUnitString(context: Context): String = formatFileSize(this, context)

internal fun Long.toMillis() = TimeUnit.SECONDS.toMillis(this)

/**
 * Get the corresponding text by duration in days
 *
 * @param context       Context
 * @param daysStringId  String resource id for dynamic days
 * @param permanentStringId String resource id for permanent
 * @param storage       Storage string
 * @return              The corresponding text by duration in days
 */
internal fun Int.getTextByDurationInDays(
    context: Context,
    @StringRes daysStringId: Int,
    @StringRes permanentStringId: Int,
    storage: String,
) =
    if (this == 0) {
        context.getString(permanentStringId, storage)
    } else {
        context.getString(daysStringId, storage, this)
    }
