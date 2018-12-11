package br.com.wellington.chatsocket.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.view.ContextThemeWrapper;
import android.support.v7.widget.PopupMenu;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Objects;

import br.com.wellington.chatsocket.utils.ConnectionHelper;
import br.com.wellington.chatsocket.R;
import br.com.wellington.chatsocket.service.WebSocketService;
import br.com.wellington.chatsocket.adapter.PagerAdapter;
import br.com.wellington.chatsocket.adapter.UsersOnlineAdapter;
import br.com.wellington.chatsocket.bean.Message;
import br.com.wellington.chatsocket.fragment.MainChatFragment;
import br.com.wellington.chatsocket.fragment.PrivateChatFragment;

public class MainActivity extends AppCompatActivity {

    //UI Elements
    UsersOnlineAdapter adapterUsersOnline;
    Menu mainMenu;
    ViewPager viewPager;
    TabLayout tabLayout;

    //Other Elements
    private static String userName;
    public static final String BROADCAST_ACTION = "br.com.wellington.chatsocket";
    private HashMap<Integer, String> mUserList = new HashMap<>();
    WebSocketReceiver webSocketReceiver;
    private boolean connected = false;
    private int userID;
    public static boolean active = false;

    //Constants
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    @Override
    public void onStart() {
        super.onStart();
        active = true;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable("userList", mUserList);
        outState.putInt("userID", userID);
        outState.putBoolean("connected", connected);
        outState.putString("userName", userName);
    }

    @Override
    public void onStop() {
        //unregisterReceiver(webSocketReceiver);
        super.onStop();
        active = false;
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(webSocketReceiver);
        super.onDestroy();
        active = false;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (ConnectionHelper.getUserSave(this.getApplicationContext()) != null && !ConnectionHelper.isUserEmpty(this.getApplicationContext())) {
            userName = ConnectionHelper.getUserSave(this.getBaseContext());
            connected = true;
        }
        requestPermissions();
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        tabLayout = findViewById(R.id.tab_layout);
        viewPager = findViewById(R.id.pager);
        tabLayout.setupWithViewPager(viewPager);
        setupTabLayout(tabLayout, viewPager);

        if (WebSocketService.messagesReceivedInactive != null) {
            for (Message msg : WebSocketService.messagesReceivedInactive)
                if (msg.getTo() == 0)
                    displayMessage(msg);
                else
                    displayPrivateMessage(msg);

            WebSocketService.messagesReceivedInactive.clear();
        }

        final DrawerLayout drawer = findViewById(R.id.drawer_layout);
        final ListView mDrawerList = findViewById(R.id.list_users_online);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        adapterUsersOnline = new UsersOnlineAdapter(this, mUserList);
        mDrawerList.setAdapter(adapterUsersOnline);
        mDrawerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> arg0,
                                    View arg1, int pos, long arg3) {
                PagerAdapter adapter = ((PagerAdapter) viewPager.getAdapter());
                PrivateChatFragment privateChat;
                if (adapter != null) {
                    privateChat = (PrivateChatFragment) adapter.getItem(1);
                    privateChat.setConversation(adapterUsersOnline.getItem(pos));
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    Objects.requireNonNull(tabLayout.getTabAt(1)).select();
                }
                drawer.closeDrawers();
            }
        });

        EditText mSearchEdit = findViewById(R.id.search_edit);
        mSearchEdit.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Call back the Adapter with current character to Filter
                adapterUsersOnline.getFilter().filter(s.toString());
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        if (!ConnectionHelper.isConnected(this.getApplicationContext())) {
            ConnectionHelper.showIsNotConected(this);
            showLoginDialog();
        } else if (connected) {
            webSocketReceiver = new WebSocketReceiver();
            registerMyReceiver();
            ConnectionHelper.saveUser(userName, getApplicationContext());
            if (getSupportActionBar() != null)
                getSupportActionBar().setSubtitle(getString(R.string.chatting_as, userName));
            connectWebSocket(userName);
        } else
            showLoginDialog();
    }

    private void showLoginDialog() {
        LayoutInflater layoutInflaterAndroid = this.getLayoutInflater();
        View mView = layoutInflaterAndroid.inflate(R.layout.login_dialog_box, null);
        @SuppressLint("RestrictedApi")
        AlertDialog.Builder alertDialogBuilderUserInput = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.AlertDialogTheme));
        alertDialogBuilderUserInput.setView(mView);
        final EditText mUserName = mView.findViewById(R.id.userName);
        final TextInputLayout mUserNameTextInputLayout = mView.findViewById(R.id.userNameTextInputLayout);
        alertDialogBuilderUserInput
                .setCancelable(false)
                .setPositiveButton(getString(R.string.lbl_join), null)
                .setNegativeButton(getString(R.string.lbl_cancel),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialogBox, int id) {
                                dialogBox.cancel();
                                finish();
                            }
                        });

        final AlertDialog alertDialogAndroid = alertDialogBuilderUserInput.create();
        alertDialogAndroid.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(final DialogInterface dialog) {
                final Button button = ((AlertDialog) dialog).getButton(AlertDialog.BUTTON_POSITIVE);

                button.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {
                        if (mUserName.getText().toString().isEmpty()) {
                            mUserNameTextInputLayout.setError(getString(R.string.error_empty_user_name));
                        } else if (!ConnectionHelper.isConnected(MainActivity.this.getApplicationContext())) {
                            ConnectionHelper.showIsNotConected(MainActivity.this);
                        } else {
                            webSocketReceiver = new WebSocketReceiver();
                            registerMyReceiver();
                            mUserNameTextInputLayout.setError(null);
                            mUserNameTextInputLayout.setErrorEnabled(false);
                            userName = mUserName.getText().toString().trim();
                            ConnectionHelper.saveUser(userName, getApplicationContext());
                            if (getSupportActionBar() != null)
                                getSupportActionBar().setSubtitle(getString(R.string.chatting_as, userName));
                            connectWebSocket(userName);
                            alertDialogAndroid.dismiss();
                        }
                    }
                });

                mUserName.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                        if (actionId == EditorInfo.IME_ACTION_SEND) {
                            button.performClick();
                            return true;
                        }
                        return false;
                    }
                });

            }
        });
        alertDialogAndroid.show();
    }

    private void setupTabLayout(TabLayout tabLayout, ViewPager viewPager) {
        PopupMenu p = new PopupMenu(this, null);
        PagerAdapter adapter = new PagerAdapter(getSupportFragmentManager());
        Fragment[] fragments = {new MainChatFragment(), new PrivateChatFragment()};
        Menu menu = p.getMenu();
        getMenuInflater().inflate(R.menu.navigation, menu);

        for (int i = 0; i < menu.size(); i++) {
            adapter.addFragment(fragments[i], String.valueOf(menu.getItem(i).getTitle()));
            tabLayout.addTab(tabLayout.newTab().setText(menu.getItem(i).getTitle()));
        }

        viewPager.setAdapter(adapter);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        mainMenu = menu;
        mainMenu.findItem(R.id.action_logoff).setVisible(connected);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_logoff) {
            disconnect();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void registerMyReceiver() {
        try {
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(BROADCAST_ACTION);
            registerReceiver(webSocketReceiver, intentFilter);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void requestPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_EXTERNAL_STORAGE:
                if (grantResults.length <= 0
                        || grantResults[0] != PackageManager.PERMISSION_GRANTED)
                    finish();

            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void connectWebSocket(String userName) {
        sendToService(this.getString(R.string.connect_tag), userName);
    }

    public void sendToService(String key, String value) {
        Intent myServiceIntent = new Intent(this, WebSocketService.class);
        myServiceIntent.putExtra(key, value);
        startService(myServiceIntent);
    }

    public void sendToService(String key, String value, int userReceiverPrivate) {
        Intent myServiceIntent = new Intent(this, WebSocketService.class);
        myServiceIntent.putExtra(key, value);
        myServiceIntent.putExtra("to", userReceiverPrivate);
        startService(myServiceIntent);
    }

    private void disconnect() {
        clearChat();
        ConnectionHelper.saveUser("", getApplicationContext());
        sendToService(getString(R.string.disconnect_tag), "");
        stopService(new Intent(getApplicationContext(), WebSocketService.class));
    }

    private void updateUserList() {
        connected = false;
        mainMenu.findItem(R.id.action_logoff).setVisible(connected);
        PagerAdapter adapter = ((PagerAdapter) viewPager.getAdapter());
        PrivateChatFragment privateChat;
        if (adapter != null) {
            privateChat = (PrivateChatFragment) adapter.getItem(1);
            privateChat.updateUserList(mUserList);
        }
    }

    private void clearChat() {
        connected = false;
        mainMenu.findItem(R.id.action_logoff).setVisible(connected);
        adapterUsersOnline.clear();
        adapterUsersOnline.notifyDataSetChanged();
        PagerAdapter adapter = ((PagerAdapter) viewPager.getAdapter());
        MainChatFragment mainChat;
        if (adapter != null) {
            mainChat = (MainChatFragment) adapter.getItem(0);
            mainChat.setConnected(connected);
            mainChat.clearChat();
        }
        PrivateChatFragment privateChat;
        if (adapter != null) {
            privateChat = (PrivateChatFragment) adapter.getItem(1);
            privateChat.setConnected(connected, -1);
            privateChat.clearChat();
        }
        if (getSupportActionBar() != null)
            getSupportActionBar().setSubtitle("");
    }

    public void displayMessage(Message msg) {
        PagerAdapter adapter = ((PagerAdapter) viewPager.getAdapter());
        MainChatFragment mainChat;
        if (adapter != null) {
            mainChat = (MainChatFragment) adapter.getItem(0);
            mainChat.addNewMessage(msg);
        }
    }

    public void displayPrivateMessage(Message msg) {
        PagerAdapter adapter = ((PagerAdapter) viewPager.getAdapter());
        PrivateChatFragment privateChat;
        if (adapter != null) {
            privateChat = (PrivateChatFragment) adapter.getItem(1);
            privateChat.addNewMessage(msg);
        }
    }

    class WebSocketReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                if (intent.getStringExtra("response") != null) {
                    try {
                        JSONObject response = new JSONObject(intent.getStringExtra("response"));
                        switch (response.getInt("type")) {
                            case ConnectionHelper.CONNECTION_ACCEPTED:
                                connected = true;
                                mainMenu.findItem(R.id.action_logoff).setVisible(connected);
                                userID = response.getInt("connectionID");
                                JSONArray userList = new JSONArray(response.getString("userList"));
                                for (int i = 0; i < userList.length(); i++) {
                                    JSONObject user = userList.getJSONObject(i);
                                    String name = user.getString("name");
                                    if (!name.equals(userName)) {
                                        mUserList.put(user.getInt("id"), name);
                                        adapterUsersOnline.add(name);
                                    }
                                }
                                adapterUsersOnline.notifyDataSetChanged();
                                PagerAdapter adapter = ((PagerAdapter) viewPager.getAdapter());
                                MainChatFragment mainChat;
                                if (adapter != null) {
                                    mainChat = (MainChatFragment) adapter.getItem(0);
                                    mainChat.setConnected(connected);
                                }
                                PrivateChatFragment privateChat;
                                if (adapter != null) {
                                    privateChat = (PrivateChatFragment) adapter.getItem(1);
                                    privateChat.setConnected(connected, userID);
                                    privateChat.updateUserList(mUserList);
                                }
                                break;
                            case ConnectionHelper.CONNECTION_REFUSED:
                                if (getSupportActionBar() != null)
                                    getSupportActionBar().setSubtitle("");
                                AlertDialog.Builder dlg = new AlertDialog.Builder(new ContextThemeWrapper(MainActivity.this, R.style.AlertDialogTheme));
                                dlg.setMessage(String.format(getString(R.string.exists_user), userName));
                                dlg.setNeutralButton(getString(R.string.lbl_ok), new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialogBox, int id) {
                                        dialogBox.dismiss();
                                        showLoginDialog();
                                        unregisterReceiver(webSocketReceiver);
                                        stopService(new Intent(getApplicationContext(), WebSocketService.class));
                                    }
                                });
                                dlg.show();
                                break;
                            case ConnectionHelper.CONNECT_MESSAGE:
                                Message msg = new Message();
                                msg.fromJSON(response.get("message").toString());
                                mUserList.put((int) msg.getIdUser(), msg.getAuthorMessage());
                                adapterUsersOnline.add(msg.getAuthorMessage());
                                updateUserList();
                                displayMessage(msg);
                                break;
                            case ConnectionHelper.DISCONNECT_MESSAGE:
                                msg = new Message();
                                msg.fromJSON(response.get("message").toString());
                                mUserList.remove((int) msg.getIdUser());
                                adapterUsersOnline.removeItem(msg.getAuthorMessage());
                                updateUserList();
                                displayMessage(msg);
                                break;
                            case ConnectionHelper.ERROR:
                                unregisterReceiver(webSocketReceiver);
                                dlg = new AlertDialog.Builder(new ContextThemeWrapper(MainActivity.this, R.style.AlertDialogTheme));
                                stopService(new Intent(getApplicationContext(), WebSocketService.class));
                                dlg.setTitle("Error");
                                dlg.setMessage(response.getString("error"));
                                dlg.setNeutralButton(getString(R.string.lbl_ok), null);
                                dlg.show();
                                break;
                            case ConnectionHelper.CLOSE:
                                showLoginDialog();
                                unregisterReceiver(webSocketReceiver);
                                stopService(new Intent(getApplicationContext(), WebSocketService.class));
                                break;
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else if (intent.getParcelableExtra("message") != null) {
                    Message msg = intent.getParcelableExtra("message");
                    if (msg.getTo() == 0)
                        displayMessage(msg);
                    else
                        displayPrivateMessage(msg);
                }
            }
        }
    }
}
