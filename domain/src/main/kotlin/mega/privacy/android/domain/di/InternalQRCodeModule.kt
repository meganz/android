package mega.privacy.android.domain.di

import dagger.Binds
import dagger.Module
import dagger.hilt.migration.DisableInstallInCheck
import mega.privacy.android.domain.usecase.DefaultDeleteQRCode
import mega.privacy.android.domain.usecase.DefaultGetQRCodeFile
import mega.privacy.android.domain.usecase.DefaultResetContactLink
import mega.privacy.android.domain.usecase.DeleteQRCode
import mega.privacy.android.domain.usecase.GetQRCodeFile
import mega.privacy.android.domain.usecase.ResetContactLink
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

    @Binds
    abstract fun bindDeleteQRCode(implementation: DefaultDeleteQRCode): DeleteQRCode

    @Binds
    abstract fun bindGetQRCodeFile(implementation: DefaultGetQRCodeFile): GetQRCodeFile

    @Binds
    abstract fun bindResetContactLink(implementation: DefaultResetContactLink): ResetContactLink

    /**
     * Provides GetScannedContactLink use case implementation
     */
    @Binds
    abstract fun bindGetScannedContactLink(implementation: DefaultQueryScannedContactLink): QueryScannedContactLink
}