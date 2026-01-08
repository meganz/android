package mega.privacy.android.core.formatter

import android.content.Context
import java.text.DecimalFormat
import kotlin.math.round

/**
 * Formats file size to match iOS ByteCountFormatter behavior with .memory count style:
 * - KB values are rounded to whole numbers
 * - MB values show 1 decimal place
 * - GB and above show 2 decimal places
 */
fun formatFileSize(size: Long, context: Context): String {
    val kilobyte = 1024.0
    val megabyte = kilobyte * 1024
    val gigabyte = megabyte * 1024
    val terabyte = gigabyte * 1024
    val petabyte = terabyte * 1024
    val exabyte = petabyte * 1024

    return when {
        size < kilobyte -> {
            context.getString(
                R.string.label_file_size_byte,
                size.toString()
            )
        }

        size < megabyte -> {
            // KB: Round to whole number
            val kbValue = size / kilobyte
            val roundedKb = round(kbValue).toLong()
            context.getString(
                R.string.label_file_size_kilo_byte,
                roundedKb.toString()
            )
        }

        size < gigabyte -> {
            // MB: Show 1 decimal place
            val df = DecimalFormat("#.#")
            val mbValue = size / megabyte
            context.getString(
                R.string.label_file_size_mega_byte,
                df.format(mbValue)
            )
        }

        size < terabyte -> {
            // GB: Show 2 decimal places
            val df = DecimalFormat("#.##")
            val gbValue = size / gigabyte
            context.getString(
                R.string.label_file_size_giga_byte,
                df.format(gbValue)
            )
        }

        size < petabyte -> {
            // TB: Show 2 decimal places
            val df = DecimalFormat("#.##")
            val tbValue = size / terabyte
            context.getString(
                R.string.label_file_size_tera_byte,
                df.format(tbValue)
            )
        }

        size < exabyte -> {
            // PB: Show 2 decimal places
            val df = DecimalFormat("#.##")
            val pbValue = size / petabyte
            context.getString(
                R.string.label_file_size_peta_byte,
                df.format(pbValue)
            )
        }

        else -> {
            // EB: Show 2 decimal places
            val df = DecimalFormat("#.##")
            val ebValue = size / exabyte
            context.getString(
                R.string.label_file_size_exa_byte,
                df.format(ebValue)
            )
        }
    }
}