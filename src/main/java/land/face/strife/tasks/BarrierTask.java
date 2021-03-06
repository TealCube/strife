package land.face.strife.tasks;

import java.lang.ref.WeakReference;
import land.face.strife.StrifePlugin;
import land.face.strife.data.StrifeMob;
import land.face.strife.managers.StatUpdateManager;
import land.face.strife.stats.StrifeStat;
import land.face.strife.stats.StrifeTrait;
import land.face.strife.util.StatUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.LivingEntity;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class BarrierTask extends BukkitRunnable {

  private static final long TICK_RATE = 3L;
  private static final int DELAY_TICKS = (int) ((float) 120 / TICK_RATE);
  private static final BlockData BLOCK_DATA = Bukkit.getServer()
      .createBlockData(Material.WHITE_STAINED_GLASS);

  private final WeakReference<StrifeMob> parentMob;
  private int delayTicks = 0;
  private float barrierScale = 20f;

  public BarrierTask(StrifeMob parentMob) {
    this.parentMob = new WeakReference<>(parentMob);
    this.runTaskTimer(StrifePlugin.getInstance(), 20L, TICK_RATE);
    if (parentMob.getChampion() != null) {
      updateBarrierScale();
    }
  }

  @Override
  public void run() {
    StrifeMob mob = parentMob.get();
    if (mob == null || mob.getEntity() == null || !mob.getEntity().isValid()) {
      cancel();
      return;
    }
    forceAbsorbHearts();
    if (delayTicks > 0) {
      delayTicks--;
      return;
    }
    if (mob.hasTrait(StrifeTrait.NO_BARRIER_ALLOWED) || mob.getStat(StrifeStat.BARRIER) < 0.1) {
      delayTicks = 10;
      return;
    }
    if (mob.getBarrier() >= StatUtil.getMaximumBarrier(mob)) {
      delayTicks = 10;
      return;
    }
    float barrierGain = TICK_RATE * StatUtil.getBarrierPerSecond(mob) / 20;
    mob.restoreBarrier(barrierGain);
  }

  public void updateBarrierScale() {
    barrierScale = StatUpdateManager.getBarrierScale(parentMob.get().getChampion().getSaveData()
            .getHealthDisplayType(), (float) parentMob.get().getEntity().getMaxHealth(), parentMob.get().getMaxBarrier());
  }

  public void bumpBarrierTime() {
    delayTicks = DELAY_TICKS;
  }

  public static void spawnBarrierParticles(LivingEntity entity, float amount) {
    int particleAmount = (int) Math.ceil(amount / 5);
    entity.getWorld().spawnParticle(
        Particle.BLOCK_CRACK,
        entity.getLocation().clone().add(0, entity.getEyeHeight() / 2, 0),
        particleAmount,
        0.0, 0.0, 0.0,
        0.85,
        BLOCK_DATA
    );
  }

  public void forceAbsorbHearts() {
    StrifeMob mob = parentMob.get();
    if (mob.getChampion() == null) {
      return;
    }
    if (mob.getBarrier() > 0) {
      mob.getEntity().addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION,
          99999, 50, true, false));
      float percent = Math.min(1, mob.getBarrier() / mob.getMaxBarrier());
      mob.getEntity().setAbsorptionAmount(percent * barrierScale);
    } else {
      mob.getEntity().setAbsorptionAmount(0);
      mob.getEntity().removePotionEffect(PotionEffectType.ABSORPTION);
    }
  }

}
