package mega.privacy.android.data.facade

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import mega.privacy.android.data.gateway.AssetsGateway
import java.io.InputStream
import javax.inject.Inject

internal class AssetsFacade @Inject constructor(
    @ApplicationContext private val context: Context,
) : AssetsGateway {
    override fun open(filePath: String): InputStream = context.assets.open(filePath)
}