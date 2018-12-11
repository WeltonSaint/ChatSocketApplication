package br.com.wellington.chatsocket.service;

import android.app.IntentService;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

import br.com.wellington.chatsocket.R;
import br.com.wellington.chatsocket.activity.MainActivity;
import br.com.wellington.chatsocket.bean.Message;
import br.com.wellington.chatsocket.utils.ConnectionHelper;
import br.com.wellington.chatsocket.utils.CustomNotificationManager;
import br.com.wellington.chatsocket.utils.ImageUtils;


/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions and extra parameters.
 */
public class WebSocketService extends Service {
    private final IBinder mBinder = new WebSocketBinder();
    boolean isSocketStarted = false;
    public static WebSocketClient socket = null;
    public static ArrayList<Message> messagesReceivedInactive;
    private String userName;

    Handler handler;

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        handler = new Handler();
        messagesReceivedInactive = new ArrayList<>();
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStart(intent, startId);
        if (intent != null) {
            if (intent.getStringExtra(getString(R.string.private_image_tag)) != null && intent.getExtras() != null) {
                try {
                    JSONObject message = new JSONObject();
                    message.put("type", ConnectionHelper.PRIVATE_MESSAGE);
                    message.put("image", intent.getStringExtra(getString(R.string.private_image_tag)));
                    message.put("to", intent.getExtras().getInt("to"));
                    socket.send(message.toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else if (intent.getStringExtra(getString(R.string.private_message_tag)) != null && intent.getExtras() != null) {
                try {
                    JSONObject message = new JSONObject();
                    message.put("type", ConnectionHelper.PRIVATE_MESSAGE);
                    message.put("message", intent.getStringExtra(getString(R.string.private_message_tag)));
                    message.put("to", intent.getExtras().getInt("to"));
                    socket.send(message.toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else if (intent.getStringExtra(getString(R.string.image_tag)) != null) {
                try {
                    JSONObject message = new JSONObject();
                    message.put("type", ConnectionHelper.SIMPLE_MESSAGE);
                    message.put("image", intent.getStringExtra(getString(R.string.image_tag)));
                    socket.send(message.toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else if (intent.getStringExtra(getString(R.string.message_tag)) != null) {
                try {
                    JSONObject message = new JSONObject();
                    message.put("type", ConnectionHelper.SIMPLE_MESSAGE);
                    message.put("message", intent.getStringExtra(getString(R.string.message_tag)));
                    socket.send(message.toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else if (socket == null || !socket.getConnection().isOpen() &&
                    intent.getStringExtra(getString(R.string.connect_tag)) != null) {

                userName = intent.getStringExtra(getString(R.string.connect_tag));
                connectSocket();

                Runnable mRefreshConversationsRunnable = new Runnable() {
                    @Override
                    public void run() {
                        if (userName != null && socket.getConnection() != null && !socket.getConnection().isClosed() && !socket.getConnection().isClosing()) {
                            try {
                                JSONObject message = new JSONObject();
                                message.put("type", ConnectionHelper.POLLING);
                                socket.send(message.toString());
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                        handler.postDelayed(this, 30000);
                    }
                };
                handler.postDelayed(mRefreshConversationsRunnable, 30000);

            } else if (intent.getStringExtra(getString(R.string.disconnect_tag)) != null) {
                socket.close();
                try {
                    isSocketStarted = false;
                    JSONObject response = new JSONObject();
                    response.put("type", ConnectionHelper.CLOSE);
                    sendBroadcast(response);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }


    private void runOnUiThread(Runnable runnable) {
        handler.post(runnable);
    }

    private void connectSocket() {
        if (socket != null && socket.getConnection().isOpen()) return;

        URI uri;
        try {
            uri = new URI(getString(R.string.socket_host));
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return;
        }

        socket = new WebSocketClient(uri) {
            @Override
            public void onOpen(ServerHandshake serverHandshake) {
                JSONObject message = new JSONObject();
                try {
                    message.put("type", ConnectionHelper.CONNECT_MESSAGE);
                    message.put("userName", userName);
                    socket.send(message.toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onMessage(final String s) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        handleMessage(s);
                    }
                });
            }

            @Override
            public void onClose(int i, String s, boolean b) {
                if (isSocketStarted) {
                    try {
                        isSocketStarted = false;
                        JSONObject response = new JSONObject();
                        response.put("type", ConnectionHelper.CLOSE);
                        sendBroadcast(response);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void onError(final Exception e) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        if (e.getMessage() == null || socket.getConnection().isClosed()) {
                            try {
                                JSONObject response = new JSONObject();
                                response.put("type", ConnectionHelper.ERROR);
                                response.put("error", e.getMessage());
                                sendBroadcast(response);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });
            }
        };
        socket.connect();
    }

    private void handleMessage(String msg) {
        JSONObject json;
        try {
            json = new JSONObject(msg);
            switch (json.getInt("type")) {
                case ConnectionHelper.CONNECTION_ACCEPTED:
                    JSONObject response = new JSONObject();
                    response.put("type", ConnectionHelper.CONNECTION_ACCEPTED);
                    response.put("color", json.get("data"));
                    response.put("connectionID", json.get("id"));
                    response.put("userList", json.get("listUsers"));
                    sendBroadcast(response);
                    break;
                case ConnectionHelper.CONNECTION_REFUSED:
                    socket.close();
                    response = new JSONObject();
                    response.put("type", ConnectionHelper.CONNECTION_REFUSED);
                    sendBroadcast(response);
                    break;
                case ConnectionHelper.CONNECT_MESSAGE:
                    response = new JSONObject();
                    JSONObject jsonMsg = json.getJSONObject("data");
                    Message message = new Message(jsonMsg.getLong("id"), jsonMsg.getLong("time"),
                            jsonMsg.getString("text"), jsonMsg.getString("author"),
                            jsonMsg.getString("color"), "");
                    message.setMe(userName.equals(message.getAuthorMessage()));
                    response.put("type", ConnectionHelper.CONNECT_MESSAGE);
                    response.put("message", message.toJSON());
                    displayMessage(response);
                    break;
                case ConnectionHelper.DISCONNECT_MESSAGE:
                    response = new JSONObject();
                    jsonMsg = json.getJSONObject("data");
                    message = new Message(jsonMsg.getLong("id"), jsonMsg.getLong("time"),
                            jsonMsg.getString("text"), jsonMsg.getString("author"),
                            jsonMsg.getString("color"), "");
                    message.setMe(userName.equals(message.getAuthorMessage()));
                    response.put("type", ConnectionHelper.DISCONNECT_MESSAGE);
                    response.put("message", message.toJSON());
                    displayMessage(response);
                    break;
                case ConnectionHelper.SIMPLE_MESSAGE:
                    jsonMsg = json.getJSONObject("data");
                    String image = null, text = null;
                    try {
                        image = jsonMsg.getString("image");
                        image = ImageUtils.storageImage(getApplicationContext(), String.format("msg%s.jpg", jsonMsg.getLong("time")), image);
                    } catch (JSONException ignored) {
                    }
                    try {
                        text = jsonMsg.getString("text");
                    } catch (JSONException ignored) {
                    }
                    message = new Message(jsonMsg.getLong("id"), jsonMsg.getLong("time"),
                            text, jsonMsg.getString("author"),
                            jsonMsg.getString("color"), image);
                    message.setMe(userName.equals(message.getAuthorMessage()));
                    displayMessage(message);
                    break;
                case ConnectionHelper.PRIVATE_MESSAGE:
                    jsonMsg = json.getJSONObject("data");
                    image = null;
                    text = null;
                    try {
                        image = jsonMsg.getString("image");
                        image = ImageUtils.storageImage(getApplicationContext(), String.format("msg%s.jpg", jsonMsg.getLong("time")), image);
                    } catch (JSONException ignored) {
                    }
                    try {
                        text = jsonMsg.getString("text");
                    } catch (JSONException ignored) {
                    }
                    message = new Message(jsonMsg.getLong("id"), jsonMsg.getLong("time"),
                            text, jsonMsg.getString("author"),
                            jsonMsg.getString("color"), image);
                    message.setMe(userName.equals(message.getAuthorMessage()));
                    message.setTo(jsonMsg.getLong("to"));
                    displayPrivateMessage(message);
                    break;
                case ConnectionHelper.POLLING:
                    break;
                default:
                    System.err.println("Hmm..., I\'ve never seen JSON like this: " + json);
                    break;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void displayPrivateMessage(Message message) {
        if (!MainActivity.active && !message.isMe()) {
            messagesReceivedInactive.add(message);
            String content;
            if (message.getTextMessage() != null)
                content = String.format("%s send in private: %s", message.getAuthorMessage(), message.getTextMessage());
            else
                content = String.format("%s sends a image in private", message.getAuthorMessage());
            showNotification(content);
        } else {
            sendBroadcast(message);
        }
    }

    private void displayMessage(Message message) {
        if (!MainActivity.active && !message.isMe()) {
            messagesReceivedInactive.add(message);
            String content;
            if (message.getTextMessage() != null)
                content = String.format("%s: %s", message.getAuthorMessage(), message.getTextMessage());
            else
                content = String.format("%s sends a image", message.getAuthorMessage());
            showNotification(content);
        } else {
            sendBroadcast(message);
        }
    }

    private void displayMessage(JSONObject response) {
        try {
            Message message = new Message();
            message.fromJSON(response.get("message").toString());
            if (!MainActivity.active && !message.isMe()) {
                messagesReceivedInactive.add(message);
                String content = String.format("%s %s", message.getAuthorMessage(), message.getTextMessage());
                showNotification(content);
            } else {
                sendBroadcast(response);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void showNotification(String content) {
        for (int i = messagesReceivedInactive.size() - 2; i >= 0; i--) {
            if (messagesReceivedInactive.get(i).getTo() == 0)
                if (messagesReceivedInactive.get(i).getTextMessage() != null)
                    if (!messagesReceivedInactive.get(i).getTextMessage().contains("connected"))
                        content = String.format("%s: %s\n", messagesReceivedInactive.get(i).getAuthorMessage(), messagesReceivedInactive.get(i).getTextMessage()).concat(content);
                    else
                        content = String.format("%s %s\n", messagesReceivedInactive.get(i).getAuthorMessage(), messagesReceivedInactive.get(i).getTextMessage()).concat(content);
                else
                    content = String.format("%s sends a image\n", messagesReceivedInactive.get(i).getAuthorMessage()).concat(content);
            else if (messagesReceivedInactive.get(i).getTextMessage() != null) {
                if (!messagesReceivedInactive.get(i).getTextMessage().contains("connected"))
                    content = String.format("%s send in private: %s\n", messagesReceivedInactive.get(i).getAuthorMessage(), messagesReceivedInactive.get(i).getTextMessage()).concat(content);
            } else
                content = String.format("%s sends a image in private\n", messagesReceivedInactive.get(i).getAuthorMessage()).concat(content);
        }
        String title, description;
        switch (messagesReceivedInactive.size()) {
            case 1:
                title = "Nova Mensagem";
                description = "%d nova mensagem";
                break;
            default:
                title = "Novas Mensagens";
                description = "%d novas mensagens";
                break;
        }
        CustomNotificationManager.showNotification(getApplicationContext(), title, content, String.format(description, messagesReceivedInactive.size()));
    }

    /**
     * This method is responsible to send broadCast to specific Action
     */
    private void sendBroadcast(Message msg) {
        try {
            Intent broadCastIntent = new Intent();
            broadCastIntent.setAction(MainActivity.BROADCAST_ACTION);
            broadCastIntent.putExtra("message", msg);
            sendBroadcast(broadCastIntent);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * This method is responsible to send broadCast to specific Action
     */
    private void sendBroadcast(JSONObject response) {
        try {
            Intent broadCastIntent = new Intent();
            broadCastIntent.setAction(MainActivity.BROADCAST_ACTION);
            broadCastIntent.putExtra("response", String.valueOf(response));
            sendBroadcast(broadCastIntent);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public class WebSocketBinder extends Binder {
        WebSocketService getService() {
            return WebSocketService.this;
        }
    }


}
