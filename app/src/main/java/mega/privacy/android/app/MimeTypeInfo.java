//package mega.privacy.android.app;
//
//import android.annotation.SuppressLint;
//import android.graphics.Bitmap;
//import android.graphics.BitmapFactory;
//import android.util.SparseArray;
//import android.webkit.MimeTypeMap;
//
//import java.util.HashMap;
//
//import mega.privacy.android.app.utils.Util;
//
//
//public class MimeTypeInfo {
//
//    // Icon resource mapping for different file type extensions
//    private static HashMap<String, Integer> resourcesCache;
//    private static SparseArray<String[]> resources;
//    static {
//        resourcesCache = new HashMap<String, Integer>();
//        resources = new SparseArray<String[]>();
//        resources.put(R.drawable.ic_3d_info, new String[] {"3ds", "3dm", "max", "obj", });
//        resources.put(R.drawable.ic_after_effects_info, new String[] {"aec", "aep", "aepx", "aes", "aet", "aetx", });
//        resources.put(R.drawable.ic_audio_info, new String[] {"aif", "aiff", "wav", "flac", "iff", "m4a", "wma", "oga", "ogg", "mp3", "3ga", });
//        resources.put(R.drawable.ic_cad_info, new String[] {"dwg", "dxf", });
//        resources.put(R.drawable.ic_compressed_info, new String[] {"bz2", "gz", "rar", "tar", "tbz", "tgz", "zip", "deb", "udeb", "rpm", "air", "apk", "dmg", "7z", "bz", "bzip2", "cab", "lha", "gzip", "ace", "arc", "pkg", });
//        resources.put(R.drawable.ic_data_info, new String[] {"accdb", "db", "dbf", "mdb", "pdb", "sql", });
//        resources.put(R.drawable.ic_dmg_info, new String[]{"dmg", });
//        resources.put(R.drawable.ic_dreamweaver_info, new String[] {"dwt", });
//        resources.put(R.drawable.ic_excell_info, new String[] {"xls", "xlsx", "xlt", "xltm", "xltx", });
//        resources.put(R.drawable.ic_executable_info, new String[] {"apk", "app", "bat", "com", "exe", "gadget", "msi", "pif", "vb", "wsf", });
//        resources.put(R.drawable.ic_fla_lang_info, new String[] {"as", "asc", "ascs", });
//        resources.put(R.drawable.ic_flash_info, new String[] {"fla", });
//        resources.put(R.drawable.ic_font_info, new String[] {"fnt", "fon", "otf", "ttf", });
//        resources.put(R.drawable.ic_gis_info, new String[] {"gpx", "kml", "kmz", });
//        resources.put(R.drawable.ic_graphic_info, new String[] {"tga", "tif", "tiff", "bmp", "gif", "png", });
//        resources.put(R.drawable.ic_html_info, new String[] {"dhtml", "htm", "html", "shtml", "xhtml", });
//        resources.put(R.drawable.ic_illustrator_info, new String[] {"ai", "aia", "aip", "ait", "art", "irs", });
//        resources.put(R.drawable.ic_image_info, new String[] {"jpg", "jpeg", });
//        resources.put(R.drawable.ic_indesign_info, new String[] {"indd", });
//        resources.put(R.drawable.ic_java_info, new String[] {"class", "jar", "java", });
//        resources.put(R.drawable.ic_midi_info, new String[] {"mid", "midi", });
//        resources.put(R.drawable.ic_pdf_info, new String[] {"pdf", });
//        resources.put(R.drawable.ic_photoshop_info, new String[] {"abr", "csh", "psb", "psd", });
//        resources.put(R.drawable.ic_playlist_info, new String[] {"asx", "m3u", "pls", });
//        resources.put(R.drawable.ic_podcast_info, new String[] {"pcast", });
//        resources.put(R.drawable.ic_powerpoint_info, new String[] {"ppc", "ppt", "pptx", "pps",  });
//        resources.put(R.drawable.ic_premiere_info, new String[] {"plb", "ppj", "prproj", "prtpset", });
//        resources.put(R.drawable.ic_raw_info, new String[] {"3fr", "mef", "arw", "bay", "cr2", "dcr", "dng", "erf", "fff", "mrw", "nef", "orf", "pef", "rw2", "rwl", "srf", });
//        resources.put(R.drawable.ic_real_audio_info, new String[] {"ra", "ram", "rm", });
//        resources.put(R.drawable.ic_source_info, new String[] {"c", "cc", "cgi", "cpp", "cxx", "dll", "h", "hpp", "pl", "py", "sh", });
//        resources.put(R.drawable.ic_spreadsheet_info, new String[] {"123", "gsheet", "nb", "ods", "ots", "sxc", "xlr", });
//        resources.put(R.drawable.ic_subtitles_info, new String[] {"srt", });
//        resources.put(R.drawable.ic_swf_info, new String[] {"swf", "flv", });
//        resources.put(R.drawable.ic_text_info, new String[] {"ans", "ascii", "log", "odt", "rtf", "txt", "wpd", });
//        resources.put(R.drawable.ic_torrent_info, new String[] {"torrent", });
//        resources.put(R.drawable.ic_vcard_info, new String[] {"vcard", "vcf", });
//        resources.put(R.drawable.ic_vector_info, new String[] {"cdr", "eps", "ps", "svg", "svgz", });
//        resources.put(R.drawable.ic_video_info, new String[] {"3g2", "3gp", "asf", "avi", "mkv", "mov", "mpeg", "mpg", "wmv", "3gpp", "h261", "h263", "h264", "jpgv", "jpm", "jpgm", "mp4", "mp4v", "mpg4", "mpe", "m1v", "m2v", "ogv", "qt", "m4u", "webm", "f4v", "fli", "m4v", "mkv", "mk3d", "vob", "movie", });
//        resources.put(R.drawable.ic_video_vob_info, new String[]{"vob", });
//        resources.put(R.drawable.ic_web_data_info, new String[] {"asp", "aspx", "php", "php3", "php4", "php5", "phtml", "css", "inc", "js", "xml", });
//        resources.put(R.drawable.ic_word_info, new String[] {"doc", "docx", "dotx", "wps", });
//    }
//
//    private String type;
//    private String extension;
//    private int resId;
//
//    private MimeTypeInfo(String type, String extension) {
//        this.type = type;
//        this.extension = extension;
//        resId = -1;
//    }
//
//    /*
//     * Get MimeType for file name
//     */
//    @SuppressLint("DefaultLocale")
//    public static MimeTypeInfo typeForName(String name) {
//        if (name == null) {
//            name = "";
//        }
//        String fixedName = name.trim().toLowerCase();
//        String extension = null;
//        int index = fixedName.lastIndexOf(".");
//        if((index != -1) && ((index+1)<fixedName.length())) {
//            extension = fixedName.substring(index + 1);
//        } else {
//            extension = fixedName;
//        }
//        String detectedType = MimeTypeMap.getSingleton()
//                .getMimeTypeFromExtension(extension);
//        if (detectedType == null) {
//            if(extension.equals("mkv"))
//                detectedType = "video/x-matroska";
//            else
//                detectedType = "application/octet-stream";
//        }
//        if (extension == null) {
//            extension = "";
//        }
//        return new MimeTypeInfo(detectedType, extension);
//    }
//
//    public String getType() {
//        return type;
//    }
//
//    public boolean isDocument(){
//        boolean r = type.startsWith("application/pdf") || type.startsWith("application/msword") || type.startsWith("application/vnd.ms-excel") || type.startsWith("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") || type.startsWith("application/vnd.openxmlformats-officedocument.wordprocessingml.document") || type.startsWith("application/rtf") || type.startsWith("text/plain");
//
//        return r;
//    }
//
//    /*
//     * Check is MimeType of image type
//     */
//    public boolean isImage() {
//        return type.startsWith("image/");
//    }
//
//    public boolean isPdf(){
//        return type.startsWith("application/pdf");
//    }
//
//    public boolean isZip(){
//
//        if(type.startsWith("application/zip")||type.startsWith("multipart/x-zip")){
//            return true;
//        }
//        return false;
//
//    }
//
//
//    /*
//     * Once a file is downloaded, prior to create the preview, check if the file is really an image
//     */
//    public boolean isImage(String path){
//        BitmapFactory.Options options = new BitmapFactory.Options();
//        options.inJustDecodeBounds = true;
//        Bitmap bitmap = BitmapFactory.decodeFile(path, options);
//        if (options.outWidth != -1 && options.outHeight != -1) {
//            // This is an image file.
//            return true;
//        }
//        else {
//            // This is not an image file.
//            return false;
//        }
//    }
//
//    /*
//     * Check is MimeType of video type
//     */
//    public boolean isVideo() {
//        return type.startsWith("video/") || extension.equals("mkv");
//    }
//
//    /*
//     * Check is MimeType of audio type
//     */
//    public boolean isAudio() {
//        return type.startsWith("audio/");
//    }
//
//    /*
//     * Get Icon for current MimeType
//     */
//    public int getIconResourceId() {
//        if (resId == -1) {
//            if (resourcesCache.containsKey(extension)) {
//                resId = resourcesCache.get(extension);
//            } else {
//                for (int i = 0, len = resources.size(); i < len; i++) {
//                    int keyResId = resources.keyAt(i);
//                    for (String valueExtension : resources.get(keyResId)) {
//                        if (extension.equals(valueExtension)) {
//                            resId = keyResId;
//                            resourcesCache.put(extension, resId);
//                            break;
//                        }
//                    }
//                    if (resId != -1) {
//                        break;
//                    }
//                }
//            }
//            if (resId == -1) {
//                resId = R.drawable.ic_generic;
//            }
//        }
//        return resId;
//    }
//
//    public static void log(String message) {
//        Util.log("MimeType", message);
//    }
//
//}
