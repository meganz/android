package mega.privacy.android.app;

import java.io.File;

/*
 * File system document representation
 */
public class FileDocument {
    private File file;
    private MimeTypeList mimeType;

    public FileDocument(File file) {
        this.file = file;
    }

    public File getFile() {
        return file;
    }

    public boolean isHidden() {
        return getName().startsWith(".");
    }

    public boolean isFolder() {
        return file.isDirectory();
    }

    public long getSize() {
        return file.length();
    }

    public String getName() {
        return file.getName();
    }

    public MimeTypeList getMimeType() {
        if (mimeType == null) {
            mimeType = MimeTypeList.typeForName(getName());
        }
        return mimeType;
    }
}
