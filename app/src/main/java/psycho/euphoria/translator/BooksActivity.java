package psycho.euphoria.translator;

import android.app.Activity;
import android.content.Intent;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.BaseAdapter;
import android.widget.ListView;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.Collator;
import java.util.Comparator;
import java.util.Locale;
import java.util.stream.Collectors;

public class BooksActivity extends Activity {
    BooksAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ListView listView = new ListView(this);
        setContentView(listView);
        registerForContextMenu(listView);
        adapter = new BooksAdapter();
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((parent, view, position, id) -> {
            String path = Utils.getExternalStorageDocumentFile(this, adapter.getItem(position)).getAbsolutePath();
            PreferenceManager.getDefaultSharedPreferences(this)
                    .edit().putString(MainActivity.KEY_NOTES, path).apply();
            Intent data = new Intent();
            data.putExtra(MainActivity.KEY_NOTES, path);
            setResult(RESULT_OK, data);
            finish();
        });
        try {
            Collator collator = Collator.getInstance(Locale.CHINA);
            adapter.update(Files.list(Utils.getExternalStorageDocumentFile(this, "").toPath())
                    .filter(x -> x.getFileName().toString().endsWith(".db"))
                    .map(x -> x.getFileName().toString())
                    .sorted(new Comparator<String>() {
                        @Override
                        public int compare(String s, String t1) {
                            return collator.compare(s, t1);
                        }
                    })
                    .collect(Collectors.toList()));
        } catch (Exception e) {
        }
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
            File file = Utils.getExternalStorageDocumentFile(this, adapter.getItem(menuInfo.position));
            file.delete();
        }
        return super.onContextItemSelected(item);
    }
}