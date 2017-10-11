package com.eshtio.lib.rps.limiter;

public class Main {

    public static void main(String[] args) throws InterruptedException {
        RpsClient rpsClient = new RpsClient(8);
        rpsClient.startConsumer();
        for (int i = 0; i < 100; i++) {
            rpsClient.sendMessage("Message_" + i);
        }
        rpsClient.waitConsumer();
    }



}
