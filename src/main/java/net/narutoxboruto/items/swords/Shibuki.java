package net.narutoxboruto.items.swords;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;


public class Shibuki extends AbstractAbilitySword {
    public Shibuki(Properties pProperties) {
        super(SwordCustomTiers.SHIBUKI, pProperties);
    }

    @Override
    public int getChakraCost() {
        return 30;
    }

    protected void doSpecialAbility(LivingEntity pTarget, ServerPlayer serverPlayer) {
        Level level = serverPlayer.level();

        //Explosion at targeted location (where player is looking)
        Vec3 start = serverPlayer.getEyePosition(1.0F);
        Vec3 lookVec = serverPlayer.getViewVector(1.0F);
        Vec3 end = start.add(lookVec.scale(30.0D)); // Extended range for projectile-like effect

        ClipContext clipContext = new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, serverPlayer);
        BlockHitResult rayTraceResult = level.clip(clipContext);
        Vec3 explosionPos = rayTraceResult.getLocation();

        // Create the main explosion
        level.explode(
                serverPlayer, // Source entity
                explosionPos.x(),
                explosionPos.y(),
                explosionPos.z(),
                2.5F, // Power
                false,  // No fire
                Level.ExplosionInteraction.NONE
        );

        // Create smaller secondary explosions around the main one
        for (int i = 0; i < 3; i++) {
            Vec3 offset = new Vec3(
                    (level.random.nextDouble() - 0.5) * 3.0,
                    (level.random.nextDouble() - 0.5) * 2.0,
                    (level.random.nextDouble() - 0.5) * 3.0
            );
            Vec3 secondaryPos = explosionPos.add(offset);

            level.explode(
                    serverPlayer,
                    secondaryPos.x(),
                    secondaryPos.y(),
                    secondaryPos.z(),
                    1.5F, // Smaller power for secondary explosions
                    false, // No fire for secondary
                    Level.ExplosionInteraction.NONE
            );
        }

        //Particle effects
        if (level instanceof ServerLevel serverLevel) {
            // Explosion particles
            serverLevel.sendParticles(
                    ParticleTypes.EXPLOSION,
                    explosionPos.x(),
                    explosionPos.y(),
                    explosionPos.z(),
                    10,
                    1.0D, 1.0D, 1.0D,
                    0.5D
            );

            // Smoke particles
            serverLevel.sendParticles(
                    ParticleTypes.CAMPFIRE_COSY_SMOKE,
                    explosionPos.x(),
                    explosionPos.y() + 1.0,
                    explosionPos.z(),
                    20,
                    2.0D, 2.0D, 2.0D,
                    0.1D
            );
        }

        //Knockback effect to nearby entities
        AABB explosionArea = new AABB(
                explosionPos.x() - 5.0, explosionPos.y() - 5.0, explosionPos.z() - 5.0,
                explosionPos.x() + 5.0, explosionPos.y() + 5.0, explosionPos.z() + 5.0
        );

        List<LivingEntity> nearbyEntities = level.getEntitiesOfClass(
                LivingEntity.class,
                explosionArea,
                entity -> entity != serverPlayer && entity != pTarget
        );

        for (LivingEntity entity : nearbyEntities) {
            Vec3 knockbackDir = entity.position().subtract(explosionPos).normalize();
            double dist = entity.position().distanceTo(explosionPos);
            double falloff = Math.max(0.0, 1.0 - dist / 5.0); // Linear falloff over 5 blocks
            double strength = 0.4 * falloff;
            entity.setDeltaMovement(entity.getDeltaMovement().add(
                    knockbackDir.x * strength,
                    Math.max(0.1, knockbackDir.y * strength + 0.15) * falloff,
                    knockbackDir.z * strength
            ));
            entity.hurtMarked = true; // Force velocity update
        }

        // Sound effect
        level.playSound(
                null,
                serverPlayer.getX(),
                serverPlayer.getY(),
                serverPlayer.getZ(),
                SoundEvents.GENERIC_EXPLODE,
                SoundSource.PLAYERS,
                1.0F,
                (1.0F + (level.random.nextFloat() - level.random.nextFloat()) * 0.2F) * 0.7F
        );

        super.doSpecialAbility(pTarget, serverPlayer);
    }
}
