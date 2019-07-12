package info.faceland.strife.conditions;

import info.faceland.strife.data.StrifeMob;

public interface Condition {

  boolean isMet(StrifeMob attacker, StrifeMob target);

  enum Comparison {
    GREATER_THAN,
    LESS_THAN,
    EQUAL,
    NONE
  }

  enum CompareTarget {
    SELF,
    OTHER
  }

  enum ConditionType {
    ATTRIBUTE,
    CHANCE,
    STAT,
    HEALTH,
    BARRIER,
    POTION_EFFECT,
    LEVEL,
    BONUS_LEVEL,
    ITS_OVER_ANAKIN,
    ENTITY_TYPE,
    GROUNDED,
    BLEEDING,
    DARKNESS,
    BURNING
  }
}
