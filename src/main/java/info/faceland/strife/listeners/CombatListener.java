/**
 * The MIT License
 * Copyright (c) 2015 Teal Cube Games
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package info.faceland.strife.listeners;

import com.tealcube.minecraft.bukkit.facecore.ui.ActionBarMessage;
import com.tealcube.minecraft.bukkit.facecore.utilities.MessageUtils;

import info.faceland.strife.StrifePlugin;
import info.faceland.strife.attributes.StrifeAttribute;
import info.faceland.strife.data.Champion;

import org.bukkit.Material;
import org.bukkit.SkullType;
import org.bukkit.Sound;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Skeleton;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Random;

public class CombatListener implements Listener {

    private static final String[] DOGE_MEMES =
        {"<aqua>wow", "<green>wow", "<light purple>wow", "<aqua>much pain", "<green>much pain",
         "<light purple>much pain", "<aqua>many disrespects", "<green>many disrespects",
         "<light purple>many disrespects", "<red>no u", "<red>2damage4me"};
    private final StrifePlugin plugin;
    private final Random random;

    public CombatListener(StrifePlugin plugin) {
        this.plugin = plugin;
        random = new Random(System.currentTimeMillis());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityBurnEvent(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof LivingEntity)) {
            return;
        }
        if (event.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
            return;
        }
        if (event.getCause() == EntityDamageEvent.DamageCause.FIRE_TICK) {
            double hpdmg = ((LivingEntity) event.getEntity()).getHealth() / 25;
            event.setDamage(EntityDamageEvent.DamageModifier.ARMOR, 0);
            event.setDamage(1 + hpdmg);
            return;
        }
        if (event.getCause() == EntityDamageEvent.DamageCause.FIRE) {
            double hpdmg = ((LivingEntity) event.getEntity()).getHealth() / 25;
            event.setDamage(EntityDamageEvent.DamageModifier.ARMOR, 0);
            event.setDamage(1 + hpdmg);
            return;
        }
        if (event.getCause() == EntityDamageEvent.DamageCause.LAVA) {
            double hpdmg = ((LivingEntity) event.getEntity()).getHealth()/20;
            event.setDamage(EntityDamageEvent.DamageModifier.ARMOR, 0);
            event.setDamage(1 + hpdmg);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        if (event.getEntity().getShooter() instanceof Entity) {
            event.getEntity().setVelocity(event.getEntity().getVelocity().add(((Entity) event.getEntity()
                    .getShooter()).getVelocity()));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity() == null || event.getEntity().getKiller() == null) {
            return;
        }
        double chance = plugin.getChampionManager().getChampion(event.getEntity().getKiller().getUniqueId())
                .getCacheAttribute(StrifeAttribute.HEAD_DROP, StrifeAttribute.HEAD_DROP.getBaseValue());
        if (chance == 0) {
            return;
        }
        if (random.nextDouble() < chance) {
            LivingEntity e = event.getEntity();
            if (e.getType() == EntityType.SKELETON) {
                if (((Skeleton)e).getSkeletonType() == Skeleton.SkeletonType.NORMAL) {
                    ItemStack skull = new ItemStack(Material.SKULL_ITEM, 1, (short)0);
                    e.getWorld().dropItemNaturally(e.getLocation(), skull);
                }
            }
            else if ((e.getType() == EntityType.ZOMBIE)) {
                ItemStack skull = new ItemStack(Material.SKULL_ITEM, 1, (short)2);
                e.getWorld().dropItemNaturally(e.getLocation(), skull);
            }
            else if ((e.getType() == EntityType.CREEPER)) {
                ItemStack skull = new ItemStack(Material.SKULL_ITEM, 1, (short)4);
                e.getWorld().dropItemNaturally(e.getLocation(), skull);
            }
            else if ((e.getType() == EntityType.PLAYER)) {
                ItemStack skull = new ItemStack(Material.SKULL_ITEM, 1, (short) SkullType.PLAYER.ordinal());
                SkullMeta skullMeta = (SkullMeta)skull.getItemMeta();
                skullMeta.setOwner(event.getEntity().getName());
                skull.setItemMeta(skullMeta);
                e.getWorld().dropItemNaturally(e.getLocation(), skull);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.isCancelled()) {
            return;
        }
        if (!(event.getEntity() instanceof LivingEntity)) {
            return;
        }
        if (event.getEntity().hasMetadata("NPC")) {
            return;
        }
        LivingEntity a;
        boolean melee = true;
        if (event.getDamager() instanceof LivingEntity) {
            a = (LivingEntity) event.getDamager();
        } else if (event.getDamager() instanceof Projectile && ((Projectile) event.getDamager())
                .getShooter() instanceof LivingEntity) {
            a = (LivingEntity) ((Projectile) event.getDamager()).getShooter();
            melee = false;
        } else {
            return;
        }
        LivingEntity b = (LivingEntity) event.getEntity();
        boolean aPlayer = false;
        boolean bPlayer = false;
        if (a instanceof Player) {
            aPlayer = true;
        }
        if (b instanceof Player) {
            bPlayer = true;
        }
        if (event.isApplicable(EntityDamageEvent.DamageModifier.ARMOR)) {
            event.setDamage(EntityDamageEvent.DamageModifier.ARMOR, 0D);
        }
        if (event.isApplicable(EntityDamageEvent.DamageModifier.RESISTANCE)) {
            event.setDamage(EntityDamageEvent.DamageModifier.RESISTANCE, 0D);
        }
        if (event.isApplicable(EntityDamageEvent.DamageModifier.BLOCKING)) {
            event.setDamage(EntityDamageEvent.DamageModifier.BLOCKING, 0D);
        }
        if (event.isApplicable(EntityDamageEvent.DamageModifier.MAGIC)) {
            event.setDamage(EntityDamageEvent.DamageModifier.MAGIC, 0D);
        }
        double healthB = b.getHealth();
        double maxHealthB = b.getMaxHealth();
        double potionMult = 1;
        double poisonMult = 1;
        double trueDamage = 0;

        if (aPlayer) {
            Player aP = (Player) a;
            Champion champA = plugin.getChampionManager().getChampion(aP.getUniqueId());
            champA.getWeaponAttributeValues();
            champA.recombineCache();
            if (bPlayer) {
                //////////////////////////////////////////////////////////////////// PLAYER V PLAYER COMBAT ///
                Player bP = (Player) b;
                Champion champB = plugin.getChampionManager().getChampion(bP.getUniqueId());
                champB.getWeaponAttributeValues();
                champB.recombineCache();
                double pvpMult = plugin.getSettings().getDouble("config.pvp-multiplier", 0.5);

                double evadeChance = champB.getCacheAttribute(StrifeAttribute.EVASION);
                if (evadeChance > 0) {
                    double accuracy;
                    accuracy = champA.getCacheAttribute(StrifeAttribute.ACCURACY);
                    evadeChance = Math.max(evadeChance * (1 - accuracy), 0);
                    evadeChance = 1 - (100 / (100 + (Math.pow((evadeChance * 100), 1.1))));
                    if (random.nextDouble() < evadeChance) {
                        if (event.getDamager() instanceof Arrow) {
                            event.getDamager().remove();
                        }
                        b.getWorld().playSound(b.getEyeLocation(), Sound.GHAST_FIREBALL, 0.5f, 2f);
                        event.setCancelled(true);
                        return;
                    }
                }

                double damage = 0;
                double attackSpeedMultA = 1D;
                double velocityMultA = 0D;
                if (melee) {
                    double attackSpeedA = StrifeAttribute.ATTACK_SPEED.getBaseValue() *
                            (1 / (1 + champA.getCacheAttribute(StrifeAttribute.ATTACK_SPEED)));
                    long timeLeft = plugin.getAttackSpeedTask().getTimeLeft(a.getUniqueId());
                    long timeToSet = Math.round(Math.max(4.0 * attackSpeedA, 0.0));
                    if (timeLeft > 0) {
                        attackSpeedMultA = Math.max(1.0 - 1.0 * timeLeft / timeToSet, 0.0);
                    }
                    plugin.getAttackSpeedTask().setTimeLeft(a.getUniqueId(), timeToSet);

                    damage = champA.getCacheAttribute(StrifeAttribute.MELEE_DAMAGE) * attackSpeedMultA;
                } else {
                    velocityMultA = Math.min(event.getDamager().getVelocity().lengthSquared() / Math.pow(3, 2), 1);
                    damage = champA.getCacheAttribute(StrifeAttribute.RANGED_DAMAGE) * velocityMultA;
                }


                if (bP.isBlocking()) {
                    if (random.nextDouble() < champB.getCacheAttribute(StrifeAttribute.ABSORB_CHANCE)) {
                        if (event.getDamager() instanceof Arrow) {
                            event.getDamager().remove();
                        }
                        b.setHealth(Math.min(healthB + (maxHealthB / 25), maxHealthB));
                        b.getWorld().playSound(a.getEyeLocation(), Sound.BLAZE_HIT, 1f, 2f);
                        event.setCancelled(true);
                        return;
                    }
                    if (melee) {
                        if (random.nextDouble() < champB.getCacheAttribute(StrifeAttribute.PARRY)) {
                            a.damage(damage * 0.2 * pvpMult);
                            b.getWorld().playSound(b.getEyeLocation(), Sound.ANVIL_LAND, 1f, 2f);
                            event.setCancelled(true);
                            return;
                        }
                    } else {
                        if (random.nextDouble() < 2 * champB.getCacheAttribute(StrifeAttribute.PARRY)) {
                            if (event.getDamager() instanceof Arrow) {
                                event.getDamager().remove();
                            }
                            b.getWorld().playSound(b.getEyeLocation(), Sound.ANVIL_LAND, 1f, 2f);
                            event.setCancelled(true);
                            return;
                        }
                    }
                    damage *= 1 - champB.getCacheAttribute(StrifeAttribute.BLOCK);
                }

                double critbonus = damage * getCritBonus(champA.getCacheAttribute(StrifeAttribute.CRITICAL_RATE),
                champA.getCacheAttribute(StrifeAttribute.CRITICAL_DAMAGE), aP);

                double overbonus = 0;
                if (velocityMultA > 0) {
                    if (velocityMultA > 0.94D) {
                        overbonus = champA.getCacheAttribute(StrifeAttribute.OVERCHARGE) * damage;
                    }
                } else {
                    if (attackSpeedMultA > 0.94D) {
                        overbonus = champA.getCacheAttribute(StrifeAttribute.OVERCHARGE) * damage;
                    }
                }

                damage = damage + critbonus + overbonus;

                double fireDamage = champA.getCacheAttribute(StrifeAttribute.FIRE_DAMAGE);
                if (fireDamage > 0) {
                    if (random.nextDouble() < ((champA.getCacheAttribute(StrifeAttribute.IGNITE_CHANCE) * (0.25 +
                            attackSpeedMultA * 0.75)) * (1 - champB.getCacheAttribute(StrifeAttribute.RESISTANCE)))) {
                        trueDamage += fireDamage / 10;
                        b.setFireTicks(Math.max(10 + (int) Math.round(fireDamage * 20), b.getFireTicks()));
                        b.getWorld().playSound(b.getEyeLocation(), Sound.FIRE_IGNITE, 1f, 1f);
                    }
                }
                double lightningDamage = champA.getCacheAttribute(StrifeAttribute.LIGHTNING_DAMAGE);
                if (lightningDamage > 0) {
                    if (random.nextDouble() < ((champA.getCacheAttribute(StrifeAttribute.SHOCK_CHANCE) * (0.25 +
                            attackSpeedMultA * 0.75)) * (1 - champB.getCacheAttribute(StrifeAttribute.RESISTANCE)))) {
                        trueDamage += lightningDamage * 0.75;
                        b.getWorld().playSound(b.getEyeLocation(), Sound.AMBIENCE_THUNDER, 1f, 1.5f);
                    }
                }
                double iceDamage = champA.getCacheAttribute(StrifeAttribute.ICE_DAMAGE);
                if (iceDamage > 0) {
                    if (random.nextDouble() < ((champA.getCacheAttribute(StrifeAttribute.FREEZE_CHANCE) * (0.25 +
                            attackSpeedMultA * 0.75)) * (1 - champB.getCacheAttribute(StrifeAttribute.RESISTANCE)))) {
                        damage = damage + iceDamage + iceDamage * (maxHealthB / 300);
                        b.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 5 + (int) iceDamage * 3, 1));
                        b.getWorld().playSound(b.getEyeLocation(), Sound.GLASS, 1f, 1f);
                    }
                }

                if (b.hasPotionEffect(PotionEffectType.WITHER)) {
                    potionMult += 0.1D;
                }
                if (b.hasPotionEffect(PotionEffectType.DAMAGE_RESISTANCE)) {
                    potionMult -= 0.1D;
                }
                if (melee) {
                    if (a.hasPotionEffect(PotionEffectType.INCREASE_DAMAGE)) {
                        potionMult += 0.1D;
                    }
                    if (a.hasPotionEffect(PotionEffectType.WEAKNESS)) {
                        potionMult -= 0.1D;
                    }
                } else {
                    double snareChance = champA.getCacheAttribute(StrifeAttribute.SNARE_CHANCE);
                    if (snareChance > 0) {
                        if (random.nextDouble() < snareChance) {
                            b.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 30, 5));
                            b.getWorld().playSound(b.getEyeLocation(), Sound.FIRE_IGNITE, 1f, 1f);
                        }
                    }
                }

                damage *= potionMult;
                damage *= getArmorMult(champB.getCacheAttribute(StrifeAttribute.ARMOR), champA.getCacheAttribute(StrifeAttribute.ARMOR_PENETRATION));
                damage += trueDamage;
                damage *= pvpMult;

                double lifeSteal = champA.getCacheAttribute(StrifeAttribute.LIFE_STEAL);
                if (lifeSteal > 0) {
                    if (a.hasPotionEffect(PotionEffectType.POISON)) {
                        poisonMult = 0.34D;
                    }
                    double hungerMult = Math.min(((double) (((Player) a).getFoodLevel())) / 7.0D, 1.0D);
                    double lifeStolen = damage * lifeSteal * poisonMult * hungerMult;
                    if (a.getHealth() > 0) {
                        a.setHealth(Math.min(a.getHealth() + lifeStolen, a.getMaxHealth()));
                    }
                }

                event.setDamage(EntityDamageEvent.DamageModifier.BASE, damage);

                double chance = champB.getCacheAttribute(StrifeAttribute.DOGE);
                if (random.nextDouble() < chance) {
                    MessageUtils.sendMessage(bP, DOGE_MEMES[random.nextInt(DOGE_MEMES.length)]);
                }

                String msg;
                if (healthB > damage) {
                    msg = "&c&lHealth: &f" + (int) (healthB - damage) + "&7/&f" + (int) (maxHealthB);
                } else {
                    msg = "&c&lHealth: &fDEAD &7/ &fKILLED";
                }
                ActionBarMessage.send((Player) a, msg);
            } else {
                ///////////////////////////////////////////////////////////////////// PLAYER V MOB COMBAT ///
                double damage = 0;
                double attackSpeedMultA = 1D;
                double velocityMultA = 0D;
                if (melee) {
                    double attackSpeedA = StrifeAttribute.ATTACK_SPEED.getBaseValue() *
                            (1 / (1 + champA.getCacheAttribute(StrifeAttribute.ATTACK_SPEED)));
                    long timeLeft = plugin.getAttackSpeedTask().getTimeLeft(a.getUniqueId());
                    long timeToSet = Math.round(Math.max(4.0 * attackSpeedA, 0.0));
                    if (timeLeft > 0) {
                        attackSpeedMultA = Math.max(1.0 - 1.0 * timeLeft / timeToSet, 0.0);
                    }
                    plugin.getAttackSpeedTask().setTimeLeft(a.getUniqueId(), timeToSet);

                    damage = champA.getCacheAttribute(StrifeAttribute.MELEE_DAMAGE) * attackSpeedMultA;
                } else {
                    velocityMultA = Math.min(event.getDamager().getVelocity().lengthSquared() / Math.pow(3, 2), 1);
                    damage = champA.getCacheAttribute(StrifeAttribute.RANGED_DAMAGE) * velocityMultA;

                }

                double critbonus = damage * getCritBonus(champA.getCacheAttribute(StrifeAttribute.CRITICAL_RATE),
                        champA.getCacheAttribute(StrifeAttribute.CRITICAL_DAMAGE), aP);

                double overbonus = 0;
                if (velocityMultA > 0) {
                    if (velocityMultA > 0.94D) {
                        overbonus = champA.getCacheAttribute(StrifeAttribute.OVERCHARGE) * damage;
                    }
                } else {
                    if (attackSpeedMultA > 0.94D) {
                        overbonus = champA.getCacheAttribute(StrifeAttribute.OVERCHARGE) * damage;
                    }
                }

                damage = damage + critbonus + overbonus;

                double fireDamage = champA.getCacheAttribute(StrifeAttribute.FIRE_DAMAGE);
                if (fireDamage > 0) {
                    if (random.nextDouble() < (champA.getCacheAttribute(StrifeAttribute.IGNITE_CHANCE) * (0.25 + attackSpeedMultA * 0.75))) {
                        trueDamage += fireDamage / 10;
                        b.setFireTicks(Math.max(10 + (int) Math.round(fireDamage * 20), b.getFireTicks()));
                        b.getWorld().playSound(b.getEyeLocation(), Sound.FIRE_IGNITE, 1f, 1f);
                    }
                }
                double lightningDamage = champA.getCacheAttribute(StrifeAttribute.LIGHTNING_DAMAGE);
                if (lightningDamage > 0) {
                    if (random.nextDouble() < (champA.getCacheAttribute(StrifeAttribute.SHOCK_CHANCE) * (0.25 + attackSpeedMultA * 0.75))) {
                        trueDamage += lightningDamage * 1.5;
                        b.getWorld().playSound(b.getEyeLocation(), Sound.AMBIENCE_THUNDER, 1f, 1.5f);
                    }
                }
                double iceDamage = champA.getCacheAttribute(StrifeAttribute.ICE_DAMAGE);
                if (iceDamage > 0) {
                    if (random.nextDouble() < (champA.getCacheAttribute(StrifeAttribute.FREEZE_CHANCE) * (0.25 + attackSpeedMultA * 0.75))) {
                        damage = damage + iceDamage + iceDamage * (maxHealthB / 300);
                        b.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 5 + (int) iceDamage * 3, 1));
                        b.getWorld().playSound(b.getEyeLocation(), Sound.GLASS, 1f, 1f);
                    }
                }

                if (b.hasPotionEffect(PotionEffectType.WITHER)) {
                    potionMult += 0.1D;
                }
                if (b.hasPotionEffect(PotionEffectType.DAMAGE_RESISTANCE)) {
                    potionMult -= 0.1D;
                }
                if (melee) {
                    if (a.hasPotionEffect(PotionEffectType.INCREASE_DAMAGE)) {
                        potionMult += 0.1D;
                    }
                    if (a.hasPotionEffect(PotionEffectType.WEAKNESS)) {
                        potionMult -= 0.1D;
                    }
                } else {
                    double snareChance = champA.getCacheAttribute(StrifeAttribute.SNARE_CHANCE);
                    if (snareChance > 0) {
                        if (random.nextDouble() < snareChance) {
                            b.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 30, 5));
                            b.getWorld().playSound(b.getEyeLocation(), Sound.FIRE_IGNITE, 1f, 1f);
                        }
                    }
                }

                damage *= potionMult;
                damage += trueDamage;

                double lifeSteal = champA.getCacheAttribute(StrifeAttribute.LIFE_STEAL);
                if (lifeSteal > 0) {
                    if (a.hasPotionEffect(PotionEffectType.POISON)) {
                        poisonMult = 0.34D;
                    }
                    double hungerMult = Math.min(((double) (((Player) a).getFoodLevel())) / 7.0D, 1.0D);
                    double lifeStolen = damage * lifeSteal * poisonMult * hungerMult;
                    if (a.getHealth() > 0) {
                        a.setHealth(Math.min(a.getHealth() + lifeStolen, a.getMaxHealth()));
                    }
                }

                event.setDamage(EntityDamageEvent.DamageModifier.BASE, damage);

                String msg;
                if (healthB > damage) {
                    msg = "&c&lHealth: &f" + (int) (healthB - damage) + "&7/&f" + (int) (maxHealthB);
                } else {
                    msg = "&c&lHealth: &fDEAD &7/ &fKILLED";
                }
                ActionBarMessage.send((Player) a, msg);
            }
        } else {
            if (bPlayer) {
                /////////////////////////////////////////////////////////////////////////////// MOB V PLAYER COMBAT ///
                Player pB = (Player) b;
                Champion champB = plugin.getChampionManager().getChampion(pB.getUniqueId());
                champB.getWeaponAttributeValues();
                champB.recombineCache();
                double evadeChance = champB.getCacheAttribute(StrifeAttribute.EVASION);
                if (evadeChance > 0) {
                    evadeChance = 1 - (100 / (100 + (Math.pow((evadeChance * 100), 1.2))));
                    if (random.nextDouble() < evadeChance) {
                        if (event.getDamager() instanceof Arrow) {
                            event.getDamager().remove();
                        }
                        b.getWorld().playSound(b.getEyeLocation(), Sound.GHAST_FIREBALL, 0.5f, 2f);
                        event.setCancelled(true);
                        return;
                    }
                }

                double damage = 0;
                if (a.hasMetadata("DAMAGE")) {
                    damage = getDamageFromMeta(a, b, event.getCause());
                } else {
                    damage = event.getDamage(EntityDamageEvent.DamageModifier.BASE);
                }

                if (pB.isBlocking()) {
                    if (random.nextDouble() < champB.getCacheAttribute(StrifeAttribute.ABSORB_CHANCE)) {
                        if (event.getDamager() instanceof Arrow) {
                            event.getDamager().remove();
                        }
                        b.setHealth(Math.min(healthB + (maxHealthB / 25), maxHealthB));
                        b.getWorld().playSound(a.getEyeLocation(), Sound.BLAZE_HIT, 1f, 2f);
                        event.setCancelled(true);
                        return;
                    }
                    if (melee) {
                        if (random.nextDouble() < champB.getCacheAttribute(StrifeAttribute.PARRY)) {
                            a.damage(damage * 0.2);
                            b.getWorld().playSound(b.getEyeLocation(), Sound.ANVIL_LAND, 1f, 2f);
                            event.setCancelled(true);
                            return;
                        }
                    } else {
                        if (random.nextDouble() < 2 * champB.getCacheAttribute(StrifeAttribute.PARRY)) {
                            if (event.getDamager() instanceof Arrow) {
                                event.getDamager().remove();
                            }
                            b.getWorld().playSound(b.getEyeLocation(), Sound.ANVIL_LAND, 1f, 2f);
                            event.setCancelled(true);
                            return;
                        }
                    }
                    damage *= 1 - champB.getCacheAttribute(StrifeAttribute.BLOCK);
                }
                damage *= getArmorMult(champB.getCacheAttribute(StrifeAttribute.ARMOR), 0);
                event.setDamage(EntityDamageEvent.DamageModifier.BASE, damage);
                double chance = champB.getCacheAttribute(StrifeAttribute.DOGE);
                if (random.nextDouble() < chance) {
                    MessageUtils.sendMessage(b, DOGE_MEMES[random.nextInt(DOGE_MEMES.length)]);
                }
            } else {
                /// MOB V MOB COMBAT ///
                double damage = 0;
                if (a.hasMetadata("DAMAGE")) {
                    damage = getDamageFromMeta(a, b, event.getCause());
                } else {
                    damage = event.getDamage(EntityDamageEvent.DamageModifier.BASE);
                }
                event.setDamage(EntityDamageEvent.DamageModifier.BASE, damage);
            }
        }
    }

    private double getDamageFromMeta (LivingEntity a, LivingEntity b, EntityDamageEvent.DamageCause d) {
        double damage = 0;
        damage = a.getMetadata("DAMAGE").get(0).asDouble();
        if (d == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION) {
            damage = damage * Math.max(0.3, 2.5 / (a.getLocation().distanceSquared(b.getLocation()) + 1));
        }
        return damage;
    }

    private double getArmorMult (double armor, double apen) {
        if (armor > 0) {
            if (apen < 1) {
                return 100 / (100 + (Math.pow(((armor * (1 - apen)) * 100), 1.2)));
            } else {
                return 1 + ((apen - 1) / 5);
            }
        } else {
            return 1 + (apen / 5);
        }
    }

    private double getCritBonus(double rate, double damage, Player a) {
        if (random.nextDouble() < rate) {
            a.getWorld().playSound(a.getEyeLocation(), Sound.FALL_BIG, 2f, 0.8f);
            return damage - 1.0;
        }
        return 1.0;
    }

}
