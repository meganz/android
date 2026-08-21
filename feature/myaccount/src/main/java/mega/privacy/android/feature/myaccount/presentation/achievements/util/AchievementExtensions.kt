package mega.privacy.android.feature.myaccount.presentation.achievements.util

import android.content.Context
import androidx.annotation.StringRes
import mega.privacy.android.core.formatter.formatFileSize
import java.util.concurrent.TimeUnit

/**
 * Gets a string from bytes in readable unit
 *
 * @return [String] unit string from bytes
 *
 * @deprecated Use [mega.privacy.android.app.presentation.mapper.file.FileSizeStringMapper] instead
 */
fun Long.toUnitString(context: Context): String = formatFileSize(this, context)

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
