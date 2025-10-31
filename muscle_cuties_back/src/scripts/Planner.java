package scripts;

import models.*;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.*;
import java.util.Locale;

public class Planner {
    static class Adjust {
        final String label; final double loadMul; final int setsMain; final int setsAssist; final int repsStrength; final int repsHyper; final int repsEndu;
        Adjust(String label, double loadMul, int setsMain, int setsAssist, int repsStrength, int repsHyper, int repsEndu) {
            this.label = label; this.loadMul = loadMul; this.setsMain = setsMain; this.setsAssist = setsAssist;
            this.repsStrength = repsStrength; this.repsHyper = repsHyper; this.repsEndu = repsEndu;
        }
    }

    static Map<phase, Adjust> PHASE = new HashMap<>();
    static {
        PHASE.put(phase.menstrual,  new Adjust("Light", 0.85, 3, 2, 3, 10, 15));
        PHASE.put(phase.follicular, new Adjust("Build", 1.00, 4, 3, 4, 12, 18));
        PHASE.put(phase.ovulatory,  new Adjust("Peak", 1.05, 5, 3, 3, 12, 20));
        PHASE.put(phase.luteal,     new Adjust("Deload", 0.90, 3, 2, 3, 10, 15));
    }

    public static List<String> buildPlan(models.user user, LocalDate day) {
        if (user == null) return java.util.Collections.singletonList("NO_PLAN");
        phase ph = user.getCyclePhase() == null ? phase.follicular : user.getCyclePhase();
        Adjust adj = PHASE.getOrDefault(ph, PHASE.get(phase.follicular));

        String dowShort = day.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
        String workoutName = (user.getCurrentWorkout() == null || user.getCurrentWorkout().trim().isEmpty())
                ? "Default" : user.getCurrentWorkout().trim();

        List<String> out = new ArrayList<>();

        // Rest day handling
        if (!user.isTrainingDay(day)) {
            out.add("PLAN " + workoutName + " " + dowShort + " restday");
            out.add("REST No workout planned today. Your mission: move gently, smile often, and enjoy something cozy. ✨");
            out.add("TIP Tomorrow, main lifts aim for RPE 8, accessories RPE 7. If you feel tired, reduce weight by ~5–10%.");
            return out;
        }

        // Header
        out.add(String.format("PLAN %s %s phase=%s adj=%s type=%s",
                workoutName, dowShort, ph.name(), adj.label, user.getPlanType()));

        final String type = (user.getPlanType() == null ? "Strength" : user.getPlanType()).toLowerCase(Locale.ROOT);

        final List<models.exercise> pool = allExercises();
        List<models.exercise> plan;
        switch (type) {
            case "hypertrophy_upper":    plan = templateHypertrophyUpper(pool);    break;
            case "hypertrophy_lower":    plan = templateHypertrophyLower(pool);    break;
            case "hypertrophy_balanced": plan = templateHypertrophyBalanced(pool); break;
            case "endurance":            plan = templateEndurance(pool);           break;
            default:                     plan = templateStrength(pool);            break;
        }

        // Rotate to provide 5 distinct days
        int idx = (day.getDayOfWeek().getValue() - 1) % 5;
        plan = rotate(plan, idx);

        // Apply per-workout exercise substitutions
        Map<String,String> subs = user.getSubstitutions();
        if (subs != null && !subs.isEmpty()) {
            for (int i=0;i<plan.size();i++) {
                String name = plan.get(i).getName();
                if (subs.containsKey(name)) {
                    String newName = subs.get(name);
                    for (models.exercise e : pool) {
                        if (e.getName().equalsIgnoreCase(newName)) { plan.set(i, e); break; }
                    }
                }
            }
        }

        // Emit EX lines, applying set/rep overrides per exercise when present
        for (models.exercise e : plan) {
            boolean isMain = e.isMainExercise();
            int sets = isMain ? adj.setsMain : adj.setsAssist;
            int reps;
            if (type.startsWith("hypertrophy"))      reps = adj.repsHyper;
            else if (type.equals("endurance"))       reps = adj.repsEndu;
            else                                     reps = adj.repsStrength;

            // Per-row overrides
            Integer oS = user.getOverrideSets(e.getName());
            Integer oR = user.getOverrideReps(e.getName());
            if (oS != null) sets = oS;
            if (oR != null) reps = oR;

            // Secondary muscles string
            String secondaries = "";
            models.muscle[] secs = e.getSecondaryMuscles();
            if (secs != null) {
                List<String> names = new ArrayList<>();
                for (models.muscle m : secs) if (m != null) names.add(m.name());
                secondaries = String.join("/", names);
            }
            out.add(String.format("EX %s;%s;%s;%d;%d",
                    e.getName(), e.getMainMuscle().name(), secondaries, sets, reps));
        }

        // Day guidance (cute + actionable)
        out.add("TIP 💗 Today: main lifts @ RPE 8, accessories @ RPE 7. "
                + "If you’re in menstrual phase, lighten loads ~10–15%. In luteal, ~5–10%. "
                + "Choose weight so last 2 reps feel challenging but doable. Hydrate 💧 and rest 2–3 min on main lifts, 60–90s on accessories.");
        return out;
    }

    private static List<models.exercise> rotate(List<models.exercise> list, int n) {
        if (list.isEmpty() || n==0) return list;
        List<models.exercise> r = new ArrayList<>(list.size());
        for (int i=0;i<list.size();i++) r.add(list.get((i+n)%list.size()));
        return r;
    }

    private static List<models.exercise> templateStrength(List<models.exercise> pool) {
        return pick(pool, new models.muscle[]{ models.muscle.middle_chest, models.muscle.latissimus, models.muscle.quadriceps, models.muscle.hamstrings, models.muscle.gluteus });
    }
    private static List<models.exercise> templateHypertrophyUpper(List<models.exercise> pool) {
        return pick(pool, new models.muscle[]{ models.muscle.middle_chest, models.muscle.upper_chest, models.muscle.triceps, models.muscle.biceps, models.muscle.front_deltoids, models.muscle.middle_deltoids, models.muscle.back_deltoids });
    }
    private static List<models.exercise> templateHypertrophyLower(List<models.exercise> pool) {
        return pick(pool, new models.muscle[]{ models.muscle.gluteus, models.muscle.quadriceps, models.muscle.hamstrings, models.muscle.calves, models.muscle.erector_spinae });
    }
    private static List<models.exercise> templateHypertrophyBalanced(List<models.exercise> pool) {
        return pick(pool, new models.muscle[]{ models.muscle.gluteus, models.muscle.quadriceps, models.muscle.middle_chest, models.muscle.latissimus, models.muscle.middle_deltoids });
    }
    private static List<models.exercise> templateEndurance(List<models.exercise> pool) {
        return pick(pool, new models.muscle[]{ models.muscle.abdominals, models.muscle.latissimus, models.muscle.gluteus, models.muscle.quadriceps, models.muscle.middle_deltoids });
    }

    private static List<models.exercise> pick(List<models.exercise> pool, models.muscle[] order) {
        List<models.exercise> r = new ArrayList<>();
        for (models.muscle m : order) {
            models.exercise main = null, accessory = null;
            for (models.exercise e : pool) {
                if (e.getMainMuscle() == m) {
                    if (e.isMainExercise() && main == null) main = e;
                    else if (!e.isMainExercise() && accessory == null) accessory = e;
                }
            }
            if (main != null) r.add(main);
            if (accessory != null) r.add(accessory);
        }
        return r;
    }

    // === Expanded catalog ===
    public static List<models.exercise> allExercises() {
        List<models.exercise> list = new ArrayList<>();
        // legs / glutes / hams / quads
        list.add(new models.exercise("Barbell Squat", models.muscle.quadriceps, models.muscle.gluteus, models.muscle.hamstrings, true));
        list.add(new models.exercise("Front Squat", models.muscle.quadriceps, models.muscle.gluteus, models.muscle.erector_spinae, true));
        list.add(new models.exercise("Hack Squat", models.muscle.quadriceps, models.muscle.gluteus, models.muscle.hamstrings, true));
        list.add(new models.exercise("Leg Press", models.muscle.quadriceps, models.muscle.gluteus, models.muscle.hamstrings, false));
        list.add(new models.exercise("Bulgarian Split Squat", models.muscle.quadriceps, models.muscle.gluteus, models.muscle.hamstrings, false));
        list.add(new models.exercise("Walking Lunge", models.muscle.quadriceps, models.muscle.gluteus, models.muscle.hamstrings, false));
        list.add(new models.exercise("Romanian Deadlift", models.muscle.hamstrings, models.muscle.gluteus, models.muscle.erector_spinae, true));
        list.add(new models.exercise("Good Morning", models.muscle.hamstrings, models.muscle.erector_spinae, models.muscle.gluteus, false));
        list.add(new models.exercise("Cable Pull-Through", models.muscle.hamstrings, models.muscle.gluteus, models.muscle.erector_spinae, false));
        list.add(new models.exercise("Nordic Ham Curl", models.muscle.hamstrings, null, null, false));
        list.add(new models.exercise("Hip Thrust", models.muscle.gluteus, models.muscle.hamstrings, models.muscle.quadriceps, true));
        list.add(new models.exercise("Barbell Glute Bridge", models.muscle.gluteus, models.muscle.hamstrings, models.muscle.quadriceps, false));
        list.add(new models.exercise("Step-Up", models.muscle.gluteus, models.muscle.quadriceps, models.muscle.hamstrings, false));
        list.add(new models.exercise("Reverse Lunge", models.muscle.gluteus, models.muscle.quadriceps, models.muscle.hamstrings, false));
        list.add(new models.exercise("Calf Raise", models.muscle.calves, models.muscle.gluteus, null, false));
        list.add(new models.exercise("Seated Calf Raise", models.muscle.calves, null, null, false));

        // chest / push
        list.add(new models.exercise("Bench Press", models.muscle.middle_chest, models.muscle.triceps, models.muscle.front_deltoids, true));
        list.add(new models.exercise("Incline DB Press", models.muscle.upper_chest, models.muscle.triceps, models.muscle.front_deltoids, false));
        list.add(new models.exercise("Decline Press", models.muscle.middle_chest, models.muscle.triceps, models.muscle.front_deltoids, false));
        list.add(new models.exercise("Chest Dip", models.muscle.middle_chest, models.muscle.triceps, models.muscle.front_deltoids, false));
        list.add(new models.exercise("Machine Chest Press", models.muscle.middle_chest, models.muscle.triceps, models.muscle.front_deltoids, false));
        list.add(new models.exercise("Overhead Press", models.muscle.front_deltoids, models.muscle.middle_deltoids, models.muscle.triceps, true));
        list.add(new models.exercise("Arnold Press", models.muscle.front_deltoids, models.muscle.middle_deltoids, models.muscle.triceps, false));
        list.add(new models.exercise("Lateral Raise", models.muscle.middle_deltoids, models.muscle.front_deltoids, models.muscle.back_deltoids, false));
        list.add(new models.exercise("Rear Delt Fly", models.muscle.back_deltoids, models.muscle.rhomboids, null, false));
        list.add(new models.exercise("Face Pull", models.muscle.back_deltoids, models.muscle.rhomboids, models.muscle.middle_deltoids, false));

        // back / pull
        list.add(new models.exercise("Lat Pulldown", models.muscle.latissimus, models.muscle.biceps, models.muscle.rhomboids, false));
        list.add(new models.exercise("Pull-Up", models.muscle.latissimus, models.muscle.biceps, models.muscle.rhomboids, true));
        list.add(new models.exercise("Seated Row", models.muscle.rhomboids, models.muscle.latissimus, models.muscle.biceps, false));
        list.add(new models.exercise("Chest-Supported Row", models.muscle.rhomboids, models.muscle.latissimus, models.muscle.biceps, false));
        list.add(new models.exercise("Single-Arm Dumbbell Row", models.muscle.latissimus, models.muscle.rhomboids, models.muscle.biceps, false));
        list.add(new models.exercise("Plank", models.muscle.abdominals, null, null, false));
        list.add(new models.exercise("Hanging Leg Raise", models.muscle.abdominals, models.muscle.hamstrings, null, false));
        list.add(new models.exercise("Cable Crunch", models.muscle.abdominals, models.muscle.hamstrings, null, false));
        list.add(new models.exercise("Deadlift", models.muscle.erector_spinae, models.muscle.gluteus, models.muscle.hamstrings, true));
        list.add(new models.exercise("Back Extension", models.muscle.erector_spinae, models.muscle.gluteus, models.muscle.hamstrings, false));
        list.add(new models.exercise("Bird Dog", models.muscle.erector_spinae, models.muscle.gluteus, models.muscle.abdominals, false));
        list.add(new models.exercise("Superman Hold", models.muscle.erector_spinae, models.muscle.gluteus, models.muscle.abdominals, false));
        list.add(new models.exercise("DB Curl", models.muscle.biceps, models.muscle.forearms, null, false));
        list.add(new models.exercise("EZ-Bar Curl", models.muscle.biceps, models.muscle.forearms, null, false));
        list.add(new models.exercise("Hammer Curl", models.muscle.biceps, models.muscle.forearms, null, false));
        list.add(new models.exercise("Triceps Pushdown", models.muscle.triceps, models.muscle.front_deltoids, null, false));
        list.add(new models.exercise("Overhead Triceps Extension", models.muscle.triceps, models.muscle.front_deltoids, null, false));

        return list;
    }
}
