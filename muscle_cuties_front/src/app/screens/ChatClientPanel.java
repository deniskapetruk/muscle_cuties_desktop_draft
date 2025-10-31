
package app.screens;

import app.ApiClient;
import app.ui.PinkButton;
import app.ui.RoundPanel;
import app.ui.RoundField;

import javax.swing.*;
import java.awt.*;

public class ChatClientPanel extends JPanel implements ApiClient.Listener {
    private final JTextArea chat = new JTextArea();
    private final RoundField input = new RoundField();
    private final PinkButton send = new PinkButton("Send");
    private final PinkButton open = new PinkButton("Open");
    private final PinkButton leave = new PinkButton("Leave");
    private final JLabel status = new JLabel("");
    private final ApiClient api;

    public ChatClientPanel(ApiClient api) {
        this.api = api;
        setLayout(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        RoundPanel header = new RoundPanel();
        header.setPreferredSize(new Dimension(1000, 60));
        header.setLayout(new BorderLayout(8, 8));
        header.setFill(new Color(239, 98, 159));
        header.setBorderColor(new Color(239, 98, 159));
        JLabel t = new JLabel("Chat with Trainer");
        t.setForeground(Color.WHITE);
        t.setHorizontalAlignment(SwingConstants.LEFT);
        header.add(t, BorderLayout.WEST);
        status.setForeground(Color.WHITE);
        header.add(status, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);
        chat.setEditable(false);
        chat.setLineWrap(true);
        chat.setWrapStyleWord(true);
        add(new JScrollPane(chat), BorderLayout.CENTER);
        JPanel bottom = new JPanel(new BorderLayout(10, 10));
        bottom.setOpaque(false);
        JPanel actions = new JPanel(new GridLayout(1, 2, 10, 10));
        actions.setOpaque(false);
        actions.add(open);
        actions.add(leave);
        bottom.add(actions, BorderLayout.NORTH);
        bottom.add(input, BorderLayout.CENTER);
        bottom.add(send, BorderLayout.EAST);
        add(bottom, BorderLayout.SOUTH);
        open.addActionListener(e -> api.send("OPEN"));
        leave.addActionListener(e -> api.send("LEAVE"));
        send.addActionListener(e -> doSend());
        api.addListener(this);
    }

    private void doSend() {
        String text = input.getText().trim();
        if (!text.isEmpty()) {
            api.send("MSG " + text);
            input.setText("");
        }
    }

    @Override
    public void onMessage(String m) {
        SwingUtilities.invokeLater(() -> {
            if (m.startsWith("MSG ")) chat.append(m.substring(4) + "\n");
            else if (m.startsWith("SESSION ")) status.setText(m);
            else if (m.startsWith("INFO ")) status.setText(m.substring(5));
            else if (m.startsWith("ERROR ")) status.setText(m);
        });
    }

    @Override public void onConnected() {}
    @Override public void onDisconnected() {}
    @Override public void onError(String e) {}
}
