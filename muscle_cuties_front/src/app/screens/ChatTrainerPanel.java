package app.screens;

import app.ApiClient;
import app.ui.PinkButton;
import app.ui.RoundPanel;
import app.ui.RoundField;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class ChatTrainerPanel extends JPanel implements ApiClient.Listener {
    private final DefaultListModel<String> sessionsModel = new DefaultListModel<>();
    private final JList<String> sessions = new JList<>(sessionsModel);
    private final JTextArea chat = new JTextArea();
    private final RoundField input = new RoundField();
    private final PinkButton send = new PinkButton("Send");
    private final PinkButton refreshSessions = new PinkButton("Refresh");
    private final PinkButton attach = new PinkButton("Attach");
    private final PinkButton leave = new PinkButton("Leave");
    private final PinkButton editWorkout = new PinkButton("Edit Workout");

    private final JLabel status = new JLabel("");
    private final ApiClient api;

    private String currentSessionId = null;

    public ChatTrainerPanel(ApiClient api) {
        this.api = api;
        setLayout(new BorderLayout(12, 12));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        RoundPanel header = new RoundPanel();
        header.setPreferredSize(new Dimension(1000, 60));
        header.setLayout(new BorderLayout(8, 8));
        header.setFill(new Color(239, 98, 159));
        header.setBorderColor(new Color(239, 98, 159));
        JLabel t = new JLabel("Trainer Chat");
        t.setForeground(Color.WHITE);
        t.setHorizontalAlignment(SwingConstants.LEFT);
        header.add(t, BorderLayout.WEST);
        status.setForeground(Color.WHITE);
        header.add(status, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        sessions.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        sessions.setFixedCellHeight(26);
        JScrollPane left = new JScrollPane(sessions);
        left.setPreferredSize(new Dimension(280, 420));
        JPanel leftPane = new JPanel(new BorderLayout(10, 10));
        leftPane.add(left, BorderLayout.CENTER);
        JPanel leftBtns = new JPanel(new GridLayout(1, 2, 10, 10));
        leftBtns.add(refreshSessions);
        leftBtns.add(attach);
        leftPane.add(leftBtns, BorderLayout.SOUTH);

        chat.setEditable(false);
        chat.setLineWrap(true);
        chat.setWrapStyleWord(true);
        JScrollPane center = new JScrollPane(chat);

        JPanel bottom = new JPanel(new BorderLayout(10, 10));
        bottom.add(input, BorderLayout.CENTER);
        bottom.add(send, BorderLayout.EAST);

        JPanel topRightBtns = new JPanel(new GridLayout(1, 2, 10, 10));
        topRightBtns.add(leave);
        topRightBtns.add(editWorkout);

        JPanel rightPane = new JPanel(new BorderLayout(10, 10));
        rightPane.add(center, BorderLayout.CENTER);
        rightPane.add(bottom, BorderLayout.SOUTH);
        rightPane.add(topRightBtns, BorderLayout.NORTH);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPane, rightPane);
        split.setResizeWeight(0.30);
        add(split, BorderLayout.CENTER);

        refreshSessions.addActionListener(e -> api.send("LIST"));
        attach.addActionListener(e -> {
            String sel = sessions.getSelectedValue();
            if (sel != null && sel.contains(";")) {
                String id = sel.split(";")[0].trim();
                currentSessionId = id;
                api.send("ATTACH " + id);
            } else {
                JOptionPane.showMessageDialog(window(), "Select a session first.", "No session", JOptionPane.WARNING_MESSAGE);
            }
        });
        leave.addActionListener(e -> { currentSessionId = null; api.send("LEAVE"); });
        editWorkout.addActionListener(e -> {
            if (currentSessionId == null || currentSessionId.isEmpty()) {
                JOptionPane.showMessageDialog(window(), "Attach to a client first.", "No client", JOptionPane.WARNING_MESSAGE);
                return;
            }
            new TrainerEditDialog(window(), api, currentSessionId).setVisible(true);
        });
        send.addActionListener(e -> doSend());

        api.addListener(this);
        api.send("LIST");
    }

    private Window window() { return SwingUtilities.getWindowAncestor(this); }

    private void doSend() {
        String text = input.getText().trim();
        if (text.isEmpty()) return;

        if (text.startsWith("/edit")) {
            if (currentSessionId == null || currentSessionId.isEmpty()) {
                chat.append("[Local] No client attached. Use Attach first.\n");
            } else {
                Map<String,String> kv = parseKeyValues(text.substring(5));
                String ph = kv.getOrDefault("phase", "follicular");
                String w  = kv.getOrDefault("workout", "Custom").replaceAll("\\s+","_");
                String tp = kv.getOrDefault("type", "Strength");
                String cmd = "EDIT_CLIENT SESSION=" + currentSessionId + " PHASE=" + ph + " WORKOUT=" + w + " TYPE=" + tp;
                api.send(cmd);
                chat.append("[Local] " + cmd + "\n");
            }
        } else {
            api.send("MSG " + text);
        }
        input.setText("");
    }

    private Map<String,String> parseKeyValues(String s) {
        Map<String,String> m = new HashMap<>();
        for (String part : s.trim().split("\\s+")) {
            int eq = part.indexOf('=');
            if (eq > 0 && eq < part.length() - 1) {
                String k = part.substring(0, eq).toLowerCase();
                String v = part.substring(eq + 1);
                m.put(k, v);
            }
        }
        return m;
    }

    @Override
    public void onMessage(String m) {
        SwingUtilities.invokeLater(() -> {
            if (m.startsWith("SESSIONS ")) {
                sessionsModel.clear();
                String body = m.substring(9).trim();
                if (!body.equals("(none)")) {
                    String[] items = body.split(",");
                    for (String it : items) sessionsModel.addElement(it.trim());
                }
            } else if (m.startsWith("MSG ")) {
                chat.append(m.substring(4) + "\n");
            } else if (m.startsWith("SESSION ")) {
                String body = m.substring(8).trim();
                String id = body;
                int semi = body.indexOf(';');
                if (semi > 0) id = body.substring(0, semi).trim();
                int sp = id.indexOf(' ');
                if (sp > 0) id = id.substring(0, sp).trim();
                if (!id.isEmpty()) currentSessionId = id;
                status.setText(m);
            } else if (m.startsWith("INFO ")) {
                status.setText(m.substring(5));
            } else if (m.startsWith("ERROR ")) {
                status.setText(m);
            }
        });
    }

    @Override public void onConnected() {}
    @Override public void onDisconnected() {}
    @Override public void onError(String e) {}
}
