package mega.privacy.android.domain.usecase

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import mega.privacy.android.domain.qualifier.IoDispatcher
import mega.privacy.android.domain.repository.AccountRepository
import javax.inject.Inject

/**
 * [DeleteQRCode] implementation
 */
class DefaultDeleteQRCode @Inject constructor(
    private val accountRepository: AccountRepository,
    private val getQRFile: GetQRFile,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : DeleteQRCode {

    override suspend fun invoke(handle: Long, fileName: String) {
        withContext(ioDispatcher) {
            accountRepository.deleteContactLink(handle)
            getQRFile(fileName)?.takeIf { it.exists() }?.delete()
        }
    }
}