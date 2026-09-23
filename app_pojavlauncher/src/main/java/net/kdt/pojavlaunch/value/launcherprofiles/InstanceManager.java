package net.kdt.pojavlaunch.value.launcherprofiles;

import java.io.File;
import java.util.Locale;

/**
 * Lightweight instance-directory manager.
 *
 * Existing profiles keep their current gameDir for backwards compatibility.
 * New profiles receive a stable, isolated directory under .minecraft/instances.
 * This prevents mods, configs, saves and options from leaking between versions.
 */
public final class InstanceManager {
    private InstanceManager() {}

    public static String allocateGameDir(String profileName, String profileKey) {
        String slug = sanitize(profileName);
        String id = profileKey == null ? "instance" : profileKey.replace("-", "");
        if (id.length() > 8) id = id.substring(0, 8);
        return "./instances/" + slug + "-" + id;
    }

    public static File resolve(File gameHome, MinecraftProfile profile) {
        if (profile == null) return gameHome;
        if (profile.gameDir == null || profile.gameDir.trim().isEmpty()) return gameHome;
        String path = profile.gameDir;
        if (path.startsWith("./")) path = path.substring(2);
        return new File(gameHome, path);
    }

    private static String sanitize(String value) {
        if (value == null || value.trim().isEmpty()) return "Minecraft";
        String s = value.trim().replaceAll("[^A-Za-z0-9._-]+", "-");
        s = s.replaceAll("-{2,}", "-");
        while (s.startsWith(".")) s = s.substring(1);
        if (s.isEmpty()) s = "Minecraft";
        if (s.length() > 48) s = s.substring(0, 48);
        return s;
    }
}
