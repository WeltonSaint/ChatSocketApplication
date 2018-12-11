package br.com.wellington.chatsocket.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class ImageUtils {

    public static String storageImage(Context context, String nameFile, String base64) {
        String path = Environment.getExternalStorageDirectory().toString();
        OutputStream fOut;
        File file = new File(path, nameFile);
        try {
            fOut = new FileOutputStream(file);
            Bitmap pictureBitmap = getImageFromStringBase64(base64);
            pictureBitmap.compress(Bitmap.CompressFormat.JPEG, 85, fOut);
            fOut.flush();
            fOut.close();
            MediaStore.Images.Media.insertImage(context.getContentResolver(), file.getAbsolutePath(), file.getName(), file.getName());

            return file.getAbsolutePath();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static boolean removeImageFromStorage(String path) {
        File file = new File(path);
        return file.delete();
    }

    private static Bitmap getImageFromStringBase64(String base64) {
        base64 = base64.replace("base64,", "").substring(base64.indexOf("base64,"));
        byte[] imageAsBytes = Base64.decode(base64.getBytes(), 0);
        return BitmapFactory.decodeByteArray(
                imageAsBytes, 0, imageAsBytes.length);
    }

    public static String getStringBase64FromImage(Bitmap bm) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.JPEG, 70, stream);
        byte[] byteFormat = stream.toByteArray();
        return String.format("data:image/jpeg;base64,%s",Base64.encodeToString(byteFormat, Base64.NO_WRAP));
    }

    public static Bitmap getImageFromPath(String path) {
        File imgFile = new  File(path);
        if(imgFile.exists())
            return BitmapFactory.decodeFile(imgFile.getAbsolutePath());

        return null;
    }

}
