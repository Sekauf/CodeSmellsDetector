package com.example.golden.two;

public class CoupledSmall {
    private final HelperA helperA = new HelperA();
    private final HelperB helperB = new HelperB();

    public int mix(int input) {
        return input + helperA.value() - helperB.value();
    }
}
