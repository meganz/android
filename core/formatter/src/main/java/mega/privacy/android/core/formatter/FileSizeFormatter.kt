package mega.privacy.android.core.formatter

import android.content.Context
import mega.privacy.android.shared.resources.R as SharedR
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
                SharedR.string.label_file_size_bytes,
                size.toString()
            )
        }

        size < megabyte -> {
            // KB: Round to whole number
            val kbValue = size / kilobyte
            val roundedKb = round(kbValue).toLong()
            context.getString(
                SharedR.string.label_file_size_kilobytes,
                roundedKb.toString()
            )
        }

        size < gigabyte -> {
            // MB: Show 1 decimal place
            val df = DecimalFormat("#.#")
            val mbValue = size / megabyte
            context.getString(
                SharedR.string.label_file_size_megabytes,
                df.format(mbValue)
            )
        }

        size < terabyte -> {
            // GB: Show 2 decimal places
            val df = DecimalFormat("#.##")
            val gbValue = size / gigabyte
            context.getString(
                SharedR.string.label_file_size_gigabytes,
                df.format(gbValue)
            )
        }

        size < petabyte -> {
            // TB: Show 2 decimal places
            val df = DecimalFormat("#.##")
            val tbValue = size / terabyte
            context.getString(
                SharedR.string.label_file_size_terabytes,
                df.format(tbValue)
            )
        }

        size < exabyte -> {
            // PB: Show 2 decimal places
            val df = DecimalFormat("#.##")
            val pbValue = size / petabyte
            context.getString(
                SharedR.string.label_file_size_petabytes,
                df.format(pbValue)
            )
        }

        else -> {
            // EB: Show 2 decimal places
            val df = DecimalFormat("#.##")
            val ebValue = size / exabyte
            context.getString(
                SharedR.string.label_file_size_exabytes,
                df.format(ebValue)
            )
        }
    }
}