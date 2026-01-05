package com.example.golden.one;

public class CohesiveLarge {
    private int total;

    public void add(int value) {
        total += value;
    }

    public void addAll(int[] values) {
        if (values == null) {
            return;
        }
        for (int value : values) {
            add(value);
        }
    }

    public int average(int count) {
        if (count <= 0) {
            return 0;
        }
        return total / count;
    }

    public int getTotal() {
        return total;
    }
}
