package psycho.euphoria.translator;

import android.net.Uri;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import psycho.euphoria.translator.Shared;

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
                for (int i = 0; i < dataList.length(); i++) {
                    JSONArray sense = dataList.getJSONObject(i).getJSONArray("sense");
                    if (dataList.getJSONObject(i).has("pinyin"))
                        sb.append(dataList.getJSONObject(i).getString("pinyin")).append("\n");
                    for (int j = 0; j < sense.length(); j++) {
                        if (sense.getJSONObject(j).has("def"))
                            sb.append(sense.getJSONObject(j).getJSONArray("def").getString(0)).append("\n");
                    }
                }
                return sb.toString();

            }
            else if(obj.has("simple")){
                JSONArray dataList = obj.getJSONObject("simple")
                        .getJSONArray("word");
                StringBuffer sb = new StringBuffer();
                for (int i = 0; i < dataList.length(); i++) {

                    if (dataList.getJSONObject(i).has("return-phrase"))
                        sb.append(dataList.getJSONObject(i).getString("return-phrase")).append(" ");
                    if (dataList.getJSONObject(i).has("phone"))
                        sb.append(dataList.getJSONObject(i).getString("phone")).append("\n");

                }
                return sb.toString();
            }
        } else {
            if (obj.has("ec")) {
                JSONObject word = obj.getJSONObject("ec")
                        .getJSONArray("word").getJSONObject(0);
                JSONArray trs = word.getJSONArray("trs");
                StringBuffer sb = new StringBuffer();
                if (word.has("usphone"))
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

    public static String createTranslationURI(String query) throws URISyntaxException {
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

    public static String chinese(String s) {
        try {
            String request = createTranslationURI(s);
            URL url = new URL(request);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            JSONObject o = new JSONObject(Shared.readString(conn));
            JSONArray array = o.getJSONArray("translation");
            StringBuilder stringBuilder = new StringBuilder();
            for (int i = 0; i < array.length(); i++) {
                stringBuilder.append(array.getString(i)).append("\n");
            }
            return stringBuilder.toString();
        } catch (Exception e) {
            return null;
        }
    }

    public static String google(String s) {
        String uri = "http://translate.google.com/translate_a/single?client=gtx&sl=auto&tl=zh&dt=t&dt=bd&ie=UTF-8&oe=UTF-8&dj=1&source=icon&q=" + Uri.encode(s);
        try {
            HttpURLConnection h = (HttpURLConnection) new URL(uri).openConnection(new Proxy(Type.HTTP, new InetSocketAddress("127.0.0.1", 10809)));
            h.addRequestProperty("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/88.0.4324.182 Safari/537.36 Edg/88.0.705.74");
            h.addRequestProperty("Accept-Encoding", "gzip, deflate, br");
            String line = null;
            BufferedReader reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(h.getInputStream())));
            StringBuilder sb1 = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                sb1.append(line).append('\n');
            }
            JSONObject object = new JSONObject(sb1.toString());
            JSONArray array = object.getJSONArray("sentences");
            final StringBuilder sb = new StringBuilder();
            for (int i = 0; i < array.length(); i++) {
                sb.append(array.getJSONObject(i).getString("trans"));
            }
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
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

    public static String translateWord(String q) {
        try {
            String catchData = "https://dictionaryapi.com/api/v3/references/learners/json/" +
                    Uri.encode(q) + "?key=cfb57e42-44bb-449d-aa59-1a61d2ca31f0";
            URL url = new URL(catchData);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            InputStream is = connection.getInputStream();
            BufferedReader in = new BufferedReader(new InputStreamReader(is));
            String line = in.readLine();
            StringBuffer json = new StringBuffer();
            while (line != null) {
                json.append(line);
                line = in.readLine();
            }
            if (json.indexOf("\"shortdef\"") == -1) {
                return null;
            }
            JSONArray jsonArray = new JSONArray(String.valueOf(json));
            JSONObject jsonObject = jsonArray.getJSONObject(0);
            if (jsonObject == null) {
                return null;
            }
            JSONArray shortdefarray = jsonObject.getJSONArray("shortdef");
            StringBuilder stringBuilder = new StringBuilder();
            for (int i = 0; i < shortdefarray.length(); i++) {
                stringBuilder.append(shortdefarray.getString(i)).append('\n');
            }
            return stringBuilder.toString();
        } catch (Exception e) {
            return null;
        }
    }

    public static String translateCollegiate(String q) {
        try {
            String catchData = "https://dictionaryapi.com/api/v3/references/collegiate/json/" +
                    Uri.encode(q) + "?key=82b5749d-12a6-499f-a916-d9b85d400161";
            URL url = new URL(catchData);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            InputStream is = connection.getInputStream();
            BufferedReader in = new BufferedReader(new InputStreamReader(is));
            String line = in.readLine();
            StringBuffer json = new StringBuffer();
            while (line != null) {
                json.append(line);
                line = in.readLine();
            }
            JSONArray jsonArray = new JSONArray(String.valueOf(json));
            JSONObject jsonObject = jsonArray.getJSONObject(0);
            JSONArray shortdefarray = jsonObject.getJSONArray("shortdef");
            StringBuilder stringBuilder = new StringBuilder();
            for (int i = 0; i < shortdefarray.length(); i++) {
                stringBuilder.append(shortdefarray.getString(i)).append('\n');
            }
            return stringBuilder.toString();
        } catch (Exception e) {
            return null;
        }

    }

    public static String translateWords(String s) {
        String results = google(s);
        if (results == null)
            results = chinese(s);
        return results;
    }
}