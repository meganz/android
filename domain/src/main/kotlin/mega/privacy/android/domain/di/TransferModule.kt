package mega.privacy.android.domain.di

import dagger.Module
import dagger.hilt.migration.DisableInstallInCheck

@Module(includes = [InternalTransferModule::class])
@DisableInstallInCheck
abstract class TransferModule