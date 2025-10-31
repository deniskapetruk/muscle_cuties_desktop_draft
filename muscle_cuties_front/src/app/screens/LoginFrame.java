package app.screens;

import app.ApiClient;
import app.PortFile;
import app.Theme;
import app.ui.RoundPanel;
import app.ui.PinkButton;
import app.ui.RoundField;
import app.ui.RoundPassword;

import javax.swing.*;
import java.awt.*;

public class LoginFrame extends JFrame implements ApiClient.Listener {
    private final RoundField user = new RoundField();
    private final RoundPassword pass = new RoundPassword();
    private final PinkButton login = new PinkButton("Sign In");
    private final JLabel status = new JLabel("");
    private final ApiClient api = new ApiClient();

    public LoginFrame() {
        setTitle("Muscle Cuties");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.PINK_SOFT);
        RoundPanel header = new RoundPanel();
        header.setFill(Theme.PINK_PRIMARY);
        header.setBorderColor(Theme.PINK_PRIMARY);
        header.setArc(6);
        header.setPreferredSize(new Dimension(860, 110));
        header.setLayout(new BorderLayout());
        JLabel title = new JLabel("MUSCLE CUTIES");
        title.setFont(Theme.brand(40f));
        title.setForeground(Color.WHITE);
        title.setHorizontalAlignment(SwingConstants.CENTER);
        header.add(title, BorderLayout.CENTER);
        JPanel center = new JPanel(new GridBagLayout());
        center.setOpaque(false);
        RoundPanel card = new RoundPanel();
        card.setFill(Color.WHITE);
        card.setBorderColor(new Color(230, 180, 197));
        card.setArc(6);
        card.setLayout(new GridBagLayout());
        card.setPreferredSize(new Dimension(580, 320));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(16, 18, 16, 18);
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.WEST;
        JLabel lu = new JLabel("Username");
        lu.setForeground(Theme.INK);
        card.add(lu, c);
        c.gridx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        user.setColumns(20);
        card.add(user, c);
        c.gridx = 0;
        c.gridy = 1;
        c.fill = GridBagConstraints.NONE;
        c.weightx = 0;
        JLabel lp = new JLabel("Password");
        lp.setForeground(Theme.INK);
        card.add(lp, c);
        c.gridx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        card.add(pass, c);
        c.gridx = 1;
        c.gridy = 2;
        c.fill = GridBagConstraints.NONE;
        c.weightx = 0;
        login.setPreferredSize(new Dimension(140, 40));
        card.add(login, c);
        c.gridx = 1;
        c.gridy = 3;
        status.setForeground(Theme.INK);
        card.add(status, c);
        center.add(card);
        root.add(header, BorderLayout.NORTH);
        root.add(center, BorderLayout.CENTER);
        setContentPane(root);
        login.addActionListener(e -> doLogin());
        api.addListener(this);
    }

    private void doLogin() {
        String uname = user.getText().trim();
        int port = PortFile.read();
        api.setUsername(uname);
        if (!api.isConnected()) {
            api.connect("127.0.0.1", port);
            status.setText("Connecting...");
        } else {
            status.setText("Already connected");
            String hello = "HELLO USER=" + uname + " PASS=" + new String(pass.getPassword());
            api.send(hello);
        }
    }

    @Override
    public void onMessage(String m) {
        SwingUtilities.invokeLater(() -> {
            status.setText(capitalizeStart(m));
            if (m.startsWith("INFO Connected")) {
                String hello = "HELLO USER=" + user.getText().trim() + " PASS=" + new String(pass.getPassword());
                api.send(hello);
            } else if (m.startsWith("ROLE ")) {
                if (m.toUpperCase().contains("CLIENT")) {
                    api.send("OPEN");
                }
                new AppFrame(api).setVisible(true);
                dispose();
            }
        });
    }

    private String capitalizeStart(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    @Override public void onConnected() { SwingUtilities.invokeLater(() -> status.setText("Connected")); }
    @Override public void onDisconnected() { SwingUtilities.invokeLater(() -> status.setText("Disconnected")); }
    @Override public void onError(String e) { SwingUtilities.invokeLater(() -> status.setText("Error " + e)); }
}
