package com.coldclient.module;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.entity.Entity;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Friend list, saved to config/coldclient-friends.txt. Combat modules can skip friends. */
public final class FriendManager {
    private static final List<String> NAMES = new ArrayList<>();
    private static boolean loaded;

    private FriendManager() {}

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("coldclient-friends.txt");
    }

    public static void load() {
        if (loaded) {
            return;
        }
        loaded = true;
        try {
            Path f = file();
            if (Files.exists(f)) {
                for (String line : Files.readAllLines(f)) {
                    String name = line.trim();
                    if (isValidName(name) && indexOf(name) < 0) {
                        NAMES.add(name);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("[Cold Client] Could not read friends: " + e.getMessage());
        }
    }

    private static void save() {
        try {
            Files.write(file(), NAMES);
        } catch (IOException e) {
            System.err.println("[Cold Client] Could not save friends: " + e.getMessage());
        }
    }

    public static boolean isValidName(String name) {
        if (name == null || name.isEmpty() || name.length() > 16) {
            return false;
        }
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            boolean ok = (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_';
            if (!ok) {
                return false;
            }
        }
        return true;
    }

    public static List<String> list() {
        load();
        return Collections.unmodifiableList(NAMES);
    }

    public static boolean add(String name) {
        load();
        name = name == null ? "" : name.trim();
        if (!isValidName(name) || indexOf(name) >= 0) {
            return false;
        }
        NAMES.add(name);
        save();
        return true;
    }

    public static void removeAt(int index) {
        load();
        if (index >= 0 && index < NAMES.size()) {
            NAMES.remove(index);
            save();
        }
    }

    public static boolean isFriend(String name) {
        load();
        return name != null && indexOf(name) >= 0;
    }

    public static boolean isFriend(Entity entity) {
        return entity != null && isFriend(entity.getName().getString());
    }

    private static int indexOf(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        for (int i = 0; i < NAMES.size(); i++) {
            if (NAMES.get(i).toLowerCase(Locale.ROOT).equals(lower)) {
                return i;
            }
        }
        return -1;
    }
}