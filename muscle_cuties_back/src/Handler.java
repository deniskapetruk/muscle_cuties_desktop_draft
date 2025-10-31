import java.io.*;
import java.net.Socket;
import java.time.LocalDate;
import java.util.*;

import scripts.Planner;
import models.*;

class Handler implements Runnable {

    private final Socket sock;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private volatile boolean go = true;

    private Role role;
    private String name = "User";

    private MuscleServer.Session session;
    private models.user clientUser;     // if authenticated as Client OR attached client's user (for Trainer)
    private models.trainer trainerUser; // if authenticated as Trainer

    Handler(Socket s) {
        this.sock = s;
        try {
            out = new ObjectOutputStream(s.getOutputStream());
            out.flush();
            in = new ObjectInputStream(s.getInputStream());
            MuscleServer.handlers.add(this);
            MuscleServer.say("Connected: " + s.getRemoteSocketAddress() + " handlers=" + MuscleServer.handlers.size());
            send("INFO Connected");

            Thread t = new Thread(this, "Handler-" + s.getPort() + "-" + System.identityHashCode(this));
            t.setDaemon(true);
            t.start();
        } catch (IOException e) {
            cleanup();
        }
    }

    @Override
    public void run() {
        try {
            // ---- Handshake ----
            String hello = readLine();
            if (hello == null || !hello.startsWith("HELLO ")) {
                send("ERROR Expected HELLO USER=<u> PASS=<p>");
                cleanup();
                return;
            }
            Map<String, String> kvHello = parseKv(hello.substring(6));
            String user = kvHello.getOrDefault("USER", "");
            String pass = kvHello.getOrDefault("PASS", "");

            Object auth = MuscleServer.repo.auth(user, pass);
            if (auth == null) {
                send("ERROR Auth");
                cleanup();
                return;
            }

            if (auth instanceof models.user) {
                clientUser = (models.user) auth;
                role = Role.CLIENT;
                name = clientUser.getUserName();
            } else {
                trainerUser = (models.trainer) auth;
                role = Role.TRAINER;
                name = trainerUser.getUsername();
            }

            send("ROLE " + role.name());
            MuscleServer.say("Authenticated: " + name + " as " + role.name() + " from " + sock.getRemoteSocketAddress());
            if (role == Role.CLIENT) {
                send("INFO Send OPEN to start or restore session");
            } else {
                send("INFO Use LIST to refresh sessions and ATTACH <id>");
            }

            // ---- Main loop ----
            while (go) {
                String line = readLine();
                if (line == null) break;
                line = line.trim();
                if (line.isEmpty()) continue;

                // ---------- Chat ----------
                if (line.startsWith("MSG ")) {
                    if (!ensureAttached()) continue;
                    Handler peer = session.peer(this);
                    if (peer != null) peer.send("MSG " + name + ": " + line.substring(4));
                }

                // ---------- Trainer session ops ----------
                else if (line.equals("LIST")) {
                    if (!ensureTrainer()) continue;
                    send("SESSIONS " + MuscleServer.sessions.summaryForTrainer(trainerUser));
                } else if (line.startsWith("ATTACH ")) {
                    if (!ensureTrainer()) continue;
                    String id = line.substring(7).trim();
                    MuscleServer.Session s = MuscleServer.sessions.attach(id, this);
                    if (s == null) {
                        send("ERROR Session not available");
                    } else {
                        this.session = s;
                        send("SESSION " + s.id);
                        Handler p = s.peer(this);
                        if (p != null) p.send("INFO Trainer joined");
                    }
                } else if (line.equals("LEAVE")) {
                    detach();
                    send("OK Left");
                }

                // ---------- Plans ----------
                else if (line.startsWith("GET_PLAN")) {
                    models.user target = (role == Role.CLIENT) ? clientUser : (session != null ? session.clientUser : null);
                    if (target == null) { send("NO_PLAN"); continue; }
                    LocalDate day = LocalDate.now();
                    List<String> plan = Planner.buildPlan(target, day);
                    for (String s : plan) send(s);
                }

                // ---------- Edit client (workout only) ----------
                else if (line.startsWith("EDIT_CLIENT")) {
                    if (!ensureTrainerAttached()) continue;
                    String args = (line.length() > 11) ? line.substring(11).trim() : "";
                    Map<String, String> changes = parseKv(args);
                    boolean changed = false;
                    StringBuilder info = new StringBuilder();

                    if (changes.containsKey("WORKOUT")) {
                        String w = changes.get("WORKOUT");
                        session.clientUser.setCurrentWorkout(w); // creates/activates custom plan if new
                        changed = true;
                        info.append("workout=").append(w).append(" ");
                    }

                    if (changed) {
                        send("OK Client updated: " + info.toString().trim());
                        Handler clientHandler = session.client;
                        if (clientHandler != null)
                            clientHandler.send("INFO Your profile updated: " + info.toString().trim());
                        MuscleServer.say("Trainer " + name + " updated client " + session.clientUser.getUserName()
                                + " -> " + info.toString().trim());
                    } else {
                        send("ERROR Nothing changed");
                    }
                }

                // ---------- Replace exercise (must match main muscle) ----------
                else if (line.startsWith("REPLACE_EX")) {
                    if (!ensureTrainerAttached()) continue;
                    String args = (line.length() > 10) ? line.substring(10).trim() : "";
                    Map<String, String> kv = parseKv(args);
                    String oldN = kv.getOrDefault("OLD", "");
                    String newN = kv.getOrDefault("NEW", "");
                    if (oldN.isEmpty() || newN.isEmpty()) { send("ERROR OLD and NEW required"); continue; }

                    models.muscle oldM = null, newM = null;
                    for (models.exercise e : Planner.allExercises()) {
                        if (e.getName().equalsIgnoreCase(oldN)) oldM = e.getMainMuscle();
                        if (e.getName().equalsIgnoreCase(newN)) newM = e.getMainMuscle();
                    }
                    if (oldM == null || newM == null) { send("ERROR Exercise not found"); continue; }
                    if (oldM != newM) { send("ERROR New exercise must have same main muscle"); continue; }

                    session.clientUser.setSubstitution(oldN, newN);
                    send("OK Replaced " + oldN + " -> " + newN);
                }

                // ---------- Per-row override (sets/reps) ----------
                else if (line.startsWith("EDIT_ROW")) {
                    if (!ensureTrainerAttached()) continue;
                    String args = (line.length() > 8) ? line.substring(8).trim() : "";
                    Map<String, String> kv = parseKv(args);
                    String ex = kv.getOrDefault("EX", "");
                    if (ex.isEmpty()) { send("ERROR EX required"); continue; }
                    Integer sets = null, reps = null;
                    if (kv.containsKey("SETS")) { try { sets = Integer.parseInt(kv.get("SETS")); } catch (Exception ignored) {} }
                    if (kv.containsKey("REPS")) { try { reps = Integer.parseInt(kv.get("REPS")); } catch (Exception ignored) {} }
                    session.clientUser.setRowOverride(ex.replace('_',' '), sets, reps);
                    send("OK Row updated for " + ex);
                }

                // ---------- Timetable (training days) ----------
                else if (line.startsWith("SET_TIMETABLE")) {
                    if (!ensureTrainerAttached()) continue;
                    String args = (line.length() > 12) ? line.substring(12).trim() : "";
                    Map<String, String> kv = parseKv(args);
                    String days = kv.getOrDefault("DAYS", "");
                    session.clientUser.setTimetableCSV(days);
                    send("TT " + days);
                }

                // ---------- Cycle (client-side Oopsie) ----------
                else if (line.startsWith("SET_CYCLE")) {
                    models.user target = (role == Role.CLIENT) ? clientUser : (session != null ? session.clientUser : null);
                    if (target == null) { send("ERROR No client attached"); continue; }
                    String args = (line.length() > 9) ? line.substring(9).trim() : "";
                    Map<String, String> kv = parseKv(args);

                    if (kv.containsKey("START_TODAY")) {
                        target.setCycleDay(1);
                    }
                    if (kv.containsKey("DAYS_AGO")) {
                        try {
                            int d = Integer.parseInt(kv.get("DAYS_AGO"));
                            target.setCycleDay(Math.max(1, d + 1));
                        } catch (NumberFormatException ex) { send("ERROR DAYS_AGO must be int"); continue; }
                    }
                    if (kv.containsKey("PHASE")) {
                        try {
                            target.setCyclePhase(models.phase.valueOf(kv.get("PHASE").toLowerCase(Locale.ROOT)));
                        } catch (IllegalArgumentException ex) { send("ERROR Invalid phase"); continue; }
                    }

                    send("OK Cycle updated day=" + target.getCycleDay()
                            + " phase=" + target.getCyclePhase());
                }

                // ---------- Presets (optional; unchanged) ----------
                else if (line.startsWith("SAVE_PRESET")) {
                    if (!ensureTrainerAttached()) continue;
                    String args = (line.length() > 11) ? line.substring(11).trim() : "";
                    Map<String, String> kv = parseKv(args);
                    String pname = kv.getOrDefault("NAME", "").trim();
                    if (pname.isEmpty()) { send("ERROR NAME required"); continue; }
                    Map<String, String> copy = new HashMap<>(session.clientUser.getSubstitutions());
                    MuscleServer.PRESETS.put(pname, copy);
                    send("OK Preset saved: " + pname);
                } else if (line.startsWith("APPLY_PRESET")) {
                    if (!ensureTrainerAttached()) continue;
                    String args = (line.length() > 12) ? line.substring(12).trim() : "";
                    Map<String, String> kv = parseKv(args);
                    String pname = kv.getOrDefault("NAME", "").trim();
                    if (!MuscleServer.PRESETS.containsKey(pname)) { send("ERROR No such preset"); continue; }
                    Map<String, String> map = MuscleServer.PRESETS.get(pname);
                    session.clientUser.clearSubstitutions();
                    for (Map.Entry<String, String> e : map.entrySet())
                        session.clientUser.setSubstitution(e.getKey(), e.getValue());
                    send("OK Preset applied: " + pname);
                }

                // ---------- Session open (client) ----------
                else if (line.equals("OPEN")) {
                    if (role == Role.CLIENT) {
                        if (this.session == null) {
                            this.session = MuscleServer.sessions.open(null, this, name);
                            this.session.clientUser = clientUser;
                            send("SESSION " + session.id);
                            MuscleServer.say("Session OPEN created: " + session.id + " for " + name);
                        } else {
                            send("INFO Using existing session " + this.session.id);
                        }
                    } else {
                        send("ERROR Only client can OPEN");
                    }
                }

                // ---------- Misc ----------
                else if (line.equals("PING")) {
                    send("PONG");
                } else if (line.equals("QUIT")) {
                    break;
                } else {
                    send("ERROR Unknown");
                }
            }
        } catch (Exception e) {
            MuscleServer.say("Handler error: " + (e == null ? "null" : e.getMessage()));
        } finally {
            cleanup();
        }
    }

    // ===== Helpers =====

    private boolean ensureTrainer() {
        if (role != Role.TRAINER) {
            send("ERROR Only trainer");
            return false;
        }
        return true;
    }

    private boolean ensureAttached() {
        if (session == null) {
            send("ERROR Not attached");
            return false;
        }
        return true;
    }

    private boolean ensureTrainerAttached() {
        if (!ensureTrainer()) return false;
        if (session == null || session.clientUser == null) {
            send("ERROR No client attached");
            return false;
        }
        return true;
    }

    private String readLine() throws IOException, ClassNotFoundException {
        try {
            Object o = in.readObject();
            return (o instanceof String) ? (String) o : null;
        } catch (EOFException eof) {
            return null;
        }
    }

    void send(String s) {
        try {
            synchronized (out) {
                out.writeObject(s);
                out.flush();
            }
        } catch (IOException e) {
            setGo(false);
        }
    }

    void setGo(boolean g) {
        go = g;
        if (!g) try { sock.close(); } catch (IOException ignored) {}
    }

    void detach() {
        if (session != null) {
            MuscleServer.sessions.detach(this, session);
            session = null;
        }
    }

    void cleanup() {
        try {
            detach();
            MuscleServer.handlers.remove(this);
            try { if (out != null) out.close(); } catch (Exception ignored) {}
            try { if (in != null) in.close(); } catch (Exception ignored) {}
            try { if (!sock.isClosed()) sock.close(); } catch (Exception ignored) {}
        } catch (Exception ignored) {}
        MuscleServer.say("Client disconnected");
    }

    static Map<String, String> parseKv(String s) {
        Map<String, String> m = new HashMap<>();
        if (s == null || s.trim().isEmpty()) return m;
        String[] parts = s.trim().split("\\s+");
        for (String p : parts) {
            int i = p.indexOf('=');
            if (i > 0) {
                String k = p.substring(0, i).trim();
                String v = p.substring(i + 1).trim();
                m.put(k.toUpperCase(Locale.ROOT), v);
            }
        }
        return m;
    }
}
