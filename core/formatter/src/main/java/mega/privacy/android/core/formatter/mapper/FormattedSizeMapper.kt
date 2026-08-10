package mega.privacy.android.core.formatter.mapper

import mega.privacy.android.shared.resources.R as SharedR
import mega.privacy.android.core.formatter.model.FormattedSize
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
     * @param usePlaceholder [Boolean]
     * @return FormattedSize
     */
    operator fun invoke(size: Int, usePlaceholder: Boolean = true): FormattedSize {
        val decimalFormatter = DecimalFormat("###.##")
        return if (size < 1024) {
            FormattedSize(
                if (usePlaceholder) SharedR.string.label_file_size_gigabytes
                else
                    mega.privacy.android.shared.resources.R.string.general_giga_byte_standalone,
                decimalFormatter.format(size)
            )
        } else {
            FormattedSize(
                if (usePlaceholder) SharedR.string.label_file_size_terabytes
                else
                    mega.privacy.android.shared.resources.R.string.general_tera_byte_standalone,
                decimalFormatter.format(size / 1024)
            )
        }
    }
}