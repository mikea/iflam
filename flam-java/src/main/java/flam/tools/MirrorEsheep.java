package flam.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 */
public class MirrorEsheep {
    public static void main(String[] args) throws Exception {
        mirror("sheeps", "http://v2d7c.sheepserver.net/cgi/best.cgi?menu=best&p=1");
        mirror("sheeps", "http://v2d7c.sheepserver.net/cgi/best.cgi?menu=best&p=2");
        mirror("sheeps", "http://v2d7c.sheepserver.net/cgi/best.cgi?menu=best&p=3");
        mirror("sheeps", "http://v2d7c.sheepserver.net/cgi/best.cgi?menu=best&p=4");
        mirror("sheeps", "http://v2d7c.sheepserver.net/cgi/best.cgi?menu=best&p=5");
    }

    private static void mirror(String dir, String u) throws Exception {
        String content = loadUrl(u);

//        Pattern deadPattern = Pattern.compile("http://\\w+.sheepserver.net/cgi/dead.cgi?id=\\d+");
        Pattern deadPattern = Pattern.compile("dead\\.cgi\\?id=(\\d+)");
        Matcher deadMatcher = deadPattern.matcher(content);
        
        while (deadMatcher.find()) {
            int id = Integer.parseInt(deadMatcher.group(1));

            File genomeFile = new File(dir + "/" + id + ".flam3");
            if (!genomeFile.exists()) {
                System.out.println("id = " + id);
                String genomeUrl =
                        String.format("http://v2d7c.sheepserver.net/gen/244/%d/electricsheep.244.%05d.flam3", id, id);
                String genome = loadUrl(genomeUrl);
                FileOutputStream os = new FileOutputStream(genomeFile);
                os.write(genome.getBytes());
                os.close();
            }
        }
    }

    private static String loadUrl(String u) throws IOException {
        URL url = new URL(u);
        BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));

        StringBuilder result = new StringBuilder();
        String line;

        while ((line = in.readLine()) != null)
            result.append(line + "\n");

        in.close();

        return result.toString();
    }
}
