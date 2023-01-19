package mega.privacy.android.domain.di

import dagger.Module
import dagger.hilt.migration.DisableInstallInCheck

/**
 * Domain setting module
 *
 */
@Module(includes = [InternalSettingModule::class])
@DisableInstallInCheck
abstract class SettingModule