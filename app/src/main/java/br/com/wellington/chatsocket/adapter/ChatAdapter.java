package br.com.wellington.chatsocket.adapter;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

import br.com.wellington.chatsocket.utils.ImageUtils;
import br.com.wellington.chatsocket.R;
import br.com.wellington.chatsocket.bean.Message;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.ViewHolder> {


    private ArrayList<Message> messageList;
    private HashMap<String, String> colors;

    public ChatAdapter(ArrayList<Message> messageList) {
        this.colors = new HashMap<>();
        colors.put("red", "#FF0000");
        colors.put("green", "#008000");
        colors.put("blue", "#0000FF");
        colors.put("magenta", "#FF00FF");
        colors.put("purple", "#800080");
        colors.put("plum", "#DDA0DD");
        colors.put("orange", "#FFA500");
        this.messageList = messageList;

    }

    @Override
    public int getItemViewType(int position) {
        // Just as an example, return 0 or 2 depending on position
        // Note that unlike in ListView adapters, types don't have to be contiguous
        return (messageList.get(position).isMe()) ? 0 : 1;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
                                         int viewType) {
        View view = (viewType == 0)
                ? LayoutInflater.from(parent.getContext()).inflate(R.layout.message_out_item, parent, false)
                : LayoutInflater.from(parent.getContext()).inflate(R.layout.message_in_item, parent, false);

        RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        view.setLayoutParams(lp);

        return new ViewHolder(view);
    }

    @SuppressLint("SimpleDateFormat")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Message chatMessage = messageList.get(position);

        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(chatMessage.getTimeMessage());
        holder.timestamp.setText(new SimpleDateFormat("HH:mm").format(c.getTime()));
        if(chatMessage.getTextMessage() != null){
            holder.contentText.setVisibility(View.VISIBLE);
            holder.contentImage.setVisibility(View.GONE);
            holder.userText.setText(chatMessage.getAuthorMessage());
            holder.message.setText(chatMessage.getTextMessage());
        } else {
            holder.contentText.setVisibility(View.GONE);
            holder.contentImage.setVisibility(View.VISIBLE);
            holder.userImage.setTextColor(Color.parseColor(colors.get(chatMessage.getColorUser())));
            holder.userImage.setText(chatMessage.getAuthorMessage());
            holder.image.setImageBitmap(ImageUtils.getImageFromPath(chatMessage.getImagePath()));
            if(!ImageUtils.removeImageFromStorage(chatMessage.getImagePath()))
                System.out.println("Image was not removed");
        }
        holder.userImage.setText(chatMessage.getAuthorMessage());
        holder.wrapper.setCardBackgroundColor(Color.parseColor(colors.get(chatMessage.getColorUser())));
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    public void clearMessages() {
        messageList.clear();
    }

    public void addAll(ArrayList<Message> messages) {
        messageList.addAll(messages);
    }


    class ViewHolder extends RecyclerView.ViewHolder {

        final CardView wrapper;
        final LinearLayout contentText;
        final RelativeLayout contentImage;
        final TextView userText;
        final TextView userImage;
        final TextView message;
        final ImageView image;
        final TextView timestamp;


        ViewHolder(View view) {
            super(view);
            wrapper = view.findViewById(R.id.wrapper);
            contentText = view.findViewById(R.id.contentText);
            contentImage = view.findViewById(R.id.contentImage);
            message = view.findViewById(R.id.message);
            image = view.findViewById(R.id.image);
            userImage = view.findViewById(R.id.userImage);
            userText = view.findViewById(R.id.userText);
            timestamp = view.findViewById(R.id.timestamp);
        }

    }

}