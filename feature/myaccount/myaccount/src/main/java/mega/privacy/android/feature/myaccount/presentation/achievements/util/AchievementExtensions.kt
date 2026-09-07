package mega.privacy.android.feature.myaccount.presentation.achievements.util

import android.content.Context
import androidx.annotation.StringRes
import mega.privacy.android.core.formatter.formatFileSize
import mega.privacy.android.shared.resources.R as sharedR
import java.text.DecimalFormat
import java.util.concurrent.TimeUnit

private const val KILOBYTE = 1024f
private const val MEGABYTE = KILOBYTE * 1024
private const val GIGABYTE = MEGABYTE * 1024
private const val TERABYTE = GIGABYTE * 1024
private const val PETABYTE = TERABYTE * 1024
private const val EXABYTE = PETABYTE * 1024

/**
 * Gets a string from bytes in readable unit
 *
 * @return [String] unit string from bytes
 *
 * @deprecated Use [mega.privacy.android.app.presentation.mapper.file.FileSizeStringMapper] instead
 */
fun Long.toUnitString(context: Context): String = formatFileSize(this, context)

/**
 * Gets the corresponding storage string depending on the long received
 *
 * @return [String] with the storage unit
 */
fun Long.toStorageString(context: Context): String {
    val formatter = DecimalFormat("#.##")

    return when {
        this < KILOBYTE -> {
            context.getString(
                sharedR.string.account_achievements_storage_quota_byte,
                this.toString()
            )
        }

        this < MEGABYTE -> {
            context.getString(
                sharedR.string.account_achievements_storage_quota_kilo_byte,
                formatter.format((this / KILOBYTE))
            )
        }

        this < GIGABYTE -> {
            context.getString(
                sharedR.string.account_achievements_storage_quota_mega_byte,
                formatter.format((this / MEGABYTE))
            )
        }

        this < TERABYTE -> {
            context.getString(
                sharedR.string.account_achievements_storage_quota_giga_byte,
                formatter.format((this / GIGABYTE))
            )
        }

        this < PETABYTE -> {
            context.getString(
                sharedR.string.account_achievements_storage_quota_tera_byte,
                formatter.format((this / TERABYTE))
            )
        }

        this < EXABYTE -> {
            context.getString(
                sharedR.string.account_achievements_storage_quota_peta_byte,
                formatter.format((this / PETABYTE))
            )
        }

        else -> {
            context.getString(
                sharedR.string.account_achievements_storage_quota_exa_byte,
                formatter.format((this / EXABYTE))
            )
        }
    }
}

fun Long.toMillis() = TimeUnit.SECONDS.toMillis(this)

/**
 * Get the corresponding text by duration in days
 *
 * @param context       Context
 * @param daysStringId  String resource id for dynamic days
 * @param permanentStringId String resource id for permanent
 * @param storage       Storage string
 * @return              The corresponding text by duration in days
 */
fun Int.getTextByDurationInDays(
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
