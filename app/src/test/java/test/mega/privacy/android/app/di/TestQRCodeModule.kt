package test.mega.privacy.android.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import mega.privacy.android.app.presentation.qrcode.mapper.QRCodeMapper
import mega.privacy.android.domain.usecase.DeleteQRCode
import mega.privacy.android.domain.usecase.GetQRCodeFile
import mega.privacy.android.domain.usecase.ResetContactLink
import mega.privacy.android.domain.usecase.qrcode.QueryScannedContactLink
import org.mockito.kotlin.mock

/**
 * Provide test dependencies for QR code use cases
 */
@TestInstallIn(
    replaces = [mega.privacy.android.domain.di.QRCodeModule::class,
        mega.privacy.android.app.di.qrcode.QRCodeMapperModule::class],
    components = [SingletonComponent::class]
)
@Module
class TestQRCodeModule {
    private val deleteQrCode = mock<DeleteQRCode>()
    private val resetContactLink = mock<ResetContactLink>()
    private val getQRCodeFile = mock<GetQRCodeFile>()
    private val qrCodeMapper = mock<QRCodeMapper>()
    private val queryScannedContactLink = mock<QueryScannedContactLink>()

    @Provides
    fun provideDeleteQRCode() = deleteQrCode

    @Provides
    fun provideResetQRCode() = resetContactLink

    @Provides
    fun provideGetQRCodeFile() = getQRCodeFile

    @Provides
    fun provideCreateQRCodeMapper() = qrCodeMapper

    @Provides
    fun provideQueryScannedContactLink() = queryScannedContactLink
}