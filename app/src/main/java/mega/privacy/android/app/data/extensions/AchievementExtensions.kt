package mega.privacy.android.app.data.extensions

import android.content.Context
import mega.privacy.android.app.R
import java.text.DecimalFormat

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
 */
fun Long.toUnitString(context: Context): String {
    val formatter = DecimalFormat("#.##")

    return when {
        this < kilobyte -> {
            context.getString(R.string.label_file_size_byte, this.toString())
        }

        this < megabyte -> {
            context.getString(
                R.string.label_file_size_kilo_byte,
                formatter.format(this / kilobyte)
            )
        }

        this < gigabyte -> {
            context.getString(
                R.string.label_file_size_mega_byte,
                formatter.format(this / megabyte)
            )
        }

        this < terabyte -> {
            context.getString(
                R.string.label_file_size_giga_byte,
                formatter.format(this / gigabyte)
            )
        }

        this < petabyte -> {
            context.getString(
                R.string.label_file_size_tera_byte,
                formatter.format(this / terabyte)
            )
        }

        this < exabyte -> {
            context.getString(
                R.string.label_file_size_peta_byte,
                formatter.format(this / petabyte)
            )
        }

        else -> {
            context.getString(R.string.label_file_size_exa_byte, formatter.format(this / exabyte))
        }
    }
}
