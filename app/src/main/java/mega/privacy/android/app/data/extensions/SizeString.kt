package mega.privacy.android.app.data.extensions

import android.content.Context
import android.text.SpannableString
import android.text.Spanned
import android.text.style.RelativeSizeSpan
import mega.privacy.android.app.R
import java.text.DecimalFormat

/**
 * Gets the corresponding storage string depending on the long received, and formats the number
 * value to a bigger text style.
 *
 * @return [Spanned] with the big formatted storage string.
 */
fun Long.getBigFormattedStorageString(context: Context): Spanned {
    val df = DecimalFormat("#.##")

    val kB = 1024f
    val mB = kB * 1024
    val gB = mB * 1024
    val tB = gB * 1024
    val pB = tB * 1024
    val eB = pB * 1024

    var storageSizeString = when {
        this < kB -> {
            context.getString(R.string.account_achievements_storage_quota_byte, this.toString())
        }
        this < mB -> {
            context.getString(
                R.string.account_achievements_storage_quota_kilo_byte,
                df.format((this / kB))
            )
        }
        this < gB -> {
            context.getString(
                R.string.account_achievements_storage_quota_mega_byte,
                df.format((this / mB))
            )
        }
        this < tB -> {
            context.getString(
                R.string.account_achievements_storage_quota_giga_byte,
                df.format((this / gB))
            )
        }
        this < pB -> {
            context.getString(
                R.string.account_achievements_storage_quota_tera_byte,
                df.format((this / tB))
            )
        }
        this < eB -> {
            context.getString(
                R.string.account_achievements_storage_quota_peta_byte,
                df.format((this / pB))
            )
        }
        else -> {
            context.getString(
                R.string.account_achievements_storage_quota_exa_byte,
                df.format((this / eB))
            )
        }
    }

    val initialPlaceHolder = "[A]"
    val finalPlaceHolder = "[/A]"
    val firstIndex = storageSizeString.indexOf(initialPlaceHolder)
    storageSizeString = storageSizeString.replace(initialPlaceHolder, "")
    val lastIndex = storageSizeString.indexOf(finalPlaceHolder)
    storageSizeString = storageSizeString.replace(finalPlaceHolder, "")

    return SpannableString(storageSizeString).apply {
        setSpan(
            RelativeSizeSpan(2.5f),
            firstIndex,
            lastIndex,
            0
        )
    }
}