package app.screens;

import app.Theme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class SplashFrame extends JWindow {
    private final Timer timer;

    public SplashFrame(Runnable onDone) {
        Theme.apply();
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Color.WHITE);

        JLabel title = new JLabel("Muscle Cuties", SwingConstants.CENTER);
        title.setFont(Theme.brand(42f));
        title.setForeground(Theme.PINK_ACCENT);
        title.setBorder(BorderFactory.createEmptyBorder(32, 24, 12, 24));

        JProgressBar bar = new JProgressBar();
        bar.setIndeterminate(true);
        bar.setBorder(BorderFactory.createEmptyBorder(0, 24, 24, 24));

        root.add(title, BorderLayout.CENTER);
        root.add(bar, BorderLayout.SOUTH);
        setContentPane(root);
        pack();
        // Nice fixed size
        setSize(360, 180);
        setLocationRelativeTo(null);

        // Auto-close after 1.2s
        timer = new Timer(1200, (ActionEvent e) -> {}); // пустой слушатель
        timer.addActionListener(e -> {
            timer.stop();
            setVisible(false);
            dispose();
            if (onDone != null) onDone.run();
        });
        timer.setRepeats(false); // необязательно, если сам вызываешь stop()
        timer.start();
    }

    public void start() {
        setVisible(true);
        timer.start();
    }
}