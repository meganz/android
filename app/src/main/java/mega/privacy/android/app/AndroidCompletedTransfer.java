package mega.privacy.android.app;


public class AndroidCompletedTransfer {

    public String fileName;
    public int type;
    public int state;
    public String size;
    public String nodeHandle;

    public AndroidCompletedTransfer(String fileName, int type, int state, String size, String nodeHandle) {
        this.fileName = fileName;
        this.type = type;
        this.state = state;
        this.size = size;
        this.nodeHandle = nodeHandle;
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
}
