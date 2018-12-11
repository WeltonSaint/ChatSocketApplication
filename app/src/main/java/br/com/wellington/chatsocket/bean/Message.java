package br.com.wellington.chatsocket.bean;

import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Classe de Mensagem
 *
 * @author Wellington
 * @version 1.0 - 29/04/2017.
 */
public class Message implements Parcelable {

    private long idUser;
    private long timeMessage;
    private boolean me;
    private long to;
    private String textMessage;
    private String imagePath;
    private String authorMessage;
    private String colorUser;

    public Message() {
    }

    Message(Parcel in) {
        this.idUser = in.readLong();
        this.me = in.readByte() != 0;
        this.to = in.readLong();
        this.timeMessage = in.readLong();
        this.imagePath = in.readString();
        this.textMessage = in.readString();
        this.authorMessage = in.readString();
        this.colorUser = in.readString();
    }

    public Message(long idUser, long timeMessage, String textMessage, String authorMessage, String colorUser, String imagePath) {
        this.idUser = idUser;
        this.timeMessage = timeMessage;
        this.textMessage = textMessage;
        this.authorMessage = authorMessage;
        this.colorUser = colorUser;
        this.imagePath = imagePath;
    }

    public long getIdUser() {
        return idUser;
    }

    public long getTimeMessage() {
        return timeMessage;
    }

    public boolean isMe() {
        return me;
    }

    public void setMe(boolean me) {
        this.me = me;
    }

    public long getTo() {
        return to;
    }

    public void setTo(long to) {
        this.to = to;
    }

    public String getTextMessage() {
        return textMessage;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath.replace("base64,", "").substring(imagePath.indexOf("base64,"));
    }

    public String getAuthorMessage() {
        return authorMessage;
    }

    public String getColorUser() {
        return colorUser;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(idUser);
        dest.writeByte((byte) (me ? 1 : 0));
        dest.writeLong(to);
        dest.writeLong(timeMessage);
        dest.writeString(imagePath);
        dest.writeString(textMessage);
        dest.writeString(authorMessage);
        dest.writeString(colorUser);
        dest.writeString(imagePath);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Message) {
            Message toCompare = (Message) obj;
            return (this.textMessage.concat(String.valueOf(timeMessage)).equalsIgnoreCase(toCompare.getTextMessage().concat(String.valueOf(toCompare.getTimeMessage()))));
        }

        return false;
    }

    @Override
    public int hashCode() {
        return (this.textMessage.concat(String.valueOf(timeMessage))).hashCode();
    }

    public String toJSON() {
        JSONObject json = new JSONObject();
        try {
            json.put("idUser", idUser);
            json.put("to", to);
            json.put("timeMessage", timeMessage);
            json.put("me", me);
            json.put("textMessage", textMessage);
            json.put("image", imagePath);
            json.put("authorMessage", authorMessage);
            json.put("colorUser", colorUser);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json.toString();
    }

    public Message fromJSON(String json) {
        try {
            JSONObject jsonObject = new JSONObject(json);
            idUser = jsonObject.getLong("idUser");
            to = jsonObject.getLong("to");
            timeMessage = jsonObject.getLong("timeMessage");
            me = jsonObject.getBoolean("me");
            textMessage = jsonObject.getString("textMessage");
            imagePath = jsonObject.getString("image");
            authorMessage = jsonObject.getString("authorMessage");
            colorUser = jsonObject.getString("colorUser");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return this;
    }

    /**
     * This field is needed for Android to be able to
     * create new objects, individually or as arrays
     * <p>
     * If you don’t do that, Android framework will through exception
     * Parcelable protocol requires a Parcelable.Creator object called CREATOR
     */
    public static final Parcelable.Creator<Message> CREATOR = new Parcelable.Creator<Message>() {

        public Message createFromParcel(Parcel in) {
            return new Message(in);
        }

        public Message[] newArray(int size) {
            return new Message[size];
        }
    };

}



