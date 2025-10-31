import javax.swing.*;
import java.awt.event.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.*;
import models.*;

public class MuscleServer extends JFrame implements Runnable, WindowListener {
    public static final java.util.concurrent.ConcurrentHashMap<String, java.util.Map<String,String>> PRESETS = new java.util.concurrent.ConcurrentHashMap<>();
    private static final long serialVersionUID = 1L;
    private ServerSocket server;
    private volatile boolean go = false;
    static final JTextArea out = new JTextArea("Ready...\n");
    private static final Logger log = Logger.getLogger(MuscleServer.class.getName());
    static final List<Handler> handlers = Collections.synchronizedList(new ArrayList<>());
    static final Sessions sessions = new Sessions();
    static final Repo repo = new Repo();

    public MuscleServer() {
        setupLogger();
        out.setEditable(false);
        this.getContentPane().add(new JScrollPane(out));
        this.setTitle("Muscle Server");
        this.setBounds(100, 100, 520, 640);
        this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        this.addWindowListener(this);
        this.setVisible(true);
        Thread t = new Thread(this, "Acceptor");
        t.setDaemon(true);
        t.start();
    }

    private void setupLogger() {
        try {
            FileHandler fh = new FileHandler("server_logs.log", true);
            fh.setFormatter(new SimpleFormatter());
            log.addHandler(fh);
            log.setUseParentHandlers(true);
        } catch (IOException e) { }
    }

    @Override
    public void run() {
        int port = readPortOrDie();
        try {
            server = new ServerSocket(port);
            go = true;
            say("Server on " + port);
        } catch (IOException e) {
            say("Server launch failed: " + e.getMessage());
            return;
        }
        while (go) {
            try {
                say("Waiting...");
                Socket s = server.accept();
                new Handler(s);
            } catch (IOException e) {
                if (!go) break;
                say("Accept error: " + e.getMessage());
            }
        }
        say("Stopped accepting.");
    }

    private int readPortOrDie() {
        String[] candidates = new String[] { "port.txt", "../port.txt", "../../port.txt" };
        for (String p : candidates) {
            File f = new File(p);
            if (f.exists()) {
                try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                    String s = br.readLine();
                    if (s == null) continue;
                    int prt = Integer.parseInt(s.trim());
                    if (prt > 0 && prt <= 65535) return prt;
                } catch (Exception ignore) { }
            }
        }
        say("ERROR: port.txt not found in expected paths");
        System.exit(1);
        return -1;
    }

    @Override public void windowClosing(WindowEvent e) {
        go = false;
        synchronized (handlers) { for (Handler h : handlers) h.setGo(false); }
        try { if (server != null) server.close(); } catch (IOException ignored) {}
        System.exit(0);
    }
    @Override public void windowActivated(WindowEvent e) {}
    @Override public void windowClosed(WindowEvent e) {}
    @Override public void windowDeactivated(WindowEvent e) {}
    @Override public void windowDeiconified(WindowEvent e) {}
    @Override public void windowIconified(WindowEvent e) {}
    @Override public void windowOpened(WindowEvent e) {}

    public static void main(String[] args) { SwingUtilities.invokeLater(MuscleServer::new); }
    static synchronized void say(String s) { out.append(s + "\n"); log.info(s); }

    static class Repo {
        final Map<String, models.user> users = new HashMap<>();
        final Map<String, models.trainer> trainers = new HashMap<>();
        Repo() {
            models.trainer t1 = new models.trainer("coach", "coach", "Coach Pink", 5, "CPT");
            models.user u1 = new models.user("alice", "alice@example.com", "alice");
            models.user u2 = new models.user("bella", "bella@example.com", "bella");
            // Alice and Bella are in different phases and have different default workouts
            u1.setCyclePhase(phase.follicular);
            u2.setCyclePhase(phase.luteal);
            u1.setCurrentWorkout("Glute Focus");
            u2.setCurrentWorkout("Upper Body");
            u1.setTrainer(t1); t1.addUser(u1);
            u2.setTrainer(t1); t1.addUser(u2);
            users.put(u1.getUserName(), u1);
            users.put(u2.getUserName(), u2);
            trainers.put(t1.getUsername(), t1);
        }
        Object auth(String user, String pass) {
            models.user u = users.get(user);
            if (u != null && u.getPassword().equals(pass)) return u;
            models.trainer t = trainers.get(user);
            if (t != null && t.getPassword().equals(pass)) return t;
            return null;
        }
    }

    static class Sessions {
        private final Map<String, Session> map = new ConcurrentHashMap<>();
        private final Random rnd = new Random();
        Session open(String reqId, Handler client, String clientName) {
            for (Session s : map.values()) {
                if (s.client == client) return s;
            }
            String id = (reqId != null && !reqId.isEmpty()) ? reqId : genId();
            Session s = new Session(id, client, clientName);
            map.put(id, s);
            say("OPEN created: " + id + " for " + clientName);
            return s;
        }
        Session attach(String id, Handler trainer) {
            Session s = map.get(id);
            if (s == null || s.client == null || s.trainer != null) return null;
            s.trainer = trainer;
            say("ATTACH: trainer attached to session " + id);
            return s;
        }
        void detach(Handler who, Session s) {
            if (s == null) return;
            if (s.client == who) {
                if (s.trainer != null) s.trainer.send("INFO Partner left");
                map.remove(s.id);
                say("CLOSED session " + s.id);
            } else if (s.trainer == who) {
                s.trainer = null;
                if (s.client != null) s.client.send("INFO Trainer left");
                say("DETACHED trainer from " + s.id);
            }
        }
        String summaryForTrainer(models.trainer t) {
            List<String> parts = new ArrayList<>();
            for (Session s : map.values()) {
                if (s.trainer == null && s.client != null) {
                    if (s.clientUser != null && s.clientUser.getTrainer() == t) {
                        parts.add(s.id + ";" + s.clientName);
                    }
                }
            }
            return parts.isEmpty() ? "(none)" : String.join(",", parts);
        }
        private String genId() {
            String alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
            StringBuilder b = new StringBuilder();
            for (int i = 0; i < 5; i++) b.append(alphabet.charAt(rnd.nextInt(alphabet.length())));
            String id = b.toString();
            return map.containsKey(id) ? genId() : id;
        }
    }

    static class Session {
        final String id;
        final Handler client;
        Handler trainer;
        final String clientName;
        final java.time.Instant opened = java.time.Instant.now();
        models.user clientUser;
        Session(String id, Handler client, String clientName) {
            this.id = id; this.client = client; this.clientName = clientName;
        }
        Handler peer(Handler h) { return h == client ? trainer : (h == trainer ? client : null); }
    }
}
