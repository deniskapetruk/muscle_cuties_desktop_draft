package models;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;

public class user {
    private String firstName;
    private String lastName;
    private String userName;
    private String email;
    private final String password;

    private phase cyclePhase;
    private int cycleDay;

    // Active workout/program name
    private String currentWorkout = "Default";

    // Planner “type” still exists internally; but we pick it from workout names when needed
    private String planType = "Strength";

    // Legacy base subs
    private final Map<String,String> baseSubs = new HashMap<>();
    // Custom plans: workout -> subs
    private final Map<String, Map<String,String>> customPlans = new HashMap<>();

    // Per-workout overrides: workout -> (exercise -> sets/reps)
    private final Map<String, Map<String,Integer>> setsOverrides = new HashMap<>();
    private final Map<String, Map<String,Integer>> repsOverrides = new HashMap<>();

    // Timetable (training days)
    private EnumSet<DayOfWeek> timetable = EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY);

    private Map<String,String> presets = new HashMap<>(); // reserved
    private trainer trainer;
    private final LocalDate createdAt;

    public user(String userName, String email, String password) {
        this.userName = userName; this.email = email; this.password = password;
        this.cyclePhase = phase.follicular; this.cycleDay = 1;
        this.createdAt = LocalDate.now(); this.trainer = null;
        customPlans.put("Default", baseSubs);
    }
    public user(String firstName, String lastName, String userName, String email, String password, phase cyclePhase, int cycleDay) {
        this.firstName = firstName; this.lastName = lastName; this.userName = userName; this.email = email; this.password = password;
        this.cyclePhase = cyclePhase; this.cycleDay = cycleDay;
        this.createdAt = LocalDate.now(); this.trainer = null;
        customPlans.put("Default", baseSubs);
    }

    // Basic props
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public phase getCyclePhase() { return cyclePhase; }
    public void setCyclePhase(phase cyclePhase) { this.cyclePhase = cyclePhase; }
    public int getCycleDay() { return cycleDay; }
    public void setCycleDay(int cycleDay) { this.cycleDay = Math.max(1, cycleDay); }
    public LocalDate getCreatedAt() { return createdAt; }
    public trainer getTrainer() { return trainer; }
    public void setTrainer(trainer trainer) { this.trainer = trainer; }

    // Workout + plan type
    public String getCurrentWorkout() { return currentWorkout; }
    public void setCurrentWorkout(String currentWorkout) {
        if (currentWorkout == null || currentWorkout.trim().isEmpty()) return;
        String w = currentWorkout.trim();
        // create snapshot of active subs if new
        if (!customPlans.containsKey(w)) customPlans.put(w, new HashMap<>(getActiveSubs()));
        this.currentWorkout = w;
        // If it matches a known preset, sync planType implicitly
        String wl = w.toLowerCase(Locale.ROOT);
        if (wl.equals("strength") || wl.equals("endurance") || wl.startsWith("hypertrophy_"))
            this.planType = w;
    }

    public String getPlanType() { return planType; }
    public void setPlanType(String t) { if (t != null && !t.trim().isEmpty()) this.planType = t.trim(); }

    // Substitutions (per active workout)
    public Map<String,String> getSubstitutions() { return new HashMap<>(getActiveSubs()); }
    public void setSubstitution(String from, String to) { if (from != null && to != null) getActiveSubs().put(from, to); }
    public void clearSubstitutions() { getActiveSubs().clear(); }

    private Map<String,String> getActiveSubs() {
        Map<String,String> m = customPlans.get(currentWorkout);
        if (m == null) { m = new HashMap<>(); customPlans.put(currentWorkout, m); }
        return m;
    }

    // Set/Rep overrides (per active workout)
    public void setRowOverride(String exName, Integer sets, Integer reps) {
        if (exName == null) return;
        String key = exName.trim();
        Map<String,Integer> so = setsOverrides.computeIfAbsent(currentWorkout, k -> new HashMap<>());
        Map<String,Integer> ro = repsOverrides.computeIfAbsent(currentWorkout, k -> new HashMap<>());
        if (sets != null) so.put(key, sets);
        if (reps != null) ro.put(key, reps);
    }
    public Integer getOverrideSets(String exName) {
        Map<String,Integer> so = setsOverrides.get(currentWorkout);
        return (so==null)? null : so.get(exName);
    }
    public Integer getOverrideReps(String exName) {
        Map<String,Integer> ro = repsOverrides.get(currentWorkout);
        return (ro==null)? null : ro.get(exName);
    }

    // Timetable
    public EnumSet<DayOfWeek> getTimetable() { return timetable.clone(); }
    public void setTimetable(EnumSet<DayOfWeek> days) {
        if (days != null && !days.isEmpty()) this.timetable = days.clone();
    }
    public void setTimetableCSV(String csv) {
        EnumSet<DayOfWeek> s = EnumSet.noneOf(DayOfWeek.class);
        if (csv != null) {
            for (String t : csv.split(",")) {
                String v = t.trim().toLowerCase(Locale.ROOT);
                if (v.startsWith("mon")) s.add(DayOfWeek.MONDAY);
                else if (v.startsWith("tue")) s.add(DayOfWeek.TUESDAY);
                else if (v.startsWith("wed")) s.add(DayOfWeek.WEDNESDAY);
                else if (v.startsWith("thu")) s.add(DayOfWeek.THURSDAY);
                else if (v.startsWith("fri")) s.add(DayOfWeek.FRIDAY);
                else if (v.startsWith("sat")) s.add(DayOfWeek.SATURDAY);
                else if (v.startsWith("sun")) s.add(DayOfWeek.SUNDAY);
            }
        }
        if (!s.isEmpty()) this.timetable = s;
    }
    public boolean isTrainingDay(LocalDate d) {
        return timetable.contains(d.getDayOfWeek());
    }

    @Override
    public String toString() {
        return "user{" +
                "userName='" + userName + '\'' +
                ", email='" + email + '\'' +
                ", cyclePhase=" + cyclePhase +
                ", cycleDay=" + cycleDay +
                ", currentWorkout=" + currentWorkout +
                ", planType=" + planType +
                ", trainer=" + (trainer != null ? trainer.getUsername() : "none") +
                '}';
    }
}
