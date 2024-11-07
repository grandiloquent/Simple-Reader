package psycho.euphoria.translator;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import psycho.euphoria.translator.InputService.Database;

import static psycho.euphoria.translator.MainActivity.KEY_NOTES;
import static psycho.euphoria.translator.Utils.getExternalStorageDocumentFile;


public class DictionaryActivity extends Activity {
    Database mDatabase;
    DictionaryAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDatabase = new Database(this,
                new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "psycho.db").getAbsolutePath());
        ListView listView = new ListView(this);
        setContentView(listView);
        adapter = new DictionaryAdapter();
        listView.setAdapter(adapter);
        adapter.update(mDatabase.listAll());
        listView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Toast.makeText(DictionaryActivity.this,
                        adapter.getItem(position), Toast.LENGTH_LONG).show();
            }
        });
        registerForContextMenu(listView);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        menu.add(0, 0, 0, "删除");
        super.onCreateContextMenu(menu, v, menuInfo);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo) item.getMenuInfo();
        int id = item.getItemId();
        if (id == 0) {
            mDatabase.deleteWord(adapter.getItem(menuInfo.position)
                    .split(":",2)[0]
            );
            adapter.update(mDatabase.listAll());
        }
        return super.onContextItemSelected(item);
    }

    public static class Database extends SQLiteOpenHelper {

        public Database(Context context, String name) {
            super(context, name, null, 1);
        }

        public void deleteWord(String word) {
            getWritableDatabase().delete("words", "word = ?", new String[]{word});
        }

        public List<String> listAll() {
            Cursor cursor = getReadableDatabase().rawQuery("select word,en from words order by create_at desc", new String[]{});
            List<String> result = new ArrayList<>();
            while (cursor.moveToNext()) {
                result.add(String.format("%s: %s", cursor.getString(0), cursor.getString(1)));
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