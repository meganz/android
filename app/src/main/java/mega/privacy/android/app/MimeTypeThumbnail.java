package mega.privacy.android.app;

import android.annotation.SuppressLint;
import android.util.SparseArray;
import android.webkit.MimeTypeMap;

import java.util.HashMap;

import mega.privacy.android.icon.pack.R;

/*
 * Mime type for files
 */
public class MimeTypeThumbnail {
	
	// Icon resource mapping for different file type extensions
	private static HashMap<String, Integer> resourcesCache;
	private static SparseArray<String[]> resources;
	static {
		resourcesCache = new HashMap<String, Integer>();
		resources = new SparseArray<String[]>();
		resources.put(R.drawable.ic_3d_medium_solid, new String[] {"3ds", "3dm", "max", "obj", });
		resources.put(R.drawable.ic_aftereffects_medium_solid, new String[] {"aec", "aep", "aepx", "aes", "aet", "aetx", });
		resources.put(R.drawable.ic_audio_medium_solid, new String[] {"aif", "aiff", "wav", "flac", "iff", "m4a", "wma", "oga", "ogg", "mp3", "3ga", "opus", "weba","ra", "ram", "rm",});
		resources.put(R.drawable.ic_cad_medium_solid, new String[] {"dwg", "dxf", });
		resources.put(R.drawable.ic_compressed_medium_solid, new String[] {"bz2", "gz", "rar", "tar", "tbz", "tgz", "zip", "deb", "udeb", "rpm", "air", "apk", "dmg", "7z", "bz", "bzip2", "cab", "lha", "gzip", "ace", "arc", "pkg", });
		resources.put(R.drawable.ic_dmg_medium_solid, new String[]{"dmg", });
		resources.put(R.drawable.ic_excel_medium_solid, new String[] {"xla", "xlam", "xll", "xlm", "xls", "xlsm", "xlsx", "xlt", "xltm", "xltx",});
		resources.put(R.drawable.ic_executable_medium_solid, new String[] {"apk", "app", "bat", "com", "exe", "gadget", "msi", "pif", "vb", "wsf", });
		resources.put(R.drawable.ic_web_lang_medium_solid, new String[] {"as", "asc", "ascs", });
		resources.put(R.drawable.ic_font_medium_solid, new String[] {"fnt", "fon", "otf", "ttf", });
		resources.put(R.drawable.ic_illustrator_medium_solid, new String[] {"ai", "aia", "aip", "ait", "art", "irs", });
		resources.put(R.drawable.ic_image_medium_solid, new String[] {"jpg", "jpeg", "tga", "tif", "tiff", "bmp", "gif", "png",});
		resources.put(R.drawable.ic_indesign_medium_solid, new String[] {"indd", });
		resources.put(R.drawable.ic_pdf_medium_solid, new String[] {"pdf", });
		resources.put(R.drawable.ic_photoshop_medium_solid, new String[] {"abr", "csh", "psb", "psd", });
		resources.put(R.drawable.ic_powerpoint_medium_solid, new String[] {"pot", "potm", "potx", "ppam", "ppc", "pps", "ppsm", "ppsx", "ppt", "pptm", "pptx", });
		resources.put(R.drawable.ic_premiere_medium_solid, new String[] {"plb", "ppj", "prproj", "prtpset", });
		resources.put(R.drawable.ic_raw_medium_solid, new String[] {"3fr", "mef", "arw", "bay", "cr2", "dcr", "dng", "erf", "fff", "mrw", "nef", "orf", "pef", "rw2", "rwl", "srf", });
		resources.put(R.drawable.ic_spreadsheet_medium_solid, new String[] {"123", "gsheet", "nb", "ots", "sxc", "xlr", });
		resources.put(R.drawable.ic_text_medium_solid, new String[] {"ans", "ascii", "log",  "rtf", "txt", "wpd", });
		resources.put(R.drawable.ic_torrent_medium_solid, new String[] {"torrent", });
		resources.put(R.drawable.ic_vector_medium_solid, new String[] {"cdr", "eps", "ps", "svg", "svgz", });
		resources.put(R.drawable.ic_video_medium_solid, new String[] {"3g2", "3gp", "asf", "avi", "mkv", "mov", "mpeg", "mpg", "wmv", "3gpp", "h261", "h263", "h264", "jpgv", "jpm", "jpgm", "mp4", "mp4v", "mpg4", "mpe", "m1v", "m2v", "ogv", "qt", "m4u", "webm", "f4v", "fli", "m4v", "mkv", "mk3d", "vob", "movie","vob", });
		resources.put(R.drawable.ic_web_data_medium_solid, new String[] {"asp", "aspx", "php", "php3", "php4", "php5", "phtml", "css", "inc", "js", "xml", });
		resources.put(R.drawable.ic_word_medium_solid, new String[] {"doc", "docm", "docx", "dot", "dotx", "wps",});
		resources.put(R.drawable.ic_pages_medium_solid, new String[] {"pages", });
		resources.put(R.drawable.ic_experiencedesign_medium_solid, new String[] {"Xd", });
		resources.put(R.drawable.ic_keynote_medium_solid, new String[] {"key", });
		resources.put(R.drawable.ic_numbers_medium_solid, new String[] {"numbers", });
		resources.put(R.drawable.ic_openoffice_medium_solid, new String[] {"odp", "odt", "ods"});
		resources.put(R.drawable.ic_url_medium_solid, new String[]{"url"});
	}

	private String type;
	private String extension;
	private int resId;

	private MimeTypeThumbnail(String type, String extension) {
		this.type = type;
		this.extension = extension;
		resId = -1;
	}

	/*
	 * Get MimeType for file name
	 */
	@SuppressLint("DefaultLocale")
	public static MimeTypeThumbnail typeForName(String name) {
		if (name == null) {
			name = "";
		}
		String fixedName = name.trim().toLowerCase();
		String extension = null;
		int index = fixedName.lastIndexOf(".");
		if((index != -1) && ((index+1)<fixedName.length())) {
			extension = fixedName.substring(index + 1);
		} else {
			extension = fixedName;
		}
		String detectedType = MimeTypeMap.getSingleton()
				.getMimeTypeFromExtension(extension);
		if (detectedType == null) {
			if(extension.equals("mkv"))
				detectedType = "video/x-matroska";
			else
				detectedType = "application/octet-stream";
		}
		if (extension == null) {
			extension = "";
		}
		return new MimeTypeThumbnail(detectedType, extension);
	}

	/*
	 * Get Icon for current MimeType
	 * @deprecated use @link{#GetFileIcon.getFileIcon}
	 */
	@Deprecated()
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
				resId = R.drawable.ic_generic_medium_solid;
			}
		}
		return resId;
	}
}