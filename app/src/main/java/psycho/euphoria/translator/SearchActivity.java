package psycho.euphoria.translator;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import java.io.File;

import static psycho.euphoria.translator.MainActivity.KEY_NOTES;
import static psycho.euphoria.translator.Utils.getExternalStorageDocumentFile;


public class SearchActivity extends Activity {
    public static final String KEY_ID = "id";
    private Notes mNotes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String noteDatabaseDefaultFullPath = getExternalStorageDocumentFile(this, "notes.db").getAbsolutePath();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String f = preferences.getString(KEY_NOTES, noteDatabaseDefaultFullPath);
        if (!new File(f).exists()) {
            f = noteDatabaseDefaultFullPath;
        }
        mNotes = new Notes(this, f);
        ListView listView = new ListView(this);
        setContentView(listView);
        SearchAdapter adapter = new SearchAdapter();
        listView.setAdapter(adapter);
        adapter.update(mNotes.queryContents(getIntent().getStringExtra("q")));
        listView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent data=new Intent();
                data.putExtra(KEY_ID,adapter.getItem(position));
                setResult(RESULT_OK,data);
                finish();
            }
        });
    }
}