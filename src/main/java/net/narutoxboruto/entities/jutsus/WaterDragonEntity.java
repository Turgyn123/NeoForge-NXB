package net.narutoxboruto.entities.jutsus;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;
import net.narutoxboruto.entities.ModEntities;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Water Dragon Projectile - Powerful Water Release Jutsu
 * 
 * Three distinct phases, each with its own animation:
 * 
 * - Phase 1 (Rising): Spawns below ground, rises vertically.
 *   "Spawn" animation unfurls the dragon as it emerges.
 * - Phase 2 (Idle): Hovers in place, rotates yaw toward target.
 *   "Idle" animation loops with gentle swaying.
 * - Phase 3 (Attack): "Attack" animation straightens the dragon from vertical
 *   to horizontal via per-bone rotations (no global pitch distortion).
 *   After the animation completes, the dragon launches toward the target.
 * - Impact: Massive AOE explosion + water damage + temporary water blocks.
 */
public class WaterDragonEntity extends Projectile implements GeoEntity {
    
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    
    private int age = 0;
    private boolean launched = false;
    private Vec3 launchDir = null;     // Direction to launch, calculated during idle/attack
    private Vec3 startPos;             // Set when launched, for range check
    private LivingEntity lockedTarget = null;  // Target locked on when entering idle phase
    
    // === Configuration ===
    private static final float DAMAGE = 20.0F;          // Direct hit damage
    private static final float AOE_DAMAGE = 10.0F;      // AOE splash damage
    private static final float SPEED = 0.5F;             // Flight speed
    // Phase durations match bbmodel animation lengths exactly:
    // Spawn = 1s (hold_on_last_frame), Idle = 2s (loop), Attack = 1s (hold_on_last_frame)
    private static final int RISE_TICKS = 20;            // 1s — matches Spawn animation length
    private static final int IDLE_TICKS = 40;            // 2s — matches one full Idle loop
    private static final int ATTACK_ANIM_TICKS = 20;     // 1s — matches Attack animation length
    private static final int PHASE2_END = RISE_TICKS + IDLE_TICKS;           // tick 60
    private static final int PHASE3_END = PHASE2_END + ATTACK_ANIM_TICKS;    // tick 80
    private static final int MAX_FLIGHT_TICKS = 60;      // ~3s flight after launch
    private static final int MAX_TOTAL_TICKS = 160;      // ~8s total lifetime
    private static final double MAX_RANGE = 50.0;        // Max travel distance after launch
    private static final float AOE_RADIUS = 4.0F;        // Large splash radius
    private static final float EXPLOSION_POWER = 2.0F;   // Visual explosion (no block damage)
    private static final int LAUNCH_GRACE_TICKS = 5;     // Ticks after launch to ignore block collisions
    private static final int FLIGHT_SCAN_INTERVAL = 5;   // Re-scan for targets every N flight ticks
    private static final double TARGET_SCAN_RANGE = 30.0; // Range for in-flight target scanning
    private static final double TARGET_SCAN_CONE = 0.5;  // Dot product threshold for flight cone (cos ~60°)
    
    public WaterDragonEntity(EntityType<? extends WaterDragonEntity> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true; // No collision during windup
        this.noCulling = true; // Prevent frustum culling — scaled model extends far beyond the small entity hitbox
    }
    
    public WaterDragonEntity(Level level, LivingEntity shooter) {
        this(ModEntities.WATER_DRAGON.get(), level);
        this.setOwner(shooter);
        
        // Spawn at the shooter's position.
        // The Spawn animation handles the visual rise from underground
        // (bone11 starts at y=-37px and animates to rest position over 1s).
        this.setPos(shooter.getX(), shooter.getY(), shooter.getZ());
        
        // Face the same direction as the shooter initially
        this.setYRot(shooter.getYHeadRot());
        this.setXRot(0);
        this.yRotO = this.getYRot();
        this.xRotO = this.getXRot();
        
        // No movement yet
        this.setDeltaMovement(Vec3.ZERO);
    }
    
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        // No additional synced data needed
    }
    
    @Override
    public void tick() {
        super.tick();
        this.age++;
        
        Entity owner = this.getOwner();
        
        // If owner dies/disconnects, discard
        if (owner == null || !owner.isAlive()) {
            if (this.launched) explodeWithWater();
            this.discard();
            return;
        }
        
        // Max lifetime check
        if (this.age > MAX_TOTAL_TICKS) {
            this.explodeWithWater();
            this.discard();
            return;
        }
        
        // ==================== PHASE 1: RISING ====================
        // Entity stays at spawn position. The Spawn animation handles the
        // visual rise (bone11 starts underground and animates to rest pose).
        if (this.age <= RISE_TICKS) {
            return;
        }
        
        // ==================== PHASE 2: IDLE / HOVERING ====================
        // Lock onto nearest enemy when entering idle, then track it.
        // Idle animation loops with gentle swaying.
        if (this.age <= PHASE2_END) {
            // Lock onto target on first idle tick
            if (this.age == RISE_TICKS + 1) {
                lockOnTarget(owner);
            }
            
            // Track locked target (or fall back to crosshair if none found)
            updateLaunchDirFromTarget(owner);
            
            // Smoothly rotate yaw toward target direction
            if (this.launchDir != null) {
                float targetYaw = -(float)(Mth.atan2(this.launchDir.x, this.launchDir.z) * (180.0 / Math.PI));
                float yawDiff = Mth.wrapDegrees(targetYaw - this.getYRot());
                float maxYawStep = 360.0f / (float) IDLE_TICKS;
                this.setYRot(this.getYRot() + Mth.clamp(yawDiff, -maxYawStep, maxYawStep));
            }
            return;
        }
        
        // ==================== PHASE 3: ATTACK ====================
        // Attack animation straightens dragon from vertical to horizontal
        // via per-bone rotations. Entity stays in place.
        if (this.age <= PHASE3_END) {
            // Continue tracking locked target
            updateLaunchDirFromTarget(owner);
            if (this.launchDir != null) {
                float targetYaw = -(float)(Mth.atan2(this.launchDir.x, this.launchDir.z) * (180.0 / Math.PI));
                float yawDiff = Mth.wrapDegrees(targetYaw - this.getYRot());
                float maxYawStep = 360.0f / (float) ATTACK_ANIM_TICKS;
                this.setYRot(this.getYRot() + Mth.clamp(yawDiff, -maxYawStep, maxYawStep));
            }
            return;
        }
        
        // ==================== FLIGHT (after attack animation) ====================
        if (!this.launched) {
            this.launched = true;
            this.startPos = this.position();
            
            // Final direction update toward locked target
            updateLaunchDirFromTarget(owner);
            
            if (this.launchDir != null) {
                this.setDeltaMovement(this.launchDir.scale(SPEED));
            } else if (owner instanceof LivingEntity shooter) {
                this.setDeltaMovement(shooter.getLookAngle().scale(SPEED));
            }
        }
        
        int flightAge = this.age - PHASE3_END;
        
        // Enable block collision after grace period (avoids clipping ground at spawn)
        if (this.noPhysics && flightAge > LAUNCH_GRACE_TICKS) {
            this.noPhysics = false;
        }
        
        // Check max flight time / range
        if (flightAge > MAX_FLIGHT_TICKS ||
            (this.startPos != null && this.startPos.distanceTo(this.position()) > MAX_RANGE)) {
            this.explodeWithWater();
            this.discard();
            return;
        }
        
        // Hit detection (entity hits always, block hits only after grace period)
        HitResult hitResult = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        if (hitResult.getType() == HitResult.Type.ENTITY) {
            this.onHit(hitResult);
            return;
        } else if (hitResult.getType() == HitResult.Type.BLOCK && !this.noPhysics) {
            this.onHit(hitResult);
            return;
        }
        
        // Update position
        Vec3 velocity = this.getDeltaMovement();
        this.setPos(this.getX() + velocity.x, this.getY() + velocity.y, this.getZ() + velocity.z);
        
        // Periodically scan for new targets during flight if none locked (or current died)
        if ((this.lockedTarget == null || !this.lockedTarget.isAlive()) 
                && flightAge % FLIGHT_SCAN_INTERVAL == 0) {
            scanForFlightTarget();
        }
        
        // Home toward locked target during flight
        if (this.lockedTarget != null && this.lockedTarget.isAlive()) {
            Vec3 toTarget = this.lockedTarget.getEyePosition().subtract(this.position()).normalize();
            Vec3 currentDir = velocity.normalize();
            // Blend 15% toward target each tick for responsive homing
            Vec3 blended = currentDir.scale(0.85).add(toTarget.scale(0.15)).normalize();
            this.setDeltaMovement(blended.scale(SPEED));
        }
        // No target: dragon keeps flying in its current direction (launchDir)
        
        // Update yaw and pitch to face movement direction
        Vec3 vel = this.getDeltaMovement();
        double horizSpeed = vel.horizontalDistance();
        if (horizSpeed > 0.001) {
            this.setYRot(-(float)(Mth.atan2(vel.x, vel.z) * (180.0 / Math.PI)));
            this.setXRot((float)(-(Mth.atan2(vel.y, horizSpeed) * (180.0 / Math.PI))));
        }
    }
    
    /**
     * Lock onto the best enemy near the player's crosshair direction.
     * Scores by both alignment to look direction AND distance (closer + more aligned = better).
     * Called once when entering the idle phase.
     */
    private void lockOnTarget(Entity owner) {
        if (!(owner instanceof Player player)) return;
        
        List<LivingEntity> nearby = this.level().getEntitiesOfClass(
            LivingEntity.class,
            this.getBoundingBox().inflate(50.0),
            e -> e != player && e.isAlive() && !e.isSpectator() && !(e instanceof Player)
        );
        
        if (nearby.isEmpty()) return;
        
        Vec3 look = player.getLookAngle();
        Vec3 eye = player.getEyePosition();
        LivingEntity best = null;
        double bestScore = -1;
        
        for (LivingEntity e : nearby) {
            Vec3 toE = e.position().subtract(eye);
            double dist = toE.length();
            if (dist < 0.5) continue; // Too close, skip
            double dot = look.dot(toE.normalize());
            if (dot < 0.3) continue; // Outside ~72° cone, skip
            // Score: alignment weighted heavily, with distance penalty
            // Closer targets get a bonus (1/dist capped), aligned targets score higher
            double score = dot * (1.0 + 10.0 / (dist + 5.0));
            if (score > bestScore) {
                bestScore = score;
                best = e;
            }
        }
        
        if (best != null) {
            this.lockedTarget = best;
        }
    }
    
    /**
     * Scan for targets during flight. Looks for enemies in a cone ahead of the dragon.
     * If a target is found, locks onto it for homing.
     */
    private void scanForFlightTarget() {
        Entity owner = this.getOwner();
        Vec3 flyDir = this.getDeltaMovement().normalize();
        
        List<LivingEntity> nearby = this.level().getEntitiesOfClass(
            LivingEntity.class,
            this.getBoundingBox().inflate(TARGET_SCAN_RANGE),
            e -> e != owner && e.isAlive() && !e.isSpectator()
                && !(e instanceof Player) && e.distanceTo(this) <= TARGET_SCAN_RANGE
        );
        
        if (nearby.isEmpty()) return;
        
        LivingEntity best = null;
        double bestScore = -1;
        
        for (LivingEntity e : nearby) {
            Vec3 toE = e.getEyePosition().subtract(this.position());
            double dist = toE.length();
            if (dist < 0.5) continue;
            double dot = flyDir.dot(toE.normalize());
            if (dot < TARGET_SCAN_CONE) continue; // Outside forward cone
            // Prefer closer targets that are more aligned with flight path
            double score = dot * (1.0 + 10.0 / (dist + 3.0));
            if (score > bestScore) {
                bestScore = score;
                best = e;
            }
        }
        
        if (best != null) {
            this.lockedTarget = best;
        }
    }
    
    /**
     * Update launchDir toward the locked target. If no locked target
     * (or it died), fall back to the owner's look direction.
     */
    private void updateLaunchDirFromTarget(Entity owner) {
        if (this.lockedTarget != null && this.lockedTarget.isAlive()) {
            this.launchDir = this.lockedTarget.getEyePosition().subtract(this.position()).normalize();
        } else if (owner instanceof LivingEntity shooter) {
            // Fallback: aim where the player is looking
            Vec3 targetPos = shooter.getEyePosition().add(shooter.getLookAngle().scale(50.0));
            this.launchDir = targetPos.subtract(this.position()).normalize();
        }
    }
    
    @Override
    protected boolean canHitEntity(Entity entity) {
        return super.canHitEntity(entity) && entity != this.getOwner();
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
            // Devastating direct hit damage
            living.hurt(this.damageSources().mobProjectile(this,
                this.getOwner() instanceof LivingEntity le ? le : null), DAMAGE);
            
            // Subtle knockback in movement direction
            Vec3 knockback = this.getDeltaMovement().normalize().scale(0.35);
            living.setDeltaMovement(living.getDeltaMovement().add(knockback.x, 0.15, knockback.z));
            living.hurtMarked = true;
        }
        
        // Massive explosion on impact
        explodeWithWater();
        this.discard();
    }
    
    @Override
    protected void onHitBlock(BlockHitResult result) {
        explodeWithWater();
        this.discard();
    }
    
    /**
     * Create a massive water explosion on impact:
     * - Large AOE damage to all nearby entities
     * - Dramatic explosion visual (power 5)
     * - Temporary water blocks (removed after 1 second)
     * - Massive water particle effects
     */
    private void explodeWithWater() {
        if (!(this.level() instanceof ServerLevel serverLevel)) return;
        
        // --- AOE Damage (hits ANY entity, not target-specific) ---
        for (LivingEntity nearby : serverLevel.getEntitiesOfClass(LivingEntity.class,
                this.getBoundingBox().inflate(AOE_RADIUS))) {
            if (nearby != this.getOwner() && nearby.isAlive()) {
                double dist = nearby.distanceTo(this);
                if (dist <= AOE_RADIUS) {
                    float dmg = AOE_DAMAGE * (float)(1.0 - dist / AOE_RADIUS);
                    nearby.hurt(this.damageSources().mobProjectile(this,
                        this.getOwner() instanceof LivingEntity le ? le : null), dmg);
                    
                    // Subtle knockback away from explosion with distance falloff
                    double falloff = 1.0 - dist / AOE_RADIUS;
                    Vec3 kb = nearby.position().subtract(this.position()).normalize().scale(0.3 * falloff);
                    nearby.setDeltaMovement(nearby.getDeltaMovement().add(kb.x, 0.1 * falloff, kb.z));
                    nearby.hurtMarked = true;
                }
            }
        }
        
        // --- Massive visual explosion (no block damage) ---
        serverLevel.explode(null, this.getX(), this.getY(), this.getZ(),
            EXPLOSION_POWER, Level.ExplosionInteraction.NONE);
        
        // --- Water particles ---
        serverLevel.sendParticles(ParticleTypes.SPLASH,
            this.getX(), this.getY(), this.getZ(), 100, 2.0, 2.0, 2.0, 0.5);
        serverLevel.sendParticles(ParticleTypes.FALLING_WATER,
            this.getX(), this.getY() + 2, this.getZ(), 60, 2.0, 0.5, 2.0, 0.3);
        serverLevel.sendParticles(ParticleTypes.RAIN,
            this.getX(), this.getY() + 3, this.getZ(), 40, 2.5, 0.5, 2.5, 0.3);
        
        // --- Temporary water blocks ---
        placeTemporaryWater(serverLevel);
        
        // --- Impact sound ---
        serverLevel.playSound(null, this.getX(), this.getY(), this.getZ(),
            SoundEvents.GENERIC_SPLASH, SoundSource.PLAYERS, 2.0F, 0.6F);
    }
    
    /**
     * Place temporary water blocks at the impact site.
     * Covers a larger area than shark bomb. Removed after ~1 second.
     */
    private void placeTemporaryWater(ServerLevel level) {
        BlockPos center = BlockPos.containing(this.position());
        List<BlockPos> waterPositions = new ArrayList<>();
        
        // Fill a larger area with water (3 block radius)
        for (BlockPos pos : BlockPos.betweenClosed(
                center.offset(-3, 0, -3), center.offset(3, 0, 3))) {
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
        this.launched = compound.getBoolean("Launched");
    }
    
    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt("Age", this.age);
        compound.putBoolean("Launched", this.launched);
    }
    
    /**
     * Allow rendering at extended distance since the 5x scaled model is visible
     * from much further than the small entity hitbox would normally allow.
     */
    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 16384.0; // Render up to 128 blocks away
    }
    
    // === GeckoLib Animation ===
    
    private static final RawAnimation SPAWN_ANIM = RawAnimation.begin().thenPlayAndHold("Spawn");
    private static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("Idle");
    private static final RawAnimation ATTACK_ANIM = RawAnimation.begin().thenPlayAndHold("Attack");
    
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main_controller", 5, state -> {
            // Phase 1 (Rise): Spawn animation unfurls dragon from coiled state.
            // Phase 2 (Idle): Idle animation — gentle swaying while hovering.
            // Phase 3 (Attack): Attack animation — dragon straightens from vertical
            //   to horizontal via per-bone rotations. Holds on last frame for flight.
            if (this.age <= RISE_TICKS) {
                return state.setAndContinue(SPAWN_ANIM);
            } else if (this.age <= PHASE2_END) {
                return state.setAndContinue(IDLE_ANIM);
            }
            return state.setAndContinue(ATTACK_ANIM);
        }));
    }
    
    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
