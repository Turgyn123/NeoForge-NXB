package net.narutoxboruto.jutsu;

/**
 * Data class representing a jutsu's properties.
 */
public class JutsuData {
    private final String id;
    private final String displayName;
    private final int chakraCost;
    private final int cooldownTicks; // 20 ticks = 1 second
    private final String translationKey;
    
    public JutsuData(String id, String displayName, int chakraCost, int cooldownTicks, String translationKey) {
        this.id = id;
        this.displayName = displayName;
        this.chakraCost = chakraCost;
        this.cooldownTicks = cooldownTicks;
        this.translationKey = translationKey;
    }
    
    public String getId() {
        return id;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public int getChakraCost() {
        return chakraCost;
    }
    
    public int getCooldownTicks() {
        return cooldownTicks;
    }
    
    public String getTranslationKey() {
        return translationKey;
    }
    
    /**
     * Get the texture path for this jutsu's icon.
     */
    public String getIconTexture() {
        return "textures/jutsu/" + id + ".png";
    }
}
