package mega.privacy.android.domain.di

import dagger.Module
import dagger.hilt.migration.DisableInstallInCheck

/**
 * Domain Dagger module for Use Cases in Export Recovery Key Activity
 */
@Module(includes = [InternalRecoveryKeyModule::class])
@DisableInstallInCheck
abstract class RecoveryKeyModule