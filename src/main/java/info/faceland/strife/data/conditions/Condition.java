package info.faceland.strife.data.conditions;

import info.faceland.strife.data.StrifeMob;

public interface Condition {

  boolean isMet(StrifeMob attacker, StrifeMob target);

  CompareTarget getCompareTarget();

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
    EQUIPMENT,
    BUFF,
    ENDLESS_EFFECT,
    BLOCKING,
    MOVING,
    IN_COMBAT,
    CHANCE,
    STAT,
    LIGHT_LEVEL,
    HEALTH,
    BARRIER,
    POTION_EFFECT,
    TIME,
    LEVEL,
    BONUS_LEVEL,
    ITS_OVER_ANAKIN,
    ENTITY_TYPE,
    GROUNDED,
    BLEEDING,
    DARKNESS,
    BURNING,
    EARTH_RUNES
  }
}