package mega.privacy.android.data.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.domain.qualifier.LogFileDirectory
import mega.privacy.android.domain.qualifier.LogZipFileDirectory
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class FileModule {
    @LogFileDirectory
    @Singleton
    @Provides
    fun provideLogFileDirectory(
        @ApplicationContext applicationContext: Context
        ): File = getLogDirectory(applicationContext.getExternalFilesDir(null), "MEGA Logs")

    @LogZipFileDirectory
    @Singleton
    @Provides
    fun provideLogZipFileDirectory(
        @ApplicationContext applicationContext: Context,
    ): File = getLogDirectory(applicationContext.getExternalFilesDir(null), "MEGA Logs Zip")

    private fun getLogDirectory(baseDirectory: File?, child: String): File {
        val localDir = File(baseDirectory, child)
        if (!localDir.exists()) {
            localDir.mkdirs()
        }
        return localDir
    }
}