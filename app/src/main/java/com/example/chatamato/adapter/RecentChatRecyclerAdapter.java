package com.example.chatamato.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatamato.Chat_Activity;
import com.example.chatamato.R;
import com.example.chatamato.model.ChatRoomModel;
import com.example.chatamato.model.UserModel;
import com.example.chatamato.utils.AndroidUtil;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.example.chatamato.utils.FirebaseUtil;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;

public class RecentChatRecyclerAdapter extends FirestoreRecyclerAdapter<ChatRoomModel, RecentChatRecyclerAdapter.ChatRoomModelViewHolder> {

    Context context;

    public RecentChatRecyclerAdapter(@NonNull FirestoreRecyclerOptions<ChatRoomModel> options, Context context) {
        super(options);
        this.context = context;
    }

    @Override
    protected void onBindViewHolder(@NonNull ChatRoomModelViewHolder holder, int position, @NonNull ChatRoomModel model) {
        FirebaseUtil.getOtherUserFromChatroom(model.getUserIds())
                .get().addOnCompleteListener(task ->{
                        if(task.isSuccessful()){
                            boolean lastMessageSentByMe = model.getLastMessageSenderId().equals(FirebaseUtil.currentUserId());

                            UserModel otherUserModel = task.getResult().toObject(UserModel.class);
                            holder.usernameText.setText(otherUserModel.getUsername());
                            if(lastMessageSentByMe)
                                holder.lastMessageText.setText("You :"+model.getLastMessage());
                            else
                                holder.lastMessageText.setText(model.getLastMessage());
                            holder.lastMessageTime.setText(model.getLastMessageTimestamp().toString());

                            holder.itemView.setOnClickListener(v -> {
                                Intent intent = new Intent(context, Chat_Activity.class);
                                AndroidUtil.passUserModelIntent(intent,otherUserModel);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);


                                context.startActivity(intent);


                            });



                    }

                });

    }

    @NonNull
    @Override
    public ChatRoomModelViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.recent_chat_recycler_row, parent, false);
        return new ChatRoomModelViewHolder(view);
    }

    class ChatRoomModelViewHolder extends RecyclerView.ViewHolder {
        TextView usernameText;
        TextView lastMessageText;
        TextView lastMessageTime;
        ImageView profilePic;

        public ChatRoomModelViewHolder(@NonNull View itemView) {
            super(itemView);
            usernameText = itemView.findViewById(R.id.user_name_text);
            lastMessageText = itemView.findViewById(R.id.last_message_text);
            lastMessageTime = itemView.findViewById(R.id.last_message_time_text);
            profilePic = itemView.findViewById(R.id.profile_pic_image_view);
        }
    }
}
