package com.example.goldenexample;

public class RegularClassExample {
    private final HelperClass helper;
    private int value;

    public RegularClassExample(HelperClass helper) {
        this.helper = helper;
    }

    public int add(int delta) {
        value += delta;
        return value;
    }

    public String describe() {
        return helper.format("value=" + value);
    }
}
