package test.mega.privacy.android.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import mega.privacy.android.app.di.qrcode.QRCodeMapperModule
import mega.privacy.android.app.presentation.qrcode.mapper.CombineQRCodeAndAvatarMapper
import mega.privacy.android.app.presentation.qrcode.mapper.GetCircleBitmapMapper
import mega.privacy.android.app.presentation.qrcode.mapper.LoadBitmapFromFileMapper
import mega.privacy.android.app.presentation.qrcode.mapper.MyQRCodeTextErrorMapper
import mega.privacy.android.app.presentation.qrcode.mapper.QRCodeMapper
import mega.privacy.android.app.presentation.qrcode.mapper.SaveBitmapToFileMapper
import mega.privacy.android.domain.di.QRCodeModule
import mega.privacy.android.domain.usecase.DeleteQRCode
import mega.privacy.android.domain.usecase.ResetContactLink
import mega.privacy.android.domain.usecase.qrcode.QueryScannedContactLink
import org.mockito.kotlin.mock

/**
 * Provide test dependencies for QR code use cases
 */
@TestInstallIn(
    replaces = [
        QRCodeModule::class,
        QRCodeMapperModule::class
    ],
    components = [SingletonComponent::class]
)
@Module
class TestQRCodeModule {
    private val deleteQrCode = mock<DeleteQRCode>()
    private val resetContactLink = mock<ResetContactLink>()
    private val qrCodeMapper = mock<QRCodeMapper>()
    private val queryScannedContactLink = mock<QueryScannedContactLink>()
    private val loadBitmapFromFileMapper = mock<LoadBitmapFromFileMapper>()
    private val saveBitmapToFileMapper = mock<SaveBitmapToFileMapper>()
    private val getCircleBitmapMapper = mock<GetCircleBitmapMapper>()
    private val combineQRCodeAndAvatarMapper = mock<CombineQRCodeAndAvatarMapper>()
    private val myQRCodeTextErrorMapper = mock<MyQRCodeTextErrorMapper>()

    @Provides
    fun provideDeleteQRCode() = deleteQrCode

    @Provides
    fun provideResetQRCode() = resetContactLink

    @Provides
    fun provideCreateQRCodeMapper() = qrCodeMapper

    @Provides
    fun provideQueryScannedContactLink() = queryScannedContactLink

    @Provides
    fun provideLoadBitmapFromFileMapper() = loadBitmapFromFileMapper

    @Provides
    fun provideSaveBitmapToFileMapper() = saveBitmapToFileMapper

    @Provides
    fun provideGetCircleBitmapMapper() = getCircleBitmapMapper

    @Provides
    fun provideCombineQRCodeAndAvatarMapper() = combineQRCodeAndAvatarMapper

    @Provides
    fun provideMyQRCodeExceptionTextMapper() = myQRCodeTextErrorMapper

}