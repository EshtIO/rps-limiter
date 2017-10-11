package com.eshtio.lib.rps.limiter;

import java.time.LocalTime;

public class Main {

    public static void main(String[] args) throws InterruptedException {
        RpsClient rpsClient = new RpsClient(5);
        for (int i = 0; i < 100; i++) {
            System.out.println("Current seconds: " + LocalTime.now().getSecond());
            rpsClient.sendMessage("Message_" + i);
            System.out.println("Current seconds after: " + LocalTime.now().getSecond());
        }
    }



}
