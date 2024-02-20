package psycho.euphoria.translator;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class Notes extends SQLiteOpenHelper {
    public Notes(Context context, String name) {
        super(context, name, null, 1);
    }

    public List<String> getIds() {
        List<String> list = new ArrayList<>();
        Cursor cursor = getReadableDatabase().rawQuery("select id from note", null);
        while (cursor.moveToNext()) {
            list.add(Integer.toString(cursor.getInt(0)));
        }
        cursor.close();
        return list;
    }

    public void insertString(String s) {
//            List<String> lines = new ArrayList<>();
//            String[] array = s.split("\n");
//            String line = "";
//            for (int i = 0; i < array.length; i++) {
//                line += array[i] + "\n";
//                if (line.length() > 800) {
//                    lines.add(line);
//                    line = "";
//                }
//            }
//            if (line.length() > 0) {
//                lines.add(line);
//            }
//            for (int i = 0; i < lines.size(); i++) {
//                ContentValues values = new ContentValues();
//                values.put("content", lines.get(i));
//                getWritableDatabase().insert("note", null, values);
//            }
        ContentValues values = new ContentValues();
        values.put("content", s);
        getWritableDatabase().insert("note", null, values);
    }

    public String queryContent(String id) {
        String s = null;
        Cursor cursor = getReadableDatabase().rawQuery("select content from note where id = ?", new String[]{id});
        if (cursor.moveToNext()) {
            s = cursor.getString(0);
        }
        cursor.close();
        return s;
    }

    public List<Integer> queryContents(String q) {
        Pattern pattern = Pattern.compile(q);
        List<Integer> ids = new ArrayList<>();
        Cursor cursor = getReadableDatabase().rawQuery("select id,content from note", null);
        while (cursor.moveToNext()) {
            if (pattern.matcher(cursor.getString(1)).find()) {
                ids.add(cursor.getInt(0));
            }
        }
        cursor.close();
        return ids;
    }

    public void removeAll() {
        getWritableDatabase().delete("note", "id<?", new String[]{
                "10000"
        });
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table note(id integer primary key,content text)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }
}
