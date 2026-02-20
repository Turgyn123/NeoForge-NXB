package net.narutoxboruto.items.jutsus;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.narutoxboruto.entities.jutsus.WaterDragonEntity;

/**
 * Water Dragon Jutsu - Water Release
 * 
 * A powerful water-style technique that summons a fast-moving dragon made of compressed water.
 * More powerful and faster than Shark Bomb, creates an explosive water impact.
 * 
 * TODO: Awaiting custom model and textures from owner.
 * Current implementation uses placeholder rendering.
 * 
 * - Long range (20 blocks)
 * - High speed
 * - Large explosion on impact
 * - Water Release required
 */
public class WaterDragon extends AbstractJutsuItem {
    
    public WaterDragon(Properties properties) {
        super(properties);
    }
    
    @Override
    public int getChakraCost() {
        return 60; // High chakra cost for powerful jutsu
    }
    
    @Override
    public int getCooldownTicks() {
        return 100; // 5 seconds cooldown
    }
    
    @Override
    public String getRequiredRelease() {
        return "water";
    }
    
    @Override
    public String getJutsuName() {
        return "Water Dragon";
    }
    
    @Override
    protected boolean executeJutsu(ServerPlayer player, Level level) {
        // Create and spawn the water dragon projectile
        WaterDragonEntity dragon = new WaterDragonEntity(level, player);
        level.addFreshEntity(dragon);
        return true;
    }
    
    @Override
    protected void playCastSound(Level level, Player player) {
        // No cast sound for water dragon
    }
}
