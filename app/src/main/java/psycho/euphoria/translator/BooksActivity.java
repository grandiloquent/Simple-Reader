package psycho.euphoria.translator;

import android.app.Activity;
import android.content.Intent;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.util.Log;
import android.widget.BaseAdapter;
import android.widget.ListView;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Collectors;

public class BooksActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ListView listView = new ListView(this);
        setContentView(listView);
        BooksAdapter adapter = new BooksAdapter();
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((parent, view, position, id) -> {
            Intent data = new Intent();
            data.putExtra(MainActivity.KEY_NOTES, Utils.getExternalStorageDocumentFile(this,adapter.getItem(position)).getAbsolutePath());
            setResult(RESULT_OK, data);
            finish();
        });
        if (VERSION.SDK_INT >= VERSION_CODES.O) {
            try {
                adapter.update(Files.list(Utils.getExternalStorageDocumentFile(this, "").toPath())
                                .filter(x->x.getFileName().toString().endsWith(".db"))
                        .map(x -> x.getFileName().toString()).collect(Collectors.toList()));
            } catch (Exception e) {

            }
        }
    }
}