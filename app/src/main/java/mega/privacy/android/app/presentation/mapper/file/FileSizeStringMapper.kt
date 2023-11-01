package mega.privacy.android.app.presentation.mapper.file

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.app.R
import java.text.DecimalFormat
import javax.inject.Inject

/**
 * Format a file size in a readable string
 */
class FileSizeStringMapper @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    companion object {
        /**
         * Kilobyte unit of Bytes
         */
        private const val kilobyte = 1024f

        /**
         * Megabyte unit of Bytes
         */
        private const val megabyte = kilobyte * 1024

        /**
         * Gigabyte unit of Bytes
         */
        private const val gigabyte = megabyte * 1024

        /**
         * Terabyte unit of Bytes
         */
        private const val terabyte = gigabyte * 1024

        /**
         * Petabyte unit of Bytes
         */
        private const val petabyte = terabyte * 1024

        /**
         * Exabyte unit of Bytes
         */
        private const val exabyte = petabyte * 1024
    }

    /**
     * Format a file size in a readable string
     *
     * @param size the size of the file or folder
     */
    operator fun invoke(size: Long): String {
        val df = DecimalFormat("#.##")

        return when {
            size < kilobyte ->
                context.getString(R.string.label_file_size_byte, size.toString())

            size < megabyte ->
                context.getString(R.string.label_file_size_kilo_byte, df.format(size / kilobyte))

            size < gigabyte ->
                context.getString(R.string.label_file_size_mega_byte, df.format(size / megabyte))

            size < terabyte ->
                context.getString(R.string.label_file_size_giga_byte, df.format(size / gigabyte))

            size < petabyte ->
                context.getString(R.string.label_file_size_tera_byte, df.format(size / terabyte))

            size < exabyte ->
                context.getString(R.string.label_file_size_peta_byte, df.format(size / petabyte))

            else ->
                context.getString(R.string.label_file_size_exa_byte, df.format(size / exabyte))
        }
    }
}
