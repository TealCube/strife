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
package info.faceland.strife.menus;

import info.faceland.strife.StrifePlugin;
import info.faceland.strife.attributes.StrifeAttribute;
import info.faceland.strife.data.Champion;
import ninja.amp.ampmenus.events.ItemClickEvent;
import ninja.amp.ampmenus.items.MenuItem;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class StatsMagicMenuItem extends MenuItem {

    private final StrifePlugin plugin;
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#");
    private static final DecimalFormat AS_FORMAT = new DecimalFormat("#.##");

    public StatsMagicMenuItem(StrifePlugin plugin) {
        super(ChatColor.WHITE + "Magic Stats", new ItemStack(Material.BLAZE_ROD));
        this.plugin = plugin;
    }

    @Override
    public ItemStack getFinalIcon(Player player) {
        Champion champion = plugin.getChampionManager().getChampion(player.getUniqueId());
        ItemStack itemStack = new ItemStack(Material.BLAZE_ROD);
        ItemMeta itemMeta = Bukkit.getItemFactory().getItemMeta(itemStack.getType());
        itemMeta.setDisplayName(getDisplayName());
        itemMeta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        List<String> lore = new ArrayList<>(getLore());
        lore.add(ChatColor.AQUA + "Magic Damage: " + ChatColor.WHITE + DECIMAL_FORMAT.format(champion.getCache()
                .getAttribute(StrifeAttribute.MAGIC_DAMAGE)));
        if (champion.getCache().getAttribute(StrifeAttribute.ATTACK_SPEED) != 0) {
            lore.add(ChatColor.AQUA + "Attack Speed: " + ChatColor.WHITE + AS_FORMAT.format(2 / (1 + champion.getCache()
                    .getAttribute(StrifeAttribute.ATTACK_SPEED)))
                    + "s " + ChatColor.GRAY + "(+" + DECIMAL_FORMAT.format(champion.getCache().getAttribute
                    (StrifeAttribute.ATTACK_SPEED) * 100) + "%)");
        } else {
            lore.add(ChatColor.AQUA + "Attack Speed: " + ChatColor.WHITE + "2.0s");
        }
        if (champion.getCache().getAttribute(StrifeAttribute.ACCURACY) != 0.2) {
            if (champion.getCache().getAttribute(StrifeAttribute.ACCURACY) < 0.85) {
                lore.add(ChatColor.AQUA + "Accuracy: " + ChatColor.WHITE + "+" + DECIMAL_FORMAT.format(100 * champion
                        .getCache().getAttribute(StrifeAttribute.ACCURACY)) + "%");
            } else {
                lore.add(ChatColor.AQUA + "Accuracy: " + ChatColor.WHITE + "85% " + ChatColor.GRAY + "(Max)");
            }
        }
        if (champion.getCache().getAttribute(StrifeAttribute.CRITICAL_RATE) > 0.05 ||
                champion.getCache().getAttribute(StrifeAttribute.CRITICAL_DAMAGE) > 1.5) {
            lore.add(ChatColor.AQUA + "Critical Strike: " + ChatColor.WHITE + DECIMAL_FORMAT.format(
                    champion.getCache().getAttribute(StrifeAttribute.CRITICAL_RATE) * 100) + "% " + ChatColor.GRAY + "(" + DECIMAL_FORMAT.format(
                    champion.getCache().getAttribute(StrifeAttribute.CRITICAL_DAMAGE) * 100) + "%)");
        }
        if (champion.getCache().getAttribute(StrifeAttribute.OVERCHARGE) != 0.1) {
            lore.add(ChatColor.AQUA + "Overcharge: " + ChatColor.WHITE + DECIMAL_FORMAT.format((champion.getCache()
                    .getAttribute(StrifeAttribute.OVERCHARGE) + 1) * 100) + "%");
        }
        if (champion.getCache().getAttribute(StrifeAttribute.ARMOR_PENETRATION) > 0) {
            if (champion.getCache().getAttribute(StrifeAttribute.ARMOR_PENETRATION) < 0.7) {
                lore.add(ChatColor.AQUA + "Armor Penetration: " + ChatColor.WHITE
                        + DECIMAL_FORMAT.format(champion.getCache().getAttribute(StrifeAttribute.ARMOR_PENETRATION) * 100) + "%");
            } else {
                lore.add(ChatColor.AQUA + "Armor Penetration: " + ChatColor.WHITE + "70% " + ChatColor.GRAY + "(Max)");
            }
        }
        if (champion.getCache().getAttribute(StrifeAttribute.FIRE_DAMAGE) > 0) {
            lore.add(ChatColor.AQUA + "Fire Damage: " + ChatColor.WHITE + DECIMAL_FORMAT.format(champion.getCache()
                    .getAttribute(
                    StrifeAttribute.FIRE_DAMAGE)) + ChatColor.GRAY + " (" + DECIMAL_FORMAT.format(champion.getCache().getAttribute(
                    StrifeAttribute.IGNITE_CHANCE) * 100) + "%)");
        }
        if (champion.getCache().getAttribute(StrifeAttribute.LIGHTNING_DAMAGE) > 0) {
            lore.add(ChatColor.AQUA + "Lightning Damage: " + ChatColor.WHITE + DECIMAL_FORMAT.format(champion.getCache()
                    .getAttribute(
                    StrifeAttribute.LIGHTNING_DAMAGE)) + ChatColor.GRAY + " (" + DECIMAL_FORMAT.format(champion.getCache().getAttribute(
                    StrifeAttribute.SHOCK_CHANCE) * 100) + "%)");
        }
        if (champion.getCache().getAttribute(StrifeAttribute.ICE_DAMAGE) > 0) {
            lore.add(ChatColor.AQUA + "Ice Damage: " + ChatColor.WHITE + DECIMAL_FORMAT.format(champion.getCache()
                    .getAttribute(
                    StrifeAttribute.ICE_DAMAGE)) + ChatColor.GRAY + " (" + DECIMAL_FORMAT.format(champion.getCache().getAttribute(
                    StrifeAttribute.FREEZE_CHANCE) * 100) + "%)");
        }
        if (champion.getCache().getAttribute(StrifeAttribute.LIFE_STEAL) > 0) {
            if (champion.getCache().getAttribute(StrifeAttribute.LIFE_STEAL) < 0.65) {
                lore.add(ChatColor.AQUA + "Life Steal: " + ChatColor.WHITE + DECIMAL_FORMAT.format(champion.getCache()
                        .getAttribute(StrifeAttribute.LIFE_STEAL) * 100) + "%");
            } else {
                lore.add(ChatColor.AQUA + "Life Steal: " + ChatColor.WHITE + "65% " + ChatColor.GRAY + "(Max)");
            }
        }
        itemMeta.setLore(lore);
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    @Override
    public void onItemClick(ItemClickEvent event) {
        super.onItemClick(event);
    }

}