package mega.privacy.mobile.home.presentation.continuewhereleftoff

import androidx.annotation.DrawableRes
import mega.privacy.android.domain.entity.continuewhereleftoff.RecentlyUsedType
import mega.privacy.android.icon.pack.R as IconPackR

@DrawableRes
internal fun iconForType(type: RecentlyUsedType): Int = when (type) {
    RecentlyUsedType.Video -> IconPackR.drawable.ic_video_medium_solid
    RecentlyUsedType.Audio -> IconPackR.drawable.ic_audio_medium_solid
    RecentlyUsedType.PDF -> IconPackR.drawable.ic_pdf_medium_solid
    RecentlyUsedType.TextEditor -> IconPackR.drawable.ic_text_medium_solid
    RecentlyUsedType.FileLink -> IconPackR.drawable.ic_link_01_medium_regular_solid
    RecentlyUsedType.FolderLink -> IconPackR.drawable.ic_folder_medium_solid
}
