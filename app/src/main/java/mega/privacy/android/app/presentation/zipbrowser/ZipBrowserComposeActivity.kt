package mega.privacy.android.app.presentation.zipbrowser

import android.app.Activity
import android.content.Context
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.domain.monitoring.CrashReporter
import timber.log.Timber
import java.nio.charset.Charset
import java.util.zip.ZipFile

/**
 * The activity for the zip browser
 */
@AndroidEntryPoint
class ZipBrowserComposeActivity : PasscodeActivity() {
    companion object {
        /**
         * Use for companion object injection
         */
        @EntryPoint
        @InstallIn(SingletonComponent::class)
        interface CrashReporterEntryPoint {
            /**
             * Get [CrashReporter]
             *
             * @return [CrashReporter] instance
             */
            fun crashReporter(): CrashReporter
        }

        /**
         * check the zip file if is error format
         * @param context context
         * @param zipFilePath zip file full path
         */
        fun zipFileFormatCheck(context: Context, zipFilePath: String): Boolean {
            val hiltEntryPoint =
                EntryPointAccessors.fromApplication(context, CrashReporterEntryPoint::class.java)

            (context as? Activity)?.run {
                // Log the Activity name that opens ZipBrowserActivity
                hiltEntryPoint.crashReporter().log("Activity name is $localClassName")
            }
            // Log the zip file path
            hiltEntryPoint.crashReporter()
                .log("Path of ZipFile(zipFileFormatCheck) is $zipFilePath")
            var zipFile: ZipFile? = null
            try {
                zipFile = ZipFile(zipFilePath)
                // Try reading the Zip File with UTF-8 Charset
                zipFile.entries().toList()
            } catch (exception: Exception) {
                Timber.e(exception, "ZipFile")
                // Throws IllegalArgumentException (thrown when malformed) / ZipException (thrown when unsupported format)
                // If zip cannot be read with UTF-8 Charset, then switch to CP-437 (Default for Most Windows Zip Software)
                // i.e: 7-Zip, PeaZip, Winrar, Winzip
                try {
                    zipFile = ZipFile(zipFilePath, Charset.forName("Cp437"))
                    zipFile.entries().toList()
                } catch (e: Exception) {
                    Timber.e(exception, "ZipFile")
                    // Close the ZipFile if fallback also fails
                    zipFile?.close()
                    return false
                }
            } finally {
                zipFile?.close()
            }
            return true
        }
    }
}