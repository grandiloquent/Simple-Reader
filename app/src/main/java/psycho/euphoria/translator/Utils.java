package psycho.euphoria.translator;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.widget.TextView;

import org.jsoup.Jsoup;
import org.jsoup.helper.Validate;
import org.jsoup.internal.StringUtil;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;
import org.jsoup.select.NodeTraversor;
import org.jsoup.select.NodeVisitor;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class Utils {

    public static List<String> collectFiles(String htmlString) {
        Document document = Jsoup.parse(htmlString);
        Elements elements = document.select("navPoint");
        if (elements.size() == 0) {
            return null;
        }
        List<String> files = new ArrayList<>();
        for (int i = 0; i < elements.size(); i++) {
            Element content = elements.get(i).selectFirst("content");
            if (content == null) continue;
            String src = content.attr("src");
            if (src.contains("#")) {
                src = src.split("#")[0];
            }
            if (files.contains(src)) {
                continue;
            }
            files.add(src);
        }
        return files;
    }

    public static String getPlainText(String htmlString) {
        // fetch the specified URL and parse to a HTML DOM
//        Document doc = Jsoup.parse(htmlString);
//        HtmlToPlainText formatter = new HtmlToPlainText();
//        return formatter.getPlainText(doc);
        Document doc = Jsoup.parse(htmlString);
        String contents = doc.body().outerHtml();
        contents=contents.replaceAll("[\r\n]+"," ");
        contents=contents.replaceAll("\\s{2,}"," ");
        contents = contents.replaceAll("</((p)|(div))>\\s*", "\n\n");
        contents = contents.replaceAll("<[^>]*>", "");

        return contents;
    }

    public static class HtmlToPlainText {
        private static final String userAgent = "Mozilla/5.0 (jsoup)";
        private static final int timeout = 5 * 1000;


        /**
         * Format an Element to plain-text
         *
         * @param element the root element to format
         * @return formatted text
         */
        public String getPlainText(Element element) {
            FormattingVisitor formatter = new FormattingVisitor();
            NodeTraversor.traverse(formatter, element); // walk the DOM, and call .head() and .tail() for each node
            return formatter.toString();
        }

        // the formatting rules, implemented in a breadth-first DOM traverse
        private class FormattingVisitor implements NodeVisitor {
            private static final int maxWidth = 80;
            private int width = 0;
            private StringBuilder accum = new StringBuilder(); // holds the accumulated text

            // hit when the node is first seen
            public void head(Node node, int depth) {
                String name = node.nodeName();
                if (node instanceof TextNode)
                    append(((TextNode) node).text()); // TextNodes carry all user-readable text in the DOM.
                else if (name.equals("li"))
                    append("\n * ");
                else if (name.equals("dt"))
                    append("  ");
                else if (StringUtil.in(name, "p", "h1", "h2", "h3", "h4", "h5", "tr"))
                    append("\n");
            }

            // hit when all of the node's children (if any) have been visited
            public void tail(Node node, int depth) {
                String name = node.nodeName();
                if (StringUtil.in(name, "br", "dd", "dt", "p", "h1", "h2", "h3", "h4", "h5"))
                    append("\n");
                else if (name.equals("a") || name.equals("span"))
                    append(" ");
            }

            // appends text to the string builder with a simple word wrap method
            private void append(String text) {
                if (text.startsWith("\n"))
                    width = 0; // reset counter if starts with a newline. only from formats above, not in natural text
                if (text.equals(" ") &&
                        (accum.length() == 0 || StringUtil.in(accum.substring(accum.length() - 1), " ", "\n")))
                    return; // don't accumulate long runs of empty spaces
                if (text.length() + width > maxWidth) { // won't fit, needs to wrap
                    String[] words = text.split("\\s+");
                    for (int i = 0; i < words.length; i++) {
                        String word = words[i];
                        boolean last = i == words.length - 1;
                        if (!last) // insert a space if not the last word
                            word = word + " ";
                        if (word.length() + width > maxWidth) { // wrap and reset counter
                            accum.append("\n").append(word);
                            width = word.length();
                        } else {
                            accum.append(word);
                            width += word.length();
                        }
                    }
                } else { // fits as is, without need to wrap text
                    accum.append(text);
                    width += text.length();
                }
            }

            @Override
            public String toString() {
                return accum.toString();
            }
        }
    }

    @TargetApi(VERSION_CODES.O)
    public static String readAllText(String path) throws Exception {
        byte[] bytes = Files.readAllBytes(Paths.get(path));
        return new String(bytes, "gbk");
    }

    public static long packRangeInLong(int start, int end) {
        return (((long) start) << 32) | end;
    }

    public static int unpackRangeEndFromLong(long range) {
        return (int) (range & 0x00000000FFFFFFFFL);
    }

    public static int unpackRangeStartFromLong(long range) {
        return (int) (range >>> 32);
    }


    public static long getCharRange(String text, int offset) {
        final int textLength = text.length();
        if (offset + 1 < textLength) {
            final char currentChar = text.charAt(offset);
            final char nextChar = text.charAt(offset + 1);
            if (Character.isSurrogatePair(currentChar, nextChar)) {
                return packRangeInLong(offset, offset + 2);
            }
        }
        if (offset < textLength) {
            return packRangeInLong(offset, offset + 1);
        }
        if (offset - 2 >= 0) {
            final char previousChar = text.charAt(offset - 1);
            final char previousPreviousChar = text.charAt(offset - 2);
            if (Character.isSurrogatePair(previousPreviousChar, previousChar)) {
                return packRangeInLong(offset - 2, offset);
            }
        }
        if (offset - 1 >= 0) {
            return packRangeInLong(offset - 1, offset);
        }
        return packRangeInLong(offset, offset);
    }

    public static File getExternalStorageDocumentFile(Context context, String fileName) {
        return new File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName);
    }

    public static void requestManageAllFilePermission(Context context) {
        if (VERSION.SDK_INT >= VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                //request for the permission
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                Uri uri = Uri.fromParts("package", context.getPackageName(), null);
                intent.setData(uri);
                context.startActivity(intent);
            }
        }
    }

}