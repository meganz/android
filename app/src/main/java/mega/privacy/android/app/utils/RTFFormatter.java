package mega.privacy.android.app.utils;

import android.graphics.Typeface;
import android.text.style.StyleSpan;

import com.vdurmont.emoji.EmojiManager;
import com.vdurmont.emoji.EmojiParser;

import java.util.regex.Pattern;

import mega.privacy.android.app.components.SimpleSpanBuilder;

public class RTFFormatter {

    String messageContent;
    SimpleSpanBuilder ssb = null;

    public RTFFormatter(String messageContent) {
        this.messageContent = messageContent;
    }

    public SimpleSpanBuilder setRTFFormat(){

        log("setRTFFormat: "+messageContent);

        String noEmojisContent = EmojiParser.removeAllEmojis(messageContent);

        boolean  italic = Pattern.matches("(.*\\s+)*_.*_(\\s+.*)*", noEmojisContent);
//                                boolean  italic = Pattern.matches(".*_.*_.*", messageContent);
        if(italic){
            char a = messageContent.charAt(0);
            int start;
            int end;

            if(ssb==null){
                ssb = new SimpleSpanBuilder();
            }

            String substring = null;

            if(a =='_'){
                start = 0;

                //Check if the next one is *
                if(messageContent.charAt(1)=='*'){
                    StringBuilder sb = new StringBuilder(messageContent);
                    sb.delete(0,2);
                    messageContent = sb.toString();

                    end = messageContent.indexOf("*_");
                    substring = messageContent.substring(0, end);
                    ssb.append(substring, new StyleSpan(Typeface.BOLD), new StyleSpan(Typeface.ITALIC));

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

                boolean bold = Pattern.matches("(.*\\s+)*\\*.*\\*(\\s+.*)*", substring);
//                                boolean  italic = Pattern.matches(".*_.*_.*", messageContent);
                if(bold){
                    applyItalicBoldFormat(substring);
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

                boolean bold = Pattern.matches("(.*\\s+)*\\*.*\\*(\\s+.*)*", substring);
//                                boolean  italic = Pattern.matches(".*_.*_.*", messageContent);
                if(bold){
                    applyItalicBoldFormat(substring);
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
                        messageContent =  messageContent.replaceFirst("_ ", " ");
                        log("Message content D: "+messageContent);

                        substring = messageContent.substring(start, end);
                        ssb.append(substring, new StyleSpan(Typeface.ITALIC));

                        sb = new StringBuilder(messageContent);
                        sb.delete(start, end);
                        messageContent = sb.toString();

                        start = messageContent.indexOf(" _");
                    }
                }
            }

            if(!messageContent.isEmpty()){
                log("Append more...");
                ssb.append(messageContent);
            }
            else{
                log("End value: "+end+" messageContent length: "+messageContent.length());
            }
        }
        return ssb;
    }

    private static void log(String log) {
        Util.log("RTFFormatter", log);
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
            ssb.append(substringB, new StyleSpan(Typeface.BOLD), new StyleSpan(Typeface.ITALIC));
        }
        else{
            log("endB position: "+endB);
            log("(10) Message content B: "+subMessageContent);
            substringB = subMessageContent.substring(0, endB);
            ssb.append(substringB, new StyleSpan(Typeface.BOLD), new StyleSpan(Typeface.ITALIC));

            endB++;
            StringBuilder sbB = new StringBuilder(subMessageContent);
            sbB.delete(0, endB);
            subMessageContent = sbB.toString();
            log("(11) Message content B: "+subMessageContent);

            startB = subMessageContent.indexOf(" \\*");
            while(startB!=-1){

                startB = startB +1;

                sbB = new StringBuilder(messageContent);
                sbB.deleteCharAt(startB);
                messageContent = sbB.toString();

                log("(B) startB position: "+startB);
                substringB = subMessageContent.substring(endB, startB);
                ssb.append(substringB, new StyleSpan(Typeface.ITALIC));

                sbB = new StringBuilder(subMessageContent);
                sbB.delete(endB, startB);
                subMessageContent = sbB.toString();

                log("Message content C: "+subMessageContent);
                endB = subMessageContent.indexOf("* ");
                if(endB==-1){
                    endB = subMessageContent.lastIndexOf("*");
                    log("(B)FINISH endB position: "+endB);
                    substringB = subMessageContent.substring(startB, endB);
                    ssb.append(substringB, new StyleSpan(Typeface.BOLD), new StyleSpan(Typeface.ITALIC));

                    sbB = new StringBuilder(subMessageContent);
                    sbB.delete(endB, startB);
                    subMessageContent = sbB.toString();
                    break;
                }
                else{
                    log("endB position: "+endB);
                    subMessageContent =  subMessageContent.replaceFirst("\\* ", " ");
                    log("Message content D: "+subMessageContent);

                    substringB = subMessageContent.substring(startB, endB);
                    ssb.append(substringB, new StyleSpan(Typeface.BOLD), new StyleSpan(Typeface.ITALIC));

                    sbB = new StringBuilder(subMessageContent);
                    sbB.delete(endB, startB);
                    subMessageContent = sbB.toString();

                    startB = subMessageContent.indexOf(" *");
                }
            }
        }

        if(!subMessageContent.isEmpty()){
            log("Append more...");
            ssb.append(subMessageContent, new StyleSpan(Typeface.ITALIC));
        }

        return ssb;
    }


}
