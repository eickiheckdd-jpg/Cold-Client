package com.coldclient;

import net.fabricmc.api.ClientModInitializer;

public class ColdClient implements ClientModInitializer {

    public static final String NAME = "Cold Client";
    public static final String VERSION = "1.0.0";

    @Override
    public void onInitializeClient() {
        System.out.println(NAME + " " + VERSION + " initialized.");
    }
}