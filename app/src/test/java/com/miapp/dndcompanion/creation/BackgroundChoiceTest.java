package com.miapp.dndcompanion.creation;

import static org.junit.Assert.*;

import java.io.*;
import java.util.*;
import org.junit.Test;

public class BackgroundChoiceTest {
  private CharacterDraft draft() {
    CharacterDraft d = new CharacterDraft();
    d.speciesKey = "srd-2024_elf";
    d.speciesName = "Elf";
    d.name = "Ada";
    d.classIndex = 5;
    d.abilities.method = AbilityScores.Method.STANDARD;
    d.abilities.recommend(new int[] {3, 2, 1, 4, 5, 0});
    d.background.select("srd-2024_sage", "Sabio", "", new int[] {2, 3, 4});
    return d;
  }

  @Test
  public void payloadSeparatesBaseFromFinalAndSurvivesSerialization() throws Exception {
    CharacterDraft d = draft();
    d.background.increases[3] = 2;
    d.background.increases[2] = 1;
    assertTrue(d.background.valid());
    assertEquals(17, d.finalScore(3));
    assertEquals(15, d.abilities.score(3));
    Map<String, Object> first = d.payload();
    assertEquals(17, first.get("intelligence"));
    Map<String, Object> creation = (Map<String, Object>) first.get("creation");
    assertEquals(15, ((Map<String, Object>) creation.get("baseScores")).get("intelligence"));
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    new ObjectOutputStream(bytes).writeObject(d);
    CharacterDraft restored =
        (CharacterDraft)
            new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray())).readObject();
    assertEquals(first, restored.payload());
    assertEquals(first, d.payload());
    d.classIndex = 1;
    assertEquals(17, d.finalScore(3));
    assertEquals("bard", ((Map<String, Object>) d.payload().get("creation")).get("classKey"));
    d.level = 2;
    assertThrows(IllegalStateException.class, d::payload);
  }

  @Test
  public void invalidChoicesCannotAdvanceAndChangingBackgroundClearsOnlyBoosts() {
    CharacterDraft d = draft();
    d.step = CharacterDraft.Step.BACKGROUND;
    assertFalse(d.canAdvance());
    d.background.increases[3] = 2;
    assertFalse(d.canAdvance());
    d.background.increases[0] = 1;
    assertFalse(d.canAdvance());
    d.background.increases[0] = 0;
    d.background.increases[4] = 1;
    assertTrue(d.canAdvance());
    d.background.select("srd-2024_soldier", "Soldado", "", new int[] {0, 1, 2});
    assertFalse(d.canAdvance());
    assertEquals(15, d.abilities.score(3));
    assertEquals(15, d.finalScore(3));
    d.background.defer();
    assertTrue(d.canAdvance());
    assertFalse(d.payload().containsKey("creation"));
    assertEquals(11, d.payload().size());
  }

  @Test
  public void rollsIncludeEveryDieAndPreserveDuplicateResults() {
    CharacterDraft d = draft();
    d.abilities.method = AbilityScores.Method.ROLL;
    d.abilities.roll(
        new Random() {
          public int nextInt(int bound) {
            return 2;
          }
        });
    for (int i = 0; i < 6; i++) d.abilities.assign(i, i);
    for (int i : d.background.allowed) d.background.increases[i] = 1;
    Map<String, Object> creation = (Map<String, Object>) d.payload().get("creation");
    List<List<Integer>> rolls = (List<List<Integer>>) creation.get("rolls");
    assertEquals(6, rolls.size());
    assertEquals(Arrays.asList(3, 3, 3, 3), rolls.get(0));
    assertEquals(10, d.finalScore(3));
    assertEquals(9, d.abilities.score(3));
  }
}
