LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := zenlib
LOCAL_C_INCLUDES := $(LOCAL_PATH)/ZenLib/Source $(LOCAL_PATH)/ZenLib/Source/ZenLib
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/ZenLib/Source
LOCAL_CFLAGS := -fexceptions -frtti -fvisibility=hidden -fvisibility-inlines-hidden -fdata-sections -ffunction-sections -DUNICODE -D_LARGE_FILES
ifeq ($(TARGET_ARCH_ABI),x86_64)
  LOCAL_CFLAGS += -D_FILE_OFFSET_BITS=64
else ifeq ($(TARGET_ARCH_ABI),arm64-v8a)
  LOCAL_CFLAGS += -D_FILE_OFFSET_BITS=64
endif

LOCAL_SRC_FILES := $(addprefix ZenLib/Source/ZenLib/, \
                       Conf.cpp \
                       CriticalSection.cpp \
                       Dir.cpp \
                       File.cpp \
                       FileName.cpp \
                       InfoMap.cpp \
                       int128s.cpp \
                       int128u.cpp \
                       MemoryDebug.cpp \
                       OS_Utils.cpp \
                       Translation.cpp \
                       Thread.cpp \
                       Utils.cpp \
                       Ztring.cpp \
                       ZtringList.cpp \
                       ZtringListList.cpp \
                       ZtringListListF.cpp \
                       Format/Html/Html_Handler.cpp \
                       Format/Html/Html_Request.cpp \
                       Format/Http/Http_Cookies.cpp \
                       Format/Http/Http_Handler.cpp \
                       Format/Http/Http_Request.cpp \
                       Format/Http/Http_Utils.cpp)
include $(BUILD_STATIC_LIBRARY)


include $(CLEAR_VARS)
LOCAL_MODULE := mediainfo
LOCAL_C_INCLUDES := $(LOCAL_PATH)/mediainfo/Source $(LOCAL_PATH)/mediainfo/Source/ThirdParty/tinyxml2 \
                    $(LOCAL_PATH)/mediainfo/Source/ThirdParty/aes-gladman $(LOCAL_PATH)/mediainfo/Source/ThirdParty/md5 \
                    $(LOCAL_PATH)/mediainfo/Source/ThirdParty/base64 $(LOCAL_PATH)/mediainfo/Source/ThirdParty/hmac_gladman \
                    $(LOCAL_PATH)/mediainfo/Source/ThirdParty/sha1-gladman $(LOCAL_PATH)/mediainfo/Source/ThirdParty/sha2-gladman
                    
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/mediainfo/Source
LOCAL_CFLAGS := -fexceptions -frtti -fvisibility=hidden -fvisibility-inlines-hidden -fdata-sections -ffunction-sections -DMEDIAINFO_MINIMIZESIZE -DMEDIAINFO_MINIMAL_YES -DMEDIAINFO_ARCHIVE_NO -DMEDIAINFO_IMAGE_NO \
    -DMEDIAINFO_TAG_NO -DMEDIAINFO_TEXT_NO -DMEDIAINFO_SWF_NO -DMEDIAINFO_FLV_NO -DMEDIAINFO_HDSF4M_NO -DMEDIAINFO_CDXA_NO -DMEDIAINFO_DPG_NO -DMEDIAINFO_PMP_NO -DMEDIAINFO_RM_NO -DMEDIAINFO_WTV_NO -DMEDIAINFO_MXF_NO \
    -DMEDIAINFO_DCP_NO -DMEDIAINFO_AAF_NO -DMEDIAINFO_BDAV_NO -DMEDIAINFO_BDMV_NO -DMEDIAINFO_DVDV_NO -DMEDIAINFO_GXF_NO -DMEDIAINFO_MIXML_NO -DMEDIAINFO_SKM_NO -DMEDIAINFO_NUT_NO -DMEDIAINFO_TSP_NO -DMEDIAINFO_HLS_NO \
    -DMEDIAINFO_DXW_NO -DMEDIAINFO_DVDIF_NO -DMEDIAINFO_DASHMPD_NO -DMEDIAINFO_AIC_NO -DMEDIAINFO_AVSV_NO -DMEDIAINFO_CANOPUS_NO -DMEDIAINFO_FFV1_NO -DMEDIAINFO_FLIC_NO -DMEDIAINFO_HUFFYUV_NO -DMEDIAINFO_PRORES_NO -DMEDIAINFO_Y4M_NO \
    -DMEDIAINFO_ADPCM_NO -DMEDIAINFO_AMR_NO -DMEDIAINFO_AMV_NO -DMEDIAINFO_APE_NO -DMEDIAINFO_AU_NO -DMEDIAINFO_LA_NO -DMEDIAINFO_CELT_NO -DMEDIAINFO_MIDI_NO -DMEDIAINFO_MPC_NO -DMEDIAINFO_OPENMG_NO -DMEDIAINFO_PCM_NO -DMEDIAINFO_PS2A_NO \
    -DMEDIAINFO_RKAU_NO -DMEDIAINFO_SPEEX_NO -DMEDIAINFO_TAK_NO -DMEDIAINFO_TTA_NO -DMEDIAINFO_TWINVQ_NO -DMEDIAINFO_REFERENCES_NO -DMEDIAINFO_ADVANCED_NO -DMEDIAINFO_ADVANCED2_NO -DMEDIAINFO_OTHER_NO -DMEDIAINFO_ANCILLARY_NO \
    -DMEDIAINFO_SEQUENCEINFO_NO -DMEDIAINFO_AFDBARDATA_NO -DMEDIAINFO_MPCSV8_NO -DMEDIAINFO_S3M_NO -DMEDIAINFO_DOLBYE_NO -DMEDIAINFO_CAF_NO -DMEDIAINFO_IT_NO -DMEDIAINFO_PCMVOB_NO -DMEDIAINFO_MOD_NO -DMEDIAINFO_DIRAC_NO -DMEDIAINFO_FRAPS_NO \
    -DMEDIAINFO_LAGARITH_NO -DMEDIAINFO_LXF_NO -DMEDIAINFO_PTX_NO -DMEDIAINFO_ISM_NO -DMEDIAINFO_P2_NO -DMEDIAINFO_IVF_NO -DMEDIAINFO_VBI_NO -DMEDIAINFO_XDCAM_NO -DMEDIAINFO_TIMECODE_NO -DMEDIAINFO_XM_NO -DMEDIAINFO_VORBISCOM_NO \
    -DUNICODE -Wno-undefined-inline

LOCAL_SRC_FILES := $(addprefix mediainfo/Source/, \
	MediaInfo/File__Analyze.cpp \
	MediaInfo/File__Analyze_Buffer.cpp \
	MediaInfo/File__Analyze_Buffer_MinimizeSize.cpp \
	MediaInfo/File__Analyze_Streams.cpp \
	MediaInfo/File__Analyze_Streams_Finish.cpp \
	MediaInfo/File__Analyze_Element.cpp \
	MediaInfo/File__Base.cpp \
	MediaInfo/File__MultipleParsing.cpp \
	MediaInfo/File__Duplicate.cpp \
	MediaInfo/File__HasReferences.cpp \
	MediaInfo/File_Dummy.cpp \
	MediaInfo/File_Other.cpp \
	MediaInfo/File_Unknown.cpp \
	MediaInfo/HashWrapper.cpp \
	MediaInfo/MediaInfo.cpp \
	MediaInfo/MediaInfo_Config.cpp \
	MediaInfo/MediaInfo_Config_Automatic.cpp \
	MediaInfo/MediaInfo_Config_MediaInfo.cpp \
	MediaInfo/MediaInfo_Config_PerPackage.cpp \
	MediaInfo/MediaInfo_File.cpp \
	MediaInfo/MediaInfo_Inform.cpp \
	MediaInfo/MediaInfo_Internal.cpp \
	MediaInfo/MediaInfoList.cpp \
	MediaInfo/MediaInfoList_Internal.cpp \
	MediaInfo/TimeCode.cpp \
	MediaInfo/Archive/File_7z.cpp \
	MediaInfo/Archive/File_Ace.cpp \
	MediaInfo/Archive/File_Bzip2.cpp \
	MediaInfo/Archive/File_Elf.cpp \
	MediaInfo/Archive/File_Gzip.cpp \
	MediaInfo/Archive/File_Iso9660.cpp \
	MediaInfo/Archive/File_Mz.cpp \
	MediaInfo/Archive/File_Rar.cpp \
	MediaInfo/Archive/File_Tar.cpp \
	MediaInfo/Archive/File_Zip.cpp \
	MediaInfo/Audio/File_Aac.cpp \
	MediaInfo/Audio/File_Aac_GeneralAudio.cpp \
	MediaInfo/Audio/File_Aac_GeneralAudio_Sbr.cpp \
	MediaInfo/Audio/File_Aac_GeneralAudio_Sbr_Ps.cpp \
	MediaInfo/Audio/File_Aac_Main.cpp \
	MediaInfo/Audio/File_Aac_Others.cpp \
	MediaInfo/Audio/File_Ac3.cpp \
	MediaInfo/Audio/File_Adpcm.cpp \
	MediaInfo/Audio/File_Als.cpp \
	MediaInfo/Audio/File_Amr.cpp \
	MediaInfo/Audio/File_Amv.cpp \
	MediaInfo/Audio/File_Ape.cpp \
	MediaInfo/Audio/File_Au.cpp \
	MediaInfo/Audio/File_Caf.cpp \
	MediaInfo/Audio/File_Celt.cpp \
	MediaInfo/Audio/File_ChannelGrouping.cpp \
	MediaInfo/Audio/File_Dts.cpp \
	MediaInfo/Audio/File_DolbyE.cpp \
	MediaInfo/Audio/File_ExtendedModule.cpp \
	MediaInfo/Audio/File_Flac.cpp \
	MediaInfo/Audio/File_ImpulseTracker.cpp \
	MediaInfo/Audio/File_La.cpp \
	MediaInfo/Audio/File_Midi.cpp \
	MediaInfo/Audio/File_Module.cpp \
	MediaInfo/Audio/File_Mpc.cpp \
	MediaInfo/Audio/File_MpcSv8.cpp \
	MediaInfo/Audio/File_Mpega.cpp \
	MediaInfo/Audio/File_OpenMG.cpp \
	MediaInfo/Audio/File_Opus.cpp \
	MediaInfo/Audio/File_Pcm.cpp \
	MediaInfo/Audio/File_Pcm_M2ts.cpp \
	MediaInfo/Audio/File_Pcm_Vob.cpp \
	MediaInfo/Audio/File_Ps2Audio.cpp \
	MediaInfo/Audio/File_Rkau.cpp \
	MediaInfo/Audio/File_ScreamTracker3.cpp \
	MediaInfo/Audio/File_SmpteSt0302.cpp \
	MediaInfo/Audio/File_SmpteSt0331.cpp \
	MediaInfo/Audio/File_SmpteSt0337.cpp \
	MediaInfo/Audio/File_Speex.cpp \
	MediaInfo/Audio/File_Tak.cpp \
	MediaInfo/Audio/File_Tta.cpp \
	MediaInfo/Audio/File_TwinVQ.cpp \
	MediaInfo/Audio/File_Vorbis.cpp \
	MediaInfo/Audio/File_Wvpk.cpp \
	MediaInfo/Duplicate/File__Duplicate__Base.cpp \
	MediaInfo/Duplicate/File__Duplicate__Writer.cpp \
	MediaInfo/Duplicate/File__Duplicate_MpegTs.cpp \
	MediaInfo/Export/Export_EbuCore.cpp \
	MediaInfo/Export/Export_Fims.cpp \
	MediaInfo/Export/Export_Mpeg7.cpp \
	MediaInfo/Export/Export_PBCore.cpp \
	MediaInfo/Export/Export_PBCore2.cpp \
	MediaInfo/Export/Export_reVTMD.cpp \
	MediaInfo/Image/File_ArriRaw.cpp \
	MediaInfo/Image/File_Bmp.cpp \
	MediaInfo/Image/File_Bpg.cpp \
	MediaInfo/Image/File_Dds.cpp \
	MediaInfo/Image/File_Dpx.cpp \
	MediaInfo/Image/File_Exr.cpp \
	MediaInfo/Image/File_Gif.cpp \
	MediaInfo/Image/File_Ico.cpp \
	MediaInfo/Image/File_Jpeg.cpp \
	MediaInfo/Image/File_Pcx.cpp \
	MediaInfo/Image/File_Png.cpp \
	MediaInfo/Image/File_Psd.cpp \
	MediaInfo/Image/File_Rle.cpp \
	MediaInfo/Image/File_Tiff.cpp \
	MediaInfo/Image/File_Tga.cpp \
	MediaInfo/Multiple/File__ReferenceFilesHelper.cpp \
	MediaInfo/Multiple/File__ReferenceFilesHelper_Resource.cpp \
	MediaInfo/Multiple/File__ReferenceFilesHelper_Sequence.cpp \
	MediaInfo/Multiple/File_Aaf.cpp \
	MediaInfo/Multiple/File_Ancillary.cpp \
	MediaInfo/Multiple/File_Bdmv.cpp \
	MediaInfo/Multiple/File_Cdxa.cpp \
	MediaInfo/Multiple/File_DashMpd.cpp \
	MediaInfo/Multiple/File_DcpAm.cpp \
	MediaInfo/Multiple/File_DcpCpl.cpp \
	MediaInfo/Multiple/File_DcpPkl.cpp \
	MediaInfo/Multiple/File_Dpg.cpp \
	MediaInfo/Multiple/File_DvDif.cpp \
	MediaInfo/Multiple/File_DvDif_Analysis.cpp \
	MediaInfo/Multiple/File_Dvdv.cpp \
	MediaInfo/Multiple/File_Dxw.cpp \
	MediaInfo/Multiple/File_Flv.cpp \
	MediaInfo/Multiple/File_Gxf.cpp \
	MediaInfo/Multiple/File_Gxf_TimeCode.cpp \
	MediaInfo/Multiple/File_HdsF4m.cpp \
	MediaInfo/Multiple/File_Hls.cpp \
	MediaInfo/Multiple/File_Ibi.cpp \
	MediaInfo/Multiple/File_Ibi_Creation.cpp \
	MediaInfo/Multiple/File_Ism.cpp \
	MediaInfo/Multiple/File_Ivf.cpp \
	MediaInfo/Multiple/File_Lxf.cpp \
	MediaInfo/Multiple/File_Mk.cpp \
	MediaInfo/Multiple/File_MiXml.cpp \
	MediaInfo/Multiple/File_Mpeg4.cpp \
	MediaInfo/Multiple/File_Mpeg4_Descriptors.cpp \
	MediaInfo/Multiple/File_Mpeg4_Elements.cpp \
	MediaInfo/Multiple/File_Mpeg4_TimeCode.cpp \
	MediaInfo/Multiple/File_Mpeg_Descriptors.cpp \
	MediaInfo/Multiple/File_Mpeg_Psi.cpp \
	MediaInfo/Multiple/File_MpegPs.cpp \
	MediaInfo/Multiple/File_MpegTs.cpp \
	MediaInfo/Multiple/File_MpegTs_Duplicate.cpp \
	MediaInfo/Multiple/File_Mxf.cpp \
	MediaInfo/Multiple/File_Nut.cpp \
	MediaInfo/Multiple/File_Ogg.cpp \
	MediaInfo/Multiple/File_Ogg_SubElement.cpp \
	MediaInfo/Multiple/File_P2_Clip.cpp \
	MediaInfo/Multiple/File_Pmp.cpp \
	MediaInfo/Multiple/File_Ptx.cpp \
	MediaInfo/Multiple/File_Riff.cpp \
	MediaInfo/Multiple/File_Riff_Elements.cpp \
	MediaInfo/Multiple/File_Rm.cpp \
	MediaInfo/Multiple/File_SequenceInfo.cpp \
	MediaInfo/Multiple/File_Skm.cpp \
	MediaInfo/Multiple/File_Swf.cpp \
	MediaInfo/Multiple/File_Umf.cpp \
	MediaInfo/Multiple/File_Vbi.cpp \
	MediaInfo/Multiple/File_Wm.cpp \
	MediaInfo/Multiple/File_Wm_Elements.cpp \
	MediaInfo/Multiple/File_Wtv.cpp \
	MediaInfo/Multiple/File_Xdcam_Clip.cpp \
	MediaInfo/Reader/Reader_Directory.cpp \
	MediaInfo/Reader/Reader_File.cpp \
	MediaInfo/Reader/Reader_libcurl.cpp \
	MediaInfo/Reader/Reader_libmms.cpp \
	MediaInfo/Tag/File__Tags.cpp \
	MediaInfo/Tag/File_ApeTag.cpp \
	MediaInfo/Tag/File_Id3.cpp \
	MediaInfo/Tag/File_Id3v2.cpp \
	MediaInfo/Tag/File_Lyrics3.cpp \
	MediaInfo/Tag/File_Lyrics3v2.cpp \
	MediaInfo/Tag/File_PropertyList.cpp \
	MediaInfo/Tag/File_VorbisCom.cpp \
	MediaInfo/Tag/File_Xmp.cpp \
	MediaInfo/Text/File_Cdp.cpp \
	MediaInfo/Text/File_Cmml.cpp \
	MediaInfo/Text/File_DvbSubtitle.cpp \
	MediaInfo/Text/File_DtvccTransport.cpp \
	MediaInfo/Text/File_Kate.cpp \
	MediaInfo/Text/File_AribStdB24B37.cpp \
	MediaInfo/Text/File_Eia608.cpp \
	MediaInfo/Text/File_Eia708.cpp \
	MediaInfo/Text/File_N19.cpp \
	MediaInfo/Text/File_OtherText.cpp \
	MediaInfo/Text/File_Pdf.cpp \
	MediaInfo/Text/File_Pgs.cpp \
	MediaInfo/Text/File_Scc.cpp \
	MediaInfo/Text/File_Scte20.cpp \
	MediaInfo/Text/File_Sdp.cpp \
	MediaInfo/Text/File_SubRip.cpp \
	MediaInfo/Text/File_Teletext.cpp \
	MediaInfo/Text/File_TimedText.cpp \
	MediaInfo/Text/File_Ttml.cpp \
	MediaInfo/Video/File_Aic.cpp \
	MediaInfo/Video/File_AfdBarData.cpp \
	MediaInfo/Video/File_Avc.cpp \
	MediaInfo/Video/File_Avc_Duplicate.cpp \
	MediaInfo/Video/File_AvsV.cpp \
	MediaInfo/Video/File_Canopus.cpp \
	MediaInfo/Video/File_Dirac.cpp \
	MediaInfo/Video/File_Ffv1.cpp \
	MediaInfo/Video/File_Flic.cpp \
	MediaInfo/Video/File_Fraps.cpp \
	MediaInfo/Video/File_Lagarith.cpp \
	MediaInfo/Video/File_H263.cpp \
	MediaInfo/Video/File_Hevc.cpp \
	MediaInfo/Video/File_HuffYuv.cpp \
	MediaInfo/Video/File_Mpeg4v.cpp \
	MediaInfo/Video/File_Mpegv.cpp \
	MediaInfo/Video/File_ProRes.cpp \
	MediaInfo/Video/File_Theora.cpp \
	MediaInfo/Video/File_Vc1.cpp \
	MediaInfo/Video/File_Vc3.cpp \
	MediaInfo/Video/File_Vp8.cpp \
	MediaInfo/Video/File_Y4m.cpp \
	MediaInfo/XmlUtils.cpp \
	MediaInfo/OutputHelpers.cpp \
	MediaInfoDLL/MediaInfoDLL.cpp \
	ThirdParty/aes-gladman/aes_modes.c \
	ThirdParty/aes-gladman/aescrypt.c \
	ThirdParty/aes-gladman/aeskey.c \
	ThirdParty/aes-gladman/aestab.c \
	ThirdParty/md5/md5.c \
	ThirdParty/sha1-gladman/sha1.c \
	ThirdParty/sha2-gladman/sha2.c \
	ThirdParty/hmac-gladman/hmac.c \
	ThirdParty/tinyxml2/tinyxml2.cpp)

LOCAL_EXPORT_CFLAGS := -DUSE_MEDIAINFO -DUNICODE
LOCAL_STATIC_LIBRARIES := zenlib
include $(BUILD_STATIC_LIBRARY)

