package app.screens;

import app.ApiClient;
import app.Theme;
import app.ui.PinkButton;
import app.ui.RoundPanel;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;

public class DashboardPanel extends JPanel implements ApiClient.Listener {
    private final JLabel day = new JLabel("Day: ?");
    private final PinkButton refresh = new PinkButton("Refresh Workout");
    private final PinkButton oopsie = new PinkButton("Oopsie");

    // Soft banner just under the header (generic info / rest headline)
    private final RoundPanel bannerWrap = new RoundPanel();
    private final JTextArea banner = new JTextArea();

    // Notes (TIP) block above the table
    private final RoundPanel infoWrap = new RoundPanel();
    private final JTextArea infoText = new JTextArea();

    private final JTable table = new JTable();
    private final DefaultTableModel model = new DefaultTableModel(
            new Object[]{ "Exercise", "Main", "Secondary", "Sets", "Reps" }, 0
    ) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };

    private final ApiClient api;

    public DashboardPanel(ApiClient api) {
        this.api = api;
        setLayout(new BorderLayout());
        setOpaque(false);

        // ===== Header =====
        RoundPanel header = new RoundPanel();
        header.setLayout(new BorderLayout());
        header.setBackground(new Color(255, 245, 249));
        JLabel title = new JLabel("Today's Plan");
        title.setFont(Theme.H1);
        title.setForeground(new Color(145, 60, 98));
        title.setBorder(BorderFactory.createEmptyBorder(14,16,8,16));

        day.setForeground(Color.DARK_GRAY);
        day.setFont(Theme.H3);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        right.setOpaque(false);
        right.add(day);
        right.add(refresh);
        right.add(oopsie);

        header.add(title, BorderLayout.WEST);
        header.add(right, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        // ===== Banner (soft info strip under header) =====
        bannerWrap.setLayout(new BorderLayout());
        bannerWrap.setBackground(new Color(252, 240, 246));
        bannerWrap.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        banner.setEditable(false);
        banner.setLineWrap(true);
        banner.setWrapStyleWord(true);
        banner.setOpaque(false);
        banner.setFont(new Font("SansSerif", Font.PLAIN, 14));
        banner.setForeground(new Color(120, 60, 100));
        bannerWrap.add(banner, BorderLayout.CENTER);

        // ===== Notes (TIP) block ABOVE the table =====
        infoWrap.setLayout(new BorderLayout());
        infoWrap.setBackground(new Color(255, 245, 249));
        infoWrap.setBorder(BorderFactory.createEmptyBorder(8, 12, 10, 12));
        JLabel infoTitle = new JLabel("Notes");
        infoTitle.setFont(Theme.H3);
        infoTitle.setForeground(new Color(145, 60, 98));
        infoTitle.setBorder(BorderFactory.createEmptyBorder(2, 2, 6, 2));
        infoText.setEditable(false);
        infoText.setLineWrap(true);
        infoText.setWrapStyleWord(true);
        infoText.setOpaque(false);
        infoText.setFont(Theme.BODY);
        infoText.setForeground(Color.DARK_GRAY);
        infoWrap.add(infoTitle, BorderLayout.NORTH);
        infoWrap.add(infoText, BorderLayout.CENTER);
        setInfo(null); // hidden by default

        // ===== Table =====
        table.setModel(model);
        table.setFillsViewportHeight(true);
        table.setRowHeight(28);
        table.setShowGrid(true);
        table.setGridColor(new Color(230, 180, 197));
        table.getTableHeader().setReorderingAllowed(false);

        // ===== Center stack: banner + notes + table =====
        JPanel center = new JPanel(new BorderLayout(0, 10));
        center.setOpaque(false);
        center.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel topStack = new JPanel();
        topStack.setOpaque(false);
        topStack.setLayout(new BoxLayout(topStack, BoxLayout.Y_AXIS));
        topStack.add(bannerWrap);
        topStack.add(Box.createVerticalStrut(10));
        topStack.add(infoWrap);

        center.add(topStack, BorderLayout.NORTH);
        center.add(new JScrollPane(table), BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);

        // ===== Actions =====
        refresh.addActionListener(e -> requestPlan());
        oopsie.addActionListener(e -> openOopsieDialog());

        api.addListener(this);
        updateDay();
        setBanner("Today’s plan is ready! Main lifts a bit tougher than accessories. Hydrate and breathe.");
        requestPlan();
    }

    private void updateDay() {
        LocalDate d = LocalDate.now();
        String label = d.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH) + ", " + d;
        day.setText(label);
    }

    private void requestPlan() {
        model.setRowCount(0);
        setInfo(null);                  // clear/hide notes
        api.send("GET_PLAN");
    }

    private void openOopsieDialog() {
        String[] phases = { "menstrual","follicular","ovulatory","luteal" };
        JPanel p = new JPanel(new GridLayout(0,1,6,6));
        JComboBox<String> ph = new JComboBox<>(phases);
        JCheckBox today = new JCheckBox("Period started today");
        JSpinner daysAgo = new JSpinner(new SpinnerNumberModel(0, 0, 28, 1));
        p.add(new JLabel("Select phase:")); p.add(ph);
        p.add(today);
        p.add(new JLabel("…or enter how many days ago it started:")); p.add(daysAgo);
        int ok = JOptionPane.showConfirmDialog(this, p, "Oopsie — adjust cycle", JOptionPane.OK_CANCEL_OPTION);
        if (ok == JOptionPane.OK_OPTION) {
            String sel = (String) ph.getSelectedItem();
            if (today.isSelected()) api.send("SET_CYCLE START_TODAY=1 PHASE=" + sel);
            else api.send("SET_CYCLE DAYS_AGO=" + daysAgo.getValue() + " PHASE=" + sel);
            requestPlan();
        }
    }

    private void setBanner(String text) {
        banner.setText(text == null ? "" : text.trim());
        bannerWrap.setVisible(text != null && !text.trim().isEmpty());
    }

    private void setInfo(String text) {
        boolean show = text != null && !text.trim().isEmpty();
        infoWrap.setVisible(show);
        infoText.setText(show ? text.trim() : "");
        infoWrap.revalidate();
        infoWrap.repaint();
    }

    private String cap(String s) {
        return (s == null || s.isEmpty()) ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    // ===== Server messages =====
    @Override
    public void onMessage(String m) {
        SwingUtilities.invokeLater(() -> {
            if (m.startsWith("PLAN ")) {
                model.setRowCount(0);
                // Keep banner friendly on plan days
                setBanner("Today’s plan is ready! Main lifts RPE ~8, accessories RPE ~7. Hydrate and take your time.");
                setInfo(null); // notes come via TIP
            } else if (m.startsWith("REST ")) {
                model.setRowCount(0);
                setBanner(" Rest day! " + m.substring(5));
                setInfo("Enjoy your life today: a short walk, cozy tea, good music. Your body builds magic while you rest ✨");
            } else if (m.startsWith("EX ")) {
                // "EX name;main;secondary;sets;reps"
                String[] parts = m.substring(3).split(";");
                String name = parts.length > 0 ? parts[0] : "";
                String main = parts.length > 1 ? parts[1] : "";
                String secondary = parts.length > 2 ? parts[2] : "";
                String sets = parts.length > 3 ? parts[3] : "";
                String reps = parts.length > 4 ? parts[4] : "";
                // IMPORTANT: only 5 columns — no trailing empty column.
                model.addRow(new Object[]{ name, cap(main), secondary, sets, reps });
            } else if (m.startsWith("TIP ")) {
                // TIPs show in the Notes block (not the banner)
                setInfo(m.substring(4));
            } else if (m.startsWith("TT ")) {
                // Timetable echo, skip on UI for now or show in tooltip if desired
            } else if (m.startsWith("INFO ")) {
                // Optional: toast or footer; we keep quiet here
            } else if (m.startsWith("ERROR ")) {
                JOptionPane.showMessageDialog(this, m, "Server", JOptionPane.WARNING_MESSAGE);
            }
        });
    }

    @Override public void onConnected() {}
    @Override public void onDisconnected() {}
    @Override public void onError(String e) {}
}
