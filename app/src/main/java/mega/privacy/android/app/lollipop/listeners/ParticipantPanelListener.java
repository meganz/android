package mega.privacy.android.app.lollipop.listeners;


import android.content.Context;
import android.view.View;

import java.security.acl.Group;

import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.lollipop.controllers.ChatController;
import mega.privacy.android.app.lollipop.megachat.GroupChatInfoActivityLollipop;
import mega.privacy.android.app.lollipop.megachat.MegaChatParticipant;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaChatRoom;

public class ParticipantPanelListener implements View.OnClickListener {

    Context context;
    ChatController chatC;

    public ParticipantPanelListener(Context context){
        log("ParticipantPanelListener created");
        this.context = context;
        chatC = new ChatController(context);
    }

    @Override
    public void onClick(View v) {
        log("onClick ParticipantPanelListener");

        MegaChatParticipant selectedParticipant = null;
        MegaChatRoom selectedChat = null;
        if(context instanceof GroupChatInfoActivityLollipop){
            selectedParticipant = ((GroupChatInfoActivityLollipop) context).getSelectedParticipant();
            selectedChat = ((GroupChatInfoActivityLollipop) context).getChat();
        }

        switch(v.getId()){

            case R.id.change_permissions_group_participants_chat_layout: {
                log("change permissions participants panel");
                ((GroupChatInfoActivityLollipop) context).showChangePermissionsDialog(selectedParticipant, selectedChat);
                break;
            }

            case R.id.remove_group_participants_chat_layout: {
                log("remove participants panel");
                break;
            }

            case R.id.out_group_participants_chat:{
                log("click participants panel");
                ((GroupChatInfoActivityLollipop)context).hideParticipantsOptionsPanel();
                break;
            }
        }

    }

    public static void log(String message) {
        Util.log("ParticipantPanelListener", message);
    }

}
