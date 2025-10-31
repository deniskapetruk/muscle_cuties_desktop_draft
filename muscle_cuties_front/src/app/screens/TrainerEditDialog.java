package app.screens;

import app.ApiClient;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.util.*;
import java.util.List;

public class TrainerEditDialog extends JDialog implements ApiClient.Listener {

    // --- Top row: change workout only (preset or custom), no phase/goal/name fields ---
    private final JComboBox<String> workoutBox = new JComboBox<>(new String[]{
            "Strength", "Endurance", "Hypertrophy_Upper", "Hypertrophy_Lower", "Hypertrophy_Balanced", "Custom…"
    });
    private final JButton applyWorkout = new JButton("Apply");
    private final JButton refreshBtn   = new JButton("Refresh");

    // --- Timetable (days) ---
    private final JCheckBox[] dayChecks = new JCheckBox[] {
            new JCheckBox("Mon"), new JCheckBox("Tue"), new JCheckBox("Wed"),
            new JCheckBox("Thu"), new JCheckBox("Fri"), new JCheckBox("Sat"), new JCheckBox("Sun")
    };
    private final JButton saveTimetable = new JButton("Save Timetable");

    // --- Editable table: Exercise (dropdown, same main), Sets (spinner), Reps (spinner) ---
    private final DefaultTableModel model = new DefaultTableModel(
            new Object[]{ "Exercise", "Main", "Secondary", "Sets", "Reps" }, 0
    ) {
        @Override public boolean isCellEditable(int r, int c) { return (c == 0 || c == 3 || c == 4); }
        @Override public Class<?> getColumnClass(int columnIndex) {
            if (columnIndex == 3 || columnIndex == 4) return Integer.class;
            return String.class;
        }
    };

    private final JTable table = new JTable(model) {
        @Override
        public TableCellEditor getCellEditor(int row, int column) {
            if (column == 0) {
                String main = String.valueOf(getValueAt(row, 1)).toLowerCase(Locale.ROOT);
                JComboBox<String> box = new JComboBox<>(optionsForMain(main));
                return new DefaultCellEditor(box);
            } else if (column == 3 || column == 4) {
                // spinner for sets/reps (1..30)
                JSpinner sp = new JSpinner(new SpinnerNumberModel(8, 1, 30, 1));
                sp.setBorder(null);
                return new DefaultCellEditor((JTextField) sp.getEditor().getComponent(0)) {
                    @Override public Object getCellEditorValue() {
                        try { return ((Number) sp.getValue()).intValue(); }
                        catch (Exception e) { return 8; }
                    }
                };
            }
            return super.getCellEditor(row, column);
        }
    };

    // Track originals to compute diffs
    private final java.util.List<String> originalExercises = new ArrayList<>();
    private final java.util.List<Integer> originalSets = new ArrayList<>();
    private final java.util.List<Integer> originalReps = new ArrayList<>();

    private final ApiClient api;
    private final String sessionId;

    // Catalog (expanded; must match server)
    private static final String[] NAMES = Catalog.NAMES;
    private static final String[] MAIN  = Catalog.MAIN;

    public TrainerEditDialog(Window owner, ApiClient api, String sessionId) {
        super(owner, "Edit Workout (Coach)", ModalityType.APPLICATION_MODAL);
        this.api = api;
        this.sessionId = sessionId;

        // --- Header (workout selection) ---
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        top.add(new JLabel("Workout:"));
        top.add(workoutBox);
        top.add(applyWorkout);
        top.add(refreshBtn);

        workoutBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED && "Custom…".equals(e.getItem())) {
                String name = JOptionPane.showInputDialog(this, "Custom workout name:");
                if (name != null && !name.trim().isEmpty()) {
                    api.send("EDIT_CLIENT WORKOUT=" + name.trim().replaceAll("\\s+","_"));
                    api.send("GET_PLAN");
                } else {
                    workoutBox.setSelectedIndex(0);
                }
            }
        });
        applyWorkout.addActionListener(e -> {
            String sel = String.valueOf(workoutBox.getSelectedItem());
            if (!"Custom…".equals(sel)) {
                api.send("EDIT_CLIENT WORKOUT=" + sel);
                api.send("GET_PLAN");
            }
        });
        refreshBtn.addActionListener(e -> api.send("GET_PLAN"));

        // --- Timetable panel ---
        JPanel tt = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
        tt.setBorder(BorderFactory.createTitledBorder("Timetable (days with workouts)"));
        for (JCheckBox cb : dayChecks) tt.add(cb);
        saveTimetable.addActionListener(e -> {
            StringBuilder sb = new StringBuilder();
            String[] tags = { "Mon","Tue","Wed","Thu","Fri","Sat","Sun" };
            for (int i=0;i<dayChecks.length;i++) if (dayChecks[i].isSelected()) {
                if (sb.length() > 0) sb.append(',');
                sb.append(tags[i]);
            }
            api.send("SET_TIMETABLE DAYS=" + sb.toString());
            JOptionPane.showMessageDialog(this, "Timetable saved.", "OK", JOptionPane.INFORMATION_MESSAGE);
            api.send("GET_PLAN");
        });
        tt.add(saveTimetable);

        // --- Table area ---
        table.setRowHeight(26);
        table.setFillsViewportHeight(true);
        table.getTableHeader().setReorderingAllowed(false);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createTitledBorder("Exercises (edit Exercise/Sets/Reps)"));

        // --- Buttons ---
        JButton save = new JButton("Save Changes");
        JButton close = new JButton("Close");
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btns.add(save); btns.add(close);

        // --- Layout root ---
        JPanel root = new JPanel(new BorderLayout(10, 10));
        JPanel north = new JPanel(new BorderLayout(0, 8));
        north.add(top, BorderLayout.NORTH);
        north.add(tt, BorderLayout.SOUTH);

        root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        root.add(north, BorderLayout.NORTH);
        root.add(scroll, BorderLayout.CENTER);
        root.add(btns, BorderLayout.SOUTH);
        setContentPane(root);
        setSize(980, 620);
        setLocationRelativeTo(owner);

        // --- Actions ---
        save.addActionListener(e -> applyChanges());
        close.addActionListener(e -> dispose());

        api.addListener(this);
        api.send("GET_PLAN");
    }

    @Override
    public void dispose() {
        api.removeListener(this);
        super.dispose();
    }

    private DefaultComboBoxModel<String> optionsForMain(String main) {
        java.util.List<String> options = new ArrayList<>();
        for (int i=0;i<NAMES.length;i++) if (MAIN[i].equalsIgnoreCase(main)) options.add(NAMES[i]);
        if (options.isEmpty()) return new DefaultComboBoxModel<>(NAMES);
        return new DefaultComboBoxModel<>(options.toArray(new String[0]));
    }

    private void applyChanges() {
        if (table.isEditing()) table.getCellEditor().stopCellEditing();

        int swaps = 0, edits = 0;
        for (int r = 0; r < model.getRowCount(); r++) {
            String oldN = (r < originalExercises.size()) ? originalExercises.get(r) : null;
            String newN = String.valueOf(model.getValueAt(r, 0));
            int newSets = (Integer) model.getValueAt(r, 3);
            int newReps = (Integer) model.getValueAt(r, 4);

            if (oldN != null && newN != null && !oldN.equals(newN)) {
                api.send("REPLACE_EX OLD=" + oldN.replace(' ', '_') + " NEW=" + newN.replace(' ', '_'));
                swaps++;
            }
            Integer os = (r < originalSets.size()) ? originalSets.get(r) : null;
            Integer or = (r < originalReps.size()) ? originalReps.get(r) : null;
            if (os == null || or == null || os != newSets || or != newReps) {
                api.send("EDIT_ROW EX=" + newN.replace(' ', '_') + " SETS=" + newSets + " REPS=" + newReps);
                edits++;
            }
        }
        JOptionPane.showMessageDialog(this, "Saved " + swaps + " swap(s), " + edits + " set/rep edit(s).", "Saved", JOptionPane.INFORMATION_MESSAGE);
        api.send("GET_PLAN");
    }

    // -------- ApiClient.Listener --------
    @Override
    public void onMessage(String m) {
        SwingUtilities.invokeLater(() -> {
            if (m.startsWith("PLAN ")) {
                model.setRowCount(0);
                originalExercises.clear();
                originalSets.clear();
                originalReps.clear();
            } else if (m.startsWith("EX ")) {
                // "EX name;main;secondary;sets;reps"
                String[] p = m.substring(3).split(";");
                String name = p.length > 0 ? p[0] : "";
                String main = p.length > 1 ? p[1] : "";
                String secondary = p.length > 2 ? p[2] : "";
                int sets = (p.length > 3) ? parseInt(p[3], 4) : 4;
                int reps = (p.length > 4) ? parseInt(p[4], 8) : 8;
                originalExercises.add(name);
                originalSets.add(sets);
                originalReps.add(reps);
                model.addRow(new Object[]{ name, main, secondary, sets, reps });
            } else if (m.startsWith("REST ")) {
                JOptionPane.showMessageDialog(this, m.substring(5), "Rest Day", JOptionPane.INFORMATION_MESSAGE);
            } else if (m.startsWith("TT ")) {
                // timetable echo, e.g. "TT Mon,Wed,Fri"
                String body = m.substring(3).trim();
                boolean[] map = new boolean[7];
                String[] tags = { "Mon","Tue","Wed","Thu","Fri","Sat","Sun" };
                for (int i=0;i<tags.length;i++) if (body.contains(tags[i])) map[i] = true;
                for (int i=0;i<dayChecks.length;i++) dayChecks[i].setSelected(map[i]);
            }
        });
    }

    private int parseInt(String s, int def) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return def; }
    }

    @Override public void onConnected() {}
    @Override public void onDisconnected() {}
    @Override public void onError(String e) {}

    // ---- Catalog (kept in one place for easy reuse) ----
    static final class Catalog {
        // Expand by muscle; keep secondaries for non-isolation
        static final java.util.List<String[]> LIST = new ArrayList<>();
        static {
            // name, main, sec1, sec2, isMain(true/false) -> encoded by placing big compounds multiple times across program logic;
            // For UI we only need name/main/secondaries; server defines main flag.
            add("Barbell Squat","quadriceps","gluteus","hamstrings");
            add("Front Squat","quadriceps","gluteus","erector_spinae");
            add("Hack Squat","quadriceps","gluteus","hamstrings");
            add("Leg Press","quadriceps","gluteus","hamstrings");
            add("Bulgarian Split Squat","quadriceps","gluteus","hamstrings");
            add("Walking Lunge","quadriceps","gluteus","hamstrings");

            add("Romanian Deadlift","hamstrings","gluteus","erector_spinae");
            add("Good Morning","hamstrings","erector_spinae","gluteus");
            add("Seated Leg Curl","hamstrings","gluteus","calves");
            add("Cable Pull-Through","hamstrings","gluteus","erector_spinae");
            add("Nordic Ham Curl","hamstrings",null,null); // isolation

            add("Hip Thrust","gluteus","hamstrings","quadriceps");
            add("Barbell Glute Bridge","gluteus","hamstrings","quadriceps");
            add("Cable Kickback","gluteus",null,null); // isolation
            add("Step-Up","gluteus","quadriceps","hamstrings");
            add("Reverse Lunge","gluteus","quadriceps","hamstrings");

            add("Bench Press","middle_chest","triceps","front_deltoids");
            add("Incline DB Press","upper_chest","triceps","front_deltoids");
            add("Decline Press","middle_chest","triceps","front_deltoids");
            add("Chest Dip","middle_chest","triceps","front_deltoids");
            add("Machine Chest Press","middle_chest","triceps","front_deltoids");

            add("Lat Pulldown","latissimus","biceps","rhomboids");
            add("Pull-Up","latissimus","biceps","rhomboids");
            add("Seated Row","rhomboids","latissimus","biceps");
            add("Chest-Supported Row","rhomboids","latissimus","biceps");
            add("Single-Arm Dumbbell Row","latissimus","rhomboids","biceps");

            add("Overhead Press","front_deltoids","middle_deltoids","triceps");
            add("Lateral Raise","middle_deltoids","front_deltoids","back_deltoids");
            add("Rear Delt Fly","back_deltoids","rhomboids",null);
            add("Arnold Press","front_deltoids","middle_deltoids","triceps");
            add("Face Pull","back_deltoids","rhomboids","middle_deltoids");

            add("Calf Raise","calves","gluteus",null);
            add("Seated Calf Raise","calves",null,null);
            add("Plank","abdominals",null,null);
            add("Hanging Leg Raise","abdominals","hip_flexors",null);
            add("Cable Crunch","abdominals","hip_flexors",null);

            add("Deadlift","erector_spinae","gluteus","hamstrings");
            add("Back Extension","erector_spinae","gluteus","hamstrings");
            add("Bird Dog","erector_spinae","gluteus","abdominals");
            add("Good Morning (Light)","erector_spinae","hamstrings","gluteus");
            add("Superman Hold","erector_spinae","gluteus","abdominals");

            add("DB Curl","biceps","forearms",null);
            add("EZ-Bar Curl","biceps","forearms",null);
            add("Hammer Curl","biceps","forearms",null);
            add("Triceps Pushdown","triceps","front_deltoids",null);
            add("Overhead Triceps Extension","triceps","front_deltoids",null);
        }
        static void add(String n, String m, String s1, String s2) { LIST.add(new String[]{n,m,(s1==null?"":s1),(s2==null?"":s2)}); }
        static final String[] NAMES = LIST.stream().map(a->a[0]).toArray(String[]::new);
        static final String[] MAIN  = LIST.stream().map(a->a[1]).toArray(String[]::new);
    }
}
