package mega.privacy.android.domain.di

import dagger.Module
import dagger.hilt.migration.DisableInstallInCheck

/**
 * Domain billing module
 *
 */
@Module(includes = [InternalBillingModule::class])
@DisableInstallInCheck
abstract class BillingModule