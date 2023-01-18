package mega.privacy.android.app.di.qrcode

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.app.presentation.qrcode.mapper.DefaultQRCodeMapper
import mega.privacy.android.app.presentation.qrcode.mapper.QRCodeMapper

/**
 * DI module for QR code creation
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class QRCodeModule {

    @Binds
    abstract fun bindQRCodeMapper(implementation: DefaultQRCodeMapper): QRCodeMapper
}
