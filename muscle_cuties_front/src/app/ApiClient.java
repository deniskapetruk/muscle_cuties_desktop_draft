package app;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.CopyOnWriteArrayList;

public class ApiClient {
    public interface Listener {
        void onMessage(String m);
        void onConnected();
        void onDisconnected();
        void onError(String e);
    }

    private final CopyOnWriteArrayList<Listener> listeners = new CopyOnWriteArrayList<>();
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private volatile boolean running;
    private volatile String username;
    private volatile String role;
    private volatile String sessionId;

    public void addListener(Listener l) { listeners.add(l); }
    public void removeListener(Listener l) { listeners.remove(l); }
    public void setUsername(String u) { username = u; }
    public String getUsername() { return username; }
    public String getRole() { return role; }
    public String getSessionId() { return sessionId; }

    public boolean isConnected() {
        try { return socket != null && socket.isConnected() && !socket.isClosed() && running; }
        catch (Exception e) { return false; }
    }

    public void connect(String host, int port) {
        if (isConnected()) return;
        try {
            socket = new Socket(host, port);
            out = new ObjectOutputStream(socket.getOutputStream()); out.flush();
            in = new ObjectInputStream(socket.getInputStream());
            running = true;
            for (Listener l : listeners) l.onConnected();
            Thread t = new Thread(() -> {
                try {
                    while (running) {
                        Object o = in.readObject();
                        if (o instanceof String) {
                            String m = (String) o;
                            if (m.startsWith("ROLE ")) role = m.substring(5).trim();
                            else if (m.startsWith("SESSION ")) sessionId = m.substring(8).trim();
                            for (Listener l : listeners) l.onMessage(m);
                        }
                    }
                } catch (Exception e) {
                    for (Listener l : listeners) l.onError(e.getMessage());
                } finally {
                    running = false;
                    for (Listener l : listeners) l.onDisconnected();
                    try { if (socket != null) socket.close(); } catch (IOException ignored) {}
                    socket = null;
                    out = null;
                    in = null;
                }
            });
            t.setDaemon(true); t.start();
        } catch (Exception e) {
            for (Listener l : listeners) l.onError(e.getMessage());
        }
    }

    public void send(String s) {
        try {
            if (!isConnected()) {
                for (Listener l : listeners) l.onError("Not connected");
                return;
            }
            if (out != null) { out.writeObject(s); out.flush(); }
        } catch (IOException e) {
            for (Listener l : listeners) l.onError(e.getMessage());
        }
    }

    public void disconnect() {
        running = false;
        try { if (socket != null) socket.close(); } catch (IOException ignored) {}
    }
}
