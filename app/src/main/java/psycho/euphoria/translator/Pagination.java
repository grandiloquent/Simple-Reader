package psycho.euphoria.translator;

import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;

import java.util.ArrayList;
import java.util.List;

public class Pagination {
    private final int mHeight;
    private final boolean mIncludePad;
    private final List<CharSequence> mPages;
    private final TextPaint mPaint;
    private final float mSpacingAdd;
    private final float mSpacingMult;
    private final CharSequence mText;
    private final int mWidth;

    public Pagination(CharSequence text, int pageW, int pageH, TextPaint paint, float spacingMult, float spacingAdd, boolean inclidePad) {
        this.mText = text;
        this.mWidth = pageW;
        this.mHeight = pageH;
        this.mPaint = paint;
        this.mSpacingMult = spacingMult;
        this.mSpacingAdd = spacingAdd;
        this.mIncludePad = inclidePad;
        this.mPages = new ArrayList<>();
        layout();
    }

    public CharSequence get(int index) {
        return (index >= 0 && index < mPages.size()) ? mPages.get(index) : null;
    }

    public int size() {
        return mPages.size();
    }

    private void addPage(CharSequence text) {
        mPages.add(text);
    }

    private void layout() {
        final StaticLayout layout = new StaticLayout(mText, mPaint, mWidth, Layout.Alignment.ALIGN_NORMAL, mSpacingMult, mSpacingAdd, mIncludePad);
        final int lines = layout.getLineCount();
        final CharSequence text = layout.getText();
        int startOffset = 0;
        int height = mHeight;
        for (int i = 0; i < lines; i++) {
            if (height < layout.getLineBottom(i)) {
                // When the layout height has been exceeded
                addPage(text.subSequence(startOffset, layout.getLineStart(i)));
                startOffset = layout.getLineStart(i);
                height = layout.getLineTop(i) + mHeight;
            }
            if (i == lines - 1) {
                // Put the rest of the text into the last page
                addPage(text.subSequence(startOffset, layout.getLineEnd(i)));
                return;
            }
        }
    }
}
