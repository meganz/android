package mega.privacy.android.app.upgradeAccount.model.mapper

import mega.privacy.android.app.R
import java.text.DecimalFormat


/**
 * Mapper for Long convert to Pair<Int, String> (to return correct string for GB or TB and formatted size)
 */
typealias FormattedSizeGBBasedMapper = (@JvmSuppressWildcards Long) -> @JvmSuppressWildcards Pair<Int, String>

/**
 * Convert Long to Pair<Int, String>
 * @param gbSize [Long]
 * @return Pair<Int, String>
 */
internal fun toFormattedSizeGBBased(gbSize: Long): Pair<Int, String> {
    val decimalFormat = DecimalFormat("###.##")
    val TB = 1024
    return if (gbSize < TB) {
        Pair(R.string.label_file_size_giga_byte, decimalFormat.format(gbSize))
    } else {
        Pair(R.string.label_file_size_tera_byte, decimalFormat.format(gbSize / TB))
    }
}