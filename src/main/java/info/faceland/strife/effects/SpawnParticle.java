package info.faceland.strife.effects;

import info.faceland.strife.data.StrifeMob;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

public class SpawnParticle extends Effect {

  private Particle particle;
  private int quantity;
  private float spread;
  private float speed;
  private double size;
  private ParticleStyle style;
  private ParticleOriginLocation particleOriginLocation = ParticleOriginLocation.CENTER;

  @Override
  public void apply(StrifeMob caster, StrifeMob target) {
    playAtLocation(getLoc(target.getEntity()), caster.getEntity().getEyeLocation().getDirection());
  }

  public void playAtLocation(Location location, Vector direction) {
    switch (style) {
      case CIRCLE:
        spawnParticleCircle(location, size);
        return;
      case LINE:
        spawnParticleLine(location, direction, size);
      case NORMAL:
      default:
        location.getWorld().spawnParticle(particle, location, quantity, spread, spread, spread, speed);
    }
  }

  public void setParticle(Particle particle) {
    this.particle = particle;
  }

  public void setQuantity(int quantity) {
    this.quantity = quantity;
  }

  public void setSpeed(float speed) {
    this.speed = speed;
  }

  public void setSpread(float spread) {
    this.spread = spread;
  }

  public void setParticleOriginLocation(ParticleOriginLocation particleOriginLocation) {
    this.particleOriginLocation = particleOriginLocation;
  }

  public void setStyle(ParticleStyle style) {
    this.style = style;
  }

  public double getSize() {
    return size;
  }

  public void setSize(double size) {
    this.size = size;
  }

  private Location getLoc(LivingEntity le) {
    switch (particleOriginLocation) {
      case HEAD:
        return le.getEyeLocation();
      case CENTER:
        return le.getEyeLocation().clone()
            .subtract(le.getEyeLocation().clone().subtract(le.getLocation()).multiply(0.5));
      case GROUND:
        return le.getLocation();
    }
    return null;
  }

  private void spawnParticleCircle(Location center, double radius) {
    for (double degree = 0; degree < 360; degree += 30/radius) {
      double radian1 = Math.toRadians(degree);
      Location loc = center.clone();
      loc.add(Math.cos(radian1) * radius, 0, Math.sin(radian1) * radius);
      loc.getWorld().spawnParticle(particle, loc, quantity, spread, spread, spread, speed);
    }
  }

  private void spawnParticleLine(Location center, Vector direction, double length) {
    Location loc = center.clone();
    for (double dist = 0; dist < length; dist += 0.3) {
      loc.add(direction.multiply(dist));
      loc.getWorld().spawnParticle(particle, loc, quantity, spread, spread, spread, speed);
    }
  }

  public enum ParticleOriginLocation {
    HEAD,
    CENTER,
    GROUND
  }

  public enum ParticleStyle {
    NORMAL,
    CIRCLE,
    LINE,
    PILLAR
  }
}
