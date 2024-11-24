package psycho.euphoria.translator;

import android.Manifest.permission;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.SearchView;
import android.widget.SearchView.OnQueryTextListener;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.regex.Pattern;

import psycho.euphoria.translator.InputService.Database;

import static psycho.euphoria.translator.Utils.getExternalStorageDocumentFile;
import static psycho.euphoria.translator.Utils.requestManageAllFilePermission;

public class MainActivity extends Activity {
    public static final String KEY_NOTES = "notes";
    public static final String KEY_Q = "q";
    public static final int SEARCH_REQUEST_CODE = 1;

    static {
/*
加载编译Rust代码后得到共享库。它完整的名称为librust.so
  */
        System.loadLibrary("nativelib");
    }
    public static final int DEFAULT_PORT = 8080;
    TextView mTextView;
    private Database mDatabase;
    private Notes mNotes;
    int mIndex = 0;
    private Pagination mPagination;
    private BlobCache mBlobCache;
    String mFile;

    public static native void deleteCamera();

    public static native void openCamera();

    public static native void takePhoto();

    public static void takePhotos() {
        openCamera();
        new CountDownTimer(30000, 1000) {
            public void onFinish() {
                deleteCamera();
            }

            public void onTick(long millisUntilFinished) {
                takePhoto();
            }
        }.start();
    }
    public static native String startServer(Context context, AssetManager assetManager, String host, int port);


    private void importEpub() {
        ClipboardManager clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        String contents = clipboardManager.getText().toString();
        File dir = new File("/storage/emulated/0/Books/导入");
        if (!dir.isDirectory())
            dir.mkdirs();
        if (new File(contents).exists()) {
            for (File d : dir.listFiles(new FileFilter() {
                @Override
                public boolean accept(File file) {
                    return file.isDirectory();
                }
            })) {
                try {
                    File f = Files.find(d.toPath(), 5, new BiPredicate<Path, BasicFileAttributes>() {
                        @Override
                        public boolean test(Path path, BasicFileAttributes basicFileAttributes) {
                            return !basicFileAttributes.isDirectory() && path.toFile().getName().endsWith(".ncx");
                        }
                    }).findFirst().get().toFile();
                    if (f != null) {
                        readToc(f.getAbsolutePath());
                    }
                } catch (Exception ignored) {
                }
            }
        } else {
            insertString(contents);
        }
    }

    private void importFile() {
        ClipboardManager clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        String v = clipboardManager.getText().toString();
        File f = new File(v);
        if (f.exists()) {

            f = f.getParentFile();
            File[] files = f.listFiles(new FileFilter() {
                @Override
                public boolean accept(File file) {
                    return file.isFile() && file.getName().endsWith(".txt");
                }
            });
            if (files != null && files.length > 0) {
                for (File file : files) {
                    File finalF = file;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, finalF.getName(), Toast.LENGTH_SHORT).show();
                        }
                    });
                    importTexts(file.getAbsolutePath());
                }
            }
        } else {
            importTexts(v);
        }
    }

    private void importTexts(String v) {
        if (new File(v).exists()) {
            String fileName = Shared.substringAfterLast(v, "/");
            fileName = Shared.substringBeforeLast(fileName, ".");
            fileName = getExternalStorageDocumentFile(this, fileName + ".db").getAbsolutePath();
            if (new File(fileName).exists())
                return;
            Notes notes = new Notes(this, fileName);
            String contents = null;
            try {
                contents = Utils.readAllText(v);
                insertString(contents, notes);
            } catch (Exception e) {
            }

        } else {
            insertString(v);
        }
    }

    private void insertString(String contents) {
        mPagination = new Pagination(contents,
                1080 - 132 * 2,
                mTextView.getHeight() - 132
                ,
                mTextView.getPaint(),
                mTextView.getLineSpacingMultiplier(),
                mTextView.getLineSpacingExtra(),
                mTextView.getIncludeFontPadding());
        for (int i = 0; i < mPagination.size(); i++) {
            mNotes.insertString(mPagination.get(i).toString());
        }
    }

    private void insertString(String contents, Notes notes) {
        mPagination = new Pagination(contents,
                1080 - 132 * 2,
                1600//mTextView.getHeight() - 132 * 2
                ,
                mTextView.getPaint(),
                mTextView.getLineSpacingMultiplier(),
                mTextView.getLineSpacingExtra(),
                mTextView.getIncludeFontPadding());
        for (int i = 0; i < mPagination.size(); i++) {
            notes.insertString(mPagination.get(i).toString());
        }
    }

    private void readToc(String file) {
        if (VERSION.SDK_INT >= VERSION_CODES.O) {
            try {
                String s = Utils.readAllText(file);
                String fileName = Shared.substringAfter(s, "<docTitle>");
                fileName = Shared.substringBefore(fileName, "</docTitle>");
                fileName = Shared.substringAfter(fileName, "<text>");
                fileName = Shared.substringBefore(fileName, "</text>");
                fileName = fileName.trim();
                fileName = fileName.replaceAll("[^0-9A-Za-z- ]+", "");
                fileName = getExternalStorageDocumentFile(this, fileName + ".db").getAbsolutePath();
                Notes notes = new Notes(this, fileName);
                List<String> files = Utils.collectFiles(s);
                File parent = new File(file).getParentFile();
                for (int i = 0; i < files.size(); i++) {
                    File f = new File(parent, files.get(i));
                    String contents = Utils.getPlainText(Utils.readAllText(f.getAbsolutePath()));
                    if (contents != null) {
                        insertString(contents, notes);
                    }
                }
            } catch (Exception e) {
            }
        }
    }

    private void saveSet() {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(bos);
            dos.writeInt(mIndex);
            dos.flush();
            mBlobCache.insert(mFile.hashCode(), bos.toByteArray());
        } catch (Exception ignored) {
        }
    }

    private String trans(String s) {
        if (s.length() > 1 && TranslatorApi.mChinese.matcher(s).find()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < s.length(); i++) {
                try {
                    sb.append(InputService.translateChineseWord(s.substring(i, i + 1), mDatabase))
                            .append("\n");
                } catch (Exception e) {
                }
            }
            return sb.toString();
        } else {
            try {
                return InputService.translateChineseWord(s, mDatabase);
            } catch (Exception e) {
                return null;
            }
        }
    }

    private void translate(String s) {
        new Thread(() -> {
            try {
                String v = trans(s);
                if (v == null) {
                    ClipboardManager manager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    manager.setPrimaryClip(ClipData.newPlainText(null, s));
                    Shared.postOnMainThread(() -> {
                        Toast.makeText(MainActivity.this, "已复制到剪切板", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }
                Shared.postOnMainThread(() -> {
                    Shared.createFloatView(this, s + "\n" + v);
                });
            } catch (Exception e) {
                ClipboardManager manager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                manager.setPrimaryClip(ClipData.newPlainText(null, s));
                Shared.postOnMainThread(() -> {
                    Toast.makeText(MainActivity.this, "已复制到剪切板", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == SEARCH_REQUEST_CODE) {
            mIndex = data.getIntExtra(SearchActivity.KEY_ID, SEARCH_REQUEST_CODE);
            loadSpecifiedPage();
        }
        if (resultCode == RESULT_OK && requestCode == 2) {
            String notes = data.getStringExtra(KEY_NOTES);
            if (notes == null) {
                return;
            }
            mFile = notes;
            mNotes = new Notes(this, mFile);
            mIndex = 1;
            try {
                byte[] datav = mBlobCache.lookup(mFile.hashCode());
                if (datav != null) {
                    DataInputStream dis = new DataInputStream(
                            new ByteArrayInputStream(datav));
                    mIndex = dis.readInt();
                }
            } catch (IOException e) {
            }
            loadSpecifiedPage();
            PreferenceManager.getDefaultSharedPreferences(this).edit()
                    .putString(KEY_NOTES, notes).apply();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestManageAllFilePermission(this);
        List<String> permissions = new ArrayList<>();
        if (checkSelfPermission(permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(permission.CAMERA);
        }
        if (checkSelfPermission(permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(permission.WRITE_EXTERNAL_STORAGE);
        }
        if (!permissions.isEmpty()) {
            requestPermissions(permissions.toArray(new String[0]), 0);
        }
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        mDatabase = new Database(this,
                new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "psycho.db").getAbsolutePath());
        String noteDatabaseDefaultFullPath = getExternalStorageDocumentFile(this, "notes.db").getAbsolutePath();
        mFile = preferences.getString(KEY_NOTES, noteDatabaseDefaultFullPath);
        if (!new File(mFile).exists()) {
            mFile = noteDatabaseDefaultFullPath;
        }
        File cacheDir = getExternalCacheDir();
        String path = cacheDir.getAbsolutePath() + "/bookmark";
        try {
            mBlobCache = new BlobCache(path, 100 * 10, 10 * 10 * 1024, false,
                    1);
            byte[] data = mBlobCache.lookup(mFile.hashCode());
            if (data != null) {
                DataInputStream dis = new DataInputStream(
                        new ByteArrayInputStream(data));
                mIndex = dis.readInt();
            }
        } catch (IOException e) {
            mIndex = 1;
        }
        mNotes = new Notes(this, mFile);
        setContentView(R.layout.main);
        mTextView = findViewById(R.id.text_view);
        final WordIterator wordIterator = new WordIterator();
        mTextView.setText(mNotes.queryContent(Integer.toString(mIndex)));
        float dip = 48f;
        Resources r = getResources();
        float px = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dip,
                r.getDisplayMetrics()
        );
        mTextView.setOnTouchListener((view, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    final float x = event.getX();
                    final float y = event.getY();
                    if (x < 132) {//
                        navigateToPreviousPage();
                        if (y > 1800) {
                            takePhotos();
                        }
                        return true;
                    }
                    if (x > 1080 - 132) {
                        navigateToNextPage();
                        return true;
                    }
                    String s = getWordTouched(x, y, wordIterator);
                    if (s.length() > 0) {
                        translate(s);
                    }
                    // Remember finger down position, to be able to start selection from there
                    //Log.e("B5aOx2", String.format("onTouch, %s",  mTextView.getText().subSequence(selectionStart,selectionEnd)));
            }
//                    mOffset = mTextView.getOffsetForPosition(motionEvent.getX(), motionEvent.getY());
//                    String s = findWordForRightHanded(mTextView.getText().toString(), mOffset);
//                    if (s.length() > 0) {
//                        Matcher matcher = mPattern.matcher(s);
//                        if (matcher.find()) {
//                            translate(matcher.group());
//                        }
//                    }
            return false;
        });
//        findViewById(R.id.text_view)
//                .setOnClickListener(v -> {
//                    Toast.makeText(this, "设置 > 搜索输入法 > 语言和输入法 > 输入法管理 > 翻译工具 > 启动", Toast.LENGTH_LONG).show();
//                    ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE)).showInputMethodPicker();
//
//                });
        if (VERSION.SDK_INT >= VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, 0);
            }
        }
        mTextView.setBackgroundColor(Color.BLACK);
        mTextView.setTextColor(0xFF999999);
        //getWindow().setBackgroundDrawable(new ColorDrawable(Color.BLACK));
        //getActionBar().setBackgroundDrawable(new ColorDrawable(Color.BLACK));
//        int actionBarTitleId = Resources.getSystem().getIdentifier("action_bar_title", "id", "android");
//        if (actionBarTitleId > 0) {
//            TextView title = (TextView) findViewById(actionBarTitleId);
//            if (title != null) {
//                title.setTextColor(0xFF999999);
//            }
//        }
//        getWindow().setNavigationBarColor(Color.TRANSPARENT);
//        getWindow().setStatusBarColor(Color.BLACK);
        //getWindow().getDecorView().setSystemUiVisibility(0);
        //getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
    }

    @Override
    protected void onPause() {
        saveSet();
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        createSearchView(menu);
        MenuItem menuItem2 = menu.add(0, 7, 0, "上一页");
        menuItem2.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        MenuItem menuItem1 = menu.add(0, 3, 0, "下一页");
        menuItem1.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menu.add(0, SEARCH_REQUEST_CODE, 0, "文本");
        menu.add(0, 2, 0, "列表");
        menu.add(0, 5, 0, "清空");
        menu.add(0, 8, 0, "书籍");
        menu.add(0, 9, 0, "复制");
        menu.add(0, 10, 0, "词典");

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case SEARCH_REQUEST_CODE:
                new Thread(this::importFile).start();
                break;
            case 2:
                String[] ids = mNotes.getIds().toArray(new String[0]);
                new AlertDialog.Builder(this)
                        .setItems(ids, (dialog, which) -> {
                            mIndex = Integer.parseInt(ids[which]);
                            saveSet();
                            mTextView.setText(mNotes.queryContent(ids[which]));
                        }).show();
                break;
            case 3:
                navigateToNextPage();
                break;
            case 7:
                navigateToPreviousPage();
                break;
            case 8:
                Intent intent = new Intent(MainActivity.this, BooksActivity.class);
                startActivityForResult(intent, 2);
                break;
            case 9:
                getSystemService(ClipboardManager.class).setPrimaryClip(ClipData.newPlainText(null,
                        mTextView.getText()));
                break;
            case 5:
                mNotes.removeAll();
                break;
            case 10:
                Intent dic = new Intent(MainActivity.this, DictionaryActivity.class);
                startActivity(dic);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    native void cameraPreview();

    void createSearchView(Menu menu) {
        MenuItem searchItem = menu.add(0, 11, 0, "搜索");
        SearchView searchView = new SearchView(this);
        searchItem.setActionView(searchView);
        searchView.setIconified(true);
        searchItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        searchView.setOnQueryTextListener(new OnQueryTextListener() {
            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }

            @Override
            public boolean onQueryTextSubmit(String query) {
                Intent intent = new Intent(MainActivity.this, SearchActivity.class);
                intent.putExtra(KEY_Q, query);
                startActivityForResult(intent, SEARCH_REQUEST_CODE);
                return false;
            }
        });
    }

    void navigateToPreviousPage() {
        mIndex--;
        loadSpecifiedPage();
    }

    void navigateToNextPage() {
        mIndex++;
        loadSpecifiedPage();
    }

    void loadSpecifiedPage() {
        mTextView.setText(mNotes.queryContent(Integer.toString(mIndex)));
        this.setTitle(Integer.toString(mIndex));
        saveSet();
    }

    String getWordTouched(float x, float y, WordIterator wordIterator) {
        int t = mTextView.getOffsetForPosition(x, y);
        long lastTouchOffsets = Utils.packRangeInLong(t, t);
        final int minOffset = Utils.unpackRangeStartFromLong(lastTouchOffsets);
        final int maxOffset = Utils.unpackRangeEndFromLong(lastTouchOffsets);
        wordIterator.setCharSequence(mTextView.getText().toString(), minOffset, maxOffset);
        int selectionStart, selectionEnd;
        selectionStart = wordIterator.getBeginning(minOffset);
        selectionEnd = wordIterator.getEnd(maxOffset);
        if (selectionStart == BreakIterator.DONE || selectionEnd == BreakIterator.DONE ||
                selectionStart == selectionEnd) {
            // Possible when the word iterator does not properly handle the text's language
            long range = Utils.getCharRange(mTextView.getText().toString(), minOffset);
            selectionStart = Utils.unpackRangeStartFromLong(range);
            selectionEnd = Utils.unpackRangeEndFromLong(range);
        }
        return mTextView.getText().subSequence(selectionStart, selectionEnd).toString().trim();
    }
}