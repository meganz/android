package mega.privacy.android.domain.di

import dagger.Module
import dagger.hilt.migration.DisableInstallInCheck

/**
 * Change password dependencies module
 */
@Module(includes = [InternalChangePasswordModule::class])
@DisableInstallInCheck
class ChangePasswordModule {}