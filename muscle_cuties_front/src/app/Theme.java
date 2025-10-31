
package app;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

public class Theme {
    public static final Color PINK_PRIMARY = new Color(239, 98, 159);
    public static final Color PINK_SOFT = new Color(248, 216, 227);
    public static final Color PINK_ACCENT = new Color(225, 67, 134);
    public static final Color PINK_BORDER = new Color(216, 160, 178);
    public static final Color INK = new Color(48, 48, 52);
    public static final Color SURFACE = new Color(255, 255, 255);

    public static final Font H1 = new Font("SansSerif", Font.BOLD, 26);
    public static final Font H3 = new Font("SansSerif", Font.BOLD, 18);
    public static final Font BODY = new Font("SansSerif", Font.PLAIN, 14);

    private static Font brand;

    public static void apply() {
        try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); } catch (Exception ignored) {}
        UIManager.put("Panel.background", PINK_SOFT);
        UIManager.put("OptionPane.background", PINK_SOFT);
        UIManager.put("Label.foreground", INK);
        UIManager.put("Button.background", PINK_PRIMARY);
        UIManager.put("Button.foreground", Color.WHITE);
        UIManager.put("TextField.background", SURFACE);
        UIManager.put("PasswordField.background", SURFACE);
        UIManager.put("ComboBox.background", SURFACE);
        loadBrandFont();
    }

    public static Font brand(float size) {
        if (brand != null) return brand.deriveFont(size);
        return H1.deriveFont(size);
    }

    private static void loadBrandFont() {
        if (brand != null) return;
        String[] candidates = new String[] {
                "fonts/Balonku.ttf",
                "../fonts/Balonku.ttf",
                "../../fonts/Balonku.ttf"
        };
        for (String p : candidates) {
            try (InputStream is = new FileInputStream(new File(p))) {
                Font f = Font.createFont(Font.TRUETYPE_FONT, is);
                GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(f);
                brand = f;
                return;
            } catch (Exception ignored) {}
        }
        brand = null;
    }
}
