package psycho.euphoria.translator;

import android.net.Uri;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Pattern;

public class TranslatorApi {
    public final static Pattern mChinese = Pattern.compile("[\\u4e00-\\u9fa5]");

    public static String translateChinese(String q) throws Exception {
        boolean isChinese = mChinese.matcher(q).find();
        String uri = isChinese ? "http://dict.youdao.com/jsonapi?xmlVersion=5.1&client=&dicts=%7B%22count%22%3A99%2C%22dicts%22%3A%5B%5B%22newhh%22%5D%5D%7D&keyfrom=&model=&mid=&imei=&vendor=&screen=&ssid=&network=5g&abtest=&jsonversion=2&q=" + q :
                "http://dict.youdao.com/jsonapi?xmlVersion=5.1&client=&dicts=%7B%22count%22%3A99%2C%22dicts%22%3A%5B%5B%22ec%22%5D%5D%7D&keyfrom=&model=&mid=&imei=&vendor=&screen=&ssid=&network=5g&abtest=&jsonversion=2&q=" + q;
        HttpURLConnection c = (HttpURLConnection) new URL(uri).openConnection();
        String s = Shared.readString(c);
        JSONObject obj = new JSONObject(s);
        if (isChinese) {
            if (obj.has("newhh")) {
                JSONArray dataList = obj.getJSONObject("newhh")
                        .getJSONArray("dataList");
                StringBuffer sb = new StringBuffer();
                for (int i = 0; i <dataList.length() ; i++) {
                    JSONArray sense = dataList.getJSONObject(i).getJSONArray("sense");
                    if(dataList.getJSONObject(i).has("pinyin"))
                        sb.append(dataList.getJSONObject(i).getString("pinyin")).append("\n");
                    for (int j = 0; j < sense.length(); j++) {
                        if(sense.getJSONObject(j).has("def"))
                            sb.append(sense.getJSONObject(j).getJSONArray("def").getString(0)).append("\n");
                    }
                }
                return sb.toString();

            }
        } else {
            if (obj.has("ec")) {
                JSONObject word = obj.getJSONObject("ec")
                        .getJSONArray("word").getJSONObject(0);
                JSONArray trs = word.getJSONArray("trs");
                StringBuffer sb = new StringBuffer();
                if(word.has("usphone"))
                sb.append(word.getString("usphone")).append("\n");
                for (int i = 0; i < trs.length(); i++) {
                    sb.append(trs.getJSONObject(i).getJSONArray("tr").getJSONObject(0)
                            .getJSONObject("l")
                            .getJSONArray("i")
                            .getString(0)).append("\n");
                }
                return sb.toString();

            }
        }
        return null;
    }

    private static String createTranslationURI(String query) throws URISyntaxException {
        Uri.Builder builder = new Uri.Builder();
        String salt = String.valueOf(System.currentTimeMillis());
        builder.scheme("http")
                .authority("openapi.youdao.com")
                .appendPath("api")
                .appendQueryParameter("from", "en")
                .appendQueryParameter("to", "zh_CHS")
                .appendQueryParameter("q", query)
                .appendQueryParameter("appKey", "4da34b556074bc9f")
                .appendQueryParameter("salt", salt)
                .appendQueryParameter("sign", generateSign(query, salt));
        return builder.build().toString();
    }

    private static String generateSign(String q, String salt) {
        String src = "4da34b556074bc9f" + q + salt + "Wt5i6HHltTGFAQgSUgofeWdFZyDxKwOy";
        return md5(src);
    }

    private static String md5(String string) {
        if (string == null) {
            return null;
        }
        char hexDigits[] = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                'A', 'B', 'C', 'D', 'E', 'F'};
        byte[] btInput = string.getBytes();
        try {
            /** 获得MD5摘要算法的 MessageDigest 对象 */
            MessageDigest mdInst = MessageDigest.getInstance("MD5");
            /** 使用指定的字节更新摘要 */
            mdInst.update(btInput);
            /** 获得密文 */
            byte[] md = mdInst.digest();
            /** 把密文转换成十六进制的字符串形式 */
            int j = md.length;
            char str[] = new char[j * 2];
            int k = 0;
            for (byte byte0 : md) {
                str[k++] = hexDigits[byte0 >>> 4 & 0xf];
                str[k++] = hexDigits[byte0 & 0xf];
            }
            return new String(str);
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }
}