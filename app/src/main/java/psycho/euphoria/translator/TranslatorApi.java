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

public class TranslatorApi {
    public static String translateChinese(String q) throws Exception {
        String uri = createTranslationURI(q);
        HttpURLConnection c = (HttpURLConnection) new URL(uri).openConnection();
        String s = Shared.readString(c);
        JSONObject obj = new JSONObject(s);
        if (obj.has("errorCode") && obj.getString("errorCode").equals("0")) {
            if (obj.has("basic")) {
                JSONArray explains = obj.getJSONObject("basic").getJSONArray("explains");
                StringBuffer sb = new StringBuffer();
                for (int i = 0; i < explains.length(); i++) {
                    sb.append(explains.getString(i)).append("\n");
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