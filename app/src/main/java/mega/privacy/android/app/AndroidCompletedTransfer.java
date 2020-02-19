package mega.privacy.android.app;


import static mega.privacy.android.app.utils.TextUtil.*;

public class AndroidCompletedTransfer {

    private String fileName;
    private int type;
    private int state;
    private String size;
    private String nodeHandle;
    private String path;

    public AndroidCompletedTransfer(String fileName, int type, int state, String size, String nodeHandle, String path) {
        this.fileName = fileName;
        this.type = type;
        this.state = state;
        this.size = size;
        this.nodeHandle = nodeHandle;
        if (!isTextEmpty(path) && path.charAt(path.length() - 1) == '/') {
            path = path.substring(0, path.length() - 1);
        }
        this.path = path;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public String getNodeHandle() {
        return nodeHandle;
    }

    public void setNodeHandle(String nodeHandle) {
        this.nodeHandle = nodeHandle;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
