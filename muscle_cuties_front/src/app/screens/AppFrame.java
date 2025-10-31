
package app.screens;

import app.ApiClient;
import app.Theme;
import app.ui.RoundPanel;

import javax.swing.*;
import java.awt.*;

public class AppFrame extends JFrame {
    public AppFrame(ApiClient api) {
        setTitle("Muscle Dashboard");
        setSize(1100, 760);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        JPanel root = new JPanel(new BorderLayout());
        RoundPanel top = new RoundPanel();
        top.setFill(Theme.PINK_PRIMARY);
        top.setBorderColor(Theme.PINK_PRIMARY);
        top.setArc(6);
        top.setPreferredSize(new Dimension(1000, 70));
        top.setLayout(new BorderLayout());
        JLabel t = new JLabel("MUSCLE CUTIES");
        t.setForeground(Color.WHITE);
        t.setFont(Theme.brand(28f));
        t.setHorizontalAlignment(SwingConstants.CENTER);
        top.add(t, BorderLayout.CENTER);
        root.add(top, BorderLayout.NORTH);
        JTabbedPane tabs = new JTabbedPane();
        tabs.setBackground(Theme.PINK_SOFT);
        if ("CLIENT".equalsIgnoreCase(api.getRole())) {
            tabs.addTab("Workout", new DashboardPanel(api));
            tabs.addTab("Chat", new ChatClientPanel(api));
        } else {
            tabs.addTab("Chat", new ChatTrainerPanel(api));
        }
        root.add(tabs, BorderLayout.CENTER);
        setContentPane(root);
    }
}
