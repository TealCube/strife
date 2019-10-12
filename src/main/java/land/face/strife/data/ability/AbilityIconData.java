package land.face.strife.data.ability;

import com.tealcube.minecraft.bukkit.TextUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import land.face.strife.StrifePlugin;
import land.face.strife.data.champion.Champion;
import land.face.strife.data.champion.LifeSkillType;
import land.face.strife.data.champion.StrifeAttribute;
import land.face.strife.stats.AbilitySlot;
import org.apache.commons.lang.WordUtils;
import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;

public class AbilityIconData {

  private static String REQ_STR = StrifePlugin.getInstance().getSettings()
      .getString("language.abilities.picker-requirement-tag");

  private ItemStack stack;
  private AbilitySlot abilitySlot;
  private int levelRequirement = 0;
  private int bonusLevelRequirement = 0;
  private final Map<LifeSkillType, Integer> lifeSkillRequirements = new HashMap<>();
  private final Map<StrifeAttribute, Integer> attributeRequirement = new HashMap<>();
  private final Map<LifeSkillType, Float> expWeights = new HashMap<>();

  public AbilityIconData(ItemStack stack) {
    this.stack = stack;
  }

  public ItemStack getStack() {
    return stack;
  }

  public void setStack(ItemStack stack) {
    this.stack = stack;
  }

  public AbilitySlot getAbilitySlot() {
    return abilitySlot;
  }

  public void setAbilitySlot(AbilitySlot abilitySlot) {
    this.abilitySlot = abilitySlot;
  }

  public int getLevelRequirement() {
    return levelRequirement;
  }

  public void setLevelRequirement(int levelRequirement) {
    this.levelRequirement = levelRequirement;
  }

  public int getBonusLevelRequirement() {
    return bonusLevelRequirement;
  }

  public void setBonusLevelRequirement(int bonusLevelRequirement) {
    this.bonusLevelRequirement = bonusLevelRequirement;
  }

  public Map<LifeSkillType, Integer> getLifeSkillRequirements() {
    return lifeSkillRequirements;
  }

  public Map<StrifeAttribute, Integer> getAttributeRequirement() {
    return attributeRequirement;
  }

  public Map<LifeSkillType, Float> getExpWeights() {
    return expWeights;
  }

  public boolean isRequirementMet(Champion champion) {
    if (champion.getPlayer().getLevel() < levelRequirement) {
      return false;
    }
    if (champion.getBonusLevels() < bonusLevelRequirement) {
      return false;
    }
    for (LifeSkillType type : lifeSkillRequirements.keySet()) {
      if (champion.getLifeSkillLevel(type) < lifeSkillRequirements.get(type)) {
        return false;
      }
    }
    for (StrifeAttribute attr : attributeRequirement.keySet()) {
      if (champion.getAttributeLevel(attr) < attributeRequirement.get(attr)) {
        return false;
      }
    }
    return true;
  }

  public static List<String> buildRequirementsLore(Champion champion, AbilityIconData data) {
    List<String> strings = new ArrayList<>();
    if (champion.getPlayer().getLevel() < data.levelRequirement) {
      strings.add(REQ_STR.replace("{REQ}", "Level " + data.levelRequirement));
    }
    if (champion.getBonusLevels() < data.bonusLevelRequirement) {
      strings.add(REQ_STR.replace("{REQ}", "Bonus Level " + data.bonusLevelRequirement));
    }
    for (LifeSkillType type : data.lifeSkillRequirements.keySet()) {
      if (champion.getLifeSkillLevel(type) < data.lifeSkillRequirements.get(type)) {
        strings.add(REQ_STR.replace("{REQ}", ChatColor.stripColor(
            WordUtils.capitalize(type.name().toLowerCase().replaceAll("_", " ")) +
                " " + data.lifeSkillRequirements.get(type))));
      }
    }
    for (StrifeAttribute attr : data.attributeRequirement.keySet()) {
      if (champion.getAttributeLevel(attr) < data.attributeRequirement.get(attr)) {
        strings.add(REQ_STR.replace("{REQ}",
            ChatColor.stripColor(attr.getName() + " " + data.attributeRequirement.get(attr))));
      }
    }
    return TextUtils.color(strings);
  }
}