package me.ronsane.finditemaddon.finditemaddon.Dependencies;

import com.earth2me.essentials.Essentials;
import me.ronsane.finditemaddon.finditemaddon.Models.EssentialWarpModel;
import me.ronsane.finditemaddon.finditemaddon.Utils.LoggerUtils;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class EssentialsXPlugin {

    private static Essentials essAPI = null;
    private static List<EssentialWarpModel> allWarpsList = null;

    public static void setup() {
        if(Bukkit.getPluginManager().isPluginEnabled("Essentials")) {
            essAPI = (Essentials) Bukkit.getServer().getPluginManager().getPlugin("Essentials");
            if(essAPI != null) {
                LoggerUtils.logInfo("Found Essentials");
            }
        }
    }

    public static boolean isEnabled() {
        return essAPI != null;
    }

    public static Essentials getAPI() {
        return essAPI;
    }

    public static List<EssentialWarpModel> getAllWarps() {
        return allWarpsList;
    }

    public static void updateAllWarps() {
        if(essAPI.isEnabled()) {
            Collection<String> allWarps = EssentialsXPlugin.getAPI().getWarps().getList();
            allWarpsList = new ArrayList<>();
            allWarps.forEach(warp -> {
                try {
                    EssentialWarpModel essWarp = new EssentialWarpModel();
                    essWarp.warpName = warp;
                    essWarp.warpLoc = essAPI.getWarps().getWarp(warp);
                    allWarpsList.add(essWarp);
                } catch (Exception ignored) { }
            });
        }
    }

}