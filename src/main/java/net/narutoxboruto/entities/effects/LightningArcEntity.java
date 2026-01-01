package net.narutoxboruto.entities.effects;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Random;

/**
 * Lightning Arc Entity - Renders as a branching lightning bolt between two points.
 * 
 * Based on the EntityLightningArc from the reference mod, adapted for NeoForge 1.21.1.
 * Used by Kiba sword and Lightning Chakra Mode for visual effects.
 */
public class LightningArcEntity extends Entity {
    
    private static final EntityDataAccessor<Float> END_X = SynchedEntityData.defineId(LightningArcEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> END_Y = SynchedEntityData.defineId(LightningArcEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> END_Z = SynchedEntityData.defineId(LightningArcEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> COLOR = SynchedEntityData.defineId(LightningArcEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> THICKNESS = SynchedEntityData.defineId(LightningArcEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> MAX_SEGMENTS = SynchedEntityData.defineId(LightningArcEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> LIFE = SynchedEntityData.defineId(LightningArcEntity.class, EntityDataSerializers.INT);
    
    private static final Random RANDOM = new Random();
    
    private Vec3 originalEndVec;
    private float inaccuracy;
    
    public LightningArcEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
        this.noCulling = true;
        this.noPhysics = true;
    }
    
    /**
     * Create a lightning arc from a center point with random endpoint.
     */
    public LightningArcEntity(EntityType<?> entityType, Level level, Vec3 center, double length, 
                               double xMotion, double yMotion, double zMotion, int color, int duration, float thickness) {
        this(entityType, level);
        
        this.setPos(center.x, center.y, center.z);
        
        // Random endpoint within length
        Vec3 endVec = center.add(
            (RANDOM.nextDouble() - 0.5) * length * 2.0,
            (RANDOM.nextDouble() - 0.5) * length * 2.0,
            (RANDOM.nextDouble() - 0.5) * length * 2.0
        );
        
        this.originalEndVec = endVec;
        this.setEndVec(endVec);
        this.setColor(color);
        this.setThickness(thickness);
        this.setLifeSpan(duration > 0 ? duration : RANDOM.nextInt(3) + 1);
        this.inaccuracy = 0.1f;
        
        this.setDeltaMovement(xMotion, yMotion, zMotion);
    }
    
    /**
     * Create a lightning arc between two specific points.
     */
    public LightningArcEntity(EntityType<?> entityType, Level level, Vec3 fromVec, Vec3 toVec, 
                               int color, int duration, float thickness, int segments) {
        this(entityType, level);
        
        this.setPos(fromVec.x, fromVec.y, fromVec.z);
        this.originalEndVec = toVec;
        this.setEndVec(toVec);
        this.setColor(color);
        this.setThickness(thickness);
        this.setMaxSegments(segments);
        this.setLifeSpan(duration > 0 ? duration : RANDOM.nextInt(3) + 1);
        this.inaccuracy = 0.05f;
    }
    
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(END_X, 0.0f);
        builder.define(END_Y, 0.0f);
        builder.define(END_Z, 0.0f);
        builder.define(COLOR, 0xC0FFFFFF); // Default: white with some transparency
        builder.define(THICKNESS, 0.02f);
        builder.define(MAX_SEGMENTS, 4);
        builder.define(LIFE, 3);
    }
    
    public Vec3 getEndVec() {
        return new Vec3(
            this.entityData.get(END_X),
            this.entityData.get(END_Y),
            this.entityData.get(END_Z)
        );
    }
    
    public void setEndVec(Vec3 vec) {
        this.entityData.set(END_X, (float) vec.x);
        this.entityData.set(END_Y, (float) vec.y);
        this.entityData.set(END_Z, (float) vec.z);
    }
    
    public int getColor() {
        return this.entityData.get(COLOR);
    }
    
    public void setColor(int color) {
        this.entityData.set(COLOR, color);
    }
    
    public float getThickness() {
        return this.entityData.get(THICKNESS);
    }
    
    public void setThickness(float thickness) {
        this.entityData.set(THICKNESS, thickness);
    }
    
    public int getMaxSegments() {
        return this.entityData.get(MAX_SEGMENTS);
    }
    
    public void setMaxSegments(int segments) {
        this.entityData.set(MAX_SEGMENTS, segments);
    }
    
    public int getLifeSpan() {
        return this.entityData.get(LIFE);
    }
    
    public void setLifeSpan(int life) {
        this.entityData.set(LIFE, life);
    }
    
    @Override
    public void tick() {
        super.tick();
        
        // Add slight jitter to the endpoint for visual effect
        if (this.inaccuracy > 0 && this.originalEndVec != null) {
            Vec3 jitteredEnd = this.originalEndVec.add(
                (RANDOM.nextFloat() - 0.5f) * this.inaccuracy * 2.0,
                RANDOM.nextFloat() * this.inaccuracy * 2.0,
                RANDOM.nextFloat() * this.inaccuracy * 2.0
            );
            this.setEndVec(jitteredEnd);
        }
        
        // Apply motion
        Vec3 motion = this.getDeltaMovement();
        if (motion.x != 0 || motion.y != 0 || motion.z != 0) {
            this.setPos(this.getX() + motion.x, this.getY() + motion.y, this.getZ() + motion.z);
            if (this.originalEndVec != null) {
                this.originalEndVec = this.originalEndVec.add(motion);
            }
        }
        
        // Decrement life and remove when expired
        if (!this.level().isClientSide()) {
            int life = this.getLifeSpan();
            life--;
            if (life <= 0) {
                this.discard();
            } else {
                this.setLifeSpan(life);
            }
        }
    }
    
    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 16384.0; // Render up to 128 blocks away
    }
    
    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        // Lightning arcs are not saved
    }
    
    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
        // Lightning arcs are not saved
    }
    
    /**
     * Spawn a lightning arc as a visual effect at a location.
     * 
     * @param level The level to spawn in
     * @param x Center X position
     * @param y Center Y position
     * @param z Center Z position
     * @param length Maximum length of the arc
     * @param color ARGB color (e.g., 0xC0FFFFFF for semi-transparent white)
     * @param duration Ticks to live
     */
    public static void spawnArc(Level level, double x, double y, double z, double length, int color, int duration) {
        if (!level.isClientSide()) {
            LightningArcEntity arc = new LightningArcEntity(
                net.narutoxboruto.entities.ModEntities.LIGHTNING_ARC.get(),
                level,
                new Vec3(x, y, z),
                length,
                0, 0, 0,
                color,
                duration,
                0.015f
            );
            level.addFreshEntity(arc);
        }
    }
    
    /**
     * Spawn a lightning arc between two points.
     */
    public static void spawnArcBetween(Level level, Vec3 from, Vec3 to, int color, int duration, float thickness) {
        if (!level.isClientSide()) {
            LightningArcEntity arc = new LightningArcEntity(
                net.narutoxboruto.entities.ModEntities.LIGHTNING_ARC.get(),
                level,
                from,
                to,
                color,
                duration,
                thickness,
                4
            );
            level.addFreshEntity(arc);
        }
    }
    
    /**
     * Spawn multiple lightning arcs around a living entity (for chakra mode effects).
     * Creates a visible aura of lightning around the entity.
     */
    public static void spawnArcsAroundEntity(Level level, LivingEntity entity, int count, int color, int duration) {
        if (level.isClientSide()) return;
        
        double radius = entity.getBbWidth() * 0.7;
        double height = entity.getBbHeight();
        
        for (int i = 0; i < count; i++) {
            double angle = RANDOM.nextDouble() * Math.PI * 2;
            double yOffset = RANDOM.nextDouble() * height;
            
            // Start point on the entity body
            double startX = entity.getX() + Math.cos(angle) * radius * 0.3;
            double startY = entity.getY() + yOffset;
            double startZ = entity.getZ() + Math.sin(angle) * radius * 0.3;
            
            // End point extending outward (larger radius for visible aura)
            double endRadius = radius * (1.8 + RANDOM.nextDouble() * 0.8);
            double endX = entity.getX() + Math.cos(angle) * endRadius;
            double endY = entity.getY() + yOffset + (RANDOM.nextDouble() - 0.5) * 0.6;
            double endZ = entity.getZ() + Math.sin(angle) * endRadius;
            
            // Thicker arcs for better visibility
            spawnArcBetween(level, 
                new Vec3(startX, startY, startZ), 
                new Vec3(endX, endY, endZ),
                color, 
                duration,
                0.02f + RANDOM.nextFloat() * 0.015f
            );
        }
    }
}
