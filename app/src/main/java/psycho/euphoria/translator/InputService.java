package psycho.euphoria.translator;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputConnection;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;


// InputServiceHelper
public class InputService extends InputMethodService implements KeyboardView.OnKeyboardActionListener {

    private final Pattern mChinese = Pattern.compile("[\\u4e00-\\u9fa5]");
    private KeyboardView kv;
    private Keyboard keyboard;
    private String mCurrentString = "";
    private boolean caps = false;
    private Database mDatabase;

    public static String readAssetAsString(Context context, String assetName) {
        InputStream inputStream = null;
        try {
            inputStream = context.getAssets().open(assetName);
            int size = inputStream.available();
            byte[] buffer = new byte[size];
            inputStream.read(buffer);
            inputStream.close();
            return new String(buffer, StandardCharsets.UTF_8);

        } catch (IOException e) {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException ex) {
                }
            }

        }
        return null;
    }

    public static String translate(String to, String q) throws Exception {
        URL url = new URL("http://kingpunch.cn/translate?to=" + to + "&q=" + Uri.encode(q));
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        InputStream is = connection.getInputStream();
        BufferedReader in = new BufferedReader(new InputStreamReader(is));
        String line = in.readLine();
        StringBuilder json = new StringBuilder();
        while (line != null) {
            json.append(line);
            line = in.readLine();
        }
        JSONObject jsonObject = new JSONObject(json.toString());
        JSONArray sentences = jsonObject.getJSONArray("sentences");
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < sentences.length(); i++) {
            stringBuilder.append(sentences.getJSONObject(i).getString("trans")).append('\n');
        }
        return stringBuilder.toString();
    }

    public static String translateChineseWord(String q, Database database) throws Exception {
        String result = database.query(q);
        if (result != null) {
            return result;
        }
        result = TranslatorApi.translateChinese(q);
        if (result != null && !result.isEmpty()) {
            database.insert(q, result);
        }
        return result;
    }

    public static String translateCollegiate(String q, Database database) throws Exception {
        String result = database.query(q);
        if (result != null) {
            return result;
        }
        String catchData = "https://dictionaryapi.com/api/v3/references/collegiate/json/" +
                Uri.encode(q) + "?key=82b5749d-12a6-499f-a916-d9b85d400161";
        URL url = new URL(catchData);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        InputStream is = connection.getInputStream();
        BufferedReader in = new BufferedReader(new InputStreamReader(is));
        String line = in.readLine();
        StringBuffer json = new StringBuffer();
        while (line != null) {
            json.append(line);
            line = in.readLine();
        }
        JSONArray jsonArray = new JSONArray(String.valueOf(json));
        JSONObject jsonObject = jsonArray.getJSONObject(0);
        JSONArray shortdefarray = jsonObject.getJSONArray("shortdef");
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < shortdefarray.length(); i++) {
            stringBuilder.append(shortdefarray.getString(i)).append('\n');
        }
        if (stringBuilder.toString().length() > 0) {
            database.insert(q, stringBuilder.toString());
        }
        return stringBuilder.toString();
    }

    public static String translateWord(String q, Database database) throws Exception {
        String result = database.query(q);
        if (result != null) {
            return result;
        }
        String catchData = "https://dictionaryapi.com/api/v3/references/learners/json/" +
                Uri.encode(q) + "?key=cfb57e42-44bb-449d-aa59-1a61d2ca31f0";
        URL url = new URL(catchData);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        InputStream is = connection.getInputStream();
        BufferedReader in = new BufferedReader(new InputStreamReader(is));
        String line = in.readLine();
        StringBuffer json = new StringBuffer();
        while (line != null) {
            json.append(line);
            line = in.readLine();
        }
        if (json.indexOf("\"shortdef\"") == -1) {
            return null;
        }
        JSONArray jsonArray = new JSONArray(String.valueOf(json));
        JSONObject jsonObject = jsonArray.getJSONObject(0);
        if (jsonObject == null) {
            return null;
        }
        JSONArray shortdefarray = jsonObject.getJSONArray("shortdef");
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < shortdefarray.length(); i++) {
            stringBuilder.append(shortdefarray.getString(i)).append('\n');
        }
        if (stringBuilder.toString().length() > 0) {
            database.insert(q, stringBuilder.toString());
        }
        return stringBuilder.toString();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        ClipboardManager clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        clipboardManager.addPrimaryClipChangedListener(() -> {
            ClipData clipData = clipboardManager.getPrimaryClip();
            if (clipData == null) return;
            if (clipData.getItemCount() > 0) {
                CharSequence charSequence = clipData.getItemAt(0).getText();
                if (charSequence == null || mCurrentString.equals(charSequence.toString())) {
                    return;
                }
                mCurrentString = charSequence.toString();
                if (mCurrentString.startsWith("http://") || mCurrentString.startsWith("https://"))
                    return;
                if (!mChinese.matcher(charSequence.toString()).find()) {
                    new Thread(() -> {
                        String response = "";
                        try {
                            response = mCurrentString.contains(" ") ? translate("zh", mCurrentString) :
                                    (
                                             translateWord(mCurrentString, mDatabase));

                            if (response == null) {
                                response = translateCollegiate(mCurrentString, mDatabase);
                            }

                        } catch (Exception e) {
                        }
                        String finalResponse = response;
                        Shared.postOnMainThread(() -> {
                            Shared.createFloatView(this, finalResponse);
                        });
                    }).start();
                } else {
                    new Thread(() -> {
                        String response = "";
                        try {
                            response =translateChineseWord(mCurrentString, mDatabase); //translate("en", mCurrentString);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        String finalResponse = response;
                        Shared.postOnMainThread(() -> {
                            Shared.createFloatView(this, finalResponse);
                        });
                    }).start();
                }
            }
        });
        mDatabase = new Database(this,
                new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "psycho.db").getAbsolutePath());


    }

    @Override
    public View onCreateInputView() {
        kv = (KeyboardView) getLayoutInflater().inflate(R.layout.keyboard, null);
        keyboard = new Keyboard(this, R.xml.qwerty);
        // keyboard_sym = new Keyboard(this, R.xml.symbol);
        kv.setKeyboard(keyboard);
        kv.setOnKeyboardActionListener(this);
        return kv;
    }


    @Override
    public void onKey(int primaryCode, int[] keyCodes) {
        InputConnection ic = getCurrentInputConnection();
        switch (primaryCode) {
            case Keyboard.KEYCODE_DELETE:
                ic.deleteSurroundingText(1, 0);
                break;
//            case Keyboard.KEYCODE_SHIFT:
//                caps = !caps;
//                keyboard.setShifted(caps);
//                kv.invalidateAllKeys();
//                break;
//            case Keyboard.KEYCODE_DONE:
//                ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER));
//                break;
//            case 1000: {
//                // kv.setKeyboard(keyboard_sym);
//                break;
//            }
//            case 1001: {
//                //  kv.setKeyboard(keyboard);
//                break;
//            }
//            default:
//                char code = (char) primaryCode;
//                if (Character.isLetter(code) && caps) {
//                    code = Character.toUpperCase(code);
//                }
//                ic.commitText(String.valueOf(code), 1);
        }

    }

    @Override
    public void onPress(int primaryCode) {
        Log.e("SimpleKeyboard", "Hello3 " + primaryCode);

    }

    @Override
    public void onRelease(int primaryCode) {
    }

    @Override
    public void onText(CharSequence text) {
        Log.e("SimpleKeyboard", "Hello2 " + text);
    }

    @Override
    public void swipeDown() {
    }

    @Override
    public void swipeLeft() {
    }

    @Override
    public void swipeRight() {
    }

    @Override
    public void swipeUp() {
    }

    public static class Database extends SQLiteOpenHelper {

        public Database(Context context, String name) {
            super(context, name, null, 1);
        }

        public void insert(String word, String en) {
            ContentValues values = new ContentValues();
            values.put("word", word);
            values.put("en", en);
            values.put("create_at", System.currentTimeMillis());
            getWritableDatabase().insert("words", null, values);
        }

        public String query(String word) {
            Cursor cursor = getReadableDatabase().rawQuery("select en from words where word = ? limit 1", new String[]{word});
            String result = null;
            if (cursor.moveToNext()) {
                result = cursor.getString(0);
            }
            cursor.close();
            return result;
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("create table if not exists words (_id integer primary key,word text unique, en text, create_at integer)");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        }
    }
}
