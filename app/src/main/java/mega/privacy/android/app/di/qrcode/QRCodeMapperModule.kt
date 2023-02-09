package mega.privacy.android.app.di.qrcode

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.app.presentation.qrcode.mapper.CombineQRCodeAndAvatarMapper
import mega.privacy.android.app.presentation.qrcode.mapper.DefaultCombineQRCodeAndAvatarMapper
import mega.privacy.android.app.presentation.qrcode.mapper.DefaultGetCircleBitmapMapper
import mega.privacy.android.app.presentation.qrcode.mapper.DefaultLoadBitmapFromFileMapper
import mega.privacy.android.app.presentation.qrcode.mapper.DefaultQRCodeMapper
import mega.privacy.android.app.presentation.qrcode.mapper.DefaultSaveBitmapToFileMapper
import mega.privacy.android.app.presentation.qrcode.mapper.GetCircleBitmapMapper
import mega.privacy.android.app.presentation.qrcode.mapper.LoadBitmapFromFileMapper
import mega.privacy.android.app.presentation.qrcode.mapper.QRCodeMapper
import mega.privacy.android.app.presentation.qrcode.mapper.SaveBitmapToFileMapper

/**
 * DI module for QR code mapper creation
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class QRCodeMapperModule {

    /**
     * provide the instance of [QRCodeMapper]
     */
    @Binds
    abstract fun bindQRCodeMapper(implementation: DefaultQRCodeMapper): QRCodeMapper

    /**
     * provide the instance of [LoadBitmapFromFileMapper]
     */
    @Binds
    abstract fun bindLoadBitmapFromFile(implementation: DefaultLoadBitmapFromFileMapper): LoadBitmapFromFileMapper

    /**
     * provide the instance of [SaveBitmapToFileMapper]
     */
    @Binds
    abstract fun bindSaveBitmapToFile(implementation: DefaultSaveBitmapToFileMapper): SaveBitmapToFileMapper

    /**
     * provide the instance of [GetCircleBitmapMapper]
     */
    @Binds
    abstract fun bindGetCircleBitmap(implementation: DefaultGetCircleBitmapMapper): GetCircleBitmapMapper

    /**
     * provide the instance of [CombineQRCodeAndAvatarMapper]
     */
    @Binds
    abstract fun bindCombineQRCodeAndAvatarMapper(implementation: DefaultCombineQRCodeAndAvatarMapper): CombineQRCodeAndAvatarMapper
}
