package info.faceland.strife.util;

import static info.faceland.strife.attributes.StrifeAttribute.*;

import info.faceland.strife.attributes.StrifeAttribute;
import info.faceland.strife.data.AttributedEntity;
import org.bukkit.entity.Player;

public class StatUtil {

  public static double getRegen(AttributedEntity ae) {
    return ae.getAttribute(REGENERATION) * (1 + ae.getAttribute(REGEN_MULT) / 100);
  }

  public static double getHealth(AttributedEntity ae) {
    return ae.getAttribute(HEALTH) * (1 + ae.getAttribute(HEALTH_MULT) / 100);
  }

  public static double getBarrierPerSecond(AttributedEntity ae) {
    return ae.getAttribute(BARRIER) * 0.125 * (1 + (ae.getAttribute(BARRIER_SPEED) / 100));
  }

  public static double getDamageMult(AttributedEntity ae) {
    return 1 + ae.getAttribute(DAMAGE_MULT) / 100;
  }

  public static double getMeleeDamage(AttributedEntity ae) {
    return ae.getAttribute(MELEE_DAMAGE) * (1 + ae.getAttribute(MELEE_MULT) / 100);
  }

  public static double getRangedDamage(AttributedEntity ae) {
    return ae.getAttribute(RANGED_DAMAGE) * (1 + ae.getAttribute(RANGED_MULT) / 100);
  }

  public static double getMagicDamage(AttributedEntity ae) {
    return ae.getAttribute(MAGIC_DAMAGE) * (1 + ae.getAttribute(MAGIC_MULT) / 100);
  }

  public static double getBaseMeleeDamage(AttributedEntity attacker, AttributedEntity defender) {
    double rawDamage = getMeleeDamage(attacker);
    if (rawDamage == 0) {
      return 0D;
    }
    return rawDamage * getArmorMult(attacker, defender);
  }

  public static double getBaseRangedDamage(AttributedEntity attacker, AttributedEntity defender) {
    double rawDamage = getRangedDamage(attacker);
    if (rawDamage == 0) {
      return 0D;
    }
    return rawDamage * getArmorMult(attacker, defender);
  }

  public static double getBaseMagicDamage(AttributedEntity attacker, AttributedEntity defender) {
    double rawDamage = getMagicDamage(attacker);
    if (rawDamage == 0) {
      return 0D;
    }
    return rawDamage * getWardingMult(attacker, defender);
  }

  public static double getAttackTime(AttributedEntity ae) {
    return 2 / (1 + ae.getAttribute(ATTACK_SPEED) / 100);
  }

  public static double getOverchargeMultiplier(AttributedEntity ae) {
    return 1 + (ae.getAttribute(OVERCHARGE) / 100);
  }

  public static double getCriticalMultiplier(AttributedEntity ae) {
    return 1 + (ae.getAttribute(CRITICAL_DAMAGE) / 100);
  }

  public static double getArmor(AttributedEntity ae) {
    return ae.getAttribute(ARMOR) * (1 + ae.getAttribute(ARMOR_MULT) / 100);
  }

  public static double getWarding(AttributedEntity ae) {
    return ae.getAttribute(WARDING) * (1 + ae.getAttribute(WARD_MULT) / 100);
  }

  public static double getEvasion(AttributedEntity ae) {
    return ae.getAttribute(EVASION) * (1 + ae.getAttribute(EVASION_MULT) / 100);
  }

  public static double getArmorMult(AttributedEntity attacker, AttributedEntity defender) {
    double adjustedArmor = Math.max(getArmor(defender) - attacker.getAttribute(ARMOR_PENETRATION), 1);
    return Math.min(1, 100 / (100 + adjustedArmor));
  }

  public static double getWardingMult(AttributedEntity attacker, AttributedEntity defender) {
    double adjustedWarding = Math.max(getWarding(defender) - attacker.getAttribute(WARD_PENETRATION), 1);
    return Math.min(1, 80 / (80 + adjustedWarding));
  }

  public static double getFireResist(AttributedEntity ae) {
    double amount = ae.getAttribute(StrifeAttribute.FIRE_RESIST) + ae.getAttribute(StrifeAttribute.ALL_RESIST);
    if (ae.getEntity() instanceof Player) {
      amount = Math.min(amount, 80);
    }
    return amount;
  }

  public static double getIceResist(AttributedEntity ae) {
    double amount = ae.getAttribute(StrifeAttribute.ICE_RESIST) + ae.getAttribute(StrifeAttribute.ALL_RESIST);
    if (ae.getEntity() instanceof Player) {
      amount = Math.min(amount, 80);
    }
    return amount;
  }

  public static double getLightningResist(AttributedEntity ae) {
    double amount = ae.getAttribute(StrifeAttribute.LIGHTNING_RESIST) + ae.getAttribute(StrifeAttribute.ALL_RESIST);
    if (ae.getEntity() instanceof Player) {
      amount = Math.min(amount, 80);
    }
    return amount;
  }

  public static double getShadowResist(AttributedEntity ae) {
    double amount = ae.getAttribute(StrifeAttribute.DARK_RESIST) + ae.getAttribute(StrifeAttribute.ALL_RESIST);
    if (ae.getEntity() instanceof Player) {
      amount = Math.min(amount, 80);
    }
    return amount;
  }

  public static double getLifestealPercentage(AttributedEntity attacker) {
    return attacker.getAttribute(LIFE_STEAL) / 100;
  }

  public static double getFireDamage(AttributedEntity attacker) {
    return attacker.getAttribute(StrifeAttribute.FIRE_DAMAGE) * (1 + (attacker.getAttribute(StrifeAttribute.ELEMENTAL_MULT) / 100));
  }

  public static double getIceDamage(AttributedEntity attacker) {
    return attacker.getAttribute(StrifeAttribute.ICE_DAMAGE) * (1 + (attacker.getAttribute(StrifeAttribute.ELEMENTAL_MULT) / 100));
  }

  public static double getLightningDamage(AttributedEntity attacker) {
    return attacker.getAttribute(StrifeAttribute.LIGHTNING_DAMAGE) * (1 + (attacker.getAttribute(StrifeAttribute.ELEMENTAL_MULT) / 100));
  }

  public static double getShadowDamage(AttributedEntity attacker) {
    return attacker.getAttribute(StrifeAttribute.DARK_DAMAGE) * (1 + (attacker.getAttribute(StrifeAttribute.ELEMENTAL_MULT) / 100));
  }

  public static double getBaseFireDamage(AttributedEntity attacker, AttributedEntity defender) {
    double damage = attacker.getAttribute(StrifeAttribute.FIRE_DAMAGE);
    if (damage == 0) {
      return 0D;
    }
    damage *= 1 + attacker.getAttribute(StrifeAttribute.ELEMENTAL_MULT) / 100;
    damage *= 1 - getFireResist(defender) / 100;
    return damage;
  }

  public static double getBaseIceDamage(AttributedEntity attacker, AttributedEntity defender) {
    double damage = attacker.getAttribute(StrifeAttribute.ICE_DAMAGE);
    if (damage == 0) {
      return 0D;
    }
    damage *= 1 + attacker.getAttribute(StrifeAttribute.ELEMENTAL_MULT) / 100;
    damage *= 1 - getIceResist(defender) / 100;
    return damage;
  }

  public static double getBaseLightningDamage(AttributedEntity attacker, AttributedEntity defender) {
    double damage = attacker.getAttribute(StrifeAttribute.LIGHTNING_DAMAGE);
    if (damage == 0) {
      return 0D;
    }
    damage *= 1 + attacker.getAttribute(StrifeAttribute.ELEMENTAL_MULT) / 100;
    damage *= 1 - getLightningResist(defender) / 100;
    return damage;
  }

  public static double getBaseShadowDamage(AttributedEntity attacker, AttributedEntity defender) {
    double damage = attacker.getAttribute(StrifeAttribute.DARK_DAMAGE);
    if (damage == 0) {
      return 0D;
    }
    damage *= 1 + attacker.getAttribute(StrifeAttribute.ELEMENTAL_MULT) / 100;
    damage *= 1 - getShadowResist(defender) / 100;
    return damage;
  }
}
