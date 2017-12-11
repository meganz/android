package mega.privacy.android.app.utils;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;

import com.vdurmont.emoji.EmojiManager;
import com.vdurmont.emoji.EmojiParser;

import java.util.List;
import java.util.regex.Pattern;

import mega.privacy.android.app.components.CustomTypefaceSpan;
import mega.privacy.android.app.components.SimpleSpanBuilder;
import mega.privacy.android.app.lollipop.megachat.chatAdapters.MegaChatLollipopAdapter;

public class RTFFormatter {

    String messageContent;
    SimpleSpanBuilder ssb = null;
    Context context;
    boolean formatted = false;
    boolean recursive = false;
    Typeface font;

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

        log("setRTFFormat: "+messageContent);
        formatted = false;

        if(!messageContent.isEmpty()){
            String noEmojisContent = EmojiParser.removeAllEmojis(messageContent);

            font = Typeface.createFromAsset(context.getAssets(), "font/RobotoMono-Regular.ttf");

            boolean  multiquote = Pattern.matches("(.*\\s+)*```.*```(\\s+.*)*[\\?|!|\\.|,|;|:|\\)|%]*", noEmojisContent);
//                                boolean  italic = Pattern.matches(".*_.*_.*", messageContent);
            if(multiquote) {

                boolean bold = Pattern.matches("(.*\\s+)*\\*.*\\*(\\s+.*)*[\\?|!|\\.|,|;|:|\\)|%]*", noEmojisContent);
//                                boolean  italic = Pattern.matches(".*_.*_.*", messageContent);
                int startBold = -1;
                int startMultiquote = -1;
                if (bold) {
                    startBold = messageContent.indexOf(("*"));
                    startMultiquote = messageContent.indexOf(("```"));
                    if (startMultiquote < startBold) {
                        applyMultiQuoteFormat();
                        formatted = true;
                        return ssb;
                    }
                }

                boolean  italic = Pattern.matches("(.*\\s+)*_.*_(\\s+.*)*[\\?|!|\\.|,|;|:|\\)|%]*", noEmojisContent);
                int startItalic = -1;
                if(italic) {
                    startItalic = messageContent.indexOf(("_"));
                    startMultiquote = messageContent.indexOf(("```"));
                    if (startMultiquote < startItalic) {
                        applyMultiQuoteFormat();
                        formatted = true;
                        return ssb;
                    }
                }

                if(!bold && !italic){
                    applyMultiQuoteFormat();
                    formatted = true;
                    return ssb;
                }
            }

            boolean  quote = Pattern.matches("(.*\\s+)*`.*`(\\s+.*)*[\\?|!|\\.|,|;|:|\\)|%]*", noEmojisContent);
//                                boolean  italic = Pattern.matches(".*_.*_.*", messageContent);
            if(quote) {

                boolean bold = Pattern.matches("(.*\\s+)*\\*.*\\*(\\s+.*)*[\\?|!|\\.|,|;|:|\\)|%]*", noEmojisContent);
//                                boolean  italic = Pattern.matches(".*_.*_.*", messageContent);
                int startBold = -1;
                int startQuote = -1;
                if (bold) {
                    startBold = messageContent.indexOf(("*"));
                    startQuote = messageContent.indexOf(("`"));
                    if (startQuote < startBold) {
                        applyQuoteFormat();
                        formatted = true;
                        return ssb;
                    }
                }

                boolean  italic = Pattern.matches("(.*\\s+)*_.*_(\\s+.*)*[\\?|!|\\.|,|;|:|\\)|%]*", noEmojisContent);
                int startItalic = -1;
                if(italic) {
                    startItalic = messageContent.indexOf(("_"));
                    startQuote = messageContent.indexOf(("`"));
                    if (startQuote < startItalic) {
                        applyQuoteFormat();
                        formatted = true;
                        return ssb;
                    }
                }

                if(!bold && !italic){
                    applyQuoteFormat();
                    formatted = true;
                    return ssb;
                }
            }

            boolean  italic = Pattern.matches("(.*\\s+)*_.*_(\\s+.*)*[\\?|!|\\.|,|;|:|\\)|%]*", noEmojisContent);
//                                boolean  italic = Pattern.matches(".*_.*_.*", messageContent);
            if(italic) {
                boolean bold = Pattern.matches("(.*\\s+)*\\*.*\\*(\\s+.*)*[\\?|!|\\.|,|;|:|\\)|%]*", noEmojisContent);
//                                boolean  italic = Pattern.matches(".*_.*_.*", messageContent);
                int startBold = -1;
                int startItalic = -1;
                if (bold) {
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

            if(!messageContent.isEmpty()){
                boolean bold = Pattern.matches("(.*\\s+)*\\*.*\\*(\\s+.*)*[\\?|!|\\.|,|;|:|\\)|%]*(\\s+.*)*", noEmojisContent);
//                                boolean  italic = Pattern.matches(".*_.*_.*", messageContent);
                if(bold){
                    applyBoldFormat();
                    formatted = true;
                    return ssb;
                }
            }

            boolean  quote2 = Pattern.matches("(.*\\s+)*`.*`(\\s+.*)*[\\?|!|\\.|,|;|:|\\)|%|\\*]*", noEmojisContent);
//                                boolean  italic = Pattern.matches(".*_.*_.*", messageContent);
            if(quote2) {
                applyQuoteFormat();
                formatted = true;
                return ssb;
            }
        }

        formatted = false;
        return ssb;
    }

    private static void log(String log) {
        Util.log("RTFFormatter", log);
    }

    public void applyMultiQuoteFormat(){
        log("applyMultiQuoteFormat");

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
            start = messageContent.indexOf(" ```");

            if(start==-1){
                log("Check if there is emoji at the beginning of the string");
                start = messageContent.indexOf("```");
                String emoji = messageContent.substring(0, start);
                if(EmojiManager.isEmoji(emoji)){
                    log("The first element is emoji");
                    substring = messageContent.substring(0, start);
                    ssb.append(substring);

                    StringBuilder sb = new StringBuilder(messageContent);
                    sb.delete(0, start+3);
                    sb.insert(0, '\n');
                    messageContent = sb.toString();
                }
            }
            else{
                start++;
                substring = messageContent.substring(0, start);
                ssb.append(substring);

                StringBuilder sb = new StringBuilder(messageContent);
                sb.delete(0, start+3);
                sb.insert(0, '\n');
                messageContent = sb.toString();
            }
        }

        log("Message content: "+messageContent);
        end = messageContent.indexOf("``` ");
        if(end==-1){
            end = messageContent.lastIndexOf("```");
            log("FINISH End position: "+end);

            StringBuilder sb = new StringBuilder(messageContent);
            sb.delete(end, end+3);
            messageContent = sb.toString();

            log("Message content: "+messageContent);

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
        }
        else{
            log("End position: "+end);
            StringBuilder sb = new StringBuilder(messageContent);
            sb.delete(end, end+3);
            messageContent = sb.toString();

            log("Message content B: "+messageContent);
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

            log("Message content T: "+messageContent);
        }

        setRTFFormat();
        while(formatted){
            setRTFFormat();
        }
        if(!messageContent.isEmpty()){
            log("Append more...");
//            StringBuilder sbBMultiQuote = new StringBuilder(messageContent);
//            sbBMultiQuote.insert(0, '\n');
//            messageContent = sbBMultiQuote.toString();
            ssb.append(messageContent);
            messageContent ="";
        }
    }

    public void applyQuoteFormat(){
        log("applyQuoteFormat");

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
                log("Check if there is emoji at the beginning of the string");
                start = messageContent.indexOf("`");
                String emoji = messageContent.substring(0, start);
                if(EmojiManager.isEmoji(emoji)){
                    log("The first element is emoji");
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

                int startPrevious = messageContent.indexOf("`");
                if(startPrevious<start)
                {
                    String emoji = messageContent.substring(0, startPrevious);
                    if(EmojiManager.isEmoji(emoji)){
                        log("The first element is emoji");
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

        log("Message content: "+messageContent);
        end = messageContent.indexOf("` ");
        if(end==-1){
            end = messageContent.lastIndexOf("`");
            log("FINISH End position: "+end);

            StringBuilder sb = new StringBuilder(messageContent);
            sb.deleteCharAt(end);
            messageContent = sb.toString();

            log("Message content: "+messageContent);

            substring = messageContent.substring(0, end);

            ssb.append(substring, new CustomTypefaceSpan("", font));

            sb = new StringBuilder(messageContent);
            sb.delete(0, end);
//            sb.insert(0, '\n');
            messageContent = sb.toString();
        }
        else{
            log("End position: "+end);
            StringBuilder sb = new StringBuilder(messageContent);
            sb.deleteCharAt(end);
            messageContent = sb.toString();

            log("Message content B: "+messageContent);
            substring = messageContent.substring(0, end);

            ssb.append(substring, new CustomTypefaceSpan("", font));

            sb = new StringBuilder(messageContent);
            sb.delete(0, end);
            messageContent = sb.toString();

            log("Message content T: "+messageContent);

            start = messageContent.indexOf(" `");
            while(start!=-1){

                start = start +1;

                sb = new StringBuilder(messageContent);
                sb.deleteCharAt(start);
                messageContent = sb.toString();

                log("(B) Start position: "+start);
                substring = messageContent.substring(0, start);
                ssb.append(substring);

                sb = new StringBuilder(messageContent);
                sb.delete(0, start);
                messageContent = sb.toString();

                log("Message content C: "+messageContent);
                end = messageContent.indexOf("` ");
                if(end==-1){
                    end = messageContent.lastIndexOf("`");

                    sb = new StringBuilder(messageContent);
                    sb.deleteCharAt(end);
                    messageContent = sb.toString();

                    log("(B)FINISH End position: "+end);
                    substring = messageContent.substring(0, end);
                    ssb.append(substring, new CustomTypefaceSpan("", font));

                    sb = new StringBuilder(messageContent);
                    sb.delete(0, end);
                    messageContent = sb.toString();

                    break;
                }
                else{
                    log("End position: "+end);
                    sb = new StringBuilder(messageContent);
                    sb.deleteCharAt(end);
                    messageContent = sb.toString();
                    log("Message content D: "+messageContent);

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
            log("more to append...");
            ssb.append(messageContent);
            messageContent ="";
        }
    }

    public void applyItalicFormat(){
        char a = messageContent.charAt(0);
        int start;
        int end;

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

                    String noEmojisContent = EmojiParser.removeAllEmojis(substring);

                    if(Pattern.matches("(.*\\s+)*```.*```(\\s+.*)*[\\?|!|\\.|,|;|:|\\)|%]*", noEmojisContent)){
                        log("Multiquote");
                        applyTwoFormatsAndMultiQuoteFormat(substring);
                    }
                    else if(Pattern.matches("(.*\\s+)*`.*`(\\s+.*)*[\\?|!|\\.|,|;|:|\\)|%]*", noEmojisContent)){
                        log("Quote");
                        applyTwoFormatsAndQuoteFormat(substring);
                    }
                    else{
                        ssb.append(substring, new StyleSpan(Typeface.BOLD), new StyleSpan(Typeface.ITALIC));
                    }
                }
                else{
                    end = messageContent.indexOf("*");

                    substring = messageContent.substring(0, end);

                    String noEmojisContent = EmojiParser.removeAllEmojis(substring);

                    if(Pattern.matches("(.*\\s+)*```.*```(\\s+.*)*[\\?|!|\\.|,|;|:|\\)|%]*", noEmojisContent)){
                        log("Multiquote");
                        applyTwoFormatsAndMultiQuoteFormat(substring);
                    }
                    else if(Pattern.matches("(.*\\s+)*`.*`(\\s+.*)*[\\?|!|\\.|,|;|:|\\)|%]*", noEmojisContent)){
                        log("Quote");
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
                log("Check if there is emoji at the beginning of the string");
                start = messageContent.indexOf("_");
                String emoji = messageContent.substring(0, start);
                if(EmojiManager.isEmoji(emoji)){
                    log("The first element is emoji");
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
                int startPrevious = messageContent.indexOf("_");
                if(startPrevious<start)
                {
                    String emoji = messageContent.substring(0, startPrevious);
                    if(EmojiManager.isEmoji(emoji)){
                        log("The first element is emoji");
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

        log("Message content: "+messageContent);
        end = messageContent.indexOf("_ ");
        if(end==-1){
            end = messageContent.lastIndexOf("_");
            log("FINISH End position: "+end);

            StringBuilder sb = new StringBuilder(messageContent);
            sb.deleteCharAt(end);
            messageContent = sb.toString();

            log("Message content: "+messageContent);

            substring = messageContent.substring(0, end);

            String noEmojisContent = EmojiParser.removeAllEmojis(substring);

            boolean bold = Pattern.matches("(.*\\s+)*\\*.*\\*(\\s+.*)*[\\?|!|\\.|,|;|:|\\)|%]*", noEmojisContent);
//                                boolean  italic = Pattern.matches(".*_.*_.*", messageContent);
            if(bold){
                applyItalicBoldFormat(substring);
            }
            else if(Pattern.matches("(.*\\s+)*```.*```(\\s+.*)*[\\?|!|\\.|,|;|:|\\)|%]*", noEmojisContent)){
                log("Multiquote");
                applyOneFormatAndMultiQuoteFormat(substring, Typeface.ITALIC);
            }
            else if(Pattern.matches("(.*\\s+)*`.*`(\\s+.*)*[\\?|!|\\.|,|;|:|\\)|%]*", noEmojisContent)){
                log("Quote");
                applyOneFormatAndQuoteFormat(substring, Typeface.ITALIC);
            }
            else{
                ssb.append(substring, new StyleSpan(Typeface.ITALIC));
            }

            sb = new StringBuilder(messageContent);
            sb.delete(0, end);
            messageContent = sb.toString();
        }
        else{
            log("End position: "+end);
            StringBuilder sb = new StringBuilder(messageContent);
            sb.deleteCharAt(end);
            messageContent = sb.toString();

            log("Message content B: "+messageContent);
            substring = messageContent.substring(0, end);

            String noEmojisContent = EmojiParser.removeAllEmojis(substring);

            boolean bold = Pattern.matches("(.*\\s+)*\\*.*\\*(\\s+.*)*[\\?|!|\\.|,|;|:|\\)|%]*", noEmojisContent);
//                                boolean  italic = Pattern.matches(".*_.*_.*", messageContent);
            if(bold){
                applyItalicBoldFormat(substring);
            }
            else if(Pattern.matches("(.*\\s+)*```.*```(\\s+.*)*[\\?|!|\\.|,|;|:|\\)|%]*", noEmojisContent)){
                log("Multiquote");
                applyOneFormatAndMultiQuoteFormat(substring, Typeface.ITALIC);
            }
            else if(Pattern.matches("(.*\\s+)*`.*`(\\s+.*)*[\\?|!|\\.|,|;|:|\\)|%]*", noEmojisContent)){
                log("Quote");
                applyOneFormatAndQuoteFormat(substring, Typeface.ITALIC);
            }
            else{
                ssb.append(substring, new StyleSpan(Typeface.ITALIC));
            }

            sb = new StringBuilder(messageContent);
            sb.delete(0, end);
            messageContent = sb.toString();

            log("Message content T: "+messageContent);

            start = messageContent.indexOf(" _");
            while(start!=-1){

                start = start +1;

                sb = new StringBuilder(messageContent);
                sb.deleteCharAt(start);
                messageContent = sb.toString();

                log("(B) Start position: "+start);
                substring = messageContent.substring(0, start);
                ssb.append(substring);

                sb = new StringBuilder(messageContent);
                sb.delete(0, start);
                messageContent = sb.toString();

                log("Message content C: "+messageContent);
                end = messageContent.indexOf("_ ");
                if(end==-1){
                    end = messageContent.lastIndexOf("_");

                    sb = new StringBuilder(messageContent);
                    sb.deleteCharAt(end);
                    messageContent = sb.toString();

                    log("(B)FINISH End position: "+end);
                    substring = messageContent.substring(0, end);
                    ssb.append(substring, new StyleSpan(Typeface.ITALIC));

                    sb = new StringBuilder(messageContent);
                    sb.delete(0, end);
                    messageContent = sb.toString();

                    break;
                }
                else{
                    log("End position: "+end);
                    sb = new StringBuilder(messageContent);
                    sb.deleteCharAt(end);
                    messageContent = sb.toString();
                    log("Message content D: "+messageContent);

                    substring = messageContent.substring(0, end);
                    ssb.append(substring, new StyleSpan(Typeface.ITALIC));

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
            log("more to append...");
            ssb.append(messageContent);
            messageContent ="";
        }

    }

    public SimpleSpanBuilder applyItalicBoldFormat(String subMessageContent){
        log("applyItalicBoldFormat: "+subMessageContent);

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
            log("SubstringB is: "+substringB);
            ssb.append(substringB, new StyleSpan(Typeface.ITALIC));

            startB++;
            StringBuilder sb = new StringBuilder(subMessageContent);
            sb.delete(0, startB);
            subMessageContent = sb.toString();
            log("(8) messageContent: "+subMessageContent);
        }

        endB = subMessageContent.indexOf("* ");
        if(endB==-1){
            endB = subMessageContent.lastIndexOf("*");
            log("FINISH endB position: "+endB);

            StringBuilder sbB = new StringBuilder(subMessageContent);
            sbB.deleteCharAt(endB);
            subMessageContent = sbB.toString();

            log("(9) messageContent: "+subMessageContent);

            substringB = subMessageContent.substring(0, endB);

            log("SubstringB is: "+substringB);

            String noEmojisContent = EmojiParser.removeAllEmojis(substringB);

            if(Pattern.matches("(.*\\s+)*```.*```(\\s+.*)*[\\?|!|\\.|,|;|:|\\)|%]*", noEmojisContent)){
                log("Multiquote");
                applyTwoFormatsAndMultiQuoteFormat(substringB);
            }
            else if(Pattern.matches("(.*\\s+)*`.*`(\\s+.*)*[\\?|!|\\.|,|;|:|\\)|%]*", noEmojisContent)){
                log("Quote");
                applyTwoFormatsAndQuoteFormat(substringB);
            }
            else{
                ssb.append(substringB, new StyleSpan(Typeface.BOLD), new StyleSpan(Typeface.ITALIC));
            }

            sbB = new StringBuilder(subMessageContent);
            sbB.delete(0, endB);
            subMessageContent = sbB.toString();
        }
        else{
            log("endB position: "+endB);
            log("(10) Message content B: "+subMessageContent);
            substringB = subMessageContent.substring(0, endB);

            String noEmojisContent = EmojiParser.removeAllEmojis(substringB);

            if(Pattern.matches("(.*\\s+)*```.*```(\\s+.*)*[\\?|!|\\.|,|;|:|\\)|%]*", noEmojisContent)){
                log("Multiquote");
                applyTwoFormatsAndMultiQuoteFormat(substringB);
            }
            else if(Pattern.matches("(.*\\s+)*`.*`(\\s+.*)*[\\?|!|\\.|,|;|:|\\)|%]*", noEmojisContent)){
                log("Quote");
                applyTwoFormatsAndQuoteFormat(substringB);
            }
            else{
                ssb.append(substringB, new StyleSpan(Typeface.BOLD), new StyleSpan(Typeface.ITALIC));
            }

            endB++;
            StringBuilder sbB = new StringBuilder(subMessageContent);
            sbB.delete(0, endB);
            subMessageContent = sbB.toString();
            log("(11) Message content B: "+subMessageContent);

            startB = subMessageContent.indexOf(" *");
            while(startB!=-1){

                startB = startB +1;

                sbB = new StringBuilder(subMessageContent);
                sbB.deleteCharAt(startB);
                subMessageContent = sbB.toString();

                log("(B) startB position: "+startB);
                substringB = subMessageContent.substring(0, startB);
                ssb.append(substringB, new StyleSpan(Typeface.ITALIC));

                sbB = new StringBuilder(subMessageContent);
                sbB.delete(0, startB);
                subMessageContent = sbB.toString();

                log("Message content C: "+subMessageContent);
                endB = subMessageContent.indexOf("* ");
                if(endB==-1){
                    endB = subMessageContent.lastIndexOf("*");
                    log("(B)FINISH endB position: "+endB);

                    sbB = new StringBuilder(subMessageContent);
                    sbB.deleteCharAt(endB);
                    subMessageContent = sbB.toString();

                    log("(B)FINISH End position: "+endB);
                    substringB = subMessageContent.substring(0, endB);

                    noEmojisContent = EmojiParser.removeAllEmojis(substringB);

                    if(Pattern.matches("(.*\\s+)*```.*```(\\s+.*)*[\\?|!|\\.|,|;|:|\\)|%]*", noEmojisContent)){
                        log("Multiquote");
                        applyTwoFormatsAndMultiQuoteFormat(substringB);
                    }
                    else if(Pattern.matches("(.*\\s+)*`.*`(\\s+.*)*[\\?|!|\\.|,|;|:|\\)|%]*", noEmojisContent)){
                        log("Quote");
                        applyTwoFormatsAndQuoteFormat(substringB);
                    }
                    else{
                        ssb.append(substringB, new StyleSpan(Typeface.BOLD), new StyleSpan(Typeface.ITALIC));
                    }

                    sbB = new StringBuilder(subMessageContent);
                    sbB.delete(0, endB);
                    subMessageContent = sbB.toString();
                    break;
                }
                else{
                    log("endB position: "+endB);
                    sbB = new StringBuilder(subMessageContent);
                    sbB.deleteCharAt(endB);
                    subMessageContent = sbB.toString();
                    log("Message content D: "+subMessageContent);

                    substringB = subMessageContent.substring(0, endB);

                    noEmojisContent = EmojiParser.removeAllEmojis(substringB);

                    if(Pattern.matches("(.*\\s+)*```.*```(\\s+.*)*[\\?|!|\\.|,|;|:|\\)|%]*", noEmojisContent)){
                        log("Multiquote");
                        applyTwoFormatsAndMultiQuoteFormat(substringB);
                    }
                    else if(Pattern.matches("(.*\\s+)*`.*`(\\s+.*)*[\\?|!|\\.|,|;|:|\\)|%]*", noEmojisContent)){
                        log("Quote");
                        applyTwoFormatsAndQuoteFormat(substringB);
                    }
                    else{
                        ssb.append(substringB, new StyleSpan(Typeface.BOLD), new StyleSpan(Typeface.ITALIC));
                    }

                    sbB = new StringBuilder(subMessageContent);
                    sbB.delete(0, endB);
                    subMessageContent = sbB.toString();

                    startB = subMessageContent.indexOf(" *");
                }
            }
        }

        if(!subMessageContent.isEmpty()){
            log("(ITALICBOLD: Append more...");
            ssb.append(subMessageContent, new StyleSpan(Typeface.ITALIC));
        }

        formatted = true;
        return ssb;
    }

    public SimpleSpanBuilder applyTwoFormatsAndQuoteFormat(String subMessageContent){
        log("applyOneFormatAndQuoteFormat: "+subMessageContent);

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
            log("SubstringB is: "+substringB);
            ssb.append(substringB, new StyleSpan(Typeface.BOLD), new StyleSpan(Typeface.ITALIC));

            startB++;
            StringBuilder sb = new StringBuilder(subMessageContent);
            sb.delete(0, startB);
            subMessageContent = sb.toString();
            log("(8) messageContent: "+subMessageContent);
        }

        endB = subMessageContent.indexOf("` ");
        if(endB==-1){
            endB = subMessageContent.lastIndexOf("`");
            log("FINISH endB position: "+endB);

            StringBuilder sbB = new StringBuilder(subMessageContent);
            sbB.deleteCharAt(endB);
            subMessageContent = sbB.toString();

            log("(9) messageContent: "+subMessageContent);

            substringB = subMessageContent.substring(0, endB);

            log("SubstringB is: "+substringB);
            ssb.append(substringB, new CustomTypefaceSpan("", font), new StyleSpan(Typeface.BOLD), new StyleSpan(Typeface.ITALIC));

            sbB = new StringBuilder(subMessageContent);
            sbB.delete(0, endB);
            subMessageContent = sbB.toString();
        }
        else{
            log("endB position: "+endB);
            log("(10) Message content B: "+subMessageContent);
            substringB = subMessageContent.substring(0, endB);
            ssb.append(substringB, new CustomTypefaceSpan("", font), new StyleSpan(Typeface.BOLD), new StyleSpan(Typeface.ITALIC));

            endB++;
            StringBuilder sbB = new StringBuilder(subMessageContent);
            sbB.delete(0, endB);
            subMessageContent = sbB.toString();
            log("(11) Message content B: "+subMessageContent);

            startB = subMessageContent.indexOf(" `");
            while(startB!=-1){

                startB = startB +1;

                sbB = new StringBuilder(subMessageContent);
                sbB.deleteCharAt(startB);
                subMessageContent = sbB.toString();

                log("(B) startB position: "+startB);
                substringB = subMessageContent.substring(0, startB);
                ssb.append(substringB, new StyleSpan(Typeface.BOLD), new StyleSpan(Typeface.ITALIC));

                sbB = new StringBuilder(subMessageContent);
                sbB.delete(0, startB);
                subMessageContent = sbB.toString();

                log("Message content C: "+subMessageContent);
                endB = subMessageContent.indexOf("* ");
                if(endB==-1){
                    endB = subMessageContent.lastIndexOf("`");
                    log("(B)FINISH endB position: "+endB);

                    sbB = new StringBuilder(subMessageContent);
                    sbB.deleteCharAt(endB);
                    subMessageContent = sbB.toString();

                    log("(B)FINISH End position: "+endB);
                    substringB = subMessageContent.substring(0, endB);
                    ssb.append(substringB, new CustomTypefaceSpan("", font), new StyleSpan(Typeface.BOLD), new StyleSpan(Typeface.ITALIC));

                    sbB = new StringBuilder(subMessageContent);
                    sbB.delete(0, endB);
                    subMessageContent = sbB.toString();
                    break;
                }
                else{
                    log("endB position: "+endB);
                    sbB = new StringBuilder(subMessageContent);
                    sbB.deleteCharAt(endB);
                    subMessageContent = sbB.toString();
                    log("Message content D: "+subMessageContent);

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
            log("(ONEFORMATANDQuote: Append more...");
            ssb.append(subMessageContent, new StyleSpan(Typeface.BOLD), new StyleSpan(Typeface.ITALIC));
        }

        formatted = true;
        return ssb;
    }

    public SimpleSpanBuilder applyOneFormatAndQuoteFormat(String subMessageContent, int format){
        log("applyOneFormatAndQuoteFormat: "+subMessageContent);

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
            log("SubstringB is: "+substringB);
            ssb.append(substringB, new StyleSpan(format));

            startB++;
            StringBuilder sb = new StringBuilder(subMessageContent);
            sb.delete(0, startB);
            subMessageContent = sb.toString();
            log("(8) messageContent: "+subMessageContent);
        }

        endB = subMessageContent.indexOf("` ");
        if(endB==-1){
            endB = subMessageContent.lastIndexOf("`");
            log("FINISH endB position: "+endB);

            StringBuilder sbB = new StringBuilder(subMessageContent);
            sbB.deleteCharAt(endB);
            subMessageContent = sbB.toString();

            log("(9) messageContent: "+subMessageContent);

            substringB = subMessageContent.substring(0, endB);

            log("SubstringB is: "+substringB);
            ssb.append(substringB, new CustomTypefaceSpan("", font), new StyleSpan(format));

            sbB = new StringBuilder(subMessageContent);
            sbB.delete(0, endB);
            subMessageContent = sbB.toString();
        }
        else{
            log("endB position: "+endB);
            log("(10) Message content B: "+subMessageContent);
            substringB = subMessageContent.substring(0, endB);
            ssb.append(substringB, new CustomTypefaceSpan("", font), new StyleSpan(format));

            endB++;
            StringBuilder sbB = new StringBuilder(subMessageContent);
            sbB.delete(0, endB);
            subMessageContent = sbB.toString();
            log("(11) Message content B: "+subMessageContent);

            startB = subMessageContent.indexOf(" `");
            while(startB!=-1){

                startB = startB +1;

                sbB = new StringBuilder(subMessageContent);
                sbB.deleteCharAt(startB);
                subMessageContent = sbB.toString();

                log("(B) startB position: "+startB);
                substringB = subMessageContent.substring(0, startB);
                ssb.append(substringB, new StyleSpan(format));

                sbB = new StringBuilder(subMessageContent);
                sbB.delete(0, startB);
                subMessageContent = sbB.toString();

                log("Message content C: "+subMessageContent);
                endB = subMessageContent.indexOf("* ");
                if(endB==-1){
                    endB = subMessageContent.lastIndexOf("`");
                    log("(B)FINISH endB position: "+endB);

                    sbB = new StringBuilder(subMessageContent);
                    sbB.deleteCharAt(endB);
                    subMessageContent = sbB.toString();

                    log("(B)FINISH End position: "+endB);
                    substringB = subMessageContent.substring(0, endB);
                    ssb.append(substringB, new CustomTypefaceSpan("", font), new StyleSpan(format));

                    sbB = new StringBuilder(subMessageContent);
                    sbB.delete(0, endB);
                    subMessageContent = sbB.toString();
                    break;
                }
                else{
                    log("endB position: "+endB);
                    sbB = new StringBuilder(subMessageContent);
                    sbB.deleteCharAt(endB);
                    subMessageContent = sbB.toString();
                    log("Message content D: "+subMessageContent);

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
            log("(ONEFORMATANDQuote: Append more...");
            ssb.append(subMessageContent, new StyleSpan(format));
        }

        formatted = true;
        return ssb;
    }

    public SimpleSpanBuilder applyTwoFormatsAndMultiQuoteFormat(String subMessageContent){
        log("applyTwoFormatsAndMultiQuoteFormat: "+subMessageContent);
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
            log("SubstringB is: "+substringB);
            ssb.append(substringB, new StyleSpan(Typeface.BOLD), new StyleSpan(Typeface.ITALIC));

            StringBuilder sb = new StringBuilder(subMessageContent);
            sb.delete(0, startB+3);
            sb.insert(0, '\n');
            subMessageContent = sb.toString();
            log("(8) messageContent: "+subMessageContent);
        }

        endB = subMessageContent.indexOf("``` ");
        if(endB==-1){
            endB = subMessageContent.lastIndexOf("```");
            log("FINISH endB position: "+endB);

            StringBuilder sbB = new StringBuilder(subMessageContent);
            sbB.delete(endB, endB+3);

            subMessageContent = sbB.toString();

            log("(9) messageContent: "+subMessageContent);

            substringB = subMessageContent.substring(0, endB);
            log("SubstringB is: "+substringB);
            ssb.append(substringB, new CustomTypefaceSpan("", font), new StyleSpan(Typeface.BOLD), new StyleSpan(Typeface.ITALIC));

            sbB = new StringBuilder(subMessageContent);
            sbB.delete(0, endB+3);
            subMessageContent = sbB.toString();
        }
        else{
            log("endB position: "+endB);
            log("(10) Message content B: "+subMessageContent);
            substringB = subMessageContent.substring(0, endB);

            ssb.append(substringB, new CustomTypefaceSpan("", font), new StyleSpan(Typeface.BOLD), new StyleSpan(Typeface.ITALIC));
            endB++;
            StringBuilder sbB = new StringBuilder(subMessageContent);
            sbB.delete(0, endB+3);
            subMessageContent = sbB.toString();
            log("(11) Message content B: "+subMessageContent);
        }

        if(!subMessageContent.isEmpty()){
            log("(ITALICMULTIQUOTE: Append more...");
            StringBuilder sbBMultiQuote = new StringBuilder('\n'+subMessageContent);
//            sbBMultiQuote.insert(0, '\n');
            subMessageContent = sbBMultiQuote.toString();
            ssb.append(subMessageContent, new StyleSpan(Typeface.BOLD), new StyleSpan(Typeface.ITALIC));
        }

        formatted = true;
        return ssb;
    }

    public SimpleSpanBuilder applyOneFormatAndMultiQuoteFormat(String subMessageContent, int format){
        log("applyOneFormatAndMultiQuoteFormat: "+subMessageContent);
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
            log("SubstringB is: "+substringB);
            ssb.append(substringB, new StyleSpan(format));

            StringBuilder sb = new StringBuilder(subMessageContent);
            sb.delete(0, startB+3);
            sb.insert(0, '\n');
            subMessageContent = sb.toString();
            log("(8) messageContent: "+subMessageContent);
        }

        endB = subMessageContent.indexOf("``` ");
        if(endB==-1){
            endB = subMessageContent.lastIndexOf("```");
            log("FINISH endB position: "+endB);

            StringBuilder sbB = new StringBuilder(subMessageContent);
            sbB.delete(endB, endB+3);

            subMessageContent = sbB.toString();

            log("(9) messageContent: "+subMessageContent);

            substringB = subMessageContent.substring(0, endB);
            log("SubstringB is: "+substringB);
            ssb.append(substringB, new CustomTypefaceSpan("", font), new StyleSpan(format));

            sbB = new StringBuilder(subMessageContent);
            sbB.delete(0, endB+3);
            subMessageContent = sbB.toString();
        }
        else{
            log("endB position: "+endB);
            log("(10) Message content B: "+subMessageContent);
            substringB = subMessageContent.substring(0, endB);
//            ssb.append(substringB, new StyleSpan(typeFace.getStyle()), new StyleSpan(Typeface.ITALIC));

            ssb.append(substringB, new CustomTypefaceSpan("", font), new StyleSpan(format));
            endB++;
            StringBuilder sbB = new StringBuilder(subMessageContent);
            sbB.delete(0, endB+3);
            subMessageContent = sbB.toString();
            log("(11) Message content B: "+subMessageContent);
        }

        if(!subMessageContent.isEmpty()){
            log("(ITALICMULTIQUOTE: Append more...");
            StringBuilder sbBMultiQuote = new StringBuilder(subMessageContent);
            sbBMultiQuote.insert(0, '\n');
            subMessageContent = sbBMultiQuote.toString();
            ssb.append(subMessageContent, new StyleSpan(format));
        }

        formatted = true;
        return ssb;
    }

    public void applyBoldFormat(){
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

                    String noEmojisContent = EmojiParser.removeAllEmojis(substring);

                    if(Pattern.matches("(.*\\s+)*```.*```(\\s+.*)*[\\?|!|\\.|,|;|:|\\)|%]*", noEmojisContent)){
                        log("Multiquote");
                        applyTwoFormatsAndMultiQuoteFormat(substring);
                    }
                    else if(Pattern.matches("(.*\\s+)*`.*`(\\s+.*)*[\\?|!|\\.|,|;|:|\\)|%]*", noEmojisContent)){
                        log("Quote");
                        applyTwoFormatsAndQuoteFormat(substring);
                    }
                    else{
                        ssb.append(substring, new StyleSpan(Typeface.BOLD), new StyleSpan(Typeface.ITALIC));
                    }
                }
                else{
                    end = messageContent.indexOf("_");

                    substring = messageContent.substring(0, end);

                    String noEmojisContent = EmojiParser.removeAllEmojis(substring);

                    if(Pattern.matches("(.*\\s+)*```.*```(\\s+.*)*[\\?|!|\\.|,|;|:|\\)|%]*", noEmojisContent)){
                        log("Multiquote");
                        applyTwoFormatsAndMultiQuoteFormat(substring);
                    }
                    else if(Pattern.matches("(.*\\s+)*`.*`(\\s+.*)*[\\?|!|\\.|,|;|:|\\)|%]*", noEmojisContent)){
                        log("Quote");
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
                log("Check if there is emoji at the beginning of the string");
                start = messageContent.indexOf("*");
                String emoji = messageContent.substring(0, start);
                if(EmojiManager.isEmoji(emoji)){
                    log("The first element is emoji");
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
                        log("The first element is emoji");
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

        log("Message content: "+messageContent);
        end = messageContent.indexOf("* ");
        if(end==-1){
            end = messageContent.lastIndexOf("*");
            log("FINISH End position: "+end);

            StringBuilder sb = new StringBuilder(messageContent);
            sb.deleteCharAt(end);
            messageContent = sb.toString();

            log("Message content: "+messageContent);

            substring = messageContent.substring(0, end);

            String noEmojisContent = EmojiParser.removeAllEmojis(substring);

            boolean  italic = Pattern.matches("(.*\\s+)*_.*_(\\s+.*)*[\\?|!|\\.|,|;|:|\\)|%]*", noEmojisContent);
//                                boolean  italic = Pattern.matches(".*_.*_.*", messageContent);
            if(italic){
                applyBoldItalicFormat(substring);
            }
            else if(Pattern.matches("(.*\\s+)*```.*```(\\s+.*)*[\\?|!|\\.|,|;|:|\\)|%]*", noEmojisContent)){
                log("Multiquote");
                applyOneFormatAndMultiQuoteFormat(substring, Typeface.BOLD);
            }
            else if(Pattern.matches("(.*\\s+)*`.*`(\\s+.*)*[\\?|!|\\.|,|;|:|\\)|%]*", noEmojisContent)){
                log("Quote");
                applyOneFormatAndQuoteFormat(substring, Typeface.BOLD);
            }
            else{
                ssb.append(substring, new StyleSpan(Typeface.BOLD));
            }

            sb = new StringBuilder(messageContent);
            sb.delete(0, end);
            messageContent = sb.toString();
        }
        else{
            log("End position: "+end);
            StringBuilder sb = new StringBuilder(messageContent);
            sb.deleteCharAt(end);
            messageContent = sb.toString();

            log("Message content B: "+messageContent);
            substring = messageContent.substring(0, end);

            String noEmojisContent = EmojiParser.removeAllEmojis(substring);

            boolean  italic = Pattern.matches("(.*\\s+)*_.*_(\\s+.*)*[\\?|!|\\.|,|;|:|\\)|%]*", noEmojisContent);
//                                boolean  italic = Pattern.matches(".*_.*_.*", messageContent);
            if(italic){
                applyBoldItalicFormat(substring);
            }
            else if(Pattern.matches("(.*\\s+)*```.*```(\\s+.*)*[\\?|!|\\.|,|;|:|\\)|%]*", noEmojisContent)){
                log("Multiquote");
                applyOneFormatAndMultiQuoteFormat(substring, Typeface.BOLD);
            }
            else if(Pattern.matches("(.*\\s+)*`.*`(\\s+.*)*[\\?|!|\\.|,|;|:|\\)|%]*", noEmojisContent)){
                log("Quote");
                applyOneFormatAndQuoteFormat(substring, Typeface.BOLD);
            }
            else{
                ssb.append(substring, new StyleSpan(Typeface.BOLD));
            }

            sb = new StringBuilder(messageContent);
            sb.delete(0, end);
            messageContent = sb.toString();

            log("Message content T: "+messageContent);

            start = messageContent.indexOf(" *");
            while(start!=-1){

                start = start +1;

                sb = new StringBuilder(messageContent);
                sb.deleteCharAt(start);
                messageContent = sb.toString();

                log("(B) Start position: "+start);
                substring = messageContent.substring(0, start);
                ssb.append(substring);

                sb = new StringBuilder(messageContent);
                sb.delete(0, start);
                messageContent = sb.toString();

                log("Message content C: "+messageContent);
                end = messageContent.indexOf("* ");
                if(end==-1){
                    end = messageContent.lastIndexOf("*");

                    sb = new StringBuilder(messageContent);
                    sb.deleteCharAt(end);
                    messageContent = sb.toString();

                    log("(B)FINISH End position: "+end);
                    substring = messageContent.substring(0, end);
                    ssb.append(substring, new StyleSpan(Typeface.BOLD));

                    sb = new StringBuilder(messageContent);
                    sb.delete(0, end);
                    messageContent = sb.toString();

                    break;
                }
                else{
                    log("End position: "+end);
                    sb = new StringBuilder(messageContent);
                    sb.deleteCharAt(end);
                    messageContent = sb.toString();
                    log("Message content D: "+messageContent);

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
            log("more to append...");
            ssb.append(messageContent);
            messageContent ="";
        }
    }

    public SimpleSpanBuilder applyBoldItalicFormat(String subMessageContent){
        log("applyBoldItalicFormat: "+subMessageContent);

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
            log("SubstringB is: "+substringB);
            ssb.append(substringB, new StyleSpan(Typeface.BOLD));

            startB++;
            StringBuilder sb = new StringBuilder(subMessageContent);
            sb.delete(0, startB);
            subMessageContent = sb.toString();
            log("(8) messageContent: "+subMessageContent);
        }

        endB = subMessageContent.indexOf("_ ");
        if(endB==-1){
            endB = subMessageContent.lastIndexOf("_");
            log("FINISH endB position: "+endB);

            StringBuilder sbB = new StringBuilder(subMessageContent);
            sbB.deleteCharAt(endB);
            subMessageContent = sbB.toString();

            log("(9) messageContent: "+subMessageContent);

            substringB = subMessageContent.substring(0, endB);

            log("SubstringB is: "+substringB);
            String noEmojisContent = EmojiParser.removeAllEmojis(substringB);

            if(Pattern.matches("(.*\\s+)*```.*```(\\s+.*)*[\\?|!|\\.|,|;|:|\\)|%]*", noEmojisContent)){
                log("Multiquote");
                applyTwoFormatsAndMultiQuoteFormat(substringB);
            }
            else if(Pattern.matches("(.*\\s+)*`.*`(\\s+.*)*[\\?|!|\\.|,|;|:|\\)|%]*", noEmojisContent)){
                log("Quote");
                applyTwoFormatsAndQuoteFormat(substringB);
            }
            else{
                ssb.append(substringB, new StyleSpan(Typeface.BOLD), new StyleSpan(Typeface.ITALIC));
            }

            sbB = new StringBuilder(subMessageContent);
            sbB.delete(0, endB);
            subMessageContent = sbB.toString();
        }
        else{
            log("endB position: "+endB);
            log("(10) Message content B: "+subMessageContent);
            substringB = subMessageContent.substring(0, endB);

            String noEmojisContent = EmojiParser.removeAllEmojis(substringB);

            if(Pattern.matches("(.*\\s+)*```.*```(\\s+.*)*[\\?|!|\\.|,|;|:|\\)|%]*", noEmojisContent)){
                log("Multiquote");
                applyTwoFormatsAndMultiQuoteFormat(substringB);
            }
            else if(Pattern.matches("(.*\\s+)*`.*`(\\s+.*)*[\\?|!|\\.|,|;|:|\\)|%]*", noEmojisContent)){
                log("Quote");
                applyTwoFormatsAndQuoteFormat(substringB);
            }
            else{
                ssb.append(substringB, new StyleSpan(Typeface.BOLD), new StyleSpan(Typeface.ITALIC));
            }

            endB++;
            StringBuilder sbB = new StringBuilder(subMessageContent);
            sbB.delete(0, endB);
            subMessageContent = sbB.toString();
            log("(11) Message content B: "+subMessageContent);

            startB = subMessageContent.indexOf(" _");
            while(startB!=-1){

                startB = startB +1;

                sbB = new StringBuilder(subMessageContent);
                sbB.deleteCharAt(startB);
                subMessageContent = sbB.toString();

                log("(B) startB position: "+startB);
                substringB = subMessageContent.substring(0, startB);
                ssb.append(substringB, new StyleSpan(Typeface.BOLD));

                sbB = new StringBuilder(subMessageContent);
                sbB.delete(0, startB);
                subMessageContent = sbB.toString();

                log("Message content C: "+subMessageContent);
                endB = subMessageContent.indexOf("_ ");
                if(endB==-1){
                    endB = subMessageContent.lastIndexOf("_");
                    log("(B)FINISH endB position: "+endB);

                    sbB = new StringBuilder(subMessageContent);
                    sbB.deleteCharAt(endB);
                    subMessageContent = sbB.toString();

                    log("(B)FINISH End position: "+endB);
                    substringB = subMessageContent.substring(0, endB);

                    noEmojisContent = EmojiParser.removeAllEmojis(substringB);

                    if(Pattern.matches("(.*\\s+)*```.*```(\\s+.*)*[\\?|!|\\.|,|;|:|\\)|%]*", noEmojisContent)){
                        log("Multiquote");
                        applyTwoFormatsAndMultiQuoteFormat(substringB);
                    }
                    else if(Pattern.matches("(.*\\s+)*`.*`(\\s+.*)*[\\?|!|\\.|,|;|:|\\)|%]*", noEmojisContent)){
                        log("Quote");
                        applyTwoFormatsAndQuoteFormat(substringB);
                    }
                    else{
                        ssb.append(substringB, new StyleSpan(Typeface.BOLD), new StyleSpan(Typeface.ITALIC));
                    }

                    sbB = new StringBuilder(subMessageContent);
                    sbB.delete(0, endB);
                    subMessageContent = sbB.toString();
                    break;
                }
                else{
                    log("endB position: "+endB);
                    sbB = new StringBuilder(subMessageContent);
                    sbB.deleteCharAt(endB);
                    subMessageContent = sbB.toString();
                    log("Message content D: "+subMessageContent);

                    substringB = subMessageContent.substring(0, endB);

                    noEmojisContent = EmojiParser.removeAllEmojis(substringB);

                    if(Pattern.matches("(.*\\s+)*```.*```(\\s+.*)*[\\?|!|\\.|,|;|:|\\)|%]*", noEmojisContent)){
                        log("Multiquote");
                        applyTwoFormatsAndMultiQuoteFormat(substringB);
                    }
                    else if(Pattern.matches("(.*\\s+)*`.*`(\\s+.*)*[\\?|!|\\.|,|;|:|\\)|%]*", noEmojisContent)){
                        log("Quote");
                        applyTwoFormatsAndQuoteFormat(substringB);
                    }
                    else{
                        ssb.append(substringB, new StyleSpan(Typeface.BOLD), new StyleSpan(Typeface.ITALIC));
                    }

                    sbB = new StringBuilder(subMessageContent);
                    sbB.delete(0, endB);
                    subMessageContent = sbB.toString();

                    startB = subMessageContent.indexOf(" _");
                }
            }
        }

        if(!subMessageContent.isEmpty()){
            log("(ITALICBOLD: Append more...");
            ssb.append(subMessageContent, new StyleSpan(Typeface.BOLD));
        }

        formatted = true;
        return ssb;
    }


}
