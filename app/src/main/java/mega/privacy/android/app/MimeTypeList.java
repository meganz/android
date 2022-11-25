package mega.privacy.android.app;

import static mega.privacy.android.app.components.textFormatter.TextFormatterUtils.INVALID_INDEX;
import static mega.privacy.android.app.utils.Constants.TYPE_TEXT_PLAIN;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.SparseArray;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/*
 * Mime type for files
 */
public class MimeTypeList {
    //20MB
    private static final long MAX_SIZE_OPENABLE_TEXT_FILE = 20971520;
    private static final List<String> TEXT_EXTENSIONS = Arrays.asList(
            //Text
            "txt", "ans", "ascii", "log", "wpd", "json", "md",
            //Web data
            "html", "xml", "shtml", "dhtml", "js", "css", "jar", "java", "class",
            //Web lang
            "php", "php3", "php4", "php5", "phtml", "inc", "asp", "pl", "cgi", "py", "sql", "accdb", "db", "dbf", "mdb", "pdb", "c", "cpp", "h", "cs", "sh", "vb", "swift");

    // Icon resource mapping for different file type extensions
    private static HashMap<String, Integer> resourcesCache;
    private static SparseArray<String[]> resources;

    static {
        resourcesCache = new HashMap<String, Integer>();
        resources = new SparseArray<String[]>();
        resources.put(R.drawable.ic_3d_list, new String[]{"3ds", "3dm", "max", "obj",});
        resources.put(R.drawable.ic_aftereffects_list, new String[]{"aec", "aep", "aepx", "aes", "aet", "aetx",});
        resources.put(R.drawable.ic_audio_list, new String[]{"aif", "aiff", "wav", "flac", "iff", "m4a", "wma", "oga", "ogg", "mp3", "3ga", "opus", "weba"});
        resources.put(R.drawable.ic_cad_list, new String[]{"dwg", "dxf",});
        resources.put(R.drawable.ic_compressed_list, new String[]{"bz2", "gz", "rar", "tar", "tbz", "tgz", "zip", "deb", "udeb", "rpm", "air", "apk", "dmg", "7z", "bz", "bzip2", "cab", "lha", "gzip", "ace", "arc", "pkg",});
        resources.put(R.drawable.ic_database_list, new String[]{"accdb", "db", "dbf", "mdb", "pdb", "sql",});
        resources.put(R.drawable.ic_dmg_list, new String[]{"dmg",});
        resources.put(R.drawable.ic_dreamweaver_list, new String[]{"dwt",});
        resources.put(R.drawable.ic_excel_list, new String[]{"xla", "xlam", "xll", "xlm", "xls", "xlsm", "xlsx", "xlt", "xltm", "xltx",});
        resources.put(R.drawable.ic_executable_list, new String[]{"apk", "app", "bat", "com", "exe", "gadget", "msi", "pif", "vb", "wsf",});
        resources.put(R.drawable.ic_web_lang_list, new String[]{"as", "asc", "ascs",});
        resources.put(R.drawable.ic_flash_list, new String[]{"fla",});
        resources.put(R.drawable.ic_font_list, new String[]{"fnt", "fon", "otf", "ttf",});
        resources.put(R.drawable.ic_gis_list, new String[]{"gpx", "kml", "kmz",});
        resources.put(R.drawable.ic_html_list, new String[]{"dhtml", "htm", "html", "shtml", "xhtml",});
        resources.put(R.drawable.ic_illustrator_list, new String[]{"ai", "aia", "aip", "ait", "art", "irs",});
        resources.put(R.drawable.ic_image_list, new String[]{"jpg", "jpeg", "tga", "tif", "tiff", "bmp", "gif", "png",});
        resources.put(R.drawable.ic_indesign_list, new String[]{"indd",});
        resources.put(R.drawable.ic_java_list, new String[]{"class", "jar", "java",});
        resources.put(R.drawable.ic_midi_list, new String[]{"mid", "midi",});
        resources.put(R.drawable.ic_pdf_list, new String[]{"pdf",});
        resources.put(R.drawable.ic_photoshop_list, new String[]{"abr", "csh", "psb", "psd",});
        resources.put(R.drawable.ic_playlist_list, new String[]{"asx", "m3u", "pls",});
        resources.put(R.drawable.ic_podcast_list, new String[]{"pcast",});
        resources.put(R.drawable.ic_powerpoint_list, new String[]{"pot", "potm", "potx", "ppam", "ppc", "pps", "ppsm", "ppsx", "ppt", "pptm", "pptx", });
        resources.put(R.drawable.ic_premiere_list, new String[]{"plb", "ppj", "prproj", "prtpset",});
        resources.put(R.drawable.ic_raw_list, new String[]{"3fr", "mef", "arw", "bay", "cr2", "dcr", "dng", "erf", "fff", "mrw", "nef", "orf", "pef", "rw2", "rwl", "srf", "iiq", "k25", "kdc", "mos", "raw", "sr2","x3f","cr3","ciff",});
        resources.put(R.drawable.ic_real_audio_list, new String[]{"ra", "ram", "rm",});
        resources.put(R.drawable.ic_source_list, new String[]{"c", "cc", "cgi", "cpp", "cxx", "dll", "h", "hpp", "pl", "py", "sh",});
        resources.put(R.drawable.ic_spreadsheet_list, new String[]{"123", "gsheet", "nb", "ods", "ots", "sxc", "xlr",});
        resources.put(R.drawable.ic_subtitles_list, new String[]{"srt",});
        resources.put(R.drawable.ic_swf_list, new String[]{"swf", "flv",});
        resources.put(R.drawable.ic_text_list, new String[]{"ans", "ascii", "log", "rtf", "txt", "wpd",});
        resources.put(R.drawable.ic_torrent_list, new String[]{"torrent",});
        resources.put(R.drawable.ic_vcard_list, new String[]{"vcard", "vcf",});
        resources.put(R.drawable.ic_vector_list, new String[]{"cdr", "eps", "ps", "svg", "svgz",});
        resources.put(R.drawable.ic_video_list, new String[]{"3g2", "3gp", "asf", "avi", "mkv", "mov", "mpeg", "mpg", "wmv", "3gpp", "h261", "h263", "h264", "jpgv", "jpm", "jpgm", "mp4", "mp4v", "mpg4", "mpe", "m1v", "m2v", "ogv", "qt", "m4u", "webm", "f4v", "fli", "m4v", "mkv", "mk3d", "vob", "movie",});
        resources.put(R.drawable.ic_video_vob_list, new String[]{"vob",});
        resources.put(R.drawable.ic_web_data_list, new String[]{"asp", "aspx", "php", "php3", "php4", "php5", "phtml", "css", "inc", "js", "xml",});
        resources.put(R.drawable.ic_word_list, new String[]{"doc", "docm", "docx", "dot", "dotx", "wps",});
        resources.put(R.drawable.ic_pages_list, new String[]{"pages",});
        resources.put(R.drawable.ic_experiencedesign_list, new String[]{"Xd",});
        resources.put(R.drawable.ic_keynote_list, new String[]{"key",});
        resources.put(R.drawable.ic_numbers_list, new String[]{"numbers",});
        resources.put(R.drawable.ic_openoffice_list, new String[]{"odp", "odt", "ods"});
        resources.put(R.drawable.ic_sketch_list, new String[]{"sketch",});
        resources.put(R.drawable.ic_url_list, new String[]{"url"});
    }

    private String type;
    private String extension;
    private int resId;

    private MimeTypeList(String type, String extension) {
        this.type = type;
        this.extension = extension;
        resId = -1;
    }

    /*
     * Get MimeType for file name
     */
    @SuppressLint("DefaultLocale")
    public static MimeTypeList typeForName(String name) {
        if (name == null) {
            name = "";
        }
        String fixedName = name.trim().toLowerCase();
        String extension = "";
        int index = fixedName.lastIndexOf(".");

        if (index != INVALID_INDEX && index + 1 < fixedName.length()) {
            extension = fixedName.substring(index + 1);
        }

        String detectedType;
        if (extension.equals("dcr")) {
            detectedType = "image/dcr";
        } else {
            detectedType = MimeTypeMap.getSingleton()
                    .getMimeTypeFromExtension(extension);
        }

        if (detectedType == null) {
            switch (extension) {
                case "mkv":
                    detectedType = "video/x-matroska";
                    break;
                case "heic":
                    detectedType = "image/heic";
                    break;
                case "url":
                    detectedType = "web/url";
                    break;
                case "webp":
                    detectedType = "image/webp";
                    break;
                case "3fr":
                    detectedType = "image/3fr";
                    break;
                case "iiq":
                    detectedType = "image/iiq";
                    break;
                case "k25":
                    detectedType = "image/k25";
                    break;
                case "kdc":
                    detectedType = "image/kdc";
                    break;
                case "mef":
                    detectedType = "image/mef";
                    break;
                case "mos":
                    detectedType = "image/mos";
                    break;
                case "mrw":
                    detectedType = "image/mrw";
                    break;
                case "raw":
                    detectedType = "image/raw";
                    break;
                case "rwl":
                    detectedType = "image/rwl";
                    break;
                case "sr2":
                    detectedType = "image/sr2";
                    break;
                case "srf":
                    detectedType = "image/srf";
                    break;
                case "x3f":
                    detectedType = "image/x3f";
                    break;
                case "cr3":
                    detectedType = "image/cr3";
                    break;
                case "ciff":
                    detectedType = "image/ciff";
                    break;
                default:
                    detectedType = "application/octet-stream";
                    break;
            }
        }

        return new MimeTypeList(detectedType, extension);
    }

    public String getType() {
        return type;
    }

    public String getExtension() {
        return extension;
    }

    public static String getMimeType(File file) {

        Uri selectedUri = Uri.fromFile(file);
        String fileExtension = MimeTypeMap.getFileExtensionFromUrl(selectedUri.toString());
        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtension);

        return mimeType;
    }

    /*
     * Check is MimeType of image type
     */
    public boolean isImage() {
        return type.startsWith("image/");
    }

    public boolean isURL() {
        return type.startsWith("web/url");
    }

    public boolean isPdf() {
        return type.startsWith("application/pdf");
    }

    public boolean isZip() {

        if (type.startsWith("application/zip") || type.startsWith("multipart/x-zip")) {
            return true;
        }
        return false;

    }


    /*
     * Once a file is downloaded, prior to create the preview, check if the file is really an image
     */
    public boolean isImage(String path) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        Bitmap bitmap = BitmapFactory.decodeFile(path, options);
        if (options.outWidth != -1 && options.outHeight != -1) {
            // This is an image file.
            return true;
        } else {
            // This is not an image file.
            return false;
        }
    }

    /*
     * Check is MimeType of video type
     */
    public boolean isVideo() {
        return type.startsWith("video/") || extension.equals("mkv");
    }

    public boolean isVideoReproducible() {
        return type.startsWith("video/") || extension.equals("mkv") || extension.equals("flv")
                || extension.equals("vob") || extension.equals("avi") || extension.equals("wmv")
                || extension.equals("mpg") || extension.equals("mts");
    }

    public boolean isVideoNotSupported() {
        return extension.equals("mpg") || extension.equals("avi") || extension.equals("wmv");
    }

    public boolean isMp4Video() {
        return type.startsWith("video/") || extension.equals("mp4");
    }

    /*
     * Check is MimeType of audio type
     */
    public boolean isAudio() {
        return type.startsWith("audio/") || extension.equals("opus") || extension.equals("weba");
    }

    /*
     * Check is MimeType of voice type
     */
    public boolean isAudioVoiceClip() {
        return extension.equals("m4a");
    }

    public boolean isAudioNotSupported() {
        return extension.equals("wma") || extension.equals("aif") || extension.equals("aiff")
                || extension.equals("iff") || extension.equals("oga") || extension.equals("3ga");
    }

    /*
     * Get Icon for current MimeType
     */
    public int getIconResourceId() {
        if (resId == -1) {
            if (resourcesCache.containsKey(extension)) {
                resId = resourcesCache.get(extension);
            } else {
                for (int i = 0, len = resources.size(); i < len; i++) {
                    int keyResId = resources.keyAt(i);
                    for (String valueExtension : resources.get(keyResId)) {
                        if (extension.equals(valueExtension)) {
                            resId = keyResId;
                            resourcesCache.put(extension, resId);
                            break;
                        }
                    }
                    if (resId != -1) {
                        break;
                    }
                }
            }
            if (resId == -1) {
                resId = R.drawable.ic_generic_list;
            }
        }
        return resId;
    }

    public boolean isGIF() {
        return extension.equals("gif") || extension.equals("webp");
    }

    /**
     * Checks if a file is openable in Text editor.
     * <p>
     * All the contemplated extension are supported by Web client, so mobile clients should try
     * to support them too.
     *
     * @return True if the file is openable, false otherwise.
     */
    public boolean isValidTextFileType() {
        //Text
        return type.startsWith(TYPE_TEXT_PLAIN)

                //File extensions considered as plain text
                || TEXT_EXTENSIONS.contains(extension)

                //Files without extension
                || type.startsWith("application/octet-stream");
    }

    /**
     * Checks if a file is openable in Text editor.
     * It's openable if its size is not bigger than MAX_SIZE_OPENABLE_TEXT_FILE.
     *
     * @return True if the file is openable, false otherwise.
     */
    public boolean isOpenableTextFile(long fileSize) {
        return isValidTextFileType() && fileSize <= MAX_SIZE_OPENABLE_TEXT_FILE;
    }
}