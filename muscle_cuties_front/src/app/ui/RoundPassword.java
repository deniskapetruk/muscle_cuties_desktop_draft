
package app.ui;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class RoundPassword extends JPasswordField {
    public RoundPassword() {
        setOpaque(true);
        setBackground(Color.WHITE);
        setCaretColor(new Color(48, 48, 52));
        setSelectionColor(new Color(248, 216, 227));
        setBorder(new javax.swing.border.CompoundBorder(
                new LineBorder(new Color(216,160,178), 1, true),
                new EmptyBorder(12, 14, 12, 14)
        ));
    }
}
