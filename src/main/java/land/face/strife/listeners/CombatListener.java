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
package land.face.strife.listeners;

import static org.bukkit.event.entity.EntityDamageEvent.DamageModifier.ARMOR;
import static org.bukkit.event.entity.EntityDamageEvent.DamageModifier.BASE;
import static org.bukkit.event.entity.EntityDamageEvent.DamageModifier.BLOCKING;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import land.face.strife.StrifePlugin;
import land.face.strife.data.StrifeMob;
import land.face.strife.events.StrifeDamageEvent;
import land.face.strife.stats.StrifeStat;
import land.face.strife.util.DamageUtil;
import land.face.strife.util.DamageUtil.AttackType;
import land.face.strife.util.ItemUtil;
import land.face.strife.util.ProjectileUtil;
import land.face.strife.util.TargetingUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Bee;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Shulker;
import org.bukkit.entity.Slime;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.metadata.FixedMetadataValue;

public class CombatListener implements Listener {

  private final StrifePlugin plugin;
  private static Set<Player> FRIENDLY_PLAYER_CHECKER = new HashSet<>();
  private static HashMap<UUID, Long> SLIME_HIT_MAP = new HashMap<>();

  public CombatListener(StrifePlugin plugin) {
    this.plugin = plugin;
  }

  public static void addPlayer(Player player) {
    FRIENDLY_PLAYER_CHECKER.add(player);
  }

  public static void removePlayer(Player player) {
    FRIENDLY_PLAYER_CHECKER.remove(player);
  }

  public static boolean hasFriendlyPlayer(Player player) {
    return FRIENDLY_PLAYER_CHECKER.contains(player);
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void handleTNT(EntityDamageByEntityEvent event) {
    if (event.isCancelled()) {
      return;
    }
    if (event.getEntity() instanceof LivingEntity && event.getDamager() instanceof TNTPrimed) {
      double distance = event.getDamager().getLocation().distance(event.getEntity().getLocation());
      double multiplier = Math.max(0.3, 4 / (distance + 3));
      DamageUtil.removeDamageModifiers(event);
      event.setDamage(multiplier * (10 + ((LivingEntity) event.getEntity()).getMaxHealth() * 0.4));
    }
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void strifeDamageHandler(EntityDamageByEntityEvent event) {
    if (event.isCancelled() || event.getCause() == DamageCause.CUSTOM) {
      return;
    }
    if (plugin.getDamageManager().isHandledDamage(event.getDamager())) {
      DamageUtil.removeDamageModifiers(event);
      event.setDamage(BASE, plugin.getDamageManager().getHandledDamage(event.getDamager()));
      return;
    }
    if (!(event.getEntity() instanceof LivingEntity) || event.getEntity() instanceof ArmorStand) {
      return;
    }

    LivingEntity defendEntity = (LivingEntity) event.getEntity();
    LivingEntity attackEntity = DamageUtil.getAttacker(event.getDamager());

    if (attackEntity == null) {
      return;
    }
    if (attackEntity instanceof Slime && !canSlimeHit(attackEntity.getUniqueId())) {
      event.setCancelled(true);
      return;
    }

    boolean blocked = (event.isApplicable(BLOCKING) && event.getDamage(BLOCKING) != 0) || (
        defendEntity instanceof Shulker && event.isApplicable(ARMOR)
            && event.getDamage(ARMOR) != 0);
    DamageUtil.removeDamageModifiers(event);

    if (attackEntity instanceof Player && FRIENDLY_PLAYER_CHECKER.contains(attackEntity)) {
      FRIENDLY_PLAYER_CHECKER.remove(attackEntity);
      event.setCancelled(true);
      return;
    }

    if (event.getCause() == DamageCause.MAGIC) {
      event.setDamage(BASE, event.getDamage(BASE));
      return;
    }

    Projectile projectile = null;
    boolean isProjectile = false;
    boolean isMultishot = false;
    String[] extraEffects = null;

    if (event.getDamager() instanceof Projectile) {
      isProjectile = true;
      projectile = (Projectile) event.getDamager();
      if (defendEntity.hasMetadata("NPC")) {
        event.getDamager().remove();
        event.setCancelled(true);
        return;
      }
      if (projectile.hasMetadata("EFFECT_PROJECTILE")) {
        extraEffects = projectile.getMetadata("EFFECT_PROJECTILE").get(0).asString().split("~");
      }
      if (projectile.hasMetadata(ProjectileUtil.SHOT_ID_META)) {
        int shotId = projectile.getMetadata(ProjectileUtil.SHOT_ID_META).get(0).asInt();
        String idKey = ProjectileUtil.SHOT_ID_META + "_" + shotId;
        if (defendEntity.hasMetadata(idKey)) {
          isMultishot = true;
        } else {
          defendEntity.setMetadata(idKey, new FixedMetadataValue(StrifePlugin.getInstance(), true));
          Bukkit.getScheduler().runTaskLater(plugin, () -> defendEntity.removeMetadata(idKey, StrifePlugin.getInstance()), 1000L);
        }
      }
    }

    StrifeMob attacker = plugin.getStrifeMobManager().getStatMob(attackEntity);
    StrifeMob defender = plugin.getStrifeMobManager().getStatMob(defendEntity);

    if (TargetingUtil.isFriendly(attacker, defender)) {
      event.setCancelled(true);
      return;
    }

    float attackMultiplier = 1f;
    float healMultiplier = 1f;

    AttackType damageType = DamageUtil.getAttackType(event);

    if (isProjectile && projectile.hasMetadata(ProjectileUtil.ATTACK_SPEED_META)) {
      attackMultiplier = projectile.getMetadata(ProjectileUtil.ATTACK_SPEED_META).get(0).asFloat();
    }

    if (damageType == AttackType.MELEE) {
      attackMultiplier = plugin.getAttackSpeedManager().getAttackMultiplier(attacker);
      if (ItemUtil.isWandOrStaff(attackEntity.getEquipment().getItemInMainHand())) {
        ProjectileUtil.shootWand(attacker, attackMultiplier);
        event.setCancelled(true);
        return;
      }
      attackMultiplier = (float) Math.pow(attackMultiplier, 1.25);
    } else if (damageType == AttackType.EXPLOSION) {
      double distance = event.getDamager().getLocation().distance(event.getEntity().getLocation());
      attackMultiplier *= Math.max(0.3, 4 / (distance + 3));
      healMultiplier = 0.3f;
    }

    if (isMultishot) {
      attackMultiplier *= 0.25;
    }

    if (attackMultiplier < 0.10 && extraEffects == null) {
      event.setCancelled(true);
      if (event.getDamager() instanceof Projectile) {
        event.getDamager().remove();
      }
      return;
    }

    Bukkit.getScheduler().runTaskLater(plugin, () -> defendEntity.setNoDamageTicks(0), 0L);

    boolean isSneakAttack = isProjectile ?
        plugin.getSneakManager().isSneakAttack(projectile, defender.getEntity())
        : plugin.getSneakManager().isSneakAttack(attacker.getEntity(), defender.getEntity());

    StrifeDamageEvent strifeDamageEvent = new StrifeDamageEvent(attacker, defender, damageType);
    strifeDamageEvent.setSneakAttack(isSneakAttack);
    strifeDamageEvent.setExtraEffects(extraEffects);
    strifeDamageEvent.setHealMultiplier(healMultiplier);
    strifeDamageEvent.setAttackMultiplier(attackMultiplier);
    strifeDamageEvent.setBlocking(blocked);
    strifeDamageEvent.setConsumeEarthRunes(true);
    strifeDamageEvent.setProjectile(projectile);
    Bukkit.getPluginManager().callEvent(strifeDamageEvent);

    if (strifeDamageEvent.isCancelled()) {
      event.setCancelled(true);
      return;
    }

    putSlimeHit(attackEntity);

    if (attackEntity instanceof Bee) {
      plugin.getDamageManager().dealDamage(attacker, defender, strifeDamageEvent.getFinalDamage());
      event.setCancelled(true);
      return;
    }

    event.setDamage(BASE, strifeDamageEvent.getFinalDamage());
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void strifeEntityDeath(EntityDeathEvent event) {
    if (event.getEntity().getKiller() == null) {
      return;
    }
    StrifeMob killer = plugin.getStrifeMobManager().getStatMob(event.getEntity().getKiller());
    if (killer.getStat(StrifeStat.HP_ON_KILL) > 0.1) {
      DamageUtil.restoreHealthWithPenalties(event.getEntity().getKiller(), killer.getStat(
          StrifeStat.HP_ON_KILL));
    }
    if (killer.getStat(StrifeStat.RAGE_ON_KILL) > 0.1) {
      plugin.getRageManager().addRage(killer, killer.getStat(StrifeStat.RAGE_ON_KILL));
    }
  }

  private boolean canSlimeHit(UUID uuid) {
    return SLIME_HIT_MAP.getOrDefault(uuid, 0L) + 250 < System.currentTimeMillis();
  }

  public static void putSlimeHit(LivingEntity livingEntity) {
    if (livingEntity instanceof Slime) {
      SLIME_HIT_MAP.put(livingEntity.getUniqueId(), System.currentTimeMillis());
    }
  }
}
