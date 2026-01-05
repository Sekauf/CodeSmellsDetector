package com.example.golden.one;

import com.example.golden.two.HelperA;
import com.example.golden.two.HelperB;

public class BigGod {
    private final HelperA helperA = new HelperA();
    private final HelperB helperB = new HelperB();
    private int counter;

    public void processA() {
        counter += helperA.value();
    }

    public void processB() {
        counter += helperB.value();
    }

    public void processC() {
        for (int i = 0; i < 5; i++) {
            counter += i;
        }
    }

    public void report() {
        String message = "counter=" + counter;
        if (message.length() > 0) {
            System.out.println(message);
        }
    }

    public int compute(int input) {
        int result = input;
        result += helperA.value();
        result -= helperB.value();
        result *= 2;
        return result;
    }

    public void reset() {
        counter = 0;
    }

    public void processD() {
        counter += 4;
    }

    public void processE() {
        counter += 5;
    }

    public void processF() {
        counter += 6;
    }

    public void processG() {
        counter += 7;
    }

    public void processH() {
        counter += 8;
    }

    public void processI() {
        counter += 9;
    }

    public void processJ() {
        counter += 10;
    }

    public void processK() {
        counter += 11;
    }

    public void processL() {
        counter += 12;
    }

    public void processM() {
        counter += 13;
    }
}
