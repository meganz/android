package mega.privacy.android.app.lollipop.megachat;


import android.net.Uri;
import android.util.Patterns;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaNode;

public class AndroidMegaRichLinkMessage {

    private String url;
    private String server;
    private String folderContent;
    private MegaNode node = null;
    private boolean isFile;

    private boolean isChat;
    private String title;
    private long numParticipants;

    public AndroidMegaRichLinkMessage (String url, MegaNode node){
        this.node = node;
        this.url = url;

        Uri uri = Uri.parse(url);
        this.server = uri.getAuthority();
    }

    public AndroidMegaRichLinkMessage (String url, String title, long participants){

        this.url = url;
        this.title = title;
        this.numParticipants = participants;

        Uri uri = Uri.parse(url);
        this.server = uri.getAuthority();

        this.isChat = true;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getServer() {
        return this.server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public MegaNode getNode() {
        return node;
    }

    public void setNode(MegaNode node) {
        this.node = node;
    }

    public String getFolderContent() {
        return folderContent;
    }

    public void setFolderContent(String folderContent) {
        this.folderContent = folderContent;
    }

    public boolean isFile() {
        return isFile;
    }

    public void setFile(boolean file) {
        isFile = file;
    }

    public static String[] extractMegaLinks(String text) {
        List<String> links = new ArrayList<String>();
        Matcher m = Patterns.WEB_URL.matcher(text);
        while (m.find()) {
            String url = m.group();
            log("URL extracted: " + url);
            if (isFileLink(url)) {
                return links.toArray(new String[links.size()]);
            }
            if (isFolderLink(url)) {
                links.add(url);
                return links.toArray(new String[links.size()]);
            }
        }

        return links.toArray(new String[links.size()]);
    }

    public static String extractMegaLink(String urlIn) {

        try {
            urlIn = URLDecoder.decode(urlIn, "UTF-8");
        }
        catch (Exception e) {
            log("Error decoding URL: " + urlIn);
            log(e.toString());
        }

        urlIn.replace(' ', '+');
        if(urlIn.startsWith("mega://")){
            urlIn = urlIn.replace("mega://", "https://mega.co.nz/");
        }
        else  if(urlIn.startsWith("mega.")){
            urlIn = urlIn.replace("mega.", "https://mega.");
        }

        if (urlIn.startsWith("https://www.mega.co.nz")){
            urlIn = urlIn.replace("https://www.mega.co.nz", "https://mega.co.nz");
        }

        if (urlIn.startsWith("https://www.mega.nz")){
            urlIn = urlIn.replace("https://www.mega.nz", "https://mega.nz");
        }

        Matcher m = Patterns.WEB_URL.matcher(urlIn);
        while (m.find()) {
            String url = m.group();
            log("URL extracted: " + url);
            if (isFileLink(url)) {
                return url;
            }
            if (isFolderLink(url)) {
                return url;
            }
            if (isChatLink(url)) {
                return url;
            }
        }

        return null;
    }

    public static boolean isFolderLink(String url) {
        if (url != null && (url.matches("^https://mega\\.co\\.nz/#F!.+$") || url.matches("^https://mega\\.nz/#F!.+$"))) {
            log("folder link found");
            return true;
        }
        return false;
    }

    public static boolean isFileLink(String url) {
        if (url != null && (url.matches("^https://mega\\.co\\.nz/#!.+$") || url.matches("^https://mega\\.nz/#!.+$"))) {
            log("IS file link found");
            return true;
        }
        return false;
    }

    public static boolean isChatLink(String url) {
        if (url != null && (url.matches("^https://mega\\.co\\.nz/chat/.+$") || url.matches("^https://mega\\.nz/chat/.+$"))) {
            log("IS chat link found");
            return true;
        }
        return false;
    }

    public static boolean isContactLink(String url) {
        if (url != null && (url.matches("^https://mega\\.co\\.nz/C!.+$") || url.matches("^https://mega\\.nz/C!.+$"))) {
            log("IS contact link found");
            return true;
        }
        return false;
    }

    public boolean isChat() {
        return isChat;
    }

    public void setChat(boolean chat) {
        isChat = chat;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public long getNumParticipants() {
        return numParticipants;
    }

    public void setNumParticipants(long numParticipants) {
        this.numParticipants = numParticipants;
    }

    private static void log(String log) {
        Util.log("AndroidMegaRichLinkMessage", log);
    }

}
