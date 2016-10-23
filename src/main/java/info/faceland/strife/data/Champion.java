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
package info.faceland.strife.data;

import com.tealcube.minecraft.bukkit.facecore.utilities.MessageUtils;
import info.faceland.strife.attributes.AttributeHandler;
import info.faceland.strife.attributes.StrifeAttribute;
import info.faceland.strife.stats.StrifeStat;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Champion {

    private UUID uniqueId;
    private Map<StrifeStat, Integer> levelMap;
    private int unusedStatPoints;
    private int highestReachedLevel;
    private ChampionCache cache;

    public Champion(UUID uniqueId) {
        this.uniqueId = uniqueId;
        this.levelMap = new HashMap<>();
        this.cache = new ChampionCache(this.uniqueId);
    }

    @Override
    public int hashCode() {
        return uniqueId != null ? uniqueId.hashCode() : 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Champion)) {
            return false;
        }

        Champion champion = (Champion) o;

        return !(uniqueId != null ? !uniqueId.equals(champion.uniqueId) : champion.uniqueId != null);
    }

    public int getLevel(StrifeStat stat) {
        if (levelMap.containsKey(stat)) {
            return levelMap.get(stat);
        }
        return 0;
    }

    public void setLevel(StrifeStat stat, int level) {
        levelMap.put(stat, level);
    }

    public Map<StrifeAttribute, Double> getStatAttributeValues() {
        cache.clearStatCache();
        Map<StrifeAttribute, Double> attributeDoubleMap = new HashMap<>();
        for (StrifeAttribute attr : StrifeAttribute.values()) {
            attributeDoubleMap.put(attr, attr != StrifeAttribute.ATTACK_SPEED ? attr.getBaseValue() : 0);
        }
        for (Map.Entry<StrifeStat, Integer> entry : getLevelMap().entrySet()) {
            for (StrifeAttribute attr : StrifeAttribute.values()) {
                double val = attributeDoubleMap.get(attr);
                attributeDoubleMap.put(attr, attr.getCap() > 0D ?
                        Math.min(val + entry.getKey().getAttribute(attr) * entry.getValue(), attr.getCap()) :
                        val + entry.getKey().getAttribute(attr) * entry.getValue());
            }
        }
        cache.setAttributeStatCache(attributeDoubleMap);
        return attributeDoubleMap;
    }

    public Map<StrifeAttribute, Double> getArmorAttributeValues() {
        cache.clearArmorCache();
        Map<StrifeAttribute, Double> attributeDoubleMap = new HashMap<>();
        boolean spam = false;
        for (ItemStack itemStack : getPlayer().getEquipment().getArmorContents()) {
            if (itemStack == null || itemStack.getType() == Material.AIR) {
                continue;
            }
            if (!AttributeHandler.meetsLevelRequirement(getPlayer(), itemStack)) {
                spam = true;
                continue;
            }
            for (StrifeAttribute attr : StrifeAttribute.values()) {
                double val = AttributeHandler.getValue(itemStack, attr);
                double curVal = attributeDoubleMap.containsKey(attr) ? attributeDoubleMap.get(attr) : 0;
                attributeDoubleMap.put(attr,
                        attr.getCap() > 0D ? Math.min(attr.getCap(), val + curVal) : val + curVal);
            }
        }
        if (spam) {
            MessageUtils.sendMessage(getPlayer(), "<red>You do not meet the level requirement for a piece of your " +
                    "armor! It will not give you any stats while equipped!");
        }

        cache.setAttributeArmorCache(attributeDoubleMap);
        return attributeDoubleMap;
    }

    public Map<StrifeAttribute, Double> getWeaponAttributeValues() {
        cache.clearWeaponCache();
        Map<StrifeAttribute, Double> attributeDoubleMap = new HashMap<>();
        ItemStack mainHandItemStack = getPlayer().getEquipment().getItemInMainHand();
        ItemStack offHandItemStack = getPlayer().getEquipment().getItemInOffHand();
        boolean update = false;
        boolean nullMainHand = true;
        if (mainHandItemStack != null && mainHandItemStack.getType() != Material.AIR && !isArmor(mainHandItemStack.getType())) {
            nullMainHand = false;
            if (!AttributeHandler.meetsLevelRequirement(getPlayer(), mainHandItemStack)) {
                MessageUtils.sendMessage(getPlayer(), "<red>You do not meet the level requirement for your weapon! It " +
                        "will not give you any stats when used!");
            } else {
                for (StrifeAttribute attr : StrifeAttribute.values()) {
                    double val = AttributeHandler.getValue(mainHandItemStack, attr);
                    double curVal = attributeDoubleMap.containsKey(attr) ? attributeDoubleMap.get(attr) : 0D;
                    attributeDoubleMap.put(attr, attr.getCap() > 0D ? Math.min(attr.getCap(), val + curVal) : val + curVal);
                    update = true;
                }
            }
        }
        if (offHandItemStack != null && offHandItemStack.getType() != Material.AIR && !isArmor(offHandItemStack.getType())) {
            if (!AttributeHandler.meetsLevelRequirement(getPlayer(), offHandItemStack)) {
                MessageUtils.sendMessage(getPlayer(), "<red>You do not meet the level requirement for your offhand " +
                        "item! It will not give you any stats when used!");
                return attributeDoubleMap;
            }
            double dualWieldEfficiency = 1.0;
            if (!nullMainHand) {
                if (isWand(mainHandItemStack)) {
                    dualWieldEfficiency = 0.0;
                    if (offHandItemStack.getType() == Material.BOOK || offHandItemStack.getType() == Material.SHIELD) {
                        dualWieldEfficiency = 1.0;
                    }
                } else if (isMeleeWeapon(mainHandItemStack.getType())) {
                    if (isMeleeWeapon(offHandItemStack.getType())) {
                        dualWieldEfficiency = 0.3;
                    } else if (offHandItemStack.getType() == Material.BOW) {
                        dualWieldEfficiency = 0.3;
                    }
                } else if (mainHandItemStack.getType() == Material.BOW) {
                    dualWieldEfficiency = 0.0;
                    if (offHandItemStack.getType() == Material.ARROW) {
                        dualWieldEfficiency = 1.0;
                    }
                }
            }
            for (StrifeAttribute attr : StrifeAttribute.values()) {
                double val = AttributeHandler.getValue(offHandItemStack, attr) * dualWieldEfficiency;
                double curVal = attributeDoubleMap.containsKey(attr) ? attributeDoubleMap.get(attr) : 0D;
                attributeDoubleMap.put(attr, attr.getCap() > 0D ? Math.min(attr.getCap(), val + curVal) : val + curVal);
                update = true;
            }
        }
        if (update) {
            cache.setAttributeWeaponCache(attributeDoubleMap);
        }
        return attributeDoubleMap;
    }

    public Map<StrifeAttribute, Double> getAttributeValues(boolean refresh) {
        Map<StrifeAttribute, Double> attributeDoubleMap = new HashMap<>();
        if (getPlayer() == null || getPlayer().getEquipment() == null) {
            return attributeDoubleMap;
        }
        if (refresh) {
            cache.clear();
            attributeDoubleMap = AttributeHandler.combineMaps(
                    getStatAttributeValues(),
                    getArmorAttributeValues(),
                    getWeaponAttributeValues()
            );
            cache.recombine();
        } else {
            attributeDoubleMap = cache.getCache();
        }
        return attributeDoubleMap;
    }

    public Map<StrifeStat, Integer> getLevelMap() {
        return new HashMap<>(levelMap);
    }

    public Player getPlayer() {
        return Bukkit.getPlayer(getUniqueId());
    }

    public UUID getUniqueId() {
        return uniqueId;
    }

    public int getUnusedStatPoints() {
        return unusedStatPoints;
    }

    public void setUnusedStatPoints(int unusedStatPoints) {
        this.unusedStatPoints = unusedStatPoints;
    }

    public int getMaximumStatLevel() {
        return 10 + (getHighestReachedLevel() / 5) * 2;
    }

    public int getHighestReachedLevel() {
        return highestReachedLevel;
    }

    public void setHighestReachedLevel(int highestReachedLevel) {
        this.highestReachedLevel = highestReachedLevel;
    }

    public ChampionCache getCache() {
        return cache;
    }

    public void setCache(ChampionCache cache) {
        this.cache = cache;
    }

    private boolean isArmor(Material material) {
        String name = material.name();
        return name.contains("HELMET") || name.contains("CHESTPLATE") || name.contains("LEGGINGS") ||
                name.contains("BOOTS");
    }

    private boolean isMeleeWeapon(Material material) {
        String name = material.name();
        return name.endsWith("SWORD") || name.endsWith("AXE") || name.endsWith("HOE");
    }

    private boolean isWand(ItemStack is) {
        if (is.getType() != Material.WOOD_SWORD) {
            return false;
        }
        if (!is.hasItemMeta()) {
            return false;
        }
        if (is.getItemMeta().getLore().get(1) == null) {
            return false;
        }
        return is.getItemMeta().getLore().get(1).endsWith("Wand");
    }

}