package com.miapp.dndcompanion.creation;

import static org.junit.Assert.*;

import java.io.*;
import java.util.*;
import org.junit.Test;

public class AbilityScoresTest {
  @Test
  public void standardSlotsSwapAndMethodsKeepTheirDrafts() {
    AbilityScores a = new AbilityScores();
    a.method = AbilityScores.Method.STANDARD;
    for (int i = 0; i < 6; i++) a.assign(i, i);
    a.assign(0, 5);
    assertEquals(8, a.score(0));
    assertEquals(15, a.score(5));
    assertTrue(a.complete());
    a.method = AbilityScores.Method.POINT_BUY;
    a.balanced();
    assertEquals(0, a.remaining());
    assertTrue(a.complete());
    a.method = AbilityScores.Method.STANDARD;
    assertEquals(8, a.score(0));
  }

  @Test
  public void pointBuyEnforcesBudgetAndNonlinearCosts() {
    AbilityScores a = new AbilityScores();
    a.method = AbilityScores.Method.POINT_BUY;
    for (int i = 0; i < 3; i++) for (int j = 0; j < 7; j++) assertTrue(a.adjust(i, 1));
    assertEquals(0, a.remaining());
    assertFalse(a.adjust(3, 1));
    assertFalse(a.adjust(0, 1));
    assertTrue(a.adjust(0, -1));
    assertEquals(2, a.remaining());
    assertTrue(a.adjust(3, 1));
    assertEquals(1, a.remaining());
    assertFalse(a.adjust(4, -1));
    assertFalse(a.complete());
  }

  @Test
  public void duplicateRollTotalsRetainDistinctDiceAndAssignments() {
    AbilityScores a = new AbilityScores();
    a.method = AbilityScores.Method.ROLL;
    a.roll(
        new Random() {
          public int nextInt(int bound) {
            return 2;
          }
        });
    for (int i = 0; i < 6; i++) {
      assertEquals(9, a.pool(i));
      a.assign(i, i);
    }
    assertTrue(a.complete());
    a.assign(0, 1);
    assertEquals(0, a.assigned(1));
    int[] copy = a.dice(0);
    copy[0] = 6;
    assertEquals(3, a.dice(0)[0]);
    a.roll(new Random(42));
    assertFalse(a.complete());
    for (int i = 0; i < 6; i++) {
      int[] dice = a.dice(i);
      Arrays.sort(dice);
      assertEquals(dice[1] + dice[2] + dice[3], a.pool(i));
    }
  }

  @Test
  public void modifiersRoundDown() {
    assertEquals(-4, AbilityScores.modifier(3));
    assertEquals(-1, AbilityScores.modifier(9));
    assertEquals(2, AbilityScores.modifier(15));
  }

  @Test
  public void draftRoundTripKeepsEveryMethodAndPayloadIsStrict() throws Exception {
    CharacterDraft d = new CharacterDraft();
    d.speciesKey = "elf";
    d.speciesName = "Elfo";
    d.classIndex = 5;
    d.name = " Ada ";
    d.abilities.method = AbilityScores.Method.STANDARD;
    for (int i = 0; i < 6; i++) d.abilities.assign(i, 5 - i);
    d.abilities.method = AbilityScores.Method.POINT_BUY;
    d.abilities.balanced();
    d.step = CharacterDraft.Step.REVIEW;
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    new ObjectOutputStream(bytes).writeObject(d);
    CharacterDraft restored =
        (CharacterDraft)
            new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray())).readObject();
    assertEquals("Ada", restored.payload().get("name"));
    assertEquals(11, restored.payload().size());
    assertEquals(1, restored.payload().get("level"));
    restored.abilities.method = AbilityScores.Method.STANDARD;
    assertEquals(8, restored.abilities.score(0));
    restored.back();
    assertEquals(CharacterDraft.Step.IDENTITY, restored.step);
  }

  @Test(expected = IllegalStateException.class)
  public void incompleteDraftCannotSubmit() {
    new CharacterDraft().payload();
  }
}
