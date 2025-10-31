
package app.ui;

import javax.swing.*;
import java.awt.*;

public class RoundPanel extends JPanel {
    private int arc = 4;
    private Color fill = new Color(248, 216, 227);
    private Color border = new Color(216, 160, 178);

    public RoundPanel() { setOpaque(false); }
    public void setArc(int a) { this.arc = a; repaint(); }
    public void setFill(Color c) { this.fill = c; repaint(); }
    public void setBorderColor(Color c) { this.border = c; repaint(); }

    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth(), h = getHeight();
        g2.setColor(fill); g2.fillRoundRect(0, 0, w - 1, h - 1, arc, arc);
        g2.setColor(border); g2.drawRoundRect(0, 0, w - 1, h - 1, arc, arc);
        g2.dispose(); super.paintComponent(g);
    }
}
