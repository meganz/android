package mega.privacy.android.core.nodecomponents.mapper

import mega.privacy.android.core.nodecomponents.R
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
                R.string.label_file_size_byte to size.toDouble()
            }

            size < MEGABYTE -> {
                R.string.label_file_size_kilo_byte to (size / KILOBYTE)
            }

            size < GIGABYTE -> {
                R.string.label_file_size_mega_byte to (size / MEGABYTE)
            }

            size < TERABYTE -> {
                R.string.label_file_size_giga_byte to (size / GIGABYTE)
            }

            size < PETABYTE -> {
                R.string.label_file_size_tera_byte to (size / TERABYTE)
            }

            size < EXABYTE -> {
                R.string.label_file_size_peta_byte to (size / PETABYTE)
            }

            else -> {
                R.string.label_file_size_exa_byte to (size / EXABYTE)
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