package net.narutoxboruto.particles;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.particles.SimpleParticleType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class LightningSparksParticle extends TextureSheetParticle {
    
    private final double initialXd;
    private final double initialYd;
    private final double initialZd;
    
    protected LightningSparksParticle(ClientLevel level, double x, double y, double z, 
                                       double xSpeed, double ySpeed, double zSpeed) {
        super(level, x, y, z, xSpeed, ySpeed, zSpeed);
        
        // Store initial velocities for arc-like movement
        this.initialXd = xSpeed;
        this.initialYd = ySpeed;
        this.initialZd = zSpeed;
        
        this.xd = xSpeed;
        this.yd = ySpeed;
        this.zd = zSpeed;
        
        this.lifetime = 3 + this.random.nextInt(4); // Very short - 0.15-0.35 seconds for connected bolt look
        this.quadSize = 0.06F + this.random.nextFloat() * 0.04F; // Small particles that form a line
        this.gravity = 0.0F;
        this.hasPhysics = false; // Pass through blocks
        
        // Bright electric color tint - more blue/white
        this.rCol = 0.7F + this.random.nextFloat() * 0.3F;
        this.gCol = 0.85F + this.random.nextFloat() * 0.15F;
        this.bCol = 1.0F;
        this.alpha = 1.0F;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        
        if (this.age++ >= this.lifetime) {
            this.remove();
            return;
        }
        
        // Quick fade out in the last third of life
        float lifeRatio = (float) this.age / (float) this.lifetime;
        if (lifeRatio > 0.6F) {
            this.alpha = 1.0F - ((lifeRatio - 0.6F) / 0.4F);
        }
        
        // Electric jitter - sharp, erratic movement
        double jitterStrength = 0.08;
        this.xd = this.initialXd * 0.9 + (Math.random() - 0.5) * jitterStrength;
        this.yd = this.initialYd * 0.9 + (Math.random() - 0.5) * jitterStrength;
        this.zd = this.initialZd * 0.9 + (Math.random() - 0.5) * jitterStrength;
        
        // Occasional "snap" - sudden direction change like electricity
        if (Math.random() < 0.15) {
            this.xd += (Math.random() - 0.5) * 0.2;
            this.yd += (Math.random() - 0.5) * 0.15;
            this.zd += (Math.random() - 0.5) * 0.2;
        }
        
        this.move(this.xd, this.yd, this.zd);
        
        // Slight size fluctuation for flickering effect
        this.quadSize *= 0.95F + (float)(Math.random() * 0.1);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }
    
    @Override
    public int getLightColor(float partialTick) {
        // Make sparks emit light (fullbright)
        return 0xF000F0;
    }

    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, 
                                       double x, double y, double z, 
                                       double xSpeed, double ySpeed, double zSpeed) {
            LightningSparksParticle particle = new LightningSparksParticle(level, x, y, z, xSpeed, ySpeed, zSpeed);
            particle.pickSprite(this.sprites);
            return particle;
        }
    }
}
