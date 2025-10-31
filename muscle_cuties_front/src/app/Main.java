
package app;

import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Theme.apply();
            new app.screens.SplashFrame(() -> new app.screens.LoginFrame().setVisible(true)).start();
        });
    }
}
