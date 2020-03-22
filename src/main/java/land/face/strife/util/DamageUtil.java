package land.face.strife.util;

import static land.face.strife.util.StatUtil.getArmorMult;
import static land.face.strife.util.StatUtil.getDefenderArmor;
import static land.face.strife.util.StatUtil.getDefenderWarding;
import static land.face.strife.util.StatUtil.getEarthResist;
import static land.face.strife.util.StatUtil.getFireResist;
import static land.face.strife.util.StatUtil.getIceResist;
import static land.face.strife.util.StatUtil.getLightResist;
import static land.face.strife.util.StatUtil.getLightningResist;
import static land.face.strife.util.StatUtil.getShadowResist;
import static land.face.strife.util.StatUtil.getWardingMult;

import com.tealcube.minecraft.bukkit.TextUtils;
import com.tealcube.minecraft.bukkit.facecore.utilities.MessageUtils;
import com.tealcube.minecraft.bukkit.shade.apache.commons.lang3.StringUtils;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import land.face.strife.StrifePlugin;
import land.face.strife.data.DamageContainer;
import land.face.strife.data.DamageModifiers;
import land.face.strife.data.DamageModifiers.ElementalStatus;
import land.face.strife.data.IndicatorData;
import land.face.strife.data.IndicatorData.IndicatorStyle;
import land.face.strife.data.StrifeMob;
import land.face.strife.data.ability.EntityAbilitySet.TriggerAbilityType;
import land.face.strife.data.buff.LoadedBuff;
import land.face.strife.data.champion.LifeSkillType;
import land.face.strife.events.BlockEvent;
import land.face.strife.events.CriticalEvent;
import land.face.strife.events.EvadeEvent;
import land.face.strife.events.SneakAttackEvent;
import land.face.strife.listeners.CombatListener;
import land.face.strife.managers.BlockManager;
import land.face.strife.managers.CorruptionManager;
import land.face.strife.stats.StrifeStat;
import land.face.strife.stats.StrifeTrait;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EvokerFangs;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.ShulkerBullet;
import org.bukkit.entity.SmallFireball;
import org.bukkit.entity.WitherSkull;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDamageEvent.DamageModifier;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public class DamageUtil {

  private static StrifePlugin plugin;

  private static final String ATTACK_BLOCKED = TextUtils.color("&f&lBlocked!");
  private static final String ATTACK_DODGED = TextUtils.color("&f&lDodge!");
  public static double EVASION_THRESHOLD;

  private static final DamageModifier[] MODIFIERS = EntityDamageEvent.DamageModifier.values();
  public static final DamageType[] DMG_TYPES = DamageType.values();

  private static Vector IND_FLOAT_VECTOR;
  private static Vector IND_MISS_VECTOR;
  private static final float BLEED_PERCENT = 0.5f;

  private static float PVP_MULT;

  private static final Random RANDOM = new Random(System.currentTimeMillis());

  public static void refresh() {
    plugin = StrifePlugin.getInstance();
    float floatSpeed = (float) plugin.getSettings().getDouble("config.indicators.float-speed", 70);
    float missSpeed = (float) plugin.getSettings().getDouble("config.indicators.miss-speed", 80);
    EVASION_THRESHOLD = plugin.getSettings().getDouble("config.mechanics.evasion-threshold", 0.5);
    IND_FLOAT_VECTOR = new Vector(0, floatSpeed, 0);
    IND_MISS_VECTOR = new Vector(0, missSpeed, 0);
    PVP_MULT = (float) plugin.getSettings().getDouble("config.mechanics.pvp-multiplier", 0.5);
  }

  public static void applyExtraEffects(StrifeMob attacker, StrifeMob defender,
      String[] extraEffects) {
    if (extraEffects != null) {
      for (String s : extraEffects) {
        if (StringUtils.isBlank(s)) {
          continue;
        }
        plugin.getEffectManager()
            .execute(plugin.getEffectManager().getEffect(s), attacker, defender.getEntity());
      }
    }
  }

  public static boolean preDamage(StrifeMob attacker, StrifeMob defender, DamageModifiers mods) {

    if (attacker.getEntity() instanceof Player) {
      plugin.getChampionManager().updateEquipmentStats(
          plugin.getChampionManager().getChampion((Player) attacker.getEntity()));
    }
    if (defender.getEntity() instanceof Player) {
      plugin.getChampionManager().updateEquipmentStats(
          plugin.getChampionManager().getChampion((Player) defender.getEntity()));
    }

    if (plugin.getCounterManager().executeCounters(attacker.getEntity(), defender.getEntity())) {
      CombatListener.putSlimeHit(attacker.getEntity());
      return false;
    }

    float attackMult = mods.getAttackMultiplier();

    if (mods.isCanBeEvaded()) {
      float evasionMultiplier = DamageUtil
          .getFullEvasionMult(attacker, defender, mods.getAbilityMods());
      if (evasionMultiplier < DamageUtil.EVASION_THRESHOLD) {
        if (defender.getEntity() instanceof Player) {
          plugin.getCombatStatusManager().addPlayer((Player) defender.getEntity());
        }
        DamageUtil.doEvasion(attacker, defender);
        CombatListener.putSlimeHit(attacker.getEntity());
        TargetingUtil.expandMobRange(attacker.getEntity(), defender.getEntity());
        return false;
      }
      mods.setAttackMultiplier(attackMult * evasionMultiplier);
    }

    if (mods.isCanBeBlocked()) {
      if (plugin.getBlockManager().isAttackBlocked(attacker, defender, attackMult,
          mods.getAttackType(), mods.isBlocking())) {
        if (defender.getEntity() instanceof Player) {
          plugin.getCombatStatusManager().addPlayer((Player) defender.getEntity());
        }
        CombatListener.putSlimeHit(attacker.getEntity());
        TargetingUtil.expandMobRange(attacker.getEntity(), defender.getEntity());
        DamageUtil.doReflectedDamage(defender, attacker, mods.getAttackType());
        return false;
      }
    }

    return true;
  }

  public static Map<DamageType, Float> buildDamage(StrifeMob attacker, StrifeMob defender,
      DamageModifiers mods) {

    float attackMult = mods.getAttackMultiplier();

    Map<DamageType, Float> damageMap = DamageUtil.buildDamageMap(attacker, mods.getAttackType());
    damageMap.replaceAll((t, v) ->
        damageMap.get(t) * mods.getDamageModifiers().getOrDefault(t, 0f) * attackMult);
    for (DamageType type : DMG_TYPES) {
      if (mods.getFlatDamageBonuses().containsKey(type)) {
        damageMap
            .put(type, damageMap.getOrDefault(type, 0f) + mods.getFlatDamageBonuses().get(type));
      }
    }
    DamageUtil.applyElementalEffects(attacker, defender, damageMap, mods);

    return damageMap;
  }

  public static void reduceDamage(StrifeMob attacker, StrifeMob defender,
      Map<DamageType, Float> damageMap, DamageModifiers mods) {
    DamageUtil.applyDamageReductions(attacker, defender, damageMap, mods.getAbilityMods());
  }

  public static float damage(StrifeMob attacker, StrifeMob defender,
      Map<DamageType, Float> damageMap, DamageModifiers mods) {

    double standardDamage = damageMap.getOrDefault(DamageType.PHYSICAL, 0f) +
        damageMap.getOrDefault(DamageType.MAGICAL, 0f);
    double elementalDamage = damageMap.getOrDefault(DamageType.FIRE, 0f) +
        damageMap.getOrDefault(DamageType.ICE, 0f) +
        damageMap.getOrDefault(DamageType.LIGHTNING, 0f) +
        damageMap.getOrDefault(DamageType.DARK, 0f) +
        damageMap.getOrDefault(DamageType.EARTH, 0f) +
        damageMap.getOrDefault(DamageType.LIGHT, 0f);

    float potionMult = DamageUtil.getPotionMult(attacker.getEntity(), defender.getEntity());

    float critMult = 0;
    double bonusOverchargeMultiplier = 0;

    boolean criticalHit = isCriticalHit(attacker, defender, mods.getAttackMultiplier(),
        mods.getAbilityMods().getOrDefault(AbilityMod.CRITICAL_CHANCE, 0f));
    if (criticalHit) {
      critMult = (attacker.getStat(StrifeStat.CRITICAL_DAMAGE) +
          mods.getAbilityMods().getOrDefault(AbilityMod.CRITICAL_DAMAGE, 0f)) / 100;
    }

    boolean overcharge = mods.getAttackMultiplier() > 0.99;
    if (overcharge) {
      bonusOverchargeMultiplier = attacker.getStat(StrifeStat.OVERCHARGE) / 100;
    }

    float pvpMult = 1f;
    if (attacker.getEntity() instanceof Player && defender.getEntity() instanceof Player) {
      pvpMult = PVP_MULT;
    }

    standardDamage += standardDamage * critMult + standardDamage * bonusOverchargeMultiplier;
    standardDamage *= potionMult;
    standardDamage *= StatUtil.getDamageMult(attacker);
    standardDamage *= pvpMult;

    DamageUtil.applyLifeSteal(attacker, Math.min(standardDamage, defender.getEntity().getHealth()),
        mods.getHealMultiplier(), mods.getAbilityMods().getOrDefault(AbilityMod.LIFE_STEAL, 0f));

    if (attacker.hasTrait(StrifeTrait.ELEMENTAL_CRITS)) {
      elementalDamage += elementalDamage * critMult;
    }
    elementalDamage *= potionMult;
    elementalDamage *= StatUtil.getDamageMult(attacker);
    elementalDamage *= pvpMult;

    float damageReduction = defender.getStat(StrifeStat.DAMAGE_REDUCTION) * pvpMult;
    float rawDamage = (float) Math.max(0D, (standardDamage + elementalDamage) - damageReduction);

    rawDamage *= DamageUtil.getRageMult(defender);

    if (mods.getAttackType() == AttackType.PROJECTILE) {
      rawDamage *= DamageUtil.getProjectileMultiplier(attacker, defender);
    }
    rawDamage *= DamageUtil.getTenacityMult(defender);
    rawDamage *= DamageUtil.getMinionMult(attacker);
    rawDamage += damageMap.getOrDefault(DamageType.TRUE_DAMAGE, 0f);

    if (mods.isSneakAttack() && !defender.getEntity().hasMetadata("IGNORE_SNEAK")) {
      rawDamage += doSneakAttack(attacker, defender, mods, pvpMult);
    }

    String damageString = String.valueOf((int) Math.ceil(rawDamage));
    if (overcharge) {
      damageString = "&l" + damageString;
    }
    if (criticalHit) {
      damageString = damageString + "!";
    }
    if (attacker.getEntity() instanceof Player) {
      plugin.getIndicatorManager().addIndicator(attacker.getEntity(), defender.getEntity(),
          plugin.getDamageManager().buildHitIndicator((Player) attacker.getEntity()), damageString);
    }
    if (attacker.getMaster() != null && attacker.getMaster() instanceof Player) {
      plugin.getIndicatorManager().addIndicator(attacker.getMaster(), defender.getEntity(),
          plugin.getDamageManager().buildHitIndicator((Player) attacker.getMaster()),
          "&7" + damageString);
    }

    float finalDamage = plugin.getBarrierManager().damageBarrier(defender, rawDamage);
    plugin.getBarrierManager().updateShieldDisplay(defender);

    defender.trackDamage(attacker, finalDamage);
    return finalDamage;
  }

  public static void postDamage(StrifeMob attacker, StrifeMob defender,
      Map<DamageType, Float> damageMap, DamageModifiers mods) {

    float attackMult = mods.getAttackMultiplier();

    DamageUtil.applyHealthOnHit(attacker, mods.getAttackMultiplier(), mods.getHealMultiplier(),
        mods.getAbilityMods().getOrDefault(AbilityMod.HEALTH_ON_HIT, 0f));

    if (damageMap.containsKey(DamageType.PHYSICAL)) {
      DamageUtil.attemptBleed(attacker, defender, damageMap.get(DamageType.PHYSICAL),
          mods.getAttackMultiplier(), mods.getAbilityMods(), false);
    }

    DamageUtil.doReflectedDamage(defender, attacker, mods.getAttackType());

    if (attacker.getStat(StrifeStat.RAGE_ON_HIT) > 0.1) {
      plugin.getRageManager()
          .addRage(attacker, attacker.getStat(StrifeStat.RAGE_ON_HIT) * attackMult);
    }
    if (defender.getStat(StrifeStat.RAGE_WHEN_HIT) > 0.1) {
      plugin.getRageManager().addRage(defender, defender.getStat(StrifeStat.RAGE_WHEN_HIT));
    }

    plugin.getAbilityManager().abilityCast(attacker, defender, TriggerAbilityType.ON_HIT);
    plugin.getAbilityManager().abilityCast(defender, attacker, TriggerAbilityType.WHEN_HIT);
  }

  private static float doSneakAttack(StrifeMob attacker, StrifeMob defender, DamageModifiers mods,
      float pvpMult) {
    Player player = (Player) attacker.getEntity();
    float sneakSkill = plugin.getChampionManager().getChampion(player)
        .getEffectiveLifeSkillLevel(LifeSkillType.SNEAK, false);
    float sneakDamage = sneakSkill;
    sneakDamage += defender.getEntity().getMaxHealth() * (0.1 + 0.002 * sneakSkill);
    sneakDamage *= mods.getAttackMultiplier();
    sneakDamage *= pvpMult;
    SneakAttackEvent sneakEvent = DamageUtil
        .callSneakAttackEvent(attacker, defender, sneakSkill, sneakDamage);
    if (!sneakEvent.isCancelled()) {
      defender.getEntity().setMetadata("IGNORE_SNEAK", new FixedMetadataValue(plugin, true));
      StrifePlugin.getInstance().getIndicatorManager().addIndicator(attacker.getEntity(),
          defender.getEntity(), buildFloatIndicator((Player) attacker.getEntity()),
          "&7Sneak Attack!");
      return sneakEvent.getSneakAttackDamage();
    }
    return 0f;
  }

  private static boolean isCriticalHit(StrifeMob attacker, StrifeMob defender, float attackMult,
      float bonusCrit) {
    if (DamageUtil.isCrit(attacker, attackMult, bonusCrit)) {
      DamageUtil.callCritEvent(attacker, attacker);
      defender.getEntity().getWorld().playSound(defender.getEntity().getEyeLocation(),
          Sound.ENTITY_GENERIC_BIG_FALL, 2f, 0.8f);
      return true;
    }
    return false;
  }

  public static float getRawDamage(StrifeMob attacker, DamageType damageType, AttackType type) {
    switch (damageType) {
      case PHYSICAL:
        float damage = attacker.getStat(StrifeStat.PHYSICAL_DAMAGE);
        if (type == AttackType.MELEE) {
          damage *= 1 + attacker.getStat(StrifeStat.MELEE_PHYSICAL_MULT) / 100;
        } else if (type == AttackType.RANGED || type == AttackType.PROJECTILE) {
          damage *= 1 + attacker.getStat(StrifeStat.RANGED_PHYSICAL_MULT) / 100;
        }
        return damage;
      case MAGICAL:
        return attacker.getStat(StrifeStat.MAGIC_DAMAGE) * (1 + attacker.getStat(
            StrifeStat.MAGIC_MULT) / 100);
      case FIRE:
        return attacker.getStat(StrifeStat.FIRE_DAMAGE) * (1 + attacker.getStat(
            StrifeStat.ELEMENTAL_MULT) / 100);
      case ICE:
        return attacker.getStat(StrifeStat.ICE_DAMAGE) * (1 + attacker.getStat(
            StrifeStat.ELEMENTAL_MULT) / 100);
      case LIGHTNING:
        return attacker.getStat(StrifeStat.LIGHTNING_DAMAGE) * (1 + attacker.getStat(
            StrifeStat.ELEMENTAL_MULT) / 100);
      case DARK:
        return attacker.getStat(StrifeStat.DARK_DAMAGE) * (1 + attacker.getStat(
            StrifeStat.ELEMENTAL_MULT) / 100);
      case EARTH:
        return attacker.getStat(StrifeStat.EARTH_DAMAGE) * (1 + attacker.getStat(
            StrifeStat.ELEMENTAL_MULT) / 100);
      case LIGHT:
        return attacker.getStat(StrifeStat.LIGHT_DAMAGE) * (1 + attacker.getStat(
            StrifeStat.ELEMENTAL_MULT) / 100);
      case TRUE_DAMAGE:
        return attacker.getStat(StrifeStat.TRUE_DAMAGE);
      default:
        return 0;
    }
  }

  public static float applyDamageScale(StrifeMob caster, StrifeMob target,
      DamageContainer damageContainer, AttackType attackType) {
    float amount = damageContainer.getAmount();
    switch (damageContainer.getDamageScale()) {
      case FLAT:
        return amount;
      case CASTER_STAT_PERCENT:
        return damageContainer.getAmount() * caster.getStat(damageContainer.getDamageStat());
      case TARGET_STAT_PERCENT:
        return damageContainer.getAmount() * target.getStat(damageContainer.getDamageStat());
      case CASTER_LEVEL:
        return amount * StatUtil.getMobLevel(caster.getEntity());
      case CASTER_DAMAGE:
        return amount * DamageUtil
            .getRawDamage(caster, damageContainer.getDamageType(), attackType);
      case TARGET_CURRENT_HEALTH:
        return amount * (float) target.getEntity().getHealth();
      case CASTER_CURRENT_HEALTH:
        return amount * (float) caster.getEntity().getHealth();
      case TARGET_MISSING_HEALTH:
        return amount * (float) (target.getEntity().getMaxHealth() - target.getEntity()
            .getHealth());
      case CASTER_MISSING_HEALTH:
        return amount * (float) (caster.getEntity().getMaxHealth() - caster.getEntity()
            .getHealth());
      case TARGET_MAX_HEALTH:
        return amount * (float) target.getEntity().getMaxHealth();
      case CASTER_MAX_HEALTH:
        return amount * (float) caster.getEntity().getMaxHealth();
      case TARGET_CURRENT_BARRIER:
        return amount * StatUtil.getBarrier(target);
      case CASTER_CURRENT_BARRIER:
        return amount * StatUtil.getBarrier(caster);
      case TARGET_MISSING_BARRIER:
        return amount * (StatUtil.getMaximumBarrier(target) - StatUtil.getBarrier(target));
      case CASTER_MISSING_BARRIER:
        return amount * (StatUtil.getMaximumBarrier(caster) - StatUtil.getBarrier(caster));
      case TARGET_MAX_BARRIER:
        return amount * StatUtil.getMaximumBarrier(target);
      case CASTER_MAX_BARRIER:
        return amount * StatUtil.getMaximumBarrier(caster);
      case TARGET_CURRENT_ENERGY:
        return amount * StatUtil.getEnergy(target);
      case CASTER_CURRENT_ENERGY:
        return amount * StatUtil.getEnergy(caster);
      case TARGET_MISSING_ENERGY:
        return amount * (StatUtil.getMaximumEnergy(target) - StatUtil.getEnergy(target));
      case CASTER_MISSING_ENERGY:
        return amount * (StatUtil.getMaximumEnergy(caster) - StatUtil.getEnergy(caster));
      case TARGET_MAX_ENERGY:
        return amount * StatUtil.getMaximumEnergy(target);
      case CASTER_MAX_ENERGY:
        return amount * StatUtil.getMaximumEnergy(caster);
    }
    return amount;
  }

  public static Map<DamageType, Float> buildDamageMap(StrifeMob attacker, AttackType attackType) {
    Map<DamageType, Float> damageMap = new HashMap<>();
    for (DamageType damageType : DMG_TYPES) {
      float amount = getRawDamage(attacker, damageType, attackType);
      if (amount > 0) {
        damageMap.put(damageType, amount);
      }
    }
    return damageMap;
  }

  public static void applyDamageReductions(StrifeMob attacker, StrifeMob defender,
      Map<DamageType, Float> damageMap, Map<AbilityMod, Float> abilityMods) {
    damageMap.replaceAll((t, v) ->
        damageMap.get(t) * getDamageReduction(t, attacker, defender, abilityMods));
  }

  public static void applyAttackTypeMods(StrifeMob attacker, AttackType attackType,
      Map<DamageType, Float> damageMap) {
    if (attackType == AttackType.MELEE && damageMap.containsKey(DamageType.PHYSICAL)) {
      damageMap.put(DamageType.PHYSICAL,
          damageMap.get(DamageType.PHYSICAL) * 1
              + attacker.getStat(StrifeStat.MELEE_PHYSICAL_MULT) / 100);
    } else if (attackType == AttackType.RANGED && damageMap.containsKey(DamageType.PHYSICAL)) {
      damageMap.put(DamageType.PHYSICAL,
          damageMap.get(DamageType.PHYSICAL) * 1
              + attacker.getStat(StrifeStat.RANGED_PHYSICAL_MULT) / 100);
    }
  }

  public static void applyElementalEffects(StrifeMob attacker, StrifeMob defender,
      Map<DamageType, Float> damageMap, DamageModifiers mods) {
    for (DamageType type : damageMap.keySet()) {
      float bonus;
      switch (type) {
        case FIRE:
          bonus = attemptIgnite(damageMap.get(type), attacker, defender.getEntity());
          if (bonus != 0) {
            mods.getElementalStatuses().add(ElementalStatus.IGNITE);
            damageMap.put(type, damageMap.get(type) + bonus);
          }
          break;
        case ICE:
          bonus = attemptFreeze(damageMap.get(type), attacker, defender.getEntity());
          if (bonus != 0) {
            mods.getElementalStatuses().add(ElementalStatus.FREEZE);
            damageMap.put(type, damageMap.get(type) + bonus);
          }
          break;
        case LIGHTNING:
          bonus = attemptShock(damageMap.get(type), attacker, defender.getEntity());
          if (bonus != 0) {
            mods.getElementalStatuses().add(ElementalStatus.SHOCK);
            damageMap.put(type, damageMap.get(type) + bonus);
          }
          break;
        case DARK:
          bonus =
              damageMap.get(type) * getDarknessManager().getCorruptionMult(defender.getEntity());
          boolean corrupt = attemptCorrupt(damageMap.get(type), attacker, defender.getEntity());
          if (corrupt) {
            mods.getElementalStatuses().add(ElementalStatus.CORRUPT);
            damageMap.put(type, damageMap.get(type) + bonus);
          }
          break;
        case EARTH:
          if (!mods.isConsumeEarthRunes()) {
            break;
          }
          int earthRunes = consumeEarthRune(attacker, defender.getEntity());
          if (earthRunes != 0) {
            damageMap.put(type, damageMap.get(type) * (1 + earthRunes * 0.3f));
          }
          break;
        case LIGHT:
          bonus = getLightBonus(damageMap.get(type), attacker, defender.getEntity());
          if (bonus > damageMap.get(type) / 2) {
            damageMap.put(type, damageMap.get(type) + bonus);
          }
          break;
      }
    }
  }

  public static float getDamageReduction(DamageType type, StrifeMob attack, StrifeMob defend,
      Map<AbilityMod, Float> modDoubleMap) {
    switch (type) {
      case PHYSICAL:
        float armor = getDefenderArmor(attack, defend);
        armor *= 1 - modDoubleMap.getOrDefault(AbilityMod.ARMOR_PEN_MULT, 0f);
        armor -= modDoubleMap.getOrDefault(AbilityMod.ARMOR_PEN, 0f);
        return getArmorMult(armor);
      case MAGICAL:
        float warding = getDefenderWarding(attack, defend);
        warding *= 1 - modDoubleMap.getOrDefault(AbilityMod.WARD_PEN_MULT, 0f);
        warding -= modDoubleMap.getOrDefault(AbilityMod.WARD_PEN, 0f);
        return getWardingMult(warding);
      case FIRE:
        return 1 - getFireResist(defend) / 100;
      case ICE:
        return 1 - getIceResist(defend) / 100;
      case LIGHTNING:
        return 1 - getLightningResist(defend) / 100;
      case DARK:
        return 1 - getShadowResist(defend) / 100;
      case EARTH:
        return 1 - getEarthResist(defend) / 100;
      case LIGHT:
        return 1 - getLightResist(defend) / 100;
      case TRUE_DAMAGE:
      default:
        return 1;
    }
  }

  public static float getDamageMult(StrifeMob attacker, StrifeMob defender) {
    float mult = 1.0f;
    mult *= DamageUtil.getPotionMult(attacker.getEntity(), defender.getEntity());
    mult *= 1 + (attacker.getStat(StrifeStat.DAMAGE_MULT) / 100);
    mult *= getRageMult(defender);
    mult *= getTenacityMult(defender);
    mult *= getMinionMult(attacker);
    return mult;
  }

  public static double getMinionMult(StrifeMob mob) {
    return 1 + mob.getStat(StrifeStat.MINION_MULT_INTERNAL) / 100;
  }

  public static float getTenacityMult(StrifeMob defender) {
    if (defender.getStat(StrifeStat.TENACITY) < 1) {
      return 1;
    }
    double percent = defender.getEntity().getHealth() / defender.getEntity().getMaxHealth();
    float maxReduction = 1 - (float) Math.pow(0.5f, defender.getStat(StrifeStat.TENACITY) / 200);
    return 1 - (maxReduction * (float) Math.pow(1 - percent, 1.5));
  }

  public static float getRageMult(StrifeMob defender) {
    return 200 / (200 + StrifePlugin.getInstance().getRageManager().getRage(defender.getEntity()));
  }

  public static LivingEntity getAttacker(Entity entity) {
    if (!entity.getPassengers().isEmpty()) {
      if (entity.getPassengers().get(0) instanceof LivingEntity) {
        return (LivingEntity) entity.getPassengers().get(0);
      }
    }
    if (entity instanceof LivingEntity) {
      return (LivingEntity) entity;
    } else if (entity instanceof Projectile) {
      if (((Projectile) entity).getShooter() instanceof LivingEntity) {
        return (LivingEntity) ((Projectile) entity).getShooter();
      }
    } else if (entity instanceof EvokerFangs) {
      return ((EvokerFangs) entity).getOwner();
    }
    return null;
  }

  public static float attemptIgnite(float damage, StrifeMob attacker, LivingEntity defender) {
    if (rollDouble() >= attacker.getStat(StrifeStat.IGNITE_CHANCE) / 100) {
      return 0;
    }
    float bonusDamage = defender.getFireTicks() > 0 ? damage : 1f;
    defender.setFireTicks(Math.max(60 + (int) damage, defender.getFireTicks()));
    defender.getWorld().playSound(defender.getEyeLocation(), Sound.ITEM_FLINTANDSTEEL_USE, 1f, 1f);
    defender.getWorld()
        .spawnParticle(Particle.FLAME, defender.getEyeLocation(), 6 + (int) damage / 2,
            0.3, 0.3, 0.3, 0.03);
    return bonusDamage;
  }

  public static float attemptShock(float damage, StrifeMob attacker, LivingEntity defender) {
    if (rollDouble() >= attacker.getStat(StrifeStat.SHOCK_CHANCE) / 100) {
      return 0;
    }
    float multiplier = 0.5f;
    float percentHealth =
        (float) defender.getHealth() / (float) defender.getAttribute(Attribute.GENERIC_MAX_HEALTH)
            .getValue();
    if (percentHealth < 0.5f) {
      multiplier = 1f / (float) Math.max(0.16, percentHealth * 2);
    }
    double particles = damage * multiplier * 0.5;
    double particleRange = 0.8 + multiplier * 0.2;
    defender.getWorld()
        .playSound(defender.getEyeLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.7f, 2f);
    defender.getWorld()
        .spawnParticle(Particle.CRIT_MAGIC, defender.getEyeLocation(), 10 + (int) particles,
            particleRange, particleRange, particleRange, 0.12);
    if (defender instanceof Creeper) {
      ((Creeper) defender).setPowered(true);
    }
    return damage * multiplier;
  }

  public static float attemptFreeze(float damage, StrifeMob attacker, LivingEntity defender) {
    if (rollDouble() >= attacker.getStat(StrifeStat.FREEZE_CHANCE) / 100) {
      return 0;
    }
    float multiplier = 0.25f + 0.25f * (StatUtil.getHealth(attacker) / 100);
    if (!defender.hasPotionEffect(PotionEffectType.SLOW)) {
      defender.getActivePotionEffects().add(new PotionEffect(PotionEffectType.SLOW, 30, 1));
    }
    defender.getWorld().playSound(defender.getEyeLocation(), Sound.BLOCK_GLASS_BREAK, 1f, 1.0f);
    defender.getWorld()
        .spawnParticle(Particle.SNOWBALL, defender.getEyeLocation(), 4 + (int) damage / 2,
            0.3, 0.3, 0.2, 0.0);
    return damage * multiplier;
  }

  public static int consumeEarthRune(StrifeMob attacker, LivingEntity defender) {
    return StrifePlugin.getInstance().getBlockManager().consumeEarthRune(attacker, defender);
  }

  public static float getLightBonus(float damage, StrifeMob attacker, LivingEntity defender) {
    float light = attacker.getEntity().getLocation().getBlock().getLightLevel();
    float multiplier = (light - 4) / 10;
    if (multiplier >= 0.5) {
      defender.getWorld()
          .playSound(defender.getEyeLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1f, 2f);
      defender.getWorld().spawnParticle(
          Particle.FIREWORKS_SPARK,
          defender.getEyeLocation(),
          (int) (20 * multiplier),
          0.1, 0.1, 0.1,
          0.1
      );
    }
    return damage * multiplier;
  }

  public static boolean attemptCorrupt(float baseDamage, StrifeMob attacker,
      LivingEntity defender) {
    if (rollDouble() >= attacker.getStat(StrifeStat.CORRUPT_CHANCE) / 100) {
      return false;
    }
    applyCorrupt(defender, baseDamage);
    return true;
  }

  public static boolean isCrit(StrifeMob attacker, float aMult, float bonusCrit) {
    float critChance = StatUtil.getCriticalChance(attacker, aMult, bonusCrit);
    return critChance >= rollDouble(hasLuck(attacker.getEntity()));
  }

  public static float getFullEvasionMult(StrifeMob attacker, StrifeMob defender,
      Map<AbilityMod, Float> mods) {

    float totalEvasion = StatUtil.getEvasion(defender);
    float totalAccuracy = StatUtil.getAccuracy(attacker);
    totalAccuracy *= 1 + mods.getOrDefault(AbilityMod.ACCURACY_MULT, 0f) / 100;
    totalAccuracy += mods.getOrDefault(AbilityMod.ACCURACY, 0f);

    float evasionMultiplier = StatUtil.getMinimumEvasionMult(totalEvasion, totalAccuracy);
    evasionMultiplier = evasionMultiplier + (rollDouble() * (1 - evasionMultiplier));

    return evasionMultiplier;
  }

  public static void doEvasion(StrifeMob attacker, StrifeMob defender) {
    callEvadeEvent(defender, attacker);
    defender.getEntity().getWorld()
        .playSound(defender.getEntity().getEyeLocation(), Sound.ENTITY_GHAST_SHOOT, 0.5f, 2f);
    if (defender.getEntity() instanceof Player) {
      MessageUtils.sendActionBar((Player) defender.getEntity(), ATTACK_DODGED);
    }
    if (attacker.getEntity() instanceof Player) {
      StrifePlugin.getInstance().getIndicatorManager().addIndicator(attacker.getEntity(),
          defender.getEntity(), buildMissIndicator((Player) attacker.getEntity()), "&7&lMiss");
    }
  }

  public static void doBlock(StrifeMob attacker, StrifeMob defender) {
    callBlockEvent(defender, attacker);
    defender.getEntity().getWorld()
        .playSound(defender.getEntity().getEyeLocation(), Sound.ITEM_SHIELD_BLOCK, 1f, 1f);
    String defenderBar = ATTACK_BLOCKED;
    int runes = getBlockManager().getEarthRunes(defender.getEntity().getUniqueId());
    if (runes > 0) {
      StringBuilder sb = new StringBuilder(defenderBar);
      sb.append(TextUtils.color("&2 "));
      sb.append(IntStream.range(0, runes).mapToObj(i -> "▼").collect(Collectors.joining("")));
      defenderBar = sb.toString();
    }
    if (defender.getEntity() instanceof Player) {
      MessageUtils.sendActionBar((Player) defender.getEntity(), defenderBar);
    }
    if (attacker.getEntity() instanceof Player) {
      MessageUtils.sendActionBar((Player) attacker.getEntity(), ATTACK_BLOCKED);
    }
  }

  public static float getPotionMult(LivingEntity attacker, LivingEntity defender) {
    float potionMult = 1.0f;
    Collection<PotionEffect> attackerEffects = attacker.getActivePotionEffects();
    Collection<PotionEffect> defenderEffects = defender.getActivePotionEffects();
    for (PotionEffect effect : attackerEffects) {
      if (effect.getType().equals(PotionEffectType.INCREASE_DAMAGE)) {
        potionMult += 0.1 * (effect.getAmplifier() + 1);
      } else if (effect.getType().equals(PotionEffectType.WEAKNESS)) {
        potionMult -= 0.1 * (effect.getAmplifier() + 1);
      }
    }

    for (PotionEffect effect : defenderEffects) {
      if (effect.getType().equals(PotionEffectType.WITHER)) {
        potionMult += 0.15 * (effect.getAmplifier() + 1);
      } else if (effect.getType().equals(PotionEffectType.DAMAGE_RESISTANCE)) {
        potionMult -= 0.1 * (effect.getAmplifier() + 1);
      }
    }
    return Math.max(0, potionMult);
  }

  public static double getResistPotionMult(LivingEntity defender) {
    double mult = 1.0;
    Collection<PotionEffect> defenderEffects = defender.getActivePotionEffects();
    for (PotionEffect effect : defenderEffects) {
      if (effect.getType().equals(PotionEffectType.DAMAGE_RESISTANCE)) {
        mult -= 0.1 * (effect.getAmplifier() + 1);
        return mult;
      }
    }
    return mult;
  }

  public static boolean canAttack(Player attacker, Player defender) {
    CombatListener.addPlayer(attacker);
    defender.damage(0, attacker);
    boolean friendly = CombatListener.hasFriendlyPlayer(attacker);
    CombatListener.removePlayer(attacker);
    return !friendly;
  }

  public static double getProjectileMultiplier(StrifeMob atk, StrifeMob def) {
    return Math.max(0.05D, 1
        + (atk.getStat(StrifeStat.PROJECTILE_DAMAGE) - def.getStat(StrifeStat.PROJECTILE_REDUCTION))
        / 100);
  }

  public static void applyLifeSteal(StrifeMob attacker, double damage, double healMultiplier,
      double bonus) {
    double lifeSteal = (attacker.getStat(StrifeStat.LIFE_STEAL) + bonus) / 100;
    restoreHealthWithPenalties(attacker.getEntity(), damage * lifeSteal * healMultiplier);
  }

  public static void applyHealthOnHit(StrifeMob attacker, double attackMultiplier,
      double healMultiplier, double bonus) {
    double health =
        (attacker.getStat(StrifeStat.HP_ON_HIT) + bonus) * attackMultiplier * healMultiplier;
    restoreHealthWithPenalties(attacker.getEntity(), health);
  }

  public static boolean attemptBleed(StrifeMob attacker, StrifeMob defender, float rawPhysical,
      float attackMult, Map<AbilityMod, Float> abilityMods, boolean bypassBarrier) {
    if (StrifePlugin.getInstance().getBarrierManager().isBarrierUp(defender)) {
      return false;
    }
    if (defender.getStat(StrifeStat.BLEED_RESIST) > 99) {
      return false;
    }
    float chance = (attacker.getStat(StrifeStat.BLEED_CHANCE) +
        abilityMods.getOrDefault(AbilityMod.BLEED_CHANCE, 0f)) / 100;
    if (chance >= rollDouble()) {
      float damage = rawPhysical * attackMult * BLEED_PERCENT;
      float damageMult = 1 + (attacker.getStat(StrifeStat.BLEED_DAMAGE) +
          abilityMods.getOrDefault(AbilityMod.BLEED_DAMAGE, 0f)) / 100;
      damage *= damageMult;
      damage *= 1 - defender.getStat(StrifeStat.BLEED_RESIST) / 100;
      applyBleed(defender, damage, bypassBarrier);
    }
    return false;
  }

  public static void applyBleed(StrifeMob defender, float amount, boolean bypassBarrier) {
    if (amount < 0.1) {
      return;
    }
    StrifePlugin.getInstance().getBleedManager().addBleed(defender, amount, bypassBarrier);
    defender.getEntity().getWorld()
        .playSound(defender.getEntity().getLocation(), Sound.ENTITY_SHEEP_SHEAR, 1f, 1f);
  }

  public static void applyCorrupt(LivingEntity defender, float amount) {
    StrifePlugin.getInstance().getCorruptionManager().applyCorruption(defender, amount);
    defender.getWorld().playSound(defender.getEyeLocation(), Sound.ENTITY_WITHER_SHOOT, 0.7f, 2f);
    defender.getWorld()
        .spawnParticle(Particle.SMOKE_NORMAL, defender.getEyeLocation(), 10, 0.4, 0.4, 0.5, 0.1);
  }

  public static void doReflectedDamage(StrifeMob defender, StrifeMob attacker,
      AttackType damageType) {
    if (defender.getStat(StrifeStat.DAMAGE_REFLECT) < 0.1) {
      return;
    }
    double reflectDamage = defender.getStat(StrifeStat.DAMAGE_REFLECT);
    reflectDamage = damageType == AttackType.MELEE ? reflectDamage : reflectDamage * 0.6D;
    defender.getEntity().getWorld()
        .playSound(defender.getEntity().getLocation(), Sound.ENCHANT_THORNS_HIT, 0.2f, 1f);
    attacker.getEntity().setHealth(Math.max(0D, attacker.getEntity().getHealth() - reflectDamage));
  }

  public static void applyBuff(LoadedBuff buff, StrifeMob target) {
    applyBuff(buff, target, 1);
  }

  public static void applyBuff(LoadedBuff loadedBuff, StrifeMob target, double durationMult) {
    StrifePlugin.getInstance().getStrifeMobManager()
        .addBuff(target.getEntity().getUniqueId(), loadedBuff, durationMult);
  }

  public static LoadedBuff getBuff(String id) {
    return StrifePlugin.getInstance().getBuffManager().getBuffFromId(id);
  }

  public static void callCritEvent(StrifeMob attacker, StrifeMob victim) {
    CriticalEvent c = new CriticalEvent(attacker, victim);
    Bukkit.getPluginManager().callEvent(c);
  }

  public static void callEvadeEvent(StrifeMob evader, StrifeMob attacker) {
    EvadeEvent ev = new EvadeEvent(evader, attacker);
    Bukkit.getPluginManager().callEvent(ev);
  }

  public static SneakAttackEvent callSneakAttackEvent(StrifeMob attacker, StrifeMob victim,
      float sneakSkill, float sneakDamage) {
    SneakAttackEvent sneakAttackEvent = new SneakAttackEvent(attacker, victim, sneakSkill,
        sneakDamage);
    Bukkit.getPluginManager().callEvent(sneakAttackEvent);
    return sneakAttackEvent;
  }

  public static void callBlockEvent(StrifeMob evader, StrifeMob attacker) {
    BlockEvent ev = new BlockEvent(evader, attacker);
    Bukkit.getPluginManager().callEvent(ev);
  }

  public static boolean hasLuck(LivingEntity entity) {
    return entity.hasPotionEffect(PotionEffectType.LUCK);
  }

  public static double applyHealPenalties(LivingEntity entity, double amount) {
    if (entity.hasPotionEffect(PotionEffectType.POISON)) {
      return 0;
    }
    if (amount <= 0 || entity.getHealth() <= 0 || entity.isDead()) {
      return 0;
    }
    if (entity instanceof Player) {
      amount *= Math.min(((Player) entity).getFoodLevel() / 7.0D, 1.0D);
    }
    return amount;
  }

  public static void restoreHealthWithPenalties(LivingEntity entity, double amount) {
    restoreHealth(entity, applyHealPenalties(entity, amount));
  }

  public static void restoreHealth(LivingEntity livingEntity, double amount) {
    if (amount == 0) {
      return;
    }
    livingEntity.setHealth(Math.min(livingEntity.getHealth() + amount,
        livingEntity.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue()));
  }

  public static void restoreBarrier(StrifeMob strifeMob, float amount) {
    StrifePlugin.getInstance().getBarrierManager().restoreBarrier(strifeMob, amount);
  }

  public static void restoreEnergy(StrifeMob strifeMob, float amount) {
    StrifePlugin.getInstance().getEnergyManager().changeEnergy(strifeMob, amount);
  }

  public static AttackType getAttackType(EntityDamageByEntityEvent event) {
    if (event.getCause() == DamageCause.ENTITY_EXPLOSION) {
      return AttackType.EXPLOSION;
    } else if (event.getDamager() instanceof ShulkerBullet || event
        .getDamager() instanceof SmallFireball || event.getDamager() instanceof WitherSkull || event
        .getDamager() instanceof EvokerFangs) {
      return AttackType.MAGIC;
    } else if (event.getDamager() instanceof Projectile) {
      return AttackType.RANGED;
    }
    return AttackType.MELEE;
  }

  public static void removeDamageModifiers(EntityDamageEvent event) {
    for (DamageModifier modifier : MODIFIERS) {
      if (event.isApplicable(modifier)) {
        event.setDamage(modifier, 0D);
      }
    }
  }

  public static double rollDouble(boolean lucky) {
    return lucky ? Math.max(rollDouble(), rollDouble()) : rollDouble();
  }

  public static float rollDouble() {
    return RANDOM.nextFloat();
  }

  public static boolean rollBool(float chance, boolean lucky) {
    return lucky ? rollBool(chance) || rollBool(chance) : rollBool(chance);
  }

  public static boolean rollBool(float chance) {
    return RANDOM.nextFloat() <= chance;
  }

  private static BlockManager getBlockManager() {
    return StrifePlugin.getInstance().getBlockManager();
  }

  private static CorruptionManager getDarknessManager() {
    return StrifePlugin.getInstance().getCorruptionManager();
  }

  public static IndicatorData buildMissIndicator(Player player) {
    IndicatorData data = new IndicatorData(IND_MISS_VECTOR.clone(), IndicatorStyle.GRAVITY);
    data.addOwner(player);
    return data;
  }

  public static IndicatorData buildFloatIndicator(Player player) {
    IndicatorData data = new IndicatorData(IND_FLOAT_VECTOR.clone(), IndicatorStyle.FLOAT_UP);
    data.addOwner(player);
    return data;
  }

  public enum DamageScale {
    FLAT,
    CASTER_STAT_PERCENT,
    TARGET_STAT_PERCENT,
    CASTER_LEVEL,
    CASTER_DAMAGE,
    TARGET_CURRENT_HEALTH,
    CASTER_CURRENT_HEALTH,
    TARGET_MISSING_HEALTH,
    CASTER_MISSING_HEALTH,
    TARGET_MAX_HEALTH,
    CASTER_MAX_HEALTH,
    TARGET_CURRENT_BARRIER,
    CASTER_CURRENT_BARRIER,
    TARGET_MISSING_BARRIER,
    CASTER_MISSING_BARRIER,
    TARGET_MAX_BARRIER,
    CASTER_MAX_BARRIER,
    TARGET_CURRENT_ENERGY,
    CASTER_CURRENT_ENERGY,
    TARGET_MISSING_ENERGY,
    CASTER_MISSING_ENERGY,
    TARGET_MAX_ENERGY,
    CASTER_MAX_ENERGY,
  }

  public enum OriginLocation {
    ABOVE_HEAD,
    BELOW_HEAD,
    HEAD,
    CENTER,
    GROUND
  }

  public enum DamageType {
    TRUE_DAMAGE,
    PHYSICAL,
    MAGICAL,
    FIRE,
    ICE,
    LIGHTNING,
    EARTH,
    LIGHT,
    DARK
  }

  public enum AbilityMod {
    ACCURACY,
    ACCURACY_MULT,
    ARMOR_PEN,
    ARMOR_PEN_MULT,
    WARD_PEN,
    WARD_PEN_MULT,
    CRITICAL_CHANCE,
    CRITICAL_DAMAGE,
    LIFE_STEAL,
    HEALTH_ON_HIT,
    BLEED_CHANCE,
    BLEED_DAMAGE
  }

  public enum AttackType {
    MELEE, PROJECTILE, AREA, RANGED, MAGIC, EXPLOSION, OTHER
  }
}
