package mega.privacy.android.domain.entity.node.serialisation

import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import mega.privacy.android.domain.entity.AudioFileTypeInfo
import mega.privacy.android.domain.entity.FileTypeInfo
import mega.privacy.android.domain.entity.GifFileTypeInfo
import mega.privacy.android.domain.entity.PdfFileTypeInfo
import mega.privacy.android.domain.entity.RawFileTypeInfo
import mega.privacy.android.domain.entity.StaticImageFileTypeInfo
import mega.privacy.android.domain.entity.SvgFileTypeInfo
import mega.privacy.android.domain.entity.TextFileTypeInfo
import mega.privacy.android.domain.entity.UnMappedFileTypeInfo
import mega.privacy.android.domain.entity.UnknownFileTypeInfo
import mega.privacy.android.domain.entity.UrlFileTypeInfo
import mega.privacy.android.domain.entity.VideoFileTypeInfo
import mega.privacy.android.domain.entity.ZipFileTypeInfo
import mega.privacy.android.domain.entity.node.DefaultTypedFileNode
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.chat.ChatDefaultFile
import mega.privacy.android.domain.entity.node.chat.ChatFile

/**
 * NodeSerialisationModule
 */
val nodeSerialisationModule = SerializersModule {
    polymorphic(FileTypeInfo::class) {
        subclass(UnknownFileTypeInfo::class)
        subclass(PdfFileTypeInfo::class)
        subclass(ZipFileTypeInfo::class)
        subclass(UrlFileTypeInfo::class)
        subclass(StaticImageFileTypeInfo::class)
        subclass(AudioFileTypeInfo::class)
        subclass(GifFileTypeInfo::class)
        subclass(RawFileTypeInfo::class)
        subclass(SvgFileTypeInfo::class)
        subclass(TextFileTypeInfo::class)
        subclass(UnMappedFileTypeInfo::class)
        subclass(VideoFileTypeInfo::class)
    }
    polymorphic(ChatFile::class) {
        subclass(ChatDefaultFile::class)
    }
    polymorphic(TypedFileNode::class) {
        subclass(DefaultTypedFileNode::class)
    }
}