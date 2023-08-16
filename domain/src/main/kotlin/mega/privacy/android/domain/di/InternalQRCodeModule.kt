package mega.privacy.android.domain.di

import dagger.Binds
import dagger.Module
import dagger.hilt.migration.DisableInstallInCheck
import mega.privacy.android.domain.usecase.qrcode.DefaultQueryScannedContactLink
import mega.privacy.android.domain.usecase.qrcode.QueryScannedContactLink

/**
 * QR Code module
 *
 * Provides QR Code use cases
 *
 */
@Module
@DisableInstallInCheck
internal abstract class InternalQRCodeModule {

    /**
     * Provides GetScannedContactLink use case implementation
     */
    @Binds
    abstract fun bindGetScannedContactLink(implementation: DefaultQueryScannedContactLink): QueryScannedContactLink
}