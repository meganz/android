package mega.privacy.android.data.mapper

import java.text.DecimalFormat
import javax.inject.Inject

/**
 * Mapper to convert file byte size to a calculatedValue for locale-aware formatting
 */
class FileSizeMapper @Inject constructor() {

    /**
     * @param size The file size in bytes
     * @return calculatedValue in final size unit
     */
    operator fun invoke(size: Long) = when {
        size < KILOBYTE -> size.toDouble()

        size < MEGABYTE -> DecimalFormat("#.##").format(size / KILOBYTE).toDouble()

        size < GIGABYTE -> DecimalFormat("#.##").format(size / MEGABYTE).toDouble()

        size < TERABYTE -> DecimalFormat("#.##").format(size / GIGABYTE).toDouble()

        size < PETABYTE -> DecimalFormat("#.##").format(size / TERABYTE).toDouble()

        size < EXABYTE -> DecimalFormat("#.##").format(size / PETABYTE).toDouble()

        else -> DecimalFormat("#.##").format(size / EXABYTE).toDouble()
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