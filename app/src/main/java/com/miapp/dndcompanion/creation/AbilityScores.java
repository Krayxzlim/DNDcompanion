package com.miapp.dndcompanion.creation;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Random;

public final class AbilityScores implements Serializable {
  public enum Method {
    STANDARD,
    POINT_BUY,
    ROLL
  }

  public Method method;
  private final int[] standard = {15, 14, 13, 12, 10, 8};
  private final int[] standardSlots = {-1, -1, -1, -1, -1, -1};
  private final int[] rolledSlots = {-1, -1, -1, -1, -1, -1};
  private final int[] points = {8, 8, 8, 8, 8, 8};
  private int[][] dice;
  private static final int[] COST = {0, 1, 2, 3, 4, 5, 7, 9};

  public static int modifier(int score) {
    return Math.floorDiv(score - 10, 2);
  }

  public int remaining() {
    int spent = 0;
    for (int value : points) spent += COST[value - 8];
    return 27 - spent;
  }

  public boolean adjust(int ability, int delta) {
    int value = points[ability] + delta;
    if (value < 8 || value > 15 || remaining() + COST[points[ability] - 8] - COST[value - 8] < 0)
      return false;
    points[ability] = value;
    return true;
  }

  public void balanced() {
    System.arraycopy(new int[] {13, 13, 13, 12, 12, 12}, 0, points, 0, 6);
  }

  public boolean recommend(int[] order) {
    if (order == null
        || order.length != 6
        || method == null
        || (method == Method.ROLL && !hasRolls())) return false;
    boolean[] seen = new boolean[6];
    for (int index : order) {
      if (index < 0 || index >= 6 || seen[index]) return false;
      seen[index] = true;
    }
    if (method == Method.POINT_BUY) {
      for (int rank = 0; rank < 6; rank++) points[order[rank]] = standard[rank];
    } else {
      Integer[] ranked = {0, 1, 2, 3, 4, 5};
      Arrays.sort(ranked, (left, right) -> Integer.compare(pool(right), pool(left)));
      for (int rank = 0; rank < 6; rank++) slots()[order[rank]] = ranked[rank];
    }
    return true;
  }

  public int ownerOf(int slot) {
    if (method == null || method == Method.POINT_BUY) return -1;
    for (int i = 0; i < 6; i++) if (slots()[i] == slot) return i;
    return -1;
  }

  public boolean hasRolls() {
    return dice != null;
  }

  public void roll(Random random) {
    dice = new int[6][4];
    for (int[] row : dice) for (int i = 0; i < 4; i++) row[i] = random.nextInt(6) + 1;
    Arrays.fill(rolledSlots, -1);
  }

  public int[] dice(int slot) {
    return dice[slot].clone();
  }

  public int pool(int slot) {
    if (method == Method.STANDARD) return standard[slot];
    if (dice == null) return 0;
    int sum = 0, min = 6;
    for (int n : dice[slot]) {
      sum += n;
      min = Math.min(min, n);
    }
    return sum - min;
  }

  private int[] slots() {
    return method == Method.STANDARD ? standardSlots : rolledSlots;
  }

  public int assigned(int ability) {
    return slots()[ability];
  }

  public void assign(int ability, int slot) {
    if (method == null || method == Method.POINT_BUY || slot < -1 || slot > 5)
      throw new IllegalArgumentException();
    int[] assigned = slots();
    int previous = assigned[ability];
    if (slot >= 0)
      for (int i = 0; i < 6; i++) if (i != ability && assigned[i] == slot) assigned[i] = previous;
    assigned[ability] = slot;
  }

  public int score(int ability) {
    return method == Method.POINT_BUY
        ? points[ability]
        : slots()[ability] < 0 ? 0 : pool(slots()[ability]);
  }

  public boolean complete() {
    if (method == null) return false;
    if (method == Method.POINT_BUY) return remaining() == 0;
    if (method == Method.ROLL && dice == null) return false;
    for (int slot : slots()) if (slot < 0) return false;
    return true;
  }
}
