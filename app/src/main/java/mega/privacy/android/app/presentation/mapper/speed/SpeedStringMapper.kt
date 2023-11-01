package mega.privacy.android.app.presentation.mapper.speed

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.app.R
import java.text.DecimalFormat
import javax.inject.Inject

/**
 * Format a speed in a readable string
 */
class SpeedStringMapper @Inject constructor(
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
    }

    /**
     * Format a file speed in a readable string
     *
     * @param speed A speed defined in bytes per second
     */
    operator fun invoke(speed: Long): String {
        val df = DecimalFormat("#.##")

        return when {
            speed < kilobyte ->
                context.getString(R.string.label_file_speed_byte, speed.toString())

            speed < megabyte ->
                context.getString(R.string.label_file_speed_kilo_byte, df.format(speed / kilobyte))

            speed < gigabyte ->
                context.getString(R.string.label_file_speed_mega_byte, df.format(speed / megabyte))

            speed < terabyte ->
                context.getString(R.string.label_file_speed_giga_byte, df.format(speed / gigabyte))

            else ->
                context.getString(R.string.label_file_speed_tera_byte, df.format(speed / terabyte))
        }
    }
}
