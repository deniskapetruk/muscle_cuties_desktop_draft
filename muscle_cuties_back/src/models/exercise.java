
package models;

public class exercise {
    private String name;
    private muscle mainMuscle;
    private muscle[] secondaryMuscles = new muscle[2];
    private boolean isMainExercise;

    public exercise(String name, muscle mainMuscle, muscle secondaryMuscle1, muscle secondaryMuscle2, boolean isMainExercise) {
        this.name = name;
        this.mainMuscle = mainMuscle;
        this.secondaryMuscles[0] = secondaryMuscle1;
        this.secondaryMuscles[1] = secondaryMuscle2;
        this.isMainExercise = isMainExercise;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public muscle getMainMuscle() { return mainMuscle; }
    public void setMainMuscle(muscle mainMuscle) { this.mainMuscle = mainMuscle; }
    public muscle[] getSecondaryMuscles() { return secondaryMuscles; }
    public void setSecondaryMuscles(muscle[] secondaryMuscles) { this.secondaryMuscles = secondaryMuscles; }
    public boolean isMainExercise() { return isMainExercise; }
    public void setMainExercise(boolean mainExercise) { isMainExercise = mainExercise; }

    @Override
    public String toString() {
        String s1 = secondaryMuscles[0] == null ? "null" : secondaryMuscles[0].name();
        String s2 = secondaryMuscles[1] == null ? "null" : secondaryMuscles[1].name();
        return "exercise{name=\"" + name + "\", mainMuscle=" + mainMuscle + ", secondaryMuscles=[" + s1 + ", " + s2 + "]}";
    }
}
