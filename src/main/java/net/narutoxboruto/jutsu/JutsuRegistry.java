package net.narutoxboruto.jutsu;

import java.util.*;

/**
 * Registry of all available jutsus organized by nature/release type.
 * This serves as a central place to define what jutsus exist in the mod.
 */
public class JutsuRegistry {
    
    private static final Map<String, List<JutsuData>> JUTSUS_BY_NATURE = new HashMap<>();
    private static final Map<String, JutsuData> JUTSUS_BY_ID = new HashMap<>();
    
    static {
        // Register Earth Release jutsus (lower costs for testing - default max chakra is 10)
        register("earth", new JutsuData("earth_wall", "Earth Wall", 5, 100, "jutsu.earth_wall"));
        
        // Register Fire Release jutsus
        register("fire", new JutsuData("fire_ball", "Fire Ball", 5, 60, "jutsu.fire_ball"));
        
        // Future jutsus can be added here:
        // register("water", new JutsuData("water_dragon", "Water Dragon", 30, 120, "jutsu.water_dragon"));
        // register("lightning", new JutsuData("chidori", "Chidori", 25, 80, "jutsu.chidori"));
        // register("wind", new JutsuData("wind_blade", "Wind Blade", 15, 60, "jutsu.wind_blade"));
    }
    
    private static void register(String nature, JutsuData jutsu) {
        JUTSUS_BY_NATURE.computeIfAbsent(nature.toLowerCase(), k -> new ArrayList<>()).add(jutsu);
        JUTSUS_BY_ID.put(jutsu.getId(), jutsu);
    }
    
    /**
     * Get all jutsus available for a specific nature/release.
     */
    public static List<JutsuData> getJutsusForNature(String nature) {
        return JUTSUS_BY_NATURE.getOrDefault(nature.toLowerCase(), Collections.emptyList());
    }
    
    /**
     * Get all jutsus the player can use based on their release list.
     */
    public static List<JutsuData> getAvailableJutsus(String releaseList) {
        List<JutsuData> available = new ArrayList<>();
        if (releaseList == null || releaseList.isEmpty()) {
            return available;
        }
        
        String[] releases = releaseList.split(",");
        for (String release : releases) {
            String trimmed = release.trim().toLowerCase();
            available.addAll(getJutsusForNature(trimmed));
        }
        return available;
    }
    
    /**
     * Get a jutsu by its ID.
     */
    public static JutsuData getJutsuById(String id) {
        return JUTSUS_BY_ID.get(id);
    }
    
    /**
     * Check if a jutsu exists.
     */
    public static boolean exists(String id) {
        return JUTSUS_BY_ID.containsKey(id);
    }
    
    /**
     * Get all registered jutsus.
     */
    public static Collection<JutsuData> getAllJutsus() {
        return JUTSUS_BY_ID.values();
    }
    
    /**
     * Get the nature/release type for a jutsu.
     */
    public static String getNatureForJutsu(String jutsuId) {
        for (Map.Entry<String, List<JutsuData>> entry : JUTSUS_BY_NATURE.entrySet()) {
            for (JutsuData jutsu : entry.getValue()) {
                if (jutsu.getId().equals(jutsuId)) {
                    return entry.getKey();
                }
            }
        }
        return null;
    }
}
