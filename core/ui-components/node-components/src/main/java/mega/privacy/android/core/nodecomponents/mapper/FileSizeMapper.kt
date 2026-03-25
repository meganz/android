package mega.privacy.android.core.nodecomponents.mapper

import mega.privacy.android.shared.resources.R as SharedR
import javax.inject.Inject

/**
 * Mapper to convert file byte size to a Pair of (stringResourceId, calculatedValue) for locale-aware formatting
 */
class FileSizeMapper @Inject constructor() {

    /**
     * @param size The file size in bytes
     * @return Pair of (stringResourceId, calculatedValue)
     */
    operator fun invoke(size: Long): Pair<Int, Double> {
        return when {
            size < KILOBYTE -> {
                SharedR.string.label_file_size_bytes to size.toDouble()
            }

            size < MEGABYTE -> {
                SharedR.string.label_file_size_kilobytes to (size / KILOBYTE)
            }

            size < GIGABYTE -> {
                SharedR.string.label_file_size_megabytes to (size / MEGABYTE)
            }

            size < TERABYTE -> {
                SharedR.string.label_file_size_gigabytes to (size / GIGABYTE)
            }

            size < PETABYTE -> {
                SharedR.string.label_file_size_terabytes to (size / TERABYTE)
            }

            size < EXABYTE -> {
                SharedR.string.label_file_size_petabytes to (size / PETABYTE)
            }

            else -> {
                SharedR.string.label_file_size_exabytes to (size / EXABYTE)
            }
        }
    }

    companion object {
        private const val KILOBYTE = 1024.0
        private const val MEGABYTE = KILOBYTE * 1024
        private const val GIGABYTE = MEGABYTE * 1024
        private const val TERABYTE = GIGABYTE * 1024
        private const val PETABYTE = TERABYTE * 1024
        private const val EXABYTE = PETABYTE * 1024
    }
}