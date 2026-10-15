package com.miapp.dndcompanion.creation;

import java.io.Serializable;
import java.util.Arrays;

public final class BackgroundChoice implements Serializable {
  public String key = "", name = "", details = "";
  public int[] allowed = new int[0];
  public final int[] increases = new int[6];
  public boolean deferred;

  public void select(String key, String name, String details, int[] allowed) {
    this.key = key;
    this.name = name;
    this.details = details;
    this.allowed = allowed.clone();
    deferred = false;
    Arrays.fill(increases, 0);
  }

  public void defer() {
    select("", "", "", new int[0]);
    deferred = true;
  }

  public boolean valid() {
    if (key.isEmpty() || allowed.length != 3) return false;
    boolean[] seen = new boolean[6];
    for (int index : allowed) {
      if (index < 0 || index >= 6 || seen[index]) return false;
      seen[index] = true;
    }
    int sum = 0;
    for (int i = 0; i < 6; i++) {
      if (increases[i] < 0 || increases[i] > 2 || (increases[i] > 0 && !seen[i])) return false;
      sum += increases[i];
    }
    return sum == 3;
  }
}
