package br.com.wellington.chatsocket.fragment;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.widget.AppCompatSpinner;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import br.com.wellington.chatsocket.utils.ImageUtils;
import br.com.wellington.chatsocket.R;
import br.com.wellington.chatsocket.activity.MainActivity;
import br.com.wellington.chatsocket.adapter.ChatAdapter;
import br.com.wellington.chatsocket.bean.Message;

public class PrivateChatFragment extends Fragment {

    //UI Elements
    private RecyclerView mMessagesContainerPrivate;
    private FloatingActionButton mChatSendButtonPrivate;
    private FloatingActionButton mToEndButtonPrivate;
    private EditText mMessageEditPrivate;
    private AppCompatSpinner mSpinnerOnlineUsers;
    private ChatAdapter chatAdapter;
    private ArrayAdapter<String> spinnerAdapter;

    //Others Elements
    private HashMap<Integer, ArrayList<Message>> mPrivateMessages = new HashMap<>();
    private HashMap<Integer, String> mUserList = new HashMap<>();
    private boolean connected = false;
    private int userID;
    private int userReceiverPrivate = -1;

    public PrivateChatFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_private_chat, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        mMessagesContainerPrivate = view.findViewById(R.id.messagesContainerPrivate);
        mChatSendButtonPrivate = view.findViewById(R.id.chatSendButtonPrivate);
        mToEndButtonPrivate = view.findViewById(R.id.toEndButtonPrivate);
        mMessageEditPrivate = view.findViewById(R.id.messageEditPrivate);
        ImageButton mMessageImagePrivate = view.findViewById(R.id.messageImagePrivate);
        mSpinnerOnlineUsers = view.findViewById(R.id.spinnerOnlineUsers);

        chatAdapter = new ChatAdapter(new ArrayList<Message>());
        LayoutAnimationController animation = AnimationUtils.loadLayoutAnimation(getActivity(), R.anim.layout_anim_chat);
        mMessagesContainerPrivate.setLayoutAnimation(animation);
        LinearLayoutManager llm = new LinearLayoutManager(getActivity());
        llm.setOrientation(LinearLayoutManager.VERTICAL);
        mMessagesContainerPrivate.setLayoutManager(llm);
        mMessagesContainerPrivate.setAdapter(chatAdapter);

        mChatSendButtonPrivate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mMessageEditPrivate.getText().toString().isEmpty() && connected && userReceiverPrivate != -1 && getActivity() != null) {
                    ((MainActivity) getActivity()).sendToService(getString(R.string.private_message_tag), mMessageEditPrivate.getText().toString(), userReceiverPrivate);
                    mMessageEditPrivate.setText("");
                }
            }
        });

        mMessageImagePrivate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (userReceiverPrivate != -1) {
                    Intent pickPhoto = new Intent(Intent.ACTION_PICK,
                            android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    startActivityForResult(pickPhoto, 0);
                }
            }
        });

        mToEndButtonPrivate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scroll();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    mToEndButtonPrivate.animate()
                            .translationY(mToEndButtonPrivate.getHeight())
                            .alpha(0.0f)
                            .withEndAction(new Runnable() {
                                @Override
                                public void run() {
                                    if (mToEndButtonPrivate.getVisibility() == View.VISIBLE)
                                        mToEndButtonPrivate.hide();
                                }
                            });
                }
            }
        });

        mMessageEditPrivate.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    mChatSendButtonPrivate.performClick();
                    return true;
                }
                return false;
            }
        });

        mMessagesContainerPrivate.addOnScrollListener(new RecyclerView.OnScrollListener() {
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
                        mToEndButtonPrivate.animate()
                                .translationY(mToEndButtonPrivate.getHeight())
                                .alpha(0.0f)
                                .withEndAction(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (mToEndButtonPrivate.getVisibility() == View.VISIBLE)
                                            mToEndButtonPrivate.hide();
                                    }
                                });
                    }
                } else if (mToEndButtonPrivate.getVisibility() != View.VISIBLE) {
                    mToEndButtonPrivate.show();
                    mToEndButtonPrivate.setAlpha(0.0f);
                    mToEndButtonPrivate.animate()
                            .translationY(0)
                            .alpha(1.0f)
                            .setListener(null);
                }
            }
        });

        mSpinnerOnlineUsers.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                chatAdapter.clearMessages();
                TextView textView = (TextView) mSpinnerOnlineUsers.getSelectedView();
                String selected = textView.getText().toString();
                for (Map.Entry<Integer, String> entry : mUserList.entrySet()) {
                    if (selected.equals(entry.getValue())) {
                        userReceiverPrivate = entry.getKey();
                    }
                }
                if (mPrivateMessages.get(userReceiverPrivate) != null)
                    chatAdapter.addAll(mPrivateMessages.get(userReceiverPrivate));
                else
                    chatAdapter.clearMessages();

                chatAdapter.notifyDataSetChanged();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
        if (getContext() != null) {
            spinnerAdapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_spinner_item, new ArrayList<>(mUserList.values())) {
                // Disable click item < month current
                @Override
                public boolean isEnabled(int position) {
                    return position != 0;
                }

                // Change color item
                @Override
                public View getDropDownView(int position, View convertView,
                                            @NonNull ViewGroup parent) {
                    // TODO Auto-generated method stub
                    View mView = super.getDropDownView(position, convertView, parent);
                    TextView mTextView = (TextView) mView;
                    if (position == 0) {
                        mTextView.setTextColor(Color.GRAY);
                    } else {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && getActivity() != null) {
                            mTextView.setTextColor(getActivity().getColor(R.color.colorAccent));
                        }
                    }
                    return mView;
                }
            };
        }
        if (getActivity() != null)
            spinnerAdapter.add(getActivity().getString(R.string.choose_your_option));

        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinnerOnlineUsers.setAdapter(spinnerAdapter);

        if (mSpinnerOnlineUsers.getBackground().getConstantState() != null) {
            Drawable spinnerDrawable = mSpinnerOnlineUsers.getBackground().getConstantState().newDrawable();
            spinnerDrawable.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);

            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
                mSpinnerOnlineUsers.setBackground(spinnerDrawable);
            else
                mSpinnerOnlineUsers.setBackgroundDrawable(spinnerDrawable);
        }
    }

    private void scroll() {
        mMessagesContainerPrivate.scrollToPosition(chatAdapter.getItemCount() - 1);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);
        switch (requestCode) {
            case 0:
                if (getActivity() != null && imageReturnedIntent != null) {
                    Uri selectedImage = imageReturnedIntent.getData();
                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), selectedImage);
                        ((MainActivity) getActivity()).sendToService(getString(R.string.private_image_tag), ImageUtils.getStringBase64FromImage(bitmap), userReceiverPrivate);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                break;
        }
    }

    public void addNewMessage(Message msg) {
        int idAnotherUser = (msg.getIdUser() != userID) ? (int) msg.getIdUser() : (int) msg.getTo();

        if (mPrivateMessages.get(idAnotherUser) == null)
            mPrivateMessages.put(idAnotherUser, new ArrayList<Message>());

        mPrivateMessages.get(idAnotherUser).add(msg);
        if (chatAdapter != null && idAnotherUser == userReceiverPrivate) {
            chatAdapter.clearMessages();
            chatAdapter.addAll(mPrivateMessages.get(idAnotherUser));
            chatAdapter.notifyItemInserted(mPrivateMessages.get(idAnotherUser).size() - 1);
            if (mToEndButtonPrivate.getVisibility() == View.GONE) {
                scroll();
            }
        }
    }

    public void setConnected(boolean connected, int id) {
        this.connected = connected;
        this.userID = id;
        this.userReceiverPrivate = -1;
    }

    public void clearChat() {
        chatAdapter.clearMessages();
        chatAdapter.notifyDataSetChanged();
        mToEndButtonPrivate.hide();
    }

    public void setConversation(String userName) {
        int spinnerPosition = (spinnerAdapter.getPosition(userName) != -1) ? spinnerAdapter.getPosition(userName) : 0;
        mSpinnerOnlineUsers.setSelection(spinnerPosition);
    }

    public void updateUserList(HashMap<Integer, String> mUserList) {
        if (getActivity() != null) {
            this.mUserList = mUserList;
            spinnerAdapter.clear();
            spinnerAdapter.add(getActivity().getString(R.string.choose_your_option));
            spinnerAdapter.addAll(mUserList.values());
            spinnerAdapter.notifyDataSetChanged();
        }
    }
}
