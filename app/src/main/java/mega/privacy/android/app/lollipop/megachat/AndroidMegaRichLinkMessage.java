package mega.privacy.android.app.lollipop.megachat;


import android.net.Uri;
import android.util.Patterns;

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

import nz.mega.sdk.MegaNode;

import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.LogUtil.*;
import static mega.privacy.android.app.utils.Util.*;

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
            logDebug("URL extracted: " + url);
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

        urlIn = decodeURL(urlIn);

        Matcher m = Patterns.WEB_URL.matcher(urlIn);
        while (m.find()) {
            String url = m.group();
            logDebug("URL extracted: " + url);
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
        return matchRegexs(url, FOLDER_LINK_REGEXS);
    }

    public static boolean isFileLink(String url) {
        return matchRegexs(url, FILE_LINK_REGEXS);
    }

    public static boolean isChatLink(String url) {
       return matchRegexs(url, CHAT_LINK_REGEXS);
    }

    public static boolean isContactLink(String url) {
        return matchRegexs(url, CONTACT_LINK_REGEXS);
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
}
