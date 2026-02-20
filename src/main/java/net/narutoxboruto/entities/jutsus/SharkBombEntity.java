package net.narutoxboruto.entities.jutsus;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;
import net.narutoxboruto.entities.ModEntities;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Shark Bomb Projectile - Water Release Jutsu
 * 
 * - Launches immediately on cast, grows from tiny (0.1x) to full size (1.0x) over 3 seconds of flight
 * - Strong homing behavior, re-acquires targets every tick, passes through non-target entities
 * - Impact: AOE water damage + explosion effect + temporary water blocks
 */
public class SharkBombEntity extends Projectile implements GeoEntity {
    
    /** Synched scale for client-side rendering during charge-up */
    private static final EntityDataAccessor<Float> ENTITY_SCALE = 
        SynchedEntityData.defineId(SharkBombEntity.class, EntityDataSerializers.FLOAT);
    
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    
    private int age = 0;
    private LivingEntity targetEntity = null;
    private Vec3 startPos; // set on spawn, used for range check
    
    // === Configuration ===
    private static final float DAMAGE = 16.0F;         // Direct hit damage
    private static final float AOE_DAMAGE = 8.0F;      // AOE splash damage at center
    private static final float SPEED = 0.8F;            // Flight speed (blocks/tick)
    private static final int GROW_TICKS = 60;           // 3s to grow from tiny to full size during flight
    private static final int MAX_FLIGHT_TICKS = 120;    // 6s max flight
    private static final double MAX_RANGE = 50.0;       // Max travel distance from launch point
    private static final double HOMING_RANGE = 50.0;    // Target detection range
    private static final double HOMING_STRENGTH = 0.3;  // How aggressively it steers (30% per tick)
    private static final float AOE_RADIUS = 3.0F;       // Splash damage radius
    private static final float EXPLOSION_POWER = 2.0F;  // Visual explosion power
    
    public SharkBombEntity(EntityType<? extends SharkBombEntity> entityType, Level level) {
        super(entityType, level);
    }
    
    public SharkBombEntity(Level level, LivingEntity shooter) {
        this(ModEntities.SHARK_BOMB.get(), level);
        this.setOwner(shooter);
        
        // Spawn 1.5 blocks in front of the shooter's eyes
        Vec3 look = shooter.getLookAngle();
        Vec3 spawnPos = shooter.getEyePosition().add(look.scale(1.5));
        this.setPos(spawnPos.x, spawnPos.y, spawnPos.z);
        this.startPos = this.position();
        
        // Start very small, grows during flight
        this.entityData.set(ENTITY_SCALE, 0.1f);
        
        // Find target and launch immediately (no charge phase)
        if (shooter instanceof Player player) {
            this.targetEntity = findBestTarget(player);
        }
        
        // Set velocity toward target or look direction
        Vec3 launchDir;
        if (this.targetEntity != null) {
            launchDir = this.targetEntity.getEyePosition().subtract(this.position()).normalize();
        } else {
            launchDir = shooter.getLookAngle();
        }
        this.setDeltaMovement(launchDir.scale(SPEED));
        
        // Face the launch direction
        double horizDist = launchDir.horizontalDistance();
        this.setYRot((float)(Mth.atan2(launchDir.x, launchDir.z) * (180.0 / Math.PI)));
        this.setXRot((float)(Mth.atan2(launchDir.y, horizDist) * (180.0 / Math.PI)));
        this.yRotO = this.getYRot();
        this.xRotO = this.getXRot();
    }
    
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(ENTITY_SCALE, 1.0f);
    }
    
    /** Get the current visual scale (0.2 during start of charge, 1.0 at full size) */
    public float getEntityScale() {
        return this.entityData.get(ENTITY_SCALE);
    }
    
    @Override
    public void tick() {
        super.tick();
        this.age++;
        
        Entity owner = this.getOwner();
        
        // If owner dies/disconnects, discard
        if (owner == null || !owner.isAlive()) {
            explodeInWater();
            this.discard();
            return;
        }
        
        // Max flight time / range check
        if (this.age > MAX_FLIGHT_TICKS || 
            (this.startPos != null && this.startPos.distanceTo(this.position()) > MAX_RANGE)) {
            this.explodeInWater();
            this.discard();
            return;
        }
        
        // Grow during flight (0.1 -> 1.0 over GROW_TICKS)
        if (this.age <= GROW_TICKS) {
            float scale = 0.1f + 0.9f * ((float) this.age / (float) GROW_TICKS);
            this.entityData.set(ENTITY_SCALE, Math.min(scale, 1.0f));
        } else if (this.getEntityScale() < 1.0f) {
            this.entityData.set(ENTITY_SCALE, 1.0f);
        }
        
        // Homing behavior - re-target every tick
        updateHoming();
        
        // Hit detection
        HitResult hitResult = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        if (hitResult.getType() != HitResult.Type.MISS) {
            this.onHit(hitResult);
            return;
        }
        
        // Update position
        Vec3 velocity = this.getDeltaMovement();
        this.setPos(this.getX() + velocity.x, this.getY() + velocity.y, this.getZ() + velocity.z);
        
        // Update rotation from velocity (only when moving - prevents snap to 0 on sync delay)
        double horizSpeed = velocity.horizontalDistance();
        if (horizSpeed > 0.001) {
            this.setYRot((float)(Mth.atan2(velocity.x, velocity.z) * (180.0 / Math.PI)));
            this.setXRot((float)(Mth.atan2(velocity.y, horizSpeed) * (180.0 / Math.PI)));
        }
        
        // Flight particles - water drops trailing behind
        if (this.level() instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.FALLING_WATER,
                this.getX(), this.getY(), this.getZ(),
                50, 0.4, 0.4, 0.4, 0.1);
            sl.sendParticles(ParticleTypes.SPLASH,
                this.getX(), this.getY(), this.getZ(),
                50, 0.3, 0.3, 0.3, 0.05);
        }
    }
    
    /**
     * Homing behavior - re-acquire target every tick and steer toward them.
     * Based on reference mod's strong homing (30% steering per tick).
     */
    private void updateHoming() {
        Entity owner = this.getOwner();
        if (!(owner instanceof Player player)) return;
        
        // Re-target every tick if current target is invalid
        if (this.targetEntity == null || !this.targetEntity.isAlive() ||
            this.targetEntity.distanceTo(this) > HOMING_RANGE) {
            this.targetEntity = findBestTarget(player);
        }
        
        if (this.targetEntity != null) {
            // Steer toward target's eye position
            Vec3 toTarget = this.targetEntity.getEyePosition().subtract(this.position()).normalize();
            Vec3 currentDir = this.getDeltaMovement().normalize();
            
            // Strong homing - 30% correction toward target each tick
            Vec3 newDir = currentDir.scale(1.0 - HOMING_STRENGTH)
                .add(toTarget.scale(HOMING_STRENGTH))
                .normalize();
            
            this.setDeltaMovement(newDir.scale(SPEED));
        }
    }
    
    /**
     * Find the best target to home in on.
     * Prioritizes entities closest to the player's crosshair within 50 blocks.
     */
    private LivingEntity findBestTarget(Player player) {
        AABB searchBox = this.getBoundingBox().inflate(HOMING_RANGE);
        
        List<LivingEntity> nearbyEntities = this.level().getEntitiesOfClass(
            LivingEntity.class, searchBox,
            entity -> entity != player && entity.isAlive() && !entity.isSpectator()
        );
        
        if (nearbyEntities.isEmpty()) return null;
        
        // Score by crosshair alignment (dot product) with distance penalty
        Vec3 playerLook = player.getLookAngle();
        Vec3 playerPos = player.getEyePosition();
        
        LivingEntity bestTarget = null;
        double bestScore = -1;
        
        for (LivingEntity entity : nearbyEntities) {
            Vec3 toEntity = entity.position().subtract(playerPos).normalize();
            double dot = playerLook.dot(toEntity);
            double distance = entity.distanceTo(this);
            double score = dot - (distance / HOMING_RANGE) * 0.3;
            
            if (score > bestScore) {
                bestScore = score;
                bestTarget = entity;
            }
        }
        
        return bestTarget;
    }
    
    @Override
    protected boolean canHitEntity(Entity entity) {
        if (!super.canHitEntity(entity) || entity == this.getOwner()) return false;
        
        // Pass through non-target entities (like reference mod)
        // Only hit the specific locked target, or any entity if no target
        if (this.targetEntity != null && this.targetEntity.isAlive()) {
            return entity == this.targetEntity;
        }
        return entity instanceof LivingEntity;
    }
    
    @Override
    protected void onHit(HitResult hitResult) {
        super.onHit(hitResult);
        if (!this.level().isClientSide()) {
            if (hitResult.getType() == HitResult.Type.ENTITY) {
                this.onHitEntity((EntityHitResult) hitResult);
            } else if (hitResult.getType() == HitResult.Type.BLOCK) {
                this.onHitBlock((BlockHitResult) hitResult);
            }
        }
    }
    
    @Override
    protected void onHitEntity(EntityHitResult result) {
        Entity target = result.getEntity();
        if (target instanceof LivingEntity living && living != this.getOwner()) {
            // Direct hit damage
            living.hurt(this.damageSources().mobProjectile(this,
                this.getOwner() instanceof LivingEntity le ? le : null), DAMAGE);
            
            // Knockback
            Vec3 knockback = this.getDeltaMovement().normalize().scale(0.8);
            living.setDeltaMovement(living.getDeltaMovement().add(knockback));
        }
        
        // AOE explosion on impact
        explodeInWater();
        this.discard();
    }
    
    @Override
    protected void onHitBlock(BlockHitResult result) {
        explodeInWater();
        this.discard();
    }
    
    /**
     * Create a water explosion on impact:
     * - AOE damage to nearby entities (falls off with distance)
     * - Visual explosion effect
     * - Temporary water blocks (removed after 1 second)
     * - Water splash particles
     */
    private void explodeInWater() {
        if (!(this.level() instanceof ServerLevel serverLevel)) return;
        
        // --- AOE Damage ---
        for (LivingEntity nearby : serverLevel.getEntitiesOfClass(LivingEntity.class,
                this.getBoundingBox().inflate(AOE_RADIUS))) {
            if (nearby != this.getOwner() && nearby.isAlive()) {
                double dist = nearby.distanceTo(this);
                if (dist <= AOE_RADIUS) {
                    // Damage falls off with distance
                    float dmg = AOE_DAMAGE * (float)(1.0 - dist / AOE_RADIUS);
                    nearby.hurt(this.damageSources().mobProjectile(this,
                        this.getOwner() instanceof LivingEntity le ? le : null), dmg);
                    
                    // Knockback away from center
                    Vec3 kb = nearby.position().subtract(this.position()).normalize().scale(0.5);
                    nearby.setDeltaMovement(nearby.getDeltaMovement().add(kb));
                }
            }
        }
        
        // --- Visual explosion (no block damage) ---
        serverLevel.explode(null, this.getX(), this.getY(), this.getZ(),
            EXPLOSION_POWER, Level.ExplosionInteraction.NONE);
        
        // --- Water splash particles ---
        serverLevel.sendParticles(ParticleTypes.SPLASH,
            this.getX(), this.getY(), this.getZ(), 50, 1.0, 1.0, 1.0, 0.5);
        serverLevel.sendParticles(ParticleTypes.FALLING_WATER,
            this.getX(), this.getY() + 1, this.getZ(), 30, 0.5, 0.5, 0.5, 0.2);
        
        // --- Temporary water blocks (removed after 1 second) ---
        placeTemporaryWater(serverLevel);
        
        // --- Impact sound ---
        serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(),
            SoundEvents.PLAYER_SPLASH_HIGH_SPEED, SoundSource.PLAYERS, 0.3F, 1.0F);
    }
    
    /**
     * Place temporary flowing water blocks at the impact site.
     * Water is automatically removed after ~1 second (20 ticks).
     */
    private void placeTemporaryWater(ServerLevel level) {
        BlockPos center = BlockPos.containing(this.position());
        List<BlockPos> waterPositions = new ArrayList<>();
        
        // Fill a small area with water (2 block radius at ground level)
        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-2, 0, -2), center.offset(2, 0, 2))) {
            if (level.getBlockState(pos).isAir()) {
                level.setBlockAndUpdate(pos, Blocks.WATER.defaultBlockState());
                waterPositions.add(pos.immutable());
            }
        }
        
        // Schedule removal after 20 ticks (1 second)
        if (!waterPositions.isEmpty() && level.getServer() != null) {
            int removeAt = level.getServer().getTickCount() + 20;
            level.getServer().tell(new TickTask(removeAt, () -> {
                for (BlockPos pos : waterPositions) {
                    if (level.getBlockState(pos).is(Blocks.WATER)) {
                        level.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                    }
                }
            }));
        }
    }
    
    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.age = compound.getInt("Age");
    }
    
    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt("Age", this.age);
    }
    
    // === GeckoLib Animation ===
    
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "swim_controller", 0, state -> {
            return state.setAndContinue(RawAnimation.begin().thenLoop("swim"));
        }));
    }
    
    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
