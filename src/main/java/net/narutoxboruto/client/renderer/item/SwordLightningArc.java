package net.narutoxboruto.client.renderer.item;

import org.joml.Vector3f;

/**
 * Data class for a lightning arc with start/end points and expiration time.
 * Used by MixinItemRenderer to render sword lightning effects.
 */
public class SwordLightningArc {
    public final Vector3f start;
    public final Vector3f end;
    public final long expireTime;
    
    public SwordLightningArc(Vector3f start, Vector3f end, long expireTime) {
        this.start = start;
        this.end = end;
        this.expireTime = expireTime;
    }
}
