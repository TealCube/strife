package land.face.strife.data;

import java.util.Map;
import java.util.Set;
import land.face.strife.stats.StrifeStat;
import land.face.strife.stats.StrifeTrait;
import org.bukkit.inventory.ItemStack;

public class LevelPath {

  public static final Path[] PATH_VALUES = Path.values();

  private final Map<StrifeStat, Float> stats;
  private final Set<StrifeTrait> traits;
  private final ItemStack displayItem;

  public LevelPath(ItemStack displayItem, Map<StrifeStat, Float> stats, Set<StrifeTrait> traits) {
    this.displayItem = displayItem;
    this.traits = traits;
    this.stats = stats;
  }

  public Map<StrifeStat, Float> getStats() {
    return stats;
  }

  public Set<StrifeTrait> getTraits() {
    return traits;
  }

  public ItemStack getDisplayItem() {
    return displayItem;
  }

  public enum Path {
    ONE,
    TWO,
    THREE,
    FOUR,
    FIVE,
    SIX,
    SEVEN,
    EIGHT,
    NINE
  }

  public enum Choice {
    OPTION_1,
    OPTION_2,
    OPTION_3
  }
}
