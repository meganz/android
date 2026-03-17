package mega.privacy.android.core.nodecomponents.mapper

import mega.privacy.android.shared.nodes.R as NodesR
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
                NodesR.string.label_file_size_byte to size.toDouble()
            }

            size < MEGABYTE -> {
                NodesR.string.label_file_size_kilo_byte to (size / KILOBYTE)
            }

            size < GIGABYTE -> {
                NodesR.string.label_file_size_mega_byte to (size / MEGABYTE)
            }

            size < TERABYTE -> {
                NodesR.string.label_file_size_giga_byte to (size / GIGABYTE)
            }

            size < PETABYTE -> {
                NodesR.string.label_file_size_tera_byte to (size / TERABYTE)
            }

            size < EXABYTE -> {
                NodesR.string.label_file_size_peta_byte to (size / PETABYTE)
            }

            else -> {
                NodesR.string.label_file_size_exa_byte to (size / EXABYTE)
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