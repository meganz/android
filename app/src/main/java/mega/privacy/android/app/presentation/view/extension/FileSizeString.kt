package mega.privacy.android.app.presentation.view.extension

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import mega.privacy.android.app.R
import mega.privacy.android.domain.entity.node.FileNode
import java.text.DecimalFormat

@Composable
internal fun FileNode.fileSize(): String {
    val format = DecimalFormat("#.##")
    val kilobyte = 1024f
    val megabyte = kilobyte * 1024
    val gigabyte = megabyte * 1024
    val terabyte = gigabyte * 1024
    val petabyte = terabyte * 1024
    val exabyte = petabyte * 1024
    return if (size < kilobyte) {
        stringResource(
            R.string.label_file_size_byte,
            size.toString()
        )
    } else if (size < megabyte) {
        stringResource(
            R.string.label_file_size_kilo_byte,
            format.format((size / kilobyte).toDouble())
        )
    } else if (size < gigabyte) {
        stringResource(
            R.string.label_file_size_mega_byte,
            format.format((size / megabyte).toDouble())
        )
    } else if (size < terabyte) {
        stringResource(
            R.string.label_file_size_giga_byte,
            format.format((size / gigabyte).toDouble())
        )
    } else if (size < petabyte) {
        stringResource(
            R.string.label_file_size_tera_byte,
            format.format((size / terabyte).toDouble())
        )
    } else if (size < exabyte) {
        stringResource(R.string.label_file_size_peta_byte, format.format((size / petabyte).toDouble()))
    } else {
        stringResource(R.string.label_file_size_exa_byte, format.format((size / exabyte).toDouble()))
    }
}