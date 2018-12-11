package br.com.wellington.chatsocket.fragment;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;

import br.com.wellington.chatsocket.utils.ImageUtils;
import br.com.wellington.chatsocket.R;
import br.com.wellington.chatsocket.activity.MainActivity;
import br.com.wellington.chatsocket.adapter.ChatAdapter;
import br.com.wellington.chatsocket.bean.Message;

public class MainChatFragment extends Fragment {

    //UI Elements
    private RecyclerView mMessagesContainer;
    private FloatingActionButton mChatSendButton;
    private FloatingActionButton mToEndButton;
    private EditText mMessageEdit;
    private ChatAdapter chatAdapter;

    //Others Elements
    ArrayList<Message> mMessages = new ArrayList<>();
    private boolean connected = false;

    public MainChatFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_main_chat, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        mMessagesContainer = view.findViewById(R.id.messagesContainer);
        mChatSendButton = view.findViewById(R.id.chatSendButton);
        mToEndButton = view.findViewById(R.id.toEndButton);
        mMessageEdit = view.findViewById(R.id.messageEdit);
        ImageButton mMessageImage = view.findViewById(R.id.messageImage);

        chatAdapter = new ChatAdapter(mMessages);
        LayoutAnimationController animation = AnimationUtils.loadLayoutAnimation(getActivity(), R.anim.layout_anim_chat);
        mMessagesContainer.setLayoutAnimation(animation);
        LinearLayoutManager llm = new LinearLayoutManager(getActivity());
        llm.setOrientation(LinearLayoutManager.VERTICAL);
        mMessagesContainer.setLayoutManager(llm);
        mMessagesContainer.setAdapter(chatAdapter);

        mChatSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mMessageEdit.getText().toString().isEmpty() && connected && getActivity() != null) {
                    ((MainActivity) getActivity()).sendToService(getString(R.string.message_tag), mMessageEdit.getText().toString());
                    mMessageEdit.setText("");
                }
            }
        });

        mMessageImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent pickPhoto = new Intent(Intent.ACTION_PICK,
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(pickPhoto, 0);
            }
        });

        mToEndButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scroll();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    mToEndButton.animate()
                            .translationY(mToEndButton.getHeight())
                            .alpha(0.0f)
                            .withEndAction(new Runnable() {
                                @Override
                                public void run() {
                                    if (mToEndButton.getVisibility() == View.VISIBLE)
                                        mToEndButton.hide();
                                }
                            });
                }
            }
        });

        mMessageEdit.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    mChatSendButton.performClick();
                    return true;
                }
                return false;
            }
        });

        mMessagesContainer.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                LinearLayoutManager layoutManager = LinearLayoutManager.class.cast(recyclerView.getLayoutManager());
                int totalItemCount = layoutManager.getItemCount();
                int lastVisible = layoutManager.findLastVisibleItemPosition();

                boolean endHasBeenReached = lastVisible + 2 >= totalItemCount;
                if (totalItemCount > 0 && endHasBeenReached) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        mToEndButton.animate()
                                .translationY(mToEndButton.getHeight())
                                .alpha(0.0f)
                                .withEndAction(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (mToEndButton.getVisibility() == View.VISIBLE)
                                            mToEndButton.hide();
                                    }
                                });
                    }
                } else if (mToEndButton.getVisibility() != View.VISIBLE) {
                    mToEndButton.show();
                    mToEndButton.setAlpha(0.0f);
                    mToEndButton.animate()
                            .translationY(0)
                            .alpha(1.0f)
                            .setListener(null);
                }
            }
        });
    }

    private void scroll() {
        mMessagesContainer.scrollToPosition(chatAdapter.getItemCount() - 1);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);
        switch (requestCode) {
            case 0:
                if (getActivity() != null && imageReturnedIntent != null) {
                    Uri selectedImage = imageReturnedIntent.getData();
                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), selectedImage);
                        ((MainActivity) getActivity()).sendToService(getString(R.string.image_tag), ImageUtils.getStringBase64FromImage(bitmap));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                break;
        }
    }

    public void addNewMessage(Message message) {
        mMessages.add(message);
        if (chatAdapter != null) {
            chatAdapter.notifyItemInserted(mMessages.size() - 1);
            if (mToEndButton.getVisibility() == View.GONE) {
                scroll();
            }
        }
    }

    public void clearChat() {
        chatAdapter.clearMessages();
        chatAdapter.notifyDataSetChanged();
        mToEndButton.hide();
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }
}
