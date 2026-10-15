package com.miapp.dndcompanion.creation;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

public final class CharacterDraft implements Serializable {
  public enum Step {
    SPECIES,
    CLASS,
    METHOD,
    ALLOCATION,
    BACKGROUND,
    SPECIES_GUIDE,
    CLASS_GUIDE,
    IDENTITY,
    REVIEW
  }

  public static final String[] CLASSES = {
    "Bárbaro",
    "Bardo",
    "Clérigo",
    "Druida",
    "Guerrero",
    "Mago",
    "Monje",
    "Paladín",
    "Explorador",
    "Pícaro",
    "Hechicero",
    "Brujo"
  };
  public static final String[] CLASS_KEYS = {
    "barbarian",
    "bard",
    "cleric",
    "druid",
    "fighter",
    "wizard",
    "monk",
    "paladin",
    "ranger",
    "rogue",
    "sorcerer",
    "warlock"
  };
  public final BackgroundChoice background = new BackgroundChoice();

  public int finalScore(int index) {
    return abilities.score(index) + (background.key.isEmpty() ? 0 : background.increases[index]);
  }

  public static final String[] ALIGNMENTS = {
    "Legal Bueno",
    "Neutral Bueno",
    "Caótico Bueno",
    "Legal Neutral",
    "Neutral Verdadero",
    "Caótico Neutral",
    "Legal Malvado",
    "Neutral Malvado",
    "Caótico Malvado"
  };
  public static final String[] ABILITIES = {
    "Fuerza", "Destreza", "Constitución", "Inteligencia", "Sabiduría", "Carisma"
  };
  public static final String[] KEYS = {
    "strength", "dexterity", "constitution", "intelligence", "wisdom", "charisma"
  };
  public Step step = Step.SPECIES;
  public String speciesKey = "", speciesName = "", speciesDescription = "", name = "";
  public int classIndex = -1, alignment = 4, level = 1;
  public final AbilityScores abilities = new AbilityScores();

  public boolean canAdvance() {
    switch (step) {
      case SPECIES:
        return !speciesKey.isEmpty();
      case CLASS:
        return classIndex >= 0;
      case METHOD:
        return abilities.method != null;
      case ALLOCATION:
        return abilities.complete();
      case BACKGROUND:
        return background.deferred || background.valid();
      case IDENTITY:
        return !name.trim().isEmpty();
      default:
        return true;
    }
  }

  public void next() {
    if (canAdvance() && step != Step.REVIEW) step = Step.values()[step.ordinal() + 1];
  }

  public void back() {
    if (step.ordinal() > 0) step = Step.values()[step.ordinal() - 1];
  }

  public Map<String, Object> payload() {
    if (speciesKey.isEmpty()
        || classIndex < 0
        || classIndex >= CLASSES.length
        || name.trim().isEmpty()
        || name.trim().length() > 160
        || !abilities.complete()
        || level < 1
        || level > 20
        || (!background.key.isEmpty() && (!background.valid() || level != 1)))
      throw new IllegalStateException("Completá los pasos antes de crear.");
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("name", name.trim());
    result.put("race", speciesName);
    result.put("characterClass", CLASSES[classIndex]);
    result.put("level", level);
    result.put("alignment", ALIGNMENTS[alignment]);
    for (int i = 0; i < 6; i++) result.put(KEYS[i], finalScore(i));
    if (!background.key.isEmpty()) {
      Map<String, Object> creation = new LinkedHashMap<>();
      creation.put("version", 1);
      creation.put("ruleset", "srd-2024");
      creation.put("speciesKey", speciesKey);
      creation.put("classKey", CLASS_KEYS[classIndex]);
      creation.put(
          "method",
          abilities.method == AbilityScores.Method.POINT_BUY
              ? "pointBuy"
              : abilities.method == AbilityScores.Method.ROLL ? "rolled" : "standard");
      Map<String, Object> base = new LinkedHashMap<>(), increases = new LinkedHashMap<>();
      for (int i = 0; i < 6; i++) {
        base.put(KEYS[i], abilities.score(i));
        increases.put(KEYS[i], background.increases[i]);
      }
      creation.put("baseScores", base);
      if (abilities.method == AbilityScores.Method.ROLL) {
        java.util.List<java.util.List<Integer>> rolls = new java.util.ArrayList<>();
        for (int i = 0; i < 6; i++) {
          java.util.List<Integer> row = new java.util.ArrayList<>();
          for (int value : abilities.dice(i)) row.add(value);
          rolls.add(row);
        }
        creation.put("rolls", rolls);
      }
      Map<String, Object> choice = new LinkedHashMap<>();
      choice.put("key", background.key);
      choice.put("increases", increases);
      creation.put("background", choice);
      result.put("creation", creation);
    }
    return result;
  }
}
