package mega.privacy.android.app.utils;

import android.content.Context;
import android.graphics.Typeface;
import android.text.style.StyleSpan;

import com.vdurmont.emoji.EmojiManager;
import com.vdurmont.emoji.EmojiParser;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import mega.privacy.android.app.components.CustomTypefaceSpan;
import mega.privacy.android.app.components.SimpleSpanBuilder;
import nz.mega.sdk.MegaApiAndroid;

public class RTFFormatter {

    String messageContent;
    SimpleSpanBuilder ssb = null;
    Context context;
    boolean formatted = false;
    boolean recursive = false;
    Typeface font;
    Pattern pMultiQuote = Pattern.compile("(?<=[\\W\\d]|^)(```)([^```]+?.*?)(```)(?=[\\W\\d]|$)");
    Pattern pQuote = Pattern.compile("(?<=[\\W\\d]|^)(`)([^`]+?.*?[^\\n.]*?[^\\n]*?.*?[^`]+?)(`)(?=[\\W\\d]|$)");
    Pattern pItalic = Pattern.compile("(?<=[\\W\\d]|^)(\\_)([^_]+?[^_\\n]*?.*?)(\\_)(?=[\\W\\d]|$)");
    Pattern pBold = Pattern.compile("(?<=[\\W\\d]|^)(\\*)([^\\s][^*\\n]*?.*?|[^*\\n]*?[^\\s].*?)(\\*)(?=[\\W\\d]|$)");

    public boolean isFormatted() {
        return formatted;
    }

    public RTFFormatter(String messageContent, Context context) {
        this.messageContent = messageContent;
        this.context = context;
    }

    public RTFFormatter(String messageContent, Context context, SimpleSpanBuilder ssb) {
        this.messageContent = messageContent;
        this.context = context;
        this.ssb = ssb;
    }

    public SimpleSpanBuilder setRTFFormat(){

        LogUtil.logDebug("setRTFFormat");
        formatted = false;

        if(!messageContent.isEmpty()) {

            if (messageContent.lastIndexOf(" ") == (messageContent.length()-1)){
                messageContent = messageContent.substring(0,messageContent.length()-1);
            }

            font = Typeface.createFromAsset(context.getAssets(), "font/RobotoMono-Regular.ttf");

            queryIfMultiQuoteFormat();

            Matcher mMultiQuote = pMultiQuote.matcher(messageContent);

            if (mMultiQuote != null && mMultiQuote.find()) {
                LogUtil.logDebug("Multiquote found!");

                int startBold = -1;
                int startMultiquote = -1;
                boolean bold = false;
                boolean italic = false;

                Matcher mBold = pBold.matcher(messageContent);

                if(mBold!=null){
                    if(mBold.find()) {
                        bold = true;
                        startBold = messageContent.indexOf(("*"));
                        startMultiquote = messageContent.indexOf(("```"));
                        if (startMultiquote < startBold) {
                            applyMultiQuoteFormat();
                            formatted = true;
                            return ssb;
                        }
                    }
                }

                int startItalic = -1;

                Matcher mItalic = pItalic.matcher(messageContent);

                if(mItalic!=null) {
                    if(mItalic.find()) {
                        italic = true;
                        startItalic = messageContent.indexOf(("_"));
                        startMultiquote = messageContent.indexOf(("```"));
                        if (startMultiquote < startItalic) {
                            applyMultiQuoteFormat();
                            formatted = true;
                            return ssb;
                        }
                    }
                 }

                if(!bold && !italic){
                    applyMultiQuoteFormat();
                    formatted = true;
                    return ssb;
                }
            }

            Matcher mQuote = pQuote.matcher(messageContent);

            if (mQuote != null && mQuote.find()) {

                LogUtil.logDebug("Quote find!");

                int startBold = -1;
                int startQuote = -1;
                boolean bold = false;
                boolean italic = false;

                Matcher mBold = pBold.matcher(messageContent);

                if(mBold!=null){
                    if(mBold.find()) {
                        bold = true;
                        startBold = messageContent.indexOf(("*"));
                        startQuote = messageContent.indexOf(("`"));
                        if (startQuote < startBold) {
                            applyQuoteFormat();
                            formatted = true;
                            return ssb;
                        }
                    }
                }

                int startItalic = -1;

                Matcher mItalic = pItalic.matcher(messageContent);

                if(mItalic!=null) {
                    if(mItalic.find()) {
                        italic = true;
                        startItalic = messageContent.indexOf(("_"));
                        startQuote = messageContent.indexOf(("`"));
                        if (startQuote < startItalic) {
                            if (bold){
                                applyBoldFormat();
                            }
                            else {
                                applyQuoteFormat();
                            }
                            formatted = true;
                            return ssb;
                        }
                    }
                }

                if(!bold && !italic){
                    applyQuoteFormat();
                    formatted = true;
                    return ssb;
                }

            }//

            Matcher mItalic = pItalic.matcher(messageContent);

            if(mItalic!=null) {
                if(mItalic.find()){
                    LogUtil.logDebug("Find Italic!");
                    Matcher mBold = pBold.matcher(messageContent);

                    int startBold = -1;
                    int startItalic = -1;
                    if(mBold!=null){
                        if(mBold.find()){
                            startBold = messageContent.indexOf(("*"));
                            startItalic = messageContent.indexOf(("_"));
                            if (startItalic < startBold) {
                                applyItalicFormat();
                                formatted = true;
                                return ssb;
                            }
                        }
                        else{
                            applyItalicFormat();
                            formatted = true;
                            return ssb;
                        }
                    }
                    else{
                        applyItalicFormat();
                        formatted = true;
                        return ssb;
                    }
                }
            }

            if(!messageContent.isEmpty()){
//                boolean bold = Pattern.matches("(?<=[\\W\\d]|^)(\\*)([^\\s][^*\\n]*?|[^*\\n]*?[^\\s])(\\*)(?=[\\W\\d]|$)", noEmojisContent);
//                                boolean  italic = Pattern.matches(".*_.*_.*", messageContent);

                Matcher mBold = pBold.matcher(messageContent);

                if(mBold!=null && mBold.find()){
                    LogUtil.logDebug("Bold Found");
                    applyBoldFormat();
                    formatted = true;
                    return ssb;
                }
            }

            Pattern pQuote2 = Pattern.compile("(?<=[\\W\\d]|^)(`)([^`]+?.*?[^\\n.]*?[^\\n]*?.*?[^`]+?)(`)(?=[\\W\\d]|$)");

            Matcher mQuote2 = pQuote2.matcher(messageContent);

            if (mQuote2 != null) {
                if (mQuote2.find()) {
                    applyQuoteFormat();
                    formatted = true;
                    return ssb;
                }

            }
        }

        formatted = false;
        return ssb;
    }

    public void queryIfMultiQuoteFormat(){
        LogUtil.logDebug("queryIfMultiQuoteFormat");

        if (messageContent.length() > 6){

            String a = messageContent.substring(0,3);
            String message = messageContent;
            int start;
            boolean bold = false;
            boolean italic = false;
            int startBold;
            int startMultiquote;
            String substring;

            if (message.contains("```")){
                LogUtil.logDebug("Check if there is emoji at the beginning of the string");
                start = messageContent.indexOf("```");
                String emoji = messageContent.substring(0, start);
                if(EmojiManager.isEmoji(emoji)){
                    LogUtil.logDebug("The first element is emoji");
                    substring = messageContent.substring(0, start);
                    if(ssb==null){
                        ssb = new SimpleSpanBuilder();
                    }
                    ssb.append(substring+'\n');

                    StringBuilder sb = new StringBuilder(messageContent);
                    sb.delete(0, start);
                    messageContent = sb.toString();
                    queryIfMultiQuoteFormat();
                }
                else {
                    if (a.equals("```")){
                        StringBuilder sb = new StringBuilder(message);
                        sb.delete(0,3);
                        message = sb.toString();
                        if (message.length() > 3){
                            if (message.contains("```")){
                                Matcher mBold = pBold.matcher(messageContent);

                                if(mBold!=null){
                                    if(mBold.find()) {
                                        bold = true;
                                        startBold = messageContent.indexOf(("*"));
                                        startMultiquote = messageContent.indexOf(("```"));
                                        if (startMultiquote < startBold) {
                                            applyMultiQuoteFormat();
                                            formatted = true;
                                        }
                                    }
                                }

                                int startItalic = -1;

                                Matcher mItalic = pItalic.matcher(messageContent);

                                if(mItalic!=null) {
                                    if(mItalic.find()) {
                                        italic = true;
                                        startItalic = messageContent.indexOf(("_"));
                                        startMultiquote = messageContent.indexOf(("```"));
                                        if (startMultiquote < startItalic) {
                                            applyMultiQuoteFormat();
                                            formatted = true;
                                        }
                                    }
                                }

                                if(!bold && !italic){
                                    applyMultiQuoteFormat();
                                    formatted = true;
                                }
                            }
                        }
                    }
                    else {
                        start = message.indexOf(" ```");
                        if (start != -1){
                            StringBuilder sb = new StringBuilder(message);
                            sb.delete(start, start+3);
                            message = sb.toString();
                            if (message.length() > 3){
                                if (message.contains("```")){
                                    Matcher mBold = pBold.matcher(messageContent);

                                    if(mBold!=null){
                                        if(mBold.find()) {
                                            bold = true;
                                            startBold = messageContent.indexOf(("*"));
                                            startMultiquote = messageContent.indexOf(("```"));
                                            if (startMultiquote < startBold) {
                                                applyMultiQuoteFormat();
                                                formatted = true;
                                            }
                                        }
                                    }

                                    int startItalic = -1;

                                    Matcher mItalic = pItalic.matcher(messageContent);

                                    if(mItalic!=null) {
                                        if(mItalic.find()) {
                                            italic = true;
                                            startItalic = messageContent.indexOf(("_"));
                                            startMultiquote = messageContent.indexOf(("```"));
                                            if (startMultiquote < startItalic) {
                                                applyMultiQuoteFormat();
                                                formatted = true;
                                            }
                                        }
                                    }

                                    if(!bold && !italic){
                                        applyMultiQuoteFormat();
                                        formatted = true;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public void applyMultiQuoteFormat(){
        LogUtil.logDebug("applyMultiQuoteFormat");

        String a = messageContent.substring(0,3);
        int start;
        int end;

        if(ssb==null){
            ssb = new SimpleSpanBuilder();
        }

        String substring = null;

        if(a.equals("```")){

            StringBuilder sb = new StringBuilder(messageContent);
            sb.delete(0,3);
            messageContent = sb.toString();
        }
        else{
            start = messageContent.indexOf("```");

            if(start==-1){
                LogUtil.logDebug("Check if there is emoji at the beginning of the string");
                start = messageContent.indexOf("```");
                String emoji = messageContent.substring(0, start);
                if(EmojiManager.isEmoji(emoji)) {
                    LogUtil.logDebug("The first element is emoji");
                    substring = messageContent.substring(0, start);
                    ssb.append(substring);

                    StringBuilder sb = new StringBuilder(messageContent);
                    sb.delete(0, start+3);
                    sb.insert(0, '\n');
                    messageContent = sb.toString();
                }
                else {
                    StringBuilder sb = new StringBuilder(messageContent);
                    String s = messageContent.substring(0, start);
                    sb.delete(0, start+3);
                    sb.insert(0, s+'\n');
                    messageContent = sb.toString();
                }
            }
            else{
                boolean insertLine = false;
                if (messageContent.charAt(start-1) != '\n'){
                    insertLine = true;
                }
                substring = messageContent.substring(0, start);
                ssb.append(substring);

                StringBuilder sb = new StringBuilder(messageContent);
                sb.delete(0, start+3);
                if (insertLine) {
                    sb.insert(0, '\n');
                }
                messageContent = sb.toString();
            }
        }

        end = messageContent.indexOf("``` ");
        if(end==-1){
            end = messageContent.lastIndexOf("```.");
            if (end==-1) {
                end = messageContent.lastIndexOf("```");
                LogUtil.logDebug("FINISH End position: " + end);

                StringBuilder sb = new StringBuilder(messageContent);
                sb.delete(end, end+3);
                messageContent = sb.toString();

                substring = messageContent.substring(0, end);

//            StringBuilder sbBMultiQuote = new StringBuilder(substring);
//            sbBMultiQuote.append('\n');
//            substring = sbBMultiQuote.toString();

                sb = new StringBuilder(messageContent);
                sb.delete(0, end);
                messageContent = sb.toString();

                if(!messageContent.isEmpty()){
                    if(!messageContent.trim().isEmpty()){
                        StringBuilder sbBMultiQuote = new StringBuilder(substring+'\n');
                        substring = sbBMultiQuote.toString();
                        ssb.append(substring, new CustomTypefaceSpan("", font));
                    }
                }
                else{
                    ssb.append(substring, new CustomTypefaceSpan("", font));
                }

                String emoji = messageContent;
                if(EmojiManager.isEmoji(emoji)){
                    LogUtil.logDebug("The last element is emoji");
                    ssb.append(emoji);

                    messageContent = "";
                }
            }
            else {
                LogUtil.logDebug("End position: " + end);
                StringBuilder sb = new StringBuilder(messageContent);
                sb.delete(end, end+3);
                messageContent = sb.toString();

                substring = messageContent.substring(0, end);

                sb = new StringBuilder(messageContent);
                sb.delete(0, end);
                messageContent = sb.toString();

                if(!messageContent.isEmpty()){
                    if(!messageContent.trim().isEmpty()){
                        StringBuilder sbBMultiQuote = new StringBuilder(substring+'\n');
                        substring = sbBMultiQuote.toString();
                        ssb.append(substring, new CustomTypefaceSpan("", font));
                    }
                }
                else{
                    ssb.append(substring, new CustomTypefaceSpan("", font));
                }

                String emoji = messageContent;
                if(EmojiManager.isEmoji(emoji)){
                    LogUtil.logDebug("The last element is emoji");
                    ssb.append(emoji);

                    messageContent = "";
                }
            }
        }
        else{
            LogUtil.logDebug("End position: " + end);
            StringBuilder sb = new StringBuilder(messageContent);
            sb.delete(end, end+3);
            messageContent = sb.toString();

            substring = messageContent.substring(0, end);

            sb = new StringBuilder(messageContent);
            sb.delete(0, end+1);
            messageContent = sb.toString();

            if(!messageContent.isEmpty()){
                if(!messageContent.trim().isEmpty()){
                    StringBuilder sbBMultiQuote = new StringBuilder(substring+'\n');
                    substring = sbBMultiQuote.toString();
                    ssb.append(substring, new CustomTypefaceSpan("", font));
                }
            }
            else{
                ssb.append(substring, new CustomTypefaceSpan("", font));
            }

            String emoji = messageContent;
            if(EmojiManager.isEmoji(emoji)){
                LogUtil.logDebug("The last element is emoji");
                ssb.append(emoji);

                messageContent = "";
            }
        }

        setRTFFormat();
        while(formatted){
            setRTFFormat();
        }
        if(!messageContent.isEmpty()){
            LogUtil.logDebug("Append more...");
//            StringBuilder sbBMultiQuote = new StringBuilder(messageContent);
//            sbBMultiQuote.insert(0, '\n');
//            messageContent = sbBMultiQuote.toString();
            ssb.append(messageContent);
            messageContent ="";
        }
    }

    public void applyQuoteFormat(){
        LogUtil.logDebug("applyQuoteFormat");

        char a = messageContent.charAt(0);
        int start;
        int end;

        if(ssb==null){
            ssb = new SimpleSpanBuilder();
        }

        String substring = null;
        if(a =='`'){
            StringBuilder sb = new StringBuilder(messageContent);
            sb.deleteCharAt(0);
            messageContent = sb.toString();
        }
        else{
            start = messageContent.indexOf(" `");
            if(start==-1){
                start = messageContent.indexOf(".`");
                if (start == -1) {
                    start = messageContent.indexOf("\n`");
                    if (start == -1){
                        LogUtil.logDebug("Check if there is emoji at the beginning of the string");
                        start = messageContent.indexOf("`");
                        String emoji = messageContent.substring(0, start);
                        if(EmojiManager.isEmoji(emoji)){
                            LogUtil.logDebug("The first element is emoji");
                            substring = messageContent.substring(0, start);
                            ssb.append(substring);

                            StringBuilder sb = new StringBuilder(messageContent);
                            sb.delete(0, start+1);
                            messageContent = sb.toString();
                        }
                        else {
                            List<String> emojiList = EmojiParser.extractEmojis(emoji);
                            if (emojiList != null) {
                                if (!emojiList.isEmpty()) {
                                    substring = messageContent.substring(0, start);
                                    int lastSpace = substring.lastIndexOf(" ");
                                    if(lastSpace!=-1){
                                        String checkEmoji = substring.substring(lastSpace+1, start);
                                        if(EmojiManager.isEmoji(checkEmoji)){
                                            ssb.append(substring);

                                            StringBuilder sb = new StringBuilder(messageContent);
                                            sb.delete(0, start+1);
                                            messageContent = sb.toString();
                                        }
                                    }
                                }
                            }
                        }
                    }
                    else {
                        int startPrevious = messageContent.indexOf("`");
                        if(startPrevious<start){
                            String emoji = messageContent.substring(0, startPrevious);
                            if(EmojiManager.isEmoji(emoji)){
                                LogUtil.logDebug("The first element is emoji");
                                substring = messageContent.substring(0, startPrevious);
                                ssb.append(substring);

                                StringBuilder sb = new StringBuilder(messageContent);
                                sb.delete(0, startPrevious+1);
                                messageContent = sb.toString();
                            }
                        }
                        else{
                            start++;
                            substring = messageContent.substring(0, start);
                            ssb.append(substring);

                            StringBuilder sb = new StringBuilder(messageContent);
                            sb.delete(0, start+1);
                            messageContent = sb.toString();
                        }
                    }
                }
                else {
                    int startPrevious = messageContent.indexOf("`");
                    if(startPrevious<start){
                        String emoji = messageContent.substring(0, startPrevious);
                        if(EmojiManager.isEmoji(emoji)){
                            LogUtil.logDebug("The first element is emoji");
                            substring = messageContent.substring(0, startPrevious);
                            ssb.append(substring);

                            StringBuilder sb = new StringBuilder(messageContent);
                            sb.delete(0, startPrevious+1);
                            messageContent = sb.toString();
                        }
                    }
                    else{
                        start++;
                        substring = messageContent.substring(0, start);
                        ssb.append(substring);

                        StringBuilder sb = new StringBuilder(messageContent);
                        sb.delete(0, start+1);
                        messageContent = sb.toString();
                    }
                }
            }
            else{
                int startPrevious = messageContent.indexOf("`");
                if(startPrevious<start){
                    String emoji = messageContent.substring(0, startPrevious);
                    if(EmojiManager.isEmoji(emoji)){
                        LogUtil.logDebug("The first element is emoji");
                        substring = messageContent.substring(0, startPrevious);
                        ssb.append(substring);

                        StringBuilder sb = new StringBuilder(messageContent);
                        sb.delete(0, startPrevious+1);
                        messageContent = sb.toString();
                    }
                }
                else{
                    start++;
                    substring = messageContent.substring(0, start);
                    ssb.append(substring);

                    StringBuilder sb = new StringBuilder(messageContent);
                    sb.delete(0, start+1);
                    messageContent = sb.toString();
                }
            }
        }

        end = messageContent.indexOf("` ");
        if(end==-1){
            end = messageContent.indexOf("`\n");
            if (end == -1){
                end = messageContent.indexOf("`");
                if (end == -1){
                    end = messageContent.lastIndexOf("`");
                    LogUtil.logDebug("FINISH End position: " + end);

                    StringBuilder sb = new StringBuilder(messageContent);
                    sb.deleteCharAt(end);
                    messageContent = sb.toString();

                    substring = messageContent.substring(0, end);

                    ssb.append(substring, new CustomTypefaceSpan("", font));

                    sb = new StringBuilder(messageContent);
                    sb.delete(0, end);
//            sb.insert(0, '\n');
                    messageContent = sb.toString();
                }
                else {
                    LogUtil.logDebug("End position: " + end);
                    StringBuilder sb = new StringBuilder(messageContent);
                    sb.deleteCharAt(end);
                    messageContent = sb.toString();

                    substring = messageContent.substring(0, end);

                    ssb.append(substring, new CustomTypefaceSpan("", font));

                    sb = new StringBuilder(messageContent);
                    sb.delete(0, end);
                    messageContent = sb.toString();

                    String emoji = messageContent;
                    if(EmojiManager.isEmoji(emoji)){
                        LogUtil.logDebug("The last element is emoji");
                        ssb.append(emoji);

                        messageContent = "";
                    }

                    Matcher mMultiQuote = pMultiQuote.matcher(messageContent);
                    if (mMultiQuote != null && mMultiQuote.find()){
                        setRTFFormat();
                    }

                    start = messageContent.indexOf(" `");
                    while(start!=-1){

                        start = start +1;

                        sb = new StringBuilder(messageContent);
                        sb.deleteCharAt(start);
                        messageContent = sb.toString();

                        LogUtil.logDebug("(B) Start position: " + start);
                        substring = messageContent.substring(0, start);
                        ssb.append(substring);

                        sb = new StringBuilder(messageContent);
                        sb.delete(0, start);
                        messageContent = sb.toString();

                        end = messageContent.indexOf("` ");
                        if(end==-1){
                            end = messageContent.lastIndexOf("`");

                            sb = new StringBuilder(messageContent);
                            sb.deleteCharAt(end);
                            messageContent = sb.toString();

                            LogUtil.logDebug("(B)FINISH End position: " + end);
                            substring = messageContent.substring(0, end);
                            ssb.append(substring, new CustomTypefaceSpan("", font));

                            sb = new StringBuilder(messageContent);
                            sb.delete(0, end);
                            messageContent = sb.toString();

                            break;
                        }
                        else{
                            LogUtil.logDebug("End position: " + end);
                            sb = new StringBuilder(messageContent);
                            sb.deleteCharAt(end);
                            messageContent = sb.toString();

                            substring = messageContent.substring(0, end);
                            ssb.append(substring, new CustomTypefaceSpan("", font));

                            sb = new StringBuilder(messageContent);
                            sb.delete(0, end);
                            messageContent = sb.toString();

                            start = messageContent.indexOf(" `");
                        }
                    }
                }
            }
            else {
                LogUtil.logDebug("End position: " + end);
                StringBuilder sb = new StringBuilder(messageContent);
                sb.deleteCharAt(end);
                messageContent = sb.toString();

                substring = messageContent.substring(0, end);

                ssb.append(substring, new CustomTypefaceSpan("", font));

                sb = new StringBuilder(messageContent);
                sb.delete(0, end);
                messageContent = sb.toString();

                Matcher mMultiQuote = pMultiQuote.matcher(messageContent);
                if (mMultiQuote != null && mMultiQuote.find()){
                    setRTFFormat();
                }

                start = messageContent.indexOf(" `");
                while(start!=-1){

                    start = start +1;

                    sb = new StringBuilder(messageContent);
                    sb.deleteCharAt(start);
                    messageContent = sb.toString();

                    LogUtil.logDebug("(B) Start position: " + start);
                    substring = messageContent.substring(0, start);
                    ssb.append(substring);

                    sb = new StringBuilder(messageContent);
                    sb.delete(0, start);
                    messageContent = sb.toString();

                    end = messageContent.indexOf("` ");
                    if(end==-1){
                        end = messageContent.lastIndexOf("`");

                        sb = new StringBuilder(messageContent);
                        sb.deleteCharAt(end);
                        messageContent = sb.toString();

                        LogUtil.logDebug("(B)FINISH End position: " + end);
                        substring = messageContent.substring(0, end);
                        ssb.append(substring, new CustomTypefaceSpan("", font));

                        sb = new StringBuilder(messageContent);
                        sb.delete(0, end);
                        messageContent = sb.toString();

                        break;
                    }
                    else{
                        LogUtil.logDebug("End position: " + end);
                        sb = new StringBuilder(messageContent);
                        sb.deleteCharAt(end);
                        messageContent = sb.toString();

                        substring = messageContent.substring(0, end);
                        ssb.append(substring, new CustomTypefaceSpan("", font));

                        sb = new StringBuilder(messageContent);
                        sb.delete(0, end);
                        messageContent = sb.toString();

                        start = messageContent.indexOf(" `");
                    }
                }
            }
        }
        else{
            LogUtil.logDebug("End position: " + end);
            StringBuilder sb = new StringBuilder(messageContent);
            sb.deleteCharAt(end);
            messageContent = sb.toString();

            substring = messageContent.substring(0, end);

            ssb.append(substring, new CustomTypefaceSpan("", font));

            sb = new StringBuilder(messageContent);
            sb.delete(0, end);
            messageContent = sb.toString();

            Matcher mMultiQuote = pMultiQuote.matcher(messageContent);
            if (mMultiQuote != null && mMultiQuote.find()){
                setRTFFormat();
            }

            start = messageContent.indexOf(" `");
            while(start!=-1){

                start = start +1;

                sb = new StringBuilder(messageContent);
                sb.deleteCharAt(start);
                messageContent = sb.toString();

                LogUtil.logDebug("(B) Start position: "+start);
                substring = messageContent.substring(0, start);
                ssb.append(substring);

                sb = new StringBuilder(messageContent);
                sb.delete(0, start);
                messageContent = sb.toString();

                end = messageContent.indexOf("` ");
                if(end==-1){
                    end = messageContent.lastIndexOf("`");

                    sb = new StringBuilder(messageContent);
                    sb.deleteCharAt(end);
                    messageContent = sb.toString();

                    LogUtil.logDebug("(B)FINISH End position: " + end);
                    substring = messageContent.substring(0, end);
                    ssb.append(substring, new CustomTypefaceSpan("", font));

                    sb = new StringBuilder(messageContent);
                    sb.delete(0, end);
                    messageContent = sb.toString();

                    break;
                }
                else{
                    LogUtil.logDebug("End position: " + end);
                    sb = new StringBuilder(messageContent);
                    sb.deleteCharAt(end);
                    messageContent = sb.toString();

                    substring = messageContent.substring(0, end);
                    ssb.append(substring, new CustomTypefaceSpan("", font));

                    sb = new StringBuilder(messageContent);
                    sb.delete(0, end);
                    messageContent = sb.toString();

                    start = messageContent.indexOf(" `");
                }
            }
        }

        setRTFFormat();
        while(formatted){
            setRTFFormat();
        }
        if(!messageContent.isEmpty()){
            LogUtil.logDebug("more to append...");
            ssb.append(messageContent);
            messageContent ="";
        }
    }

    public void applyItalicFormat(){
        LogUtil.logDebug("applyItalicFormat");
        char a = messageContent.charAt(0);
        int start;
        int end;
        String messageContentInitial = messageContent;

        if(ssb==null){
            ssb = new SimpleSpanBuilder();
        }

        String substring = null;

        if(a =='_'){

            //Check if the next one is *
            if(messageContent.charAt(1)=='*'){
                StringBuilder sb = new StringBuilder(messageContent);
                sb.delete(0,2);
                messageContent = sb.toString();

                end = messageContent.indexOf("*_");
                if(end!=-1){
                    substring = messageContent.substring(0, end);
                }
                else{
                    end = messageContent.indexOf("*");

                    substring = messageContent.substring(0, end);
                }

                Matcher mMultiQuote = pMultiQuote.matcher(substring);

                if (mMultiQuote != null && mMultiQuote.find()) {
                    applyTwoFormatsAndMultiQuoteFormat(substring);
                }
                else{
                    Matcher mQuote = pQuote.matcher(substring);

                    if (mQuote != null && mQuote.find()) {
                        LogUtil.logDebug("Quote");
                        applyTwoFormatsAndQuoteFormat(substring);
                    }
                    else{
                        ssb.append(substring, new StyleSpan(Typeface.BOLD), new StyleSpan(Typeface.ITALIC));
                    }
                }

                sb = new StringBuilder(messageContent);
                sb.delete(0, end+1);
                messageContent = sb.toString();

            }
            else{
                StringBuilder sb = new StringBuilder(messageContent);
                sb.deleteCharAt(0);
                messageContent = sb.toString();
            }
        }
        else{
            start = messageContent.indexOf(" _");
            if(start==-1){
                start = messageContent.indexOf("._");
                if (start == -1) {
                    start = messageContent.indexOf("\n_");
                    if (start == -1){
                        LogUtil.logDebug("Check if there is emoji at the beginning of the string");
                        start = messageContent.indexOf("_");
                        String emoji = messageContent.substring(0, start);
                        if(EmojiManager.isEmoji(emoji)){
                            LogUtil.logDebug("The first element is emoji");
                            substring = messageContent.substring(0, start);
                            ssb.append(substring);

                            StringBuilder sb = new StringBuilder(messageContent);
                            sb.delete(0, start+1);
                            messageContent = sb.toString();
                        }
                        else {
                            List<String> emojiList = EmojiParser.extractEmojis(emoji);
                            if (emojiList != null) {
                                if (!emojiList.isEmpty()) {
                                    substring = messageContent.substring(0, start);
                                    int lastSpace = substring.lastIndexOf(" ");
                                    if(lastSpace!=-1){
                                        String checkEmoji = substring.substring(lastSpace+1, start);
                                        if(EmojiManager.isEmoji(checkEmoji)){
                                            ssb.append(substring);

                                            StringBuilder sb = new StringBuilder(messageContent);
                                            sb.delete(0, start+1);
                                            messageContent = sb.toString();
                                        }
                                    }
                                }
                            }

                        }
                    }
                    else {
                        int startPrevious = messageContent.indexOf("_");
                        if(startPrevious<start)
                        {
                            String emoji = messageContent.substring(0, startPrevious);
                            if(EmojiManager.isEmoji(emoji)){
                                LogUtil.logDebug("The first element is emoji");
                                substring = messageContent.substring(0, startPrevious);
                                ssb.append(substring);

                                StringBuilder sb = new StringBuilder(messageContent);
                                sb.delete(0, startPrevious+1);
                                messageContent = sb.toString();
                            }
                        }
                        else{
                            start++;
                            substring = messageContent.substring(0, start);
                            ssb.append(substring);

                            StringBuilder sb = new StringBuilder(messageContent);
                            sb.delete(0, start+1);
                            messageContent = sb.toString();
                        }
                    }
                }
                else {
                    int startPrevious = messageContent.indexOf("_");
                    if(startPrevious<start){
                        String emoji = messageContent.substring(0, startPrevious);
                        if(EmojiManager.isEmoji(emoji)){
                            LogUtil.logDebug("The first element is emoji");
                            substring = messageContent.substring(0, startPrevious);
                            ssb.append(substring);

                            StringBuilder sb = new StringBuilder(messageContent);
                            sb.delete(0, startPrevious+1);
                            messageContent = sb.toString();
                        }
                    }
                    else{
                        start++;
                        substring = messageContent.substring(0, start);
                        ssb.append(substring);

                        StringBuilder sb = new StringBuilder(messageContent);
                        sb.delete(0, start+1);
                        messageContent = sb.toString();
                    }
                }
            }
            else{
                int startPrevious = messageContent.indexOf("_");
                if(startPrevious<start)
                {
                    String emoji = messageContent.substring(0, startPrevious);
                    if(EmojiManager.isEmoji(emoji)){
                        LogUtil.logDebug("The first element is emoji");
                        substring = messageContent.substring(0, startPrevious);
                        ssb.append(substring);

                        StringBuilder sb = new StringBuilder(messageContent);
                        sb.delete(0, startPrevious+1);
                        messageContent = sb.toString();
                    }
                }
                else{
                    start++;
                    substring = messageContent.substring(0, start);
                    ssb.append(substring);

                    StringBuilder sb = new StringBuilder(messageContent);
                    sb.delete(0, start+1);
                    messageContent = sb.toString();
                }
            }
        }

        end = messageContent.indexOf("_ ");
        if (messageContentInitial.equals(messageContent) && end != -1){
            end = messageContent.indexOf("_");
            if (end != -1){
                substring = messageContent.substring(0, end+1);
                ssb.append(substring);

                StringBuilder sb = new StringBuilder(messageContent);
                sb.delete(0, end+1);
                messageContent = sb.toString();
            }
            else {
                end = messageContent.indexOf("_ ");
                if (end != -1){
                    substring = messageContent.substring(0, end+1);
                    ssb.append(substring);

                    StringBuilder sb = new StringBuilder(messageContent);
                    sb.delete(0, end+1);
                    messageContent = sb.toString();
                }
            }
        }
        else {
            if(end==-1){
                end = messageContent.indexOf("_.");
                if (end == -1) {
                    end = messageContent.indexOf("_\n");
                    if (end == -1){
                        end = messageContent.lastIndexOf("_");
                        LogUtil.logDebug("FINISH End position: " + end);

                        StringBuilder sb = new StringBuilder(messageContent);
                        sb.deleteCharAt(end);
                        messageContent = sb.toString();

                        substring = messageContent.substring(0, end);

                        Matcher mBold = pBold.matcher(substring);

                        if(mBold!=null && mBold.find()){
                            applyItalicBoldFormat(substring);
                        }
                        else {

                            Matcher mMultiQuote = pMultiQuote.matcher(substring);

                            if (mMultiQuote != null && mMultiQuote.find()) {
                                LogUtil.logDebug("Multiquote");
                                applyOneFormatAndMultiQuoteFormat(substring, Typeface.ITALIC);
                            }
                            else{
                                Matcher mQuote = pQuote.matcher(substring);

                                if (mQuote != null && mQuote.find()) {
                                    LogUtil.logDebug("Quote");
                                    applyOneFormatAndQuoteFormat(substring, Typeface.ITALIC);
                                }
                                else{
                                    ssb.append(substring, new StyleSpan(Typeface.ITALIC));
                                }
                            }
                        }

                        sb = new StringBuilder(messageContent);
                        sb.delete(0, end);
                        messageContent = sb.toString();


                        String emoji = messageContent;
                        if(EmojiManager.isEmoji(emoji)){
                            LogUtil.logDebug("The last element is emoji");
                            ssb.append(emoji);

                            messageContent = "";
                        }
                    }
                    else {
                        LogUtil.logDebug("End position: " + end);
                        StringBuilder sb = new StringBuilder(messageContent);
                        sb.deleteCharAt(end);
                        messageContent = sb.toString();

                        substring = messageContent.substring(0, end);

                        Matcher mBold = pBold.matcher(substring);

                        if(mBold!=null && mBold.find()){
                            applyItalicBoldFormat(substring);
                        }
                        else {

                            Matcher mMultiQuote = pMultiQuote.matcher(substring);

                            if (mMultiQuote != null && mMultiQuote.find()) {
                                LogUtil.logDebug("Multiquote");
                                applyOneFormatAndMultiQuoteFormat(substring, Typeface.ITALIC);
                            }
                            else{
                                Matcher mQuote = pQuote.matcher(substring);

                                if (mQuote != null && mQuote.find()) {
                                    LogUtil.logDebug("Quote");
                                    applyOneFormatAndQuoteFormat(substring, Typeface.ITALIC);
                                }
                                else{
                                    ssb.append(substring, new StyleSpan(Typeface.ITALIC));
                                }
                            }
                        }

                        sb = new StringBuilder(messageContent);
                        sb.delete(0, end);
                        messageContent = sb.toString();

                        start = messageContent.indexOf(" _");
                        while(start!=-1) {

                            start = start + 1;

                            sb = new StringBuilder(messageContent);
                            sb.deleteCharAt(start);
                            messageContent = sb.toString();

                            LogUtil.logDebug("(B) Start position: " + start);
                            substring = messageContent.substring(0, start);
                            ssb.append(substring);

                            sb = new StringBuilder(messageContent);
                            sb.delete(0, start);
                            messageContent = sb.toString();

                            end = messageContent.indexOf("_ ");
                            if (end == -1) {
                                end = messageContent.lastIndexOf("_");

                                sb = new StringBuilder(messageContent);
                                sb.deleteCharAt(end);
                                messageContent = sb.toString();

                                LogUtil.logDebug("(B)FINISH End position: " + end);
                                substring = messageContent.substring(0, end);
                                ssb.append(substring, new StyleSpan(Typeface.ITALIC));

                                sb = new StringBuilder(messageContent);
                                sb.delete(0, end);
                                messageContent = sb.toString();

                                break;
                            } else {
                                LogUtil.logDebug("End position: " + end);
                                sb = new StringBuilder(messageContent);
                                sb.deleteCharAt(end);
                                messageContent = sb.toString();

                                substring = messageContent.substring(0, end);
                                ssb.append(substring, new StyleSpan(Typeface.ITALIC));

                                sb = new StringBuilder(messageContent);
                                sb.delete(0, end);
                                messageContent = sb.toString();

                                start = messageContent.indexOf(" _");
                            }
                        }
                    }
                }
                else {
                    LogUtil.logDebug("End position: "+end);
                    StringBuilder sb = new StringBuilder(messageContent);
                    sb.deleteCharAt(end);
                    messageContent = sb.toString();

                    substring = messageContent.substring(0, end);

                    Matcher mBold = pBold.matcher(substring);

                    if(mBold!=null && mBold.find()){
                        applyItalicBoldFormat(substring);
                    }
                    else {

                        Matcher mMultiQuote = pMultiQuote.matcher(substring);

                        if (mMultiQuote != null && mMultiQuote.find()) {
                            LogUtil.logDebug("Multiquote");
                            applyOneFormatAndMultiQuoteFormat(substring, Typeface.ITALIC);
                        }
                        else{
                            Matcher mQuote = pQuote.matcher(substring);

                            if (mQuote != null && mQuote.find()) {
                                LogUtil.logDebug("Quote");
                                applyOneFormatAndQuoteFormat(substring, Typeface.ITALIC);
                            }
                            else{
                                ssb.append(substring, new StyleSpan(Typeface.ITALIC));
                            }
                        }
                    }

                    sb = new StringBuilder(messageContent);
                    sb.delete(0, end);
                    messageContent = sb.toString();

                    start = messageContent.indexOf(" _");
                    while(start!=-1) {

                        start = start + 1;

                        sb = new StringBuilder(messageContent);
                        sb.deleteCharAt(start);
                        messageContent = sb.toString();

                        LogUtil.logDebug("(B) Start position: " + start);
                        substring = messageContent.substring(0, start);
                        ssb.append(substring);

                        sb = new StringBuilder(messageContent);
                        sb.delete(0, start);
                        messageContent = sb.toString();

                        end = messageContent.indexOf("_ ");
                        if (end == -1) {
                            end = messageContent.lastIndexOf("_");

                            sb = new StringBuilder(messageContent);
                            sb.deleteCharAt(end);
                            messageContent = sb.toString();

                            LogUtil.logDebug("(B)FINISH End position: " + end);
                            substring = messageContent.substring(0, end);
                            ssb.append(substring, new StyleSpan(Typeface.ITALIC));

                            sb = new StringBuilder(messageContent);
                            sb.delete(0, end);
                            messageContent = sb.toString();

                            break;
                        } else {
                            LogUtil.logDebug("End position: " + end);
                            sb = new StringBuilder(messageContent);
                            sb.deleteCharAt(end);
                            messageContent = sb.toString();

                            substring = messageContent.substring(0, end);
                            ssb.append(substring, new StyleSpan(Typeface.ITALIC));

                            sb = new StringBuilder(messageContent);
                            sb.delete(0, end);
                            messageContent = sb.toString();

                            start = messageContent.indexOf(" _");
                        }
                    }
                }
            }
            else{
                LogUtil.logDebug("End position: " + end);
                StringBuilder sb = new StringBuilder(messageContent);
                sb.deleteCharAt(end);
                messageContent = sb.toString();

                substring = messageContent.substring(0, end);

                Matcher mBold = pBold.matcher(substring);

                if(mBold!=null && mBold.find()){
                    applyItalicBoldFormat(substring);
                }
                else {

                    Matcher mMultiQuote = pMultiQuote.matcher(substring);

                    if (mMultiQuote != null && mMultiQuote.find()) {
                        LogUtil.logDebug("Multiquote");
                        applyOneFormatAndMultiQuoteFormat(substring, Typeface.ITALIC);
                    }
                    else{
                        Matcher mQuote = pQuote.matcher(substring);

                        if (mQuote != null && mQuote.find()) {
                            LogUtil.logDebug("Quote");
                            applyOneFormatAndQuoteFormat(substring, Typeface.ITALIC);
                        }
                        else{
                            ssb.append(substring, new StyleSpan(Typeface.ITALIC));
                        }
                    }
                }

                sb = new StringBuilder(messageContent);
                sb.delete(0, end);
                messageContent = sb.toString();

                start = messageContent.indexOf(" _");
                while(start!=-1){

                    start = start +1;

                    sb = new StringBuilder(messageContent);
                    sb.deleteCharAt(start);
                    messageContent = sb.toString();

                    LogUtil.logDebug("(B) Start position: " + start);
                    substring = messageContent.substring(0, start);
                    ssb.append(substring);

                    sb = new StringBuilder(messageContent);
                    sb.delete(0, start);
                    messageContent = sb.toString();

                    end = messageContent.indexOf("_ ");
                    if(end==-1){
                        end = messageContent.lastIndexOf("_");

                        sb = new StringBuilder(messageContent);
                        sb.deleteCharAt(end);
                        messageContent = sb.toString();

                        LogUtil.logDebug("(B)FINISH End position: " + end);
                        substring = messageContent.substring(0, end);
                        ssb.append(substring, new StyleSpan(Typeface.ITALIC));

                        sb = new StringBuilder(messageContent);
                        sb.delete(0, end);
                        messageContent = sb.toString();

                        break;
                    }
                    else{
                        LogUtil.logDebug("End position: " + end);
                        sb = new StringBuilder(messageContent);
                        sb.deleteCharAt(end);
                        messageContent = sb.toString();

                        substring = messageContent.substring(0, end);
                        ssb.append(substring, new StyleSpan(Typeface.ITALIC));

                        sb = new StringBuilder(messageContent);
                        sb.delete(0, end);
                        messageContent = sb.toString();

                        start = messageContent.indexOf(" _");
                    }
                }
            }
        }

        setRTFFormat();
        while(formatted){
            setRTFFormat();
        }
        if(!messageContent.isEmpty()){
            LogUtil.logDebug("more to append...");
            ssb.append(messageContent);
            messageContent ="";
        }

    }

    public SimpleSpanBuilder applyItalicBoldFormat(String subMessageContent){
        LogUtil.logDebug("applyItalicBoldFormat: " + subMessageContent);

        char b = subMessageContent.charAt(0);
        int startB;
        int endB;

        if(ssb==null){
            ssb = new SimpleSpanBuilder();
        }

        String substringB = null;

        if(b =='*'){
            StringBuilder sb = new StringBuilder(subMessageContent);
            sb.deleteCharAt(0);
            subMessageContent = sb.toString();
        }
        else{
            startB = subMessageContent.indexOf(" *");
            startB++;
            substringB = subMessageContent.substring(0, startB);
            ssb.append(substringB, new StyleSpan(Typeface.ITALIC));

            startB++;
            StringBuilder sb = new StringBuilder(subMessageContent);
            sb.delete(0, startB);
            subMessageContent = sb.toString();
        }

        endB = subMessageContent.indexOf("* ");
        if(endB==-1){
            endB = subMessageContent.lastIndexOf("*");
            LogUtil.logDebug("FINISH endB position: " + endB);

            StringBuilder sbB = new StringBuilder(subMessageContent);
            sbB.deleteCharAt(endB);
            subMessageContent = sbB.toString();

            substringB = subMessageContent.substring(0, endB);


            Matcher mMultiQuote = pMultiQuote.matcher(substringB);

            if (mMultiQuote != null && mMultiQuote.find()) {
                LogUtil.logDebug("Multiquote");
                applyTwoFormatsAndMultiQuoteFormat(substringB);
            }
            else{
                Matcher mQuote = pQuote.matcher(substringB);

                if (mQuote != null && mQuote.find()) {
                    LogUtil.logDebug("Quote");
                    applyTwoFormatsAndQuoteFormat(substringB);
                }
                else{
                    ssb.append(substringB, new StyleSpan(Typeface.BOLD), new StyleSpan(Typeface.ITALIC));
                }
            }

            sbB = new StringBuilder(subMessageContent);
            sbB.delete(0, endB);
            subMessageContent = sbB.toString();
        }
        else{
            LogUtil.logDebug("endB position: "+endB);

            substringB = subMessageContent.substring(0, endB);

            Matcher mMultiQuote = pMultiQuote.matcher(substringB);

            if (mMultiQuote != null && mMultiQuote.find()) {
                LogUtil.logDebug("Multiquote");
                applyTwoFormatsAndMultiQuoteFormat(substringB);
            }
            else{
                Matcher mQuote = pQuote.matcher(substringB);

                if (mQuote != null && mQuote.find()) {
                    LogUtil.logDebug("Quote");
                    applyTwoFormatsAndQuoteFormat(substringB);
                }
                else{
                    ssb.append(substringB, new StyleSpan(Typeface.BOLD), new StyleSpan(Typeface.ITALIC));
                }
            }

            endB++;
            StringBuilder sbB = new StringBuilder(subMessageContent);
            sbB.delete(0, endB);
            subMessageContent = sbB.toString();

            startB = subMessageContent.indexOf(" *");
            while(startB!=-1){

                startB = startB +1;

                sbB = new StringBuilder(subMessageContent);
                sbB.deleteCharAt(startB);
                subMessageContent = sbB.toString();

                LogUtil.logDebug("(B) startB position: " + startB);
                substringB = subMessageContent.substring(0, startB);
                ssb.append(substringB, new StyleSpan(Typeface.ITALIC));

                sbB = new StringBuilder(subMessageContent);
                sbB.delete(0, startB);
                subMessageContent = sbB.toString();

                endB = subMessageContent.indexOf("* ");
                if(endB==-1){
                    endB = subMessageContent.lastIndexOf("*");
                    LogUtil.logDebug("(B)FINISH endB position: " + endB);

                    sbB = new StringBuilder(subMessageContent);
                    sbB.deleteCharAt(endB);
                    subMessageContent = sbB.toString();

                    LogUtil.logDebug("(B)FINISH End position: " + endB);
                    substringB = subMessageContent.substring(0, endB);

                    mMultiQuote = pMultiQuote.matcher(substringB);

                    if (mMultiQuote != null && mMultiQuote.find()) {
                        LogUtil.logDebug("Multiquote");
                        applyTwoFormatsAndMultiQuoteFormat(substringB);
                    }
                    else{
                        Matcher mQuote = pQuote.matcher(substringB);

                        if (mQuote != null && mQuote.find()) {
                            LogUtil.logDebug("Quote");
                            applyTwoFormatsAndQuoteFormat(substringB);
                        }
                        else{
                            ssb.append(substringB, new StyleSpan(Typeface.BOLD), new StyleSpan(Typeface.ITALIC));
                        }
                    }

                    sbB = new StringBuilder(subMessageContent);
                    sbB.delete(0, endB);
                    subMessageContent = sbB.toString();
                    break;
                }
                else{
                    LogUtil.logDebug("endB position: " + endB);
                    sbB = new StringBuilder(subMessageContent);
                    sbB.deleteCharAt(endB);
                    subMessageContent = sbB.toString();

                    substringB = subMessageContent.substring(0, endB);

                    mMultiQuote = pMultiQuote.matcher(substringB);

                    if (mMultiQuote != null && mMultiQuote.find()) {
                        LogUtil.logDebug("Multiquote");
                        applyTwoFormatsAndMultiQuoteFormat(substringB);
                    }
                    else{
                        Matcher mQuote = pQuote.matcher(substringB);

                        if (mQuote != null && mQuote.find()) {
                            LogUtil.logDebug("Quote");
                            applyTwoFormatsAndQuoteFormat(substringB);
                        }
                        else{
                            ssb.append(substringB, new StyleSpan(Typeface.BOLD), new StyleSpan(Typeface.ITALIC));
                        }
                    }

                    sbB = new StringBuilder(subMessageContent);
                    sbB.delete(0, endB);
                    subMessageContent = sbB.toString();

                    startB = subMessageContent.indexOf(" *");
                }
            }
        }

        if(!subMessageContent.isEmpty()){
            LogUtil.logDebug("(ITALICBOLD: Append more...");
            ssb.append(subMessageContent, new StyleSpan(Typeface.ITALIC));
        }

        formatted = true;
        return ssb;
    }

    public SimpleSpanBuilder applyTwoFormatsAndQuoteFormat(String subMessageContent){
        LogUtil.logDebug("applyTwoFormatAndQuoteFormat: " + subMessageContent);

        char b = subMessageContent.charAt(0);
        int startB;
        int endB;

        if(ssb==null){
            ssb = new SimpleSpanBuilder();
        }

        String substringB = null;

        if(b =='`'){
            StringBuilder sb = new StringBuilder(subMessageContent);
            sb.deleteCharAt(0);
            subMessageContent = sb.toString();
        }
        else{
            startB = subMessageContent.indexOf(" `");
            startB++;
            substringB = subMessageContent.substring(0, startB);

            ssb.append(substringB, new StyleSpan(Typeface.BOLD), new StyleSpan(Typeface.ITALIC));

            startB++;
            StringBuilder sb = new StringBuilder(subMessageContent);
            sb.delete(0, startB);
            subMessageContent = sb.toString();
        }

        endB = subMessageContent.indexOf("` ");
        if(endB==-1){
            endB = subMessageContent.lastIndexOf("`");
            LogUtil.logDebug("FINISH endB position: " + endB);

            StringBuilder sbB = new StringBuilder(subMessageContent);
            sbB.deleteCharAt(endB);
            subMessageContent = sbB.toString();

            substringB = subMessageContent.substring(0, endB);

            ssb.append(substringB, new CustomTypefaceSpan("", font), new StyleSpan(Typeface.BOLD), new StyleSpan(Typeface.ITALIC));

            sbB = new StringBuilder(subMessageContent);
            sbB.delete(0, endB);
            subMessageContent = sbB.toString();
        }
        else{
            LogUtil.logDebug("endB position: " + endB);

            substringB = subMessageContent.substring(0, endB);
            ssb.append(substringB, new CustomTypefaceSpan("", font), new StyleSpan(Typeface.BOLD), new StyleSpan(Typeface.ITALIC));

            endB++;
            StringBuilder sbB = new StringBuilder(subMessageContent);
            sbB.delete(0, endB);
            subMessageContent = sbB.toString();

            startB = subMessageContent.indexOf(" `");
            while(startB!=-1){

                startB = startB +1;

                sbB = new StringBuilder(subMessageContent);
                sbB.deleteCharAt(startB);
                subMessageContent = sbB.toString();

                LogUtil.logDebug("(B) startB position: " + startB);
                substringB = subMessageContent.substring(0, startB);
                ssb.append(substringB, new StyleSpan(Typeface.BOLD), new StyleSpan(Typeface.ITALIC));

                sbB = new StringBuilder(subMessageContent);
                sbB.delete(0, startB);
                subMessageContent = sbB.toString();

                endB = subMessageContent.indexOf("* ");
                if(endB==-1){
                    endB = subMessageContent.lastIndexOf("`");
                    LogUtil.logDebug("(B)FINISH endB position: " + endB);

                    sbB = new StringBuilder(subMessageContent);
                    sbB.deleteCharAt(endB);
                    subMessageContent = sbB.toString();

                    LogUtil.logDebug("(B)FINISH End position: " + endB);
                    substringB = subMessageContent.substring(0, endB);
                    ssb.append(substringB, new CustomTypefaceSpan("", font), new StyleSpan(Typeface.BOLD), new StyleSpan(Typeface.ITALIC));

                    sbB = new StringBuilder(subMessageContent);
                    sbB.delete(0, endB);
                    subMessageContent = sbB.toString();
                    break;
                }
                else{
                    LogUtil.logDebug("endB position: " + endB);
                    sbB = new StringBuilder(subMessageContent);
                    sbB.deleteCharAt(endB);
                    subMessageContent = sbB.toString();

                    substringB = subMessageContent.substring(0, endB);
                    ssb.append(substringB, new CustomTypefaceSpan("", font), new StyleSpan(Typeface.BOLD), new StyleSpan(Typeface.ITALIC));

                    sbB = new StringBuilder(subMessageContent);
                    sbB.delete(0, endB);
                    subMessageContent = sbB.toString();

                    startB = subMessageContent.indexOf(" `");
                }
            }
        }

        if(!subMessageContent.isEmpty()){
            LogUtil.logDebug("(ONEFORMATANDQuote: Append more...");
            ssb.append(subMessageContent, new StyleSpan(Typeface.BOLD), new StyleSpan(Typeface.ITALIC));
        }

        formatted = true;
        return ssb;
    }

    public SimpleSpanBuilder applyOneFormatAndQuoteFormat(String subMessageContent, int format){
        LogUtil.logDebug("applyOneFormatAndQuoteFormat: " + subMessageContent);

        char b = subMessageContent.charAt(0);
        int startB;
        int endB;

        if(ssb==null){
            ssb = new SimpleSpanBuilder();
        }

        String substringB = null;

        if(b =='`'){
            StringBuilder sb = new StringBuilder(subMessageContent);
            sb.deleteCharAt(0);
            subMessageContent = sb.toString();
        }
        else{
            startB = subMessageContent.indexOf(" `");
            startB++;
            substringB = subMessageContent.substring(0, startB);

            ssb.append(substringB, new StyleSpan(format));

            startB++;
            StringBuilder sb = new StringBuilder(subMessageContent);
            sb.delete(0, startB);
            subMessageContent = sb.toString();
        }

        endB = subMessageContent.indexOf("` ");
        if(endB==-1){
            endB = subMessageContent.lastIndexOf("`");
            LogUtil.logDebug("FINISH endB position: " + endB);

            StringBuilder sbB = new StringBuilder(subMessageContent);
            sbB.deleteCharAt(endB);
            subMessageContent = sbB.toString();

            substringB = subMessageContent.substring(0, endB);

            ssb.append(substringB, new CustomTypefaceSpan("", font), new StyleSpan(format));

            sbB = new StringBuilder(subMessageContent);
            sbB.delete(0, endB);
            subMessageContent = sbB.toString();
        }
        else{
            LogUtil.logDebug("endB position: " + endB);

            substringB = subMessageContent.substring(0, endB);
            ssb.append(substringB, new CustomTypefaceSpan("", font), new StyleSpan(format));

            endB++;
            StringBuilder sbB = new StringBuilder(subMessageContent);
            sbB.delete(0, endB);
            subMessageContent = sbB.toString();

            startB = subMessageContent.indexOf(" `");
            while(startB!=-1){

                startB = startB +1;

                sbB = new StringBuilder(subMessageContent);
                sbB.deleteCharAt(startB);
                subMessageContent = sbB.toString();

                LogUtil.logDebug("(B) startB position: " + startB);
                substringB = subMessageContent.substring(0, startB);
                ssb.append(substringB, new StyleSpan(format));

                sbB = new StringBuilder(subMessageContent);
                sbB.delete(0, startB);
                subMessageContent = sbB.toString();

                endB = subMessageContent.indexOf("* ");
                if(endB==-1){
                    endB = subMessageContent.lastIndexOf("`");
                    LogUtil.logDebug("(B)FINISH endB position: " + endB);

                    sbB = new StringBuilder(subMessageContent);
                    sbB.deleteCharAt(endB);
                    subMessageContent = sbB.toString();

                    LogUtil.logDebug("(B)FINISH End position: " + endB);
                    substringB = subMessageContent.substring(0, endB);
                    ssb.append(substringB, new CustomTypefaceSpan("", font), new StyleSpan(format));

                    sbB = new StringBuilder(subMessageContent);
                    sbB.delete(0, endB);
                    subMessageContent = sbB.toString();
                    break;
                }
                else{
                    LogUtil.logDebug("endB position: " + endB);
                    sbB = new StringBuilder(subMessageContent);
                    sbB.deleteCharAt(endB);
                    subMessageContent = sbB.toString();

                    substringB = subMessageContent.substring(0, endB);
                    ssb.append(substringB, new CustomTypefaceSpan("", font), new StyleSpan(format));

                    sbB = new StringBuilder(subMessageContent);
                    sbB.delete(0, endB);
                    subMessageContent = sbB.toString();

                    startB = subMessageContent.indexOf(" `");
                }
            }
        }

        if(!subMessageContent.isEmpty()){
            LogUtil.logDebug("(ONEFORMATANDQuote: Append more...");
            ssb.append(subMessageContent, new StyleSpan(format));
        }

        formatted = true;
        return ssb;
    }

    public SimpleSpanBuilder applyTwoFormatsAndMultiQuoteFormat(String subMessageContent){
        LogUtil.logDebug("applyTwoFormatsAndMultiQuoteFormat: " + subMessageContent);
//        char b = subMessageContent.charAt(0);
        String b = subMessageContent.substring(0,3);
        int startB;
        int endB;

        if(ssb==null){
            ssb = new SimpleSpanBuilder();
        }

        String substringB = null;
        Typeface typeFace = Typeface.createFromAsset(context.getAssets(), "font/RobotoMono-Medium.ttf");

        if(b.equals("```")){
            StringBuilder sb = new StringBuilder(subMessageContent);
            sb.delete(0,3);
            subMessageContent = sb.toString();
        }
        else{
            startB = subMessageContent.indexOf(" ```");
            startB=startB+1;
            substringB = subMessageContent.substring(0, startB);

            ssb.append(substringB, new StyleSpan(Typeface.BOLD), new StyleSpan(Typeface.ITALIC));

            StringBuilder sb = new StringBuilder(subMessageContent);
            sb.delete(0, startB+3);
            sb.insert(0, '\n');
            subMessageContent = sb.toString();
        }

        endB = subMessageContent.indexOf("``` ");
        if(endB==-1){
            endB = subMessageContent.lastIndexOf("```");
            LogUtil.logDebug("FINISH endB position: " + endB);

            StringBuilder sbB = new StringBuilder(subMessageContent);
            sbB.delete(endB, endB+3);

            subMessageContent = sbB.toString();

            substringB = subMessageContent.substring(0, endB);

            ssb.append(substringB, new CustomTypefaceSpan("", font), new StyleSpan(Typeface.BOLD), new StyleSpan(Typeface.ITALIC));

            sbB = new StringBuilder(subMessageContent);
            sbB.delete(0, endB+3);
            subMessageContent = sbB.toString();
        }
        else{
            LogUtil.logDebug("endB position: " + endB);

            substringB = subMessageContent.substring(0, endB);

            ssb.append(substringB, new CustomTypefaceSpan("", font), new StyleSpan(Typeface.BOLD), new StyleSpan(Typeface.ITALIC));
            endB++;
            StringBuilder sbB = new StringBuilder(subMessageContent);
            sbB.delete(0, endB+3);
            subMessageContent = sbB.toString();
        }

        if(!subMessageContent.isEmpty()){
            LogUtil.logDebug("(ITALICMULTIQUOTE: Append more...");
            StringBuilder sbBMultiQuote = new StringBuilder('\n'+subMessageContent);
//            sbBMultiQuote.insert(0, '\n');
            subMessageContent = sbBMultiQuote.toString();
            ssb.append(subMessageContent, new StyleSpan(Typeface.BOLD), new StyleSpan(Typeface.ITALIC));
        }

        formatted = true;
        return ssb;
    }

    public SimpleSpanBuilder applyOneFormatAndMultiQuoteFormat(String subMessageContent, int format){
        LogUtil.logDebug("applyOneFormatAndMultiQuoteFormat: " + subMessageContent);
//        char b = subMessageContent.charAt(0);
        String b = subMessageContent.substring(0,3);
        int startB;
        int endB;

        if(ssb==null){
            ssb = new SimpleSpanBuilder();
        }

        String substringB = null;
        Typeface typeFace = Typeface.createFromAsset(context.getAssets(), "font/RobotoMono-Medium.ttf");

        if(b.equals("```")){
            StringBuilder sb = new StringBuilder(subMessageContent);
            sb.delete(0,3);
            subMessageContent = sb.toString();
        }
        else{
            startB = subMessageContent.indexOf(" ```");
            startB=startB+1;
            substringB = subMessageContent.substring(0, startB);
            ssb.append(substringB, new StyleSpan(format));

            StringBuilder sb = new StringBuilder(subMessageContent);
            sb.delete(0, startB+3);
            sb.insert(0, '\n');
            subMessageContent = sb.toString();
        }

        endB = subMessageContent.indexOf("``` ");
        if(endB==-1){
            endB = subMessageContent.lastIndexOf("```");
            LogUtil.logDebug("FINISH endB position: " + endB);

            StringBuilder sbB = new StringBuilder(subMessageContent);
            sbB.delete(endB, endB+3);

            subMessageContent = sbB.toString();

            substringB = subMessageContent.substring(0, endB);

            ssb.append(substringB, new CustomTypefaceSpan("", font), new StyleSpan(format));

            sbB = new StringBuilder(subMessageContent);
            sbB.delete(0, endB+3);
            subMessageContent = sbB.toString();
        }
        else{
            LogUtil.logDebug("endB position: " + endB);

            substringB = subMessageContent.substring(0, endB);
//            ssb.append(substringB, new StyleSpan(typeFace.getStyle()), new StyleSpan(Typeface.ITALIC));

            ssb.append(substringB, new CustomTypefaceSpan("", font), new StyleSpan(format));
            endB++;
            StringBuilder sbB = new StringBuilder(subMessageContent);
            sbB.delete(0, endB+3);
            subMessageContent = sbB.toString();
        }

        if(!subMessageContent.isEmpty()){
            LogUtil.logDebug("(ITALICMULTIQUOTE: Append more...");
            StringBuilder sbBMultiQuote = new StringBuilder(subMessageContent);
            sbBMultiQuote.insert(0, '\n');
            subMessageContent = sbBMultiQuote.toString();
            ssb.append(subMessageContent, new StyleSpan(format));
        }

        formatted = true;
        return ssb;
    }

    public void applyBoldFormat(){
        LogUtil.logDebug("applyBoldFormat");
        char a = messageContent.charAt(0);
        int start;
        int end;

        if(ssb==null){
            ssb = new SimpleSpanBuilder();
        }

        String substring = null;

        if(a =='*'){
            start = 0;
            //Check if the next one is *
            if(messageContent.charAt(1)=='_'){
                StringBuilder sb = new StringBuilder(messageContent);
                sb.delete(0,2);
                messageContent = sb.toString();

                end = messageContent.indexOf("_*");
                if(end!=-1){
                    substring = messageContent.substring(0, end);
                }
                else{
                    end = messageContent.indexOf("_");

                    substring = messageContent.substring(0, end);
                }

                Matcher mMultiQuote = pMultiQuote.matcher(substring);

                if (mMultiQuote != null && mMultiQuote.find()) {
                    LogUtil.logDebug("Multiquote");
                    applyTwoFormatsAndMultiQuoteFormat(substring);
                }
                else{
                    Matcher mQuote = pQuote.matcher(substring);

                    if (mQuote != null && mQuote.find()) {
                        LogUtil.logDebug("Quote");
                        applyTwoFormatsAndQuoteFormat(substring);
                    }
                    else{
                        ssb.append(substring, new StyleSpan(Typeface.BOLD), new StyleSpan(Typeface.ITALIC));
                    }
                }

                sb = new StringBuilder(messageContent);
                sb.delete(0, end+1);
                messageContent = sb.toString();
            }
            else{
                StringBuilder sb = new StringBuilder(messageContent);
                sb.deleteCharAt(0);
                messageContent = sb.toString();
            }
        }
        else{
            start = messageContent.indexOf(" *");
            if(start==-1){
                start = messageContent.indexOf(".*");
                if (start == -1) {
                    start = messageContent.indexOf("\n*");
                    if (start == -1){
                        LogUtil.logDebug("Check if there is emoji at the beginning of the string");
                        start = messageContent.indexOf("*");
                        String emoji = messageContent.substring(0, start);
                        if(EmojiManager.isEmoji(emoji)){
                            LogUtil.logDebug("The first element is emoji");
                            substring = messageContent.substring(0, start);
                            ssb.append(substring);

                            StringBuilder sb = new StringBuilder(messageContent);
                            sb.delete(0, start+1);
                            messageContent = sb.toString();
                        }
                        else {
                            List<String> emojiList = EmojiParser.extractEmojis(emoji);
                            if (emojiList != null) {
                                if (!emojiList.isEmpty()) {
                                    substring = messageContent.substring(0, start);
                                    int lastSpace = substring.lastIndexOf(" ");
                                    if(lastSpace!=-1){
                                        String checkEmoji = substring.substring(lastSpace+1, start);
                                        if(EmojiManager.isEmoji(checkEmoji)){
                                            ssb.append(substring);

                                            StringBuilder sb = new StringBuilder(messageContent);
                                            sb.delete(0, start+1);
                                            messageContent = sb.toString();
                                        }
                                    }
                                }
                            }
                        }
                    }
                    else{
                        int startPrevious = messageContent.indexOf("*");
                        if(startPrevious<start)
                        {
                            String emoji = messageContent.substring(0, startPrevious);
                            if(EmojiManager.isEmoji(emoji)){
                                LogUtil.logDebug("The first element is emoji");
                                substring = messageContent.substring(0, startPrevious);
                                ssb.append(substring);

                                StringBuilder sb = new StringBuilder(messageContent);
                                sb.delete(0, startPrevious+1);
                                messageContent = sb.toString();
                            }
                        }
                        else{
                            start++;
                            substring = messageContent.substring(0, start);
                            ssb.append(substring);

                            StringBuilder sb = new StringBuilder(messageContent);
                            sb.delete(0, start+1);
                            messageContent = sb.toString();
                        }
                    }
                }
                else {
                    int startPrevious = messageContent.indexOf("*");
                    if(startPrevious<start)
                    {
                        String emoji = messageContent.substring(0, startPrevious);
                        if(EmojiManager.isEmoji(emoji)){
                            LogUtil.logDebug("The first element is emoji");
                            substring = messageContent.substring(0, startPrevious);
                            ssb.append(substring);

                            StringBuilder sb = new StringBuilder(messageContent);
                            sb.delete(0, startPrevious+1);
                            messageContent = sb.toString();
                        }
                    }
                    else{
                        start++;
                        substring = messageContent.substring(0, start);
                        ssb.append(substring);

                        StringBuilder sb = new StringBuilder(messageContent);
                        sb.delete(0, start+1);
                        messageContent = sb.toString();
                    }
                }
            }
            else{
                int startPrevious = messageContent.indexOf("*");
                if(startPrevious<start)
                {
                    String emoji = messageContent.substring(0, startPrevious);
                    if(EmojiManager.isEmoji(emoji)){
                        LogUtil.logDebug("The first element is emoji");
                        substring = messageContent.substring(0, startPrevious);
                        ssb.append(substring);

                        StringBuilder sb = new StringBuilder(messageContent);
                        sb.delete(0, startPrevious+1);
                        messageContent = sb.toString();
                    }
                }
                else{
                    start++;
                    substring = messageContent.substring(0, start);
                    ssb.append(substring);

                    StringBuilder sb = new StringBuilder(messageContent);
                    sb.delete(0, start+1);
                    messageContent = sb.toString();
                }
            }
        }

        end = messageContent.indexOf("* ");
        if(end==-1){
            end = messageContent.indexOf("*.");
            if (end == -1) {
                end = messageContent.indexOf("*\n");
                if (end == -1){
                    end = messageContent.lastIndexOf("*");
                    LogUtil.logDebug("FINISH End position: " + end);

                    StringBuilder sb = new StringBuilder(messageContent);
                    sb.deleteCharAt(end);
                    messageContent = sb.toString();

                    substring = messageContent.substring(0, end);

                    Matcher mItalic = pItalic.matcher(substring);

                    if(mItalic!=null && mItalic.find()){
                        applyBoldItalicFormat(substring);
                    }
                    else{
                        Matcher mMultiQuote = pMultiQuote.matcher(substring);

                        if (mMultiQuote != null && mMultiQuote.find()) {
                            LogUtil.logDebug("Multiquote");
                            applyOneFormatAndMultiQuoteFormat(substring, Typeface.BOLD);
                        }
                        else{
                            Matcher mQuote = pQuote.matcher(substring);

                            if (mQuote != null && mQuote.find()) {
                                LogUtil.logDebug("Quote");
                                applyOneFormatAndQuoteFormat(substring, Typeface.BOLD);
                            }
                            else{
                                ssb.append(substring, new StyleSpan(Typeface.BOLD));
                            }
                        }
                    }

                    sb = new StringBuilder(messageContent);
                    sb.delete(0, end);
                    messageContent = sb.toString();

                    String emoji = messageContent;
                    if(EmojiManager.isEmoji(emoji)){
                        LogUtil.logDebug("The last element is emoji");
                        ssb.append(emoji);

                        messageContent = "";
                    }
                }
                else {
                    LogUtil.logDebug("End position: " + end);
                    StringBuilder sb = new StringBuilder(messageContent);
                    sb.deleteCharAt(end);
                    messageContent = sb.toString();

                    substring = messageContent.substring(0, end);

                    String noEmojisContent = EmojiParser.removeAllEmojis(substring);

                    Matcher mItalic = pItalic.matcher(substring);

                    if(mItalic!=null && mItalic.find()){
                        applyBoldItalicFormat(substring);
                    }
                    else{
                        Matcher mMultiQuote = pMultiQuote.matcher(substring);

                        if (mMultiQuote != null && mMultiQuote.find()) {
                            LogUtil.logDebug("Multiquote");
                            applyOneFormatAndMultiQuoteFormat(substring, Typeface.BOLD);
                        }
                        else{
                            Matcher mQuote = pQuote.matcher(substring);

                            if (mQuote != null && mQuote.find()) {
                                LogUtil.logDebug("Quote");
                                applyOneFormatAndQuoteFormat(substring, Typeface.BOLD);
                            }
                            else{
                                ssb.append(substring, new StyleSpan(Typeface.BOLD));
                            }
                        }
                    }

                    sb = new StringBuilder(messageContent);
                    sb.delete(0, end);
                    messageContent = sb.toString();

                    start = messageContent.indexOf(" *");
                    while(start!=-1){

                        start = start +1;

                        sb = new StringBuilder(messageContent);
                        sb.deleteCharAt(start);
                        messageContent = sb.toString();

                        LogUtil.logDebug("(B) Start position: " + start);
                        substring = messageContent.substring(0, start);
                        ssb.append(substring);

                        sb = new StringBuilder(messageContent);
                        sb.delete(0, start);
                        messageContent = sb.toString();

                        end = messageContent.indexOf("* ");
                        if(end==-1){
                            end = messageContent.lastIndexOf("*");

                            sb = new StringBuilder(messageContent);
                            sb.deleteCharAt(end);
                            messageContent = sb.toString();

                            LogUtil.logDebug("(B)FINISH End position: " + end);
                            substring = messageContent.substring(0, end);
                            ssb.append(substring, new StyleSpan(Typeface.BOLD));

                            sb = new StringBuilder(messageContent);
                            sb.delete(0, end);
                            messageContent = sb.toString();

                            break;
                        }
                        else{
                            LogUtil.logDebug("End position: " + end);
                            sb = new StringBuilder(messageContent);
                            sb.deleteCharAt(end);
                            messageContent = sb.toString();

                            substring = messageContent.substring(0, end);
                            ssb.append(substring, new StyleSpan(Typeface.BOLD));

                            sb = new StringBuilder(messageContent);
                            sb.delete(0, end);
                            messageContent = sb.toString();

                            start = messageContent.indexOf(" _");
                        }
                    }
                }
            }
            else {
                LogUtil.logDebug("End position: " + end);
                StringBuilder sb = new StringBuilder(messageContent);
                sb.deleteCharAt(end);
                messageContent = sb.toString();

                substring = messageContent.substring(0, end);

                String noEmojisContent = EmojiParser.removeAllEmojis(substring);

                Matcher mItalic = pItalic.matcher(substring);

                if(mItalic!=null && mItalic.find()){
                    applyBoldItalicFormat(substring);
                }
                else{
                    Matcher mMultiQuote = pMultiQuote.matcher(substring);

                    if (mMultiQuote != null && mMultiQuote.find()) {
                        LogUtil.logDebug("Multiquote");
                        applyOneFormatAndMultiQuoteFormat(substring, Typeface.BOLD);
                    }
                    else{
                        Matcher mQuote = pQuote.matcher(substring);

                        if (mQuote != null && mQuote.find()) {
                            LogUtil.logDebug("Quote");
                            applyOneFormatAndQuoteFormat(substring, Typeface.BOLD);
                        }
                        else{
                            ssb.append(substring, new StyleSpan(Typeface.BOLD));
                        }
                    }
                }

                sb = new StringBuilder(messageContent);
                sb.delete(0, end);
                messageContent = sb.toString();

                start = messageContent.indexOf(" *");
                while(start!=-1){

                    start = start +1;

                    sb = new StringBuilder(messageContent);
                    sb.deleteCharAt(start);
                    messageContent = sb.toString();

                    LogUtil.logDebug("(B) Start position: " + start);
                    substring = messageContent.substring(0, start);
                    ssb.append(substring);

                    sb = new StringBuilder(messageContent);
                    sb.delete(0, start);
                    messageContent = sb.toString();

                    end = messageContent.indexOf("* ");
                    if(end==-1){
                        end = messageContent.lastIndexOf("*");

                        sb = new StringBuilder(messageContent);
                        sb.deleteCharAt(end);
                        messageContent = sb.toString();

                        LogUtil.logDebug("(B)FINISH End position: " + end);
                        substring = messageContent.substring(0, end);
                        ssb.append(substring, new StyleSpan(Typeface.BOLD));

                        sb = new StringBuilder(messageContent);
                        sb.delete(0, end);
                        messageContent = sb.toString();

                        break;
                    }
                    else{
                        LogUtil.logDebug("End position: " + end);
                        sb = new StringBuilder(messageContent);
                        sb.deleteCharAt(end);
                        messageContent = sb.toString();

                        substring = messageContent.substring(0, end);
                        ssb.append(substring, new StyleSpan(Typeface.BOLD));

                        sb = new StringBuilder(messageContent);
                        sb.delete(0, end);
                        messageContent = sb.toString();

                        start = messageContent.indexOf(" _");
                    }
                }
            }
        }
        else{
            LogUtil.logDebug("End position: " + end);
            StringBuilder sb = new StringBuilder(messageContent);
            sb.deleteCharAt(end);
            messageContent = sb.toString();

            substring = messageContent.substring(0, end);

            String noEmojisContent = EmojiParser.removeAllEmojis(substring);

            Matcher mItalic = pItalic.matcher(substring);

            if(mItalic!=null && mItalic.find()){
                applyBoldItalicFormat(substring);
            }
            else{
                Matcher mMultiQuote = pMultiQuote.matcher(substring);

                if (mMultiQuote != null && mMultiQuote.find()) {
                    LogUtil.logDebug("Multiquote");
                    applyOneFormatAndMultiQuoteFormat(substring, Typeface.BOLD);
                }
                else{
                    Matcher mQuote = pQuote.matcher(substring);

                    if (mQuote != null && mQuote.find()) {
                        LogUtil.logDebug("Quote");
                        applyOneFormatAndQuoteFormat(substring, Typeface.BOLD);
                    }
                    else{
                        ssb.append(substring, new StyleSpan(Typeface.BOLD));
                    }
                }
            }

            sb = new StringBuilder(messageContent);
            sb.delete(0, end);
            messageContent = sb.toString();

            start = messageContent.indexOf(" *");
            while(start!=-1){

                start = start +1;

                sb = new StringBuilder(messageContent);
                sb.deleteCharAt(start);
                messageContent = sb.toString();

                LogUtil.logDebug("(B) Start position: " + start);
                substring = messageContent.substring(0, start);
                ssb.append(substring);

                sb = new StringBuilder(messageContent);
                sb.delete(0, start);
                messageContent = sb.toString();

                end = messageContent.indexOf("* ");
                if(end==-1){
                    end = messageContent.lastIndexOf("*");

                    sb = new StringBuilder(messageContent);
                    sb.deleteCharAt(end);
                    messageContent = sb.toString();

                    LogUtil.logDebug("(B)FINISH End position: " + end);
                    substring = messageContent.substring(0, end);
                    ssb.append(substring, new StyleSpan(Typeface.BOLD));

                    sb = new StringBuilder(messageContent);
                    sb.delete(0, end);
                    messageContent = sb.toString();

                    break;
                }
                else{
                    LogUtil.logDebug("End position: " + end);
                    sb = new StringBuilder(messageContent);
                    sb.deleteCharAt(end);
                    messageContent = sb.toString();

                    substring = messageContent.substring(0, end);
                    ssb.append(substring, new StyleSpan(Typeface.BOLD));

                    sb = new StringBuilder(messageContent);
                    sb.delete(0, end);
                    messageContent = sb.toString();

                    start = messageContent.indexOf(" _");
                }
            }
        }

        setRTFFormat();
        while(formatted){
            setRTFFormat();
        }
        if(!messageContent.isEmpty()){
            LogUtil.logDebug("more to append...");
            ssb.append(messageContent);
            messageContent ="";
        }
    }

    public SimpleSpanBuilder applyBoldItalicFormat(String subMessageContent){
        LogUtil.logDebug("applyBoldItalicFormat");

        char b = subMessageContent.charAt(0);
        int startB;
        int endB;

        if(ssb==null){
            ssb = new SimpleSpanBuilder();
        }

        String substringB = null;

        if(b =='_'){
            StringBuilder sb = new StringBuilder(subMessageContent);
            sb.deleteCharAt(0);
            subMessageContent = sb.toString();
        }
        else{
            startB = subMessageContent.indexOf(" _");
            startB++;
            substringB = subMessageContent.substring(0, startB);

            ssb.append(substringB, new StyleSpan(Typeface.BOLD));

            startB++;
            StringBuilder sb = new StringBuilder(subMessageContent);
            sb.delete(0, startB);
            subMessageContent = sb.toString();
        }

        endB = subMessageContent.indexOf("_ ");
        if(endB==-1){
            endB = subMessageContent.lastIndexOf("_");
            LogUtil.logDebug("FINISH endB position: " + endB);

            StringBuilder sbB = new StringBuilder(subMessageContent);
            sbB.deleteCharAt(endB);
            subMessageContent = sbB.toString();

            substringB = subMessageContent.substring(0, endB);

            Matcher mMultiQuote = pMultiQuote.matcher(substringB);

            if (mMultiQuote != null && mMultiQuote.find()) {
                LogUtil.logDebug("Multiquote");
                applyTwoFormatsAndMultiQuoteFormat(substringB);
            }
            else{
                Matcher mQuote = pQuote.matcher(substringB);

                if (mQuote != null && mQuote.find()) {
                    LogUtil.logDebug("Quote");
                    applyTwoFormatsAndQuoteFormat(substringB);
                }
                else{
                    ssb.append(substringB, new StyleSpan(Typeface.BOLD), new StyleSpan(Typeface.ITALIC));
                }
            }

            sbB = new StringBuilder(subMessageContent);
            sbB.delete(0, endB);
            subMessageContent = sbB.toString();
        }
        else{
            LogUtil.logDebug("endB position: " + endB);

            substringB = subMessageContent.substring(0, endB);

            Matcher mMultiQuote = pMultiQuote.matcher(substringB);

            if (mMultiQuote != null && mMultiQuote.find()) {
                LogUtil.logDebug("Multiquote");
                applyTwoFormatsAndMultiQuoteFormat(substringB);
            }
            else{
                Matcher mQuote = pQuote.matcher(substringB);

                if (mQuote != null && mQuote.find()) {
                    LogUtil.logDebug("Quote");
                    applyTwoFormatsAndQuoteFormat(substringB);
                }
                else{
                    ssb.append(substringB, new StyleSpan(Typeface.BOLD), new StyleSpan(Typeface.ITALIC));
                }
            }

            endB++;
            StringBuilder sbB = new StringBuilder(subMessageContent);
            sbB.delete(0, endB);
            subMessageContent = sbB.toString();

            startB = subMessageContent.indexOf(" _");
            while(startB!=-1){

                startB = startB +1;

                sbB = new StringBuilder(subMessageContent);
                sbB.deleteCharAt(startB);
                subMessageContent = sbB.toString();

                LogUtil.logDebug("(B) startB position: " + startB);
                substringB = subMessageContent.substring(0, startB);
                ssb.append(substringB, new StyleSpan(Typeface.BOLD));

                sbB = new StringBuilder(subMessageContent);
                sbB.delete(0, startB);
                subMessageContent = sbB.toString();

                endB = subMessageContent.indexOf("_ ");
                if(endB==-1){
                    endB = subMessageContent.lastIndexOf("_");
                    LogUtil.logDebug("(B)FINISH endB position: " + endB);

                    sbB = new StringBuilder(subMessageContent);
                    sbB.deleteCharAt(endB);
                    subMessageContent = sbB.toString();

                    LogUtil.logDebug("(B)FINISH End position: " + endB);
                    substringB = subMessageContent.substring(0, endB);

                    mMultiQuote = pMultiQuote.matcher(substringB);

                    if (mMultiQuote != null && mMultiQuote.find()) {
                        LogUtil.logDebug("Multiquote");
                        applyTwoFormatsAndMultiQuoteFormat(substringB);
                    }
                    else{
                        Matcher mQuote = pQuote.matcher(substringB);

                        if (mQuote != null && mQuote.find()) {
                            LogUtil.logDebug("Quote");
                            applyTwoFormatsAndQuoteFormat(substringB);
                        }
                        else{
                            ssb.append(substringB, new StyleSpan(Typeface.BOLD), new StyleSpan(Typeface.ITALIC));
                        }
                    }

                    sbB = new StringBuilder(subMessageContent);
                    sbB.delete(0, endB);
                    subMessageContent = sbB.toString();
                    break;
                }
                else{
                    LogUtil.logDebug("endB position: " + endB);
                    sbB = new StringBuilder(subMessageContent);
                    sbB.deleteCharAt(endB);
                    subMessageContent = sbB.toString();

                    substringB = subMessageContent.substring(0, endB);

                    mMultiQuote = pMultiQuote.matcher(substringB);

                    if (mMultiQuote != null && mMultiQuote.find()) {
                        LogUtil.logDebug("Multiquote");
                        applyTwoFormatsAndMultiQuoteFormat(substringB);
                    }
                    else{
                        Matcher mQuote = pQuote.matcher(substringB);

                        if (mQuote != null && mQuote.find()) {
                            LogUtil.logDebug("Quote");
                            applyTwoFormatsAndQuoteFormat(substringB);
                        }
                        else{
                            ssb.append(substringB, new StyleSpan(Typeface.BOLD), new StyleSpan(Typeface.ITALIC));
                        }
                    }

                    sbB = new StringBuilder(subMessageContent);
                    sbB.delete(0, endB);
                    subMessageContent = sbB.toString();

                    startB = subMessageContent.indexOf(" _");
                }
            }
        }

        if(!subMessageContent.isEmpty()){
            LogUtil.logDebug("(ITALICBOLD: Append more...");
            ssb.append(subMessageContent, new StyleSpan(Typeface.BOLD));
        }

        formatted = true;
        return ssb;
    }


}
