
package app.ui;

import javax.swing.*;
import java.awt.*;

public class PinkButton extends JButton {
    private Color bg = new Color(239, 98, 159);
    private Color bgHover = new Color(227, 83, 147);
    private Color bgPress = new Color(206, 67, 132);
    private Color border = new Color(206, 120, 150);

    public PinkButton(String text) {
        super(text);
        setForeground(Color.WHITE);
        setFocusPainted(false);
        setOpaque(false);
        setContentAreaFilled(false);
        setBorder(BorderFactory.createEmptyBorder(10, 18, 10, 18));
        setFont(new Font("SansSerif", Font.BOLD, 14));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth(), h = getHeight();
        ButtonModel m = getModel();
        Color fill = m.isPressed() ? bgPress : (m.isRollover() ? bgHover : bg);
        g2.setColor(fill); g2.fillRect(0, 0, w - 1, h - 1);
        g2.setColor(border); g2.drawRect(0, 0, w - 1, h - 1);
        g2.dispose(); super.paintComponent(g);
    }
}
