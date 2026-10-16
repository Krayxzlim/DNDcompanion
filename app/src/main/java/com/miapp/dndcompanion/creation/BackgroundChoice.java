package com.miapp.dndcompanion.creation;

import java.io.Serializable;
import java.util.Arrays;

public final class BackgroundChoice implements Serializable {
  public final java.util.List<String> skillKeys = new java.util.ArrayList<>();
  public String key = "", name = "", details = "";
  public int[] allowed = new int[0];
  public final int[] increases = new int[6];
  public boolean deferred, skillGrantsApplied, loadoutSupported, toolRequired;
  public String equipmentOption = "", toolChoice = "", packageDetails = "";
  public int packageGold;
  public boolean featSupported;
  public String featKey = "",
      featName = "",
      featDescription = "",
      featList = "",
      spellOptionsJson = "[]";
  public String castingAbility = "", cantrip1 = "", cantrip2 = "", spell = "";

  public boolean featValid() {
    return !featSupported
        || !featKey.equals("magicInitiate")
        || (Arrays.asList("intelligence", "wisdom", "charisma").contains(castingAbility)
            && !cantrip1.isEmpty()
            && !cantrip2.isEmpty()
            && !cantrip1.equals(cantrip2)
            && !spell.isEmpty());
  }

  public String[] toolKeys = new String[0], toolNames = new String[0];

  public boolean loadoutValid() {
    return !loadoutSupported
        || ((!equipmentOption.isEmpty()) && Arrays.asList(toolKeys).contains(toolChoice));
  }

  public void select(String key, String name, String details, int[] allowed) {
    this.key = key;
    this.name = name;
    this.details = details;
    this.allowed = allowed.clone();
    deferred = false;
    skillGrantsApplied = false;
    skillKeys.clear();
    featSupported = false;
    featKey = "";
    featName = "";
    featDescription = "";
    featList = "";
    spellOptionsJson = "[]";
    castingAbility = "";
    cantrip1 = "";
    cantrip2 = "";
    spell = "";
    loadoutSupported = false;
    toolRequired = false;
    equipmentOption = "";
    toolChoice = "";
    packageDetails = "";
    toolKeys = new String[0];
    toolNames = new String[0];
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
