package com.miapp.dndcompanion.creation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public final class ClassSkillChoice implements Serializable {
  public String classKey = "";
  public int count;
  public final List<String> keys = new ArrayList<>(),
      names = new ArrayList<>(),
      selected = new ArrayList<>();

  public boolean valid() {
    return count > 0
        && selected.size() == count
        && new java.util.HashSet<>(selected).size() == count
        && keys.containsAll(selected);
  }

  public void clear() {
    classKey = "";
    count = 0;
    keys.clear();
    names.clear();
    selected.clear();
  }
}
