package com.miapp.dndcompanion.creation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/** La elección conserva opciones del catálogo; los valores de ficha los calcula la API. */
public final class StartingDefenseChoice implements Serializable {
  public boolean supported, shieldAllowed, shield;
  public String classKey = "", armorKey = "";
  public int classGold;
  public final List<String> keys = new ArrayList<>(),
      names = new ArrayList<>(),
      details = new ArrayList<>();
  public final List<Integer> costs = new ArrayList<>();

  public void clearSelection() {
    armorKey = "";
    shield = false;
  }

  public int budget(BackgroundChoice background) {
    return (classGold + ("B".equals(background.equipmentOption) ? 50 : background.packageGold))
        * 100;
  }

  public int cost() {
    int index = keys.indexOf(armorKey);
    return index < 0 ? -1 : costs.get(index) + (shield ? 1000 : 0);
  }

  public boolean valid(BackgroundChoice background) {
    return cost() >= 0 && cost() <= budget(background) && (!shield || shieldAllowed);
  }
}
