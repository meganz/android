package mega.privacy.android.app.lollipop.listeners;

import android.content.Context;
import android.content.Intent;
import android.view.View;

import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.controllers.ChatController;
import mega.privacy.android.app.lollipop.megachat.ContactChatInfoActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.GroupChatInfoActivityLollipop;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaChatRoom;

public class ChatPanelListener implements View.OnClickListener {

    Context context;
    ManagerActivityLollipop.DrawerItem drawerItem;
    ChatController chatC;

    public ChatPanelListener(Context context){
        log("UploadPanelListener created");
        this.context = context;
        chatC = new ChatController(context);
    }

    @Override
    public void onClick(View v) {
        log("onClick ChatPanelListener");
        log("onClick NodeOptionsPanelListener");
        MegaChatRoom selectedChat = null;
        if(context instanceof ManagerActivityLollipop){
            selectedChat = ((ManagerActivityLollipop) context).getSelectedChat();
        }

        switch(v.getId()){

            case R.id.file_list_info_chat_layout:{
                log("click contact info");
                ((ManagerActivityLollipop)context).hideChatPanel();
                if(selectedChat==null){
                    log("Selected chat NULL");
                }

                if(selectedChat.isGroup()){
                    Intent i = new Intent(context, GroupChatInfoActivityLollipop.class);
//                i.putExtra("userEmail", selectedChat.getContacts().get(0).getMail());
//                i.putExtra("userFullName", ((ManagerActivityLollipop) context).getFullNameChat());
                    i.putExtra("handle", selectedChat.getChatId());
                    context.startActivity(i);
                }
                else{
                    Intent i = new Intent(context, ContactChatInfoActivityLollipop.class);
//                i.putExtra("userEmail", selectedChat.getContacts().get(0).getMail());
//                i.putExtra("userFullName", ((ManagerActivityLollipop) context).getFullNameChat());
                    i.putExtra("handle", selectedChat.getChatId());
                    context.startActivity(i);
                }

                break;
            }

            case R.id.file_list_leave_chat_layout:{
                log("click leave chat");
                ((ManagerActivityLollipop)context).hideChatPanel();
//                Intent intent = new Intent();
//                intent.setAction(Intent.ACTION_OPEN_DOCUMENT);
//                intent.setAction(Intent.ACTION_GET_CONTENT);
//                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
//                intent.setType("*/*");
//                ((ManagerActivityLollipop)context).startActivityForResult(Intent.createChooser(intent, null), Constants.REQUEST_CODE_GET);
                break;
            }

            case R.id.file_list_clear_history_chat_layout:{
                log("click clear history chat");
                ((ManagerActivityLollipop)context).hideChatPanel();
                if(selectedChat==null){
                    log("Selected chat NULL");
                }
                log("Clear chat with: "+selectedChat.getTitle());
                chatC.clearHistory(selectedChat);
                break;
            }

            case R.id.file_list_mute_chat_layout:{
                log("click mute chat");
                ((ManagerActivityLollipop)context).hideChatPanel();
//                Intent intent = new Intent();
//                intent.setAction(Intent.ACTION_OPEN_DOCUMENT);
//                intent.setAction(Intent.ACTION_GET_CONTENT);
//                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
//                intent.setType("*/*");
//                ((ManagerActivityLollipop)context).startActivityForResult(Intent.createChooser(intent, null), Constants.REQUEST_CODE_GET);
                break;
            }

            case R.id.file_list_out_chat:{
                log("click out chat panel");
                ((ManagerActivityLollipop)context).hideChatPanel();
                break;
            }
        }
    }

    public static void log(String message) {
        Util.log("ChatPanelListener", message);
    }
}
