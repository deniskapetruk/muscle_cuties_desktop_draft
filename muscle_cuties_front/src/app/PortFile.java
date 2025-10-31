
package app;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public class PortFile {
    public static int read() {
        String[] candidates = new String[] { "port.txt", "../port.txt", "../../port.txt" };
        for (String path : candidates) {
            File f = new File(path);
            if (f.exists()) {
                try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                    String x = br.readLine();
                    int prt = Integer.parseInt(x.trim());
                    if (prt > 0 && prt <= 65535) return prt;
                } catch (Exception ignored) {}
            }
        }
        return 8000;
    }
}
