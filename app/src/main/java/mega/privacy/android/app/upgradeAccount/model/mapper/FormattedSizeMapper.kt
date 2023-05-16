package mega.privacy.android.app.upgradeAccount.model.mapper

import mega.privacy.android.app.R
import java.text.DecimalFormat
import javax.inject.Inject

/**
 * Mapper for Int convert to Pair<Int, String> (to return correct string for GB or TB and formatted size)
 */
class FormattedSizeMapper @Inject constructor() {
    /**
     * Invoke
     * Convert Int to Pair<Int, String>
     * @param size [Int]
     * @return Pair<Int, String>
     */
    internal operator fun invoke(size: Int): Pair<Int, String> {
        val decimalFormatter = DecimalFormat("###.##")
        return if (size < 1024) {
            Pair(R.string.label_file_size_giga_byte, decimalFormatter.format(size))
        } else {
            Pair(R.string.label_file_size_tera_byte, decimalFormatter.format(size / 1024))
        }
    }
}