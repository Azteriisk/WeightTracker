package net.azterisk.weighttracker;

public class WeightEntry {
    private final long id;
    private final String date;
    private final double weight;
    private double trendWeight;

    public WeightEntry(long id, String date, double weight) {
        this.id = id;
        this.date = date;
        this.weight = weight;
        this.trendWeight = weight;
    }

    public long getId() {
        return id;
    }

    public String getDate() {
        return date;
    }

    public double getWeight() {
        return weight;
    }

    public double getTrendWeight() {
        return trendWeight;
    }

    public void setTrendWeight(double trendWeight) {
        this.trendWeight = trendWeight;
    }
}
