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
package info.faceland.strife.events;

import info.faceland.strife.data.StrifeMob;
import info.faceland.strife.stats.StrifeStat;
import info.faceland.strife.util.DamageUtil.AttackType;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.entity.Projectile;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class StrifeDamageEvent extends Event implements Cancellable {

  private static final HandlerList HANDLER_LIST = new HandlerList();

  public static HandlerList getHandlerList() {
    return HANDLER_LIST;
  }

  private final StrifeMob attacker;
  private final StrifeMob defender;
  private final AttackType attackType;

  private double attackMultiplier = 1;
  private double healMultiplier = 1;
  private boolean isBlocking = false;
  private Projectile projectile;
  private String[] extraEffects;
  private final Map<StrifeStat, Double> attackerBonuses = new HashMap<>();
  private final Map<StrifeStat, Double> defenderBonuses = new HashMap<>();
  private boolean cancel;

  public StrifeDamageEvent(StrifeMob attacker, StrifeMob defender, AttackType attackType) {
    this.attacker = attacker;
    this.defender = defender;
    this.attackType = attackType;
  }

  @Override
  public HandlerList getHandlers() {
    return HANDLER_LIST;
  }

  public StrifeMob getAttacker() {
    return attacker;
  }

  public StrifeMob getDefender() {
    return defender;
  }

  public AttackType getAttackType() {
    return attackType;
  }

  public double getAttackMultiplier() {
    return attackMultiplier;
  }

  public void setAttackMultiplier(double attackMultiplier) {
    this.attackMultiplier = attackMultiplier;
  }

  public double getHealMultiplier() {
    return healMultiplier;
  }

  public void setHealMultiplier(double healMultiplier) {
    this.healMultiplier = healMultiplier;
  }

  public boolean isBlocking() {
    return isBlocking;
  }

  public void setBlocking(boolean blocking) {
    isBlocking = blocking;
  }

  public Projectile getProjectile() {
    return projectile;
  }

  public void setProjectile(Projectile projectile) {
    this.projectile = projectile;
  }

  public String[] getExtraEffects() {
    return extraEffects;
  }

  public void setExtraEffects(String[] extraEffects) {
    this.extraEffects = extraEffects;
  }

  public Map<StrifeStat, Double> getAttackerBonuses() {
    return attackerBonuses;
  }

  public Map<StrifeStat, Double> getDefenderBonuses() {
    return defenderBonuses;
  }

  public void setCancelled(boolean cancel) {
    this.cancel = cancel;
  }

  public boolean isCancelled() {
    return this.cancel;
  }
}