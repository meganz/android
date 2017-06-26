package mega.privacy.android.app.lollipop.megachat;

import java.util.ArrayList;

public class PendingMessage {
    long chatId;
    ArrayList<String> filePaths;
    ArrayList<Long> nodeHandles;

    public PendingMessage(long chatId, ArrayList<String> filePaths) {
        this.chatId = chatId;
        this.filePaths = filePaths;
        this.nodeHandles = new ArrayList<>();
    }

    public long getChatId() {
        return chatId;
    }

    public void setChatId(long chatId) {
        this.chatId = chatId;
    }

    public ArrayList<String> getFilePaths() {
        return filePaths;
    }

    public void setFilePaths(ArrayList<String> filePaths) {
        this.filePaths = filePaths;
    }

    public ArrayList<Long> getNodeHandles() {
        return nodeHandles;
    }

    public void setNodeHandles(ArrayList<Long> nodeHandles) {
        this.nodeHandles = nodeHandles;
    }

    public void addNodeHandle(long handle){
        nodeHandles.add(handle);
    }
}
