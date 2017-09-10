import com.google.gson.Gson;
import okhttp3.OkHttpClient;
import okhttp3.Request;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {

    private static final OkHttpClient HTTP_CLIENT = new OkHttpClient();

    private static final Pattern p = Pattern.compile("<td align='center';>(.*?)</td>\\r\\n(.*?)\\r\\n(.*)");
    private static final Pattern p2 = Pattern.compile("1\">([\\s\\S]*?)</a");

    public static void main(String[] args) {
        Structure structure = new Gson()
                .fromJson(
                        new InputStreamReader(Main.class.getClassLoader().getResourceAsStream("structure.json")),
                        Structure.class
                );
        File output = new File("output.csv");
        try {
            output.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        BufferedWriter writer;
        try {
            FileOutputStream fos = new FileOutputStream(output);
            fos.write(new byte[]{(byte) 0xef, (byte) 0xbb, (byte) 0xbf});
            writer = new BufferedWriter(new OutputStreamWriter(fos, StandardCharsets.UTF_8));
            writer.write("主類別,子類別,二級類別,同異類別,臺灣語詞,大陸語詞");
            writer.newLine();
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        for (Class c : structure.first) {
            for (int id = c.start; id <= c.stop; id++) {
                String second = structure.second.get(id);
                String[] third = structure.third.get(id);
                if (third == null) {
                    crawl(c.name, second, "", id, writer);
                } else {
                    for (String s : third) {
                        crawl(c.name, second, s, id, writer);
                    }
                }
            }
        }
    }

    private static void crawl(String first, String second, String third, int id, BufferedWriter writer) {
        String url = String.format("http://chinese-linguipedia.org/clk/diff?class=%d&classNa=%s&page=", id, third);
        for (int page = 1; ; page++) {
            Request req = new Request.Builder()
                    .url(url + page)
                    .build();
            String res;
            try {
                res = HTTP_CLIENT.newCall(req).execute().body().string();
            } catch (IOException e) {
                crawl(first, second, third, id, writer);
                return;
            }
            Matcher m = p.matcher(res);
            while (m.find()) {
                Matcher m2 = p2.matcher(m.group(2));
                List<String> traditional = new ArrayList<>();
                while (m2.find()) {
                    traditional.add(m2.group(1).trim());
                }
                m2 = p2.matcher(m.group(3));
                List<String> simplified = new ArrayList<>();
                while (m2.find()) {
                    simplified.add(m2.group(1).trim());
                }
                try {
                    writer.write(String.join(",",
                            first,
                            second,
                            third,
                            m.group(1),
                            String.join("/", traditional),
                            String.join("/", simplified)
                    ));
                    writer.newLine();
                    writer.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
            }
            if (!res.contains("下一頁"))
                break;
        }
    }
}

class Structure {
    Class[] first;
    Map<Integer, String> second;
    Map<Integer, String[]> third;
}

class Class {
    String name;
    int start;
    int stop;
}