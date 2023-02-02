package mega.privacy.android.domain.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * QR Code module
 *
 */
@Module(includes = [InternalQRCodeModule::class])
@InstallIn(SingletonComponent::class)
abstract class QRCodeModule