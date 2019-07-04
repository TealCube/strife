/**
 * The MIT License Copyright (c) 2015 Teal Cube Games
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package info.faceland.strife.listeners.combat;

import static org.bukkit.event.block.Action.LEFT_CLICK_AIR;
import static org.bukkit.event.block.Action.LEFT_CLICK_BLOCK;

import com.tealcube.minecraft.bukkit.facecore.utilities.MessageUtils;
import info.faceland.strife.StrifePlugin;
import info.faceland.strife.data.StrifeMob;
import info.faceland.strife.stats.StrifeStat;
import info.faceland.strife.stats.StrifeTrait;
import info.faceland.strife.util.ItemUtil;
import info.faceland.strife.util.ProjectileUtil;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Ghast;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

public class SwingListener implements Listener {

  private final StrifePlugin plugin;
  private final Random random;
  private final Set<Material> ignoredMaterials;
  private final String notChargedMessage;

  public SwingListener(StrifePlugin plugin) {
    this.plugin = plugin;
    random = new Random(System.currentTimeMillis());
    ignoredMaterials = new HashSet<>();
    ignoredMaterials.add(Material.AIR);
    ignoredMaterials.add(Material.TALL_GRASS);
    notChargedMessage = plugin.getSettings().getString("language.wand.not-charged", "");
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onGhastBallHit(EntityDamageByEntityEvent event) {
    if (!(event.getEntity() instanceof Fireball)) {
      return;
    }
    Fireball fireball = (Fireball) event.getEntity();
    if (fireball.getShooter() instanceof Ghast) {
      return;
    }
    if (event.getDamager() instanceof Projectile) {
      event.getDamager().remove();
    }
    event.setCancelled(true);
  }

  @EventHandler(priority = EventPriority.NORMAL)
  public void onSwing(PlayerInteractEvent event) {
    if (event.getHand() != EquipmentSlot.HAND) {
      return;
    }
    if (!(event.getAction() == LEFT_CLICK_AIR || event.getAction() == LEFT_CLICK_BLOCK)) {
      return;
    }
    if (ItemUtil.isWand(event.getPlayer().getEquipment().getItemInMainHand())) {
      shootWand(event.getPlayer(), event);
    } else {
      doMeleeSwing(event.getPlayer(), event, true);
    }
    plugin.getAttributeUpdateManager().updateAttackSpeed(
        plugin.getStrifeMobManager().getAttributedEntity(event.getPlayer()));
  }

  @EventHandler(priority = EventPriority.NORMAL)
  public void onEnemyHit(EntityDamageByEntityEvent event) {
    if (!(event.getDamager() instanceof Player)) {
      return;
    }
    if (ItemUtil.isWand(((Player) event.getDamager()).getEquipment().getItemInMainHand())) {
      shootWand((Player) event.getDamager(), event);
      event.setCancelled(true);
    } else {
      doMeleeSwing((Player) event.getDamager(), event, false);
    }
  }

  private void doMeleeSwing(Player player, Cancellable event, boolean resetAttack) {
    StrifeMob attacker = plugin.getStrifeMobManager().getAttributedEntity(player);

    double attackMultiplier = plugin.getAttackSpeedManager()
        .getAttackMultiplier(attacker, resetAttack);

    double range = attacker.getAttribute(StrifeStat.SPELL_STRIKE_RANGE);
    if (attacker.getAttribute(StrifeStat.SPELL_STRIKE_RANGE) < 0.5) {
      return;
    }
    if (attackMultiplier < 0.95) {
      return;
    }
    LivingEntity target = selectFirstEntityInSight(player, range);
    if (target == null) {
      return;
    }
    spawnSparkle(target);
    event.setCancelled(true);
  }

  private void shootWand(Player player, Cancellable event) {
    StrifeMob pStats = plugin.getStrifeMobManager().getAttributedEntity(player);
    double attackMultiplier = plugin.getAttackSpeedManager().getAttackMultiplier(pStats);
    attackMultiplier = Math.pow(attackMultiplier, 1.5D);

    if (attackMultiplier < 0.1) {
      MessageUtils.sendActionBar(player, notChargedMessage);
      player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 0.5f, 2.0f);
      event.setCancelled(true);
      return;
    }

    plugin.getChampionManager().updateEquipmentAttributes(
        plugin.getChampionManager().getChampion(player));

    double projectileSpeed = 1 + (pStats.getAttribute(StrifeStat.PROJECTILE_SPEED) / 100);
    double multiShot = pStats.getAttribute(StrifeStat.MULTISHOT) / 100;
    boolean gravity = !pStats.hasTrait(StrifeTrait.NO_GRAVITY_PROJECTILES);
    event.setCancelled(true);

    if (pStats.hasTrait(StrifeTrait.EXPLOSIVE_PROJECTILES)) {
      ProjectileUtil.createGhastBall(player, attackMultiplier, projectileSpeed, multiShot);
      return;
    }

    ProjectileUtil.createMagicMissile(player, attackMultiplier, projectileSpeed, 0, 0, 0, gravity);

    if (multiShot > 0) {
      int bonusProjectiles = (int) (multiShot - (multiShot % 1));
      if (multiShot % 1 >= random.nextDouble()) {
        bonusProjectiles++;
      }
      for (int i = bonusProjectiles; i > 0; i--) {
        ProjectileUtil.createMagicMissile(player, attackMultiplier, projectileSpeed,
            randomOffset(bonusProjectiles), randomOffset(bonusProjectiles),
            randomOffset(bonusProjectiles), gravity);
      }
    }
    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_HURT, 0.7f, 2f);
    plugin.getSneakManager().tempDisableSneak(player);
  }

  private double randomOffset(double magnitude) {
    magnitude = 0.12 + magnitude * 0.005;
    return (random.nextDouble() * magnitude * 2) - magnitude;
  }

  private LivingEntity selectFirstEntityInSight(Player player, double range) {
    ArrayList<Entity> entities = (ArrayList<Entity>) player.getNearbyEntities(range, range, range);
    ArrayList<Block> sightBlock = (ArrayList<Block>) player
        .getLineOfSight(ignoredMaterials, (int) range);
    ArrayList<Location> sight = new ArrayList<>();
    for (Block b : sightBlock) {
      sight.add(b.getLocation());
    }
    for (Location loc : sight) {
      for (Entity entity : entities) {
        if (!(entity instanceof LivingEntity)) {
          continue;
        }
        if (Math.abs(entity.getLocation().getX() - loc.getX()) < 1 &&
            Math.abs(entity.getLocation().getY() - loc.getY()) < 1 &&
            Math.abs(entity.getLocation().getZ() - loc.getZ()) < 1) {
          return (LivingEntity) entity;
        }
      }
    }
    return null;
  }

  private void spawnSparkle(LivingEntity target) {
    Location location = target.getLocation().clone().add(
        target.getEyeLocation().clone().subtract(target.getLocation()).multiply(0.5));
    location.getWorld().spawnParticle(Particle.CRIT_MAGIC, location, 20, 5, 5, 5);
    location.getWorld().spawnParticle(Particle.SWEEP_ATTACK, location, 1);
    location.getWorld().playSound(location, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 2.0f, 1.0f);
    location.getWorld().playSound(location, Sound.ENTITY_BLAZE_HURT, 2.0f, 1.0f);
  }
}
