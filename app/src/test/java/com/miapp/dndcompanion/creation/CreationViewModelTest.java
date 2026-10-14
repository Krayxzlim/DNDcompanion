package com.miapp.dndcompanion.creation;

import static org.junit.Assert.*;

import com.miapp.dndcompanion.network.ApiClient;
import java.util.concurrent.atomic.AtomicInteger;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class CreationViewModelTest {
  @Test
  public void pendingSubmissionCannotDuplicateAndRetryKeepsDraft() throws Exception {
    CreationViewModel m = new CreationViewModel();
    m.draft.speciesKey = "elf";
    m.draft.speciesName = "Elfo";
    m.draft.classIndex = 5;
    m.draft.name = "Ada";
    m.draft.abilities.method = AbilityScores.Method.POINT_BUY;
    m.draft.abilities.balanced();
    AtomicInteger calls = new AtomicInteger();
    ApiClient.Callback[] callback = new ApiClient.Callback[1];
    CreationViewModel.Submit submit =
        (body, result) -> {
          calls.incrementAndGet();
          callback[0] = result;
          assertEquals(11, body.length());
        };
    m.save(submit);
    m.save(submit);
    assertEquals(1, calls.get());
    assertTrue(m.saving);
    callback[0].failure("Sin conexión", 503);
    assertFalse(m.saving);
    assertEquals("Ada", m.draft.name);
    m.save(submit);
    assertEquals(2, calls.get());
    callback[0].success(new JSONObject().put("id", "created"));
    m.save(submit);
    assertEquals(2, calls.get());
    assertEquals("created", m.createdId);
  }

  @Test
  public void uncertainPostResponseCannotBeSubmittedAgain() {
    for (boolean missingId : new boolean[] {false, true}) {
      CreationViewModel model = new CreationViewModel();
      model.draft.speciesKey = "elf";
      model.draft.speciesName = "Elfo";
      model.draft.classIndex = 5;
      model.draft.name = "Ada";
      model.draft.abilities.method = AbilityScores.Method.POINT_BUY;
      model.draft.abilities.balanced();
      model.save(
          (body, callback) -> {
            if (missingId) callback.success(new JSONObject());
            else callback.failure("Respuesta interrumpida", 0);
          });
      assertFalse(model.saving);
      assertTrue(model.uncertainSubmission);
      model.save((body, callback) -> fail("An uncertain POST must not be repeated"));
    }
  }

  @Test
  public void catalogRefreshAndClassChangeNeverApplyRecommendation() throws Exception {
    CreationViewModel model = new CreationViewModel();
    model.draft.classIndex = 5;
    model.draft.abilities.method = AbilityScores.Method.POINT_BUY;
    model.draft.abilities.balanced();
    com.miapp.dndcompanion.fragua.GameRepository.Result[] result =
        new com.miapp.dndcompanion.fragua.GameRepository.Result[1];
    model.loadCreation(
        (path, callback) -> {
          assertEquals("/catalog/creation", path);
          result[0] = callback;
        });
    JSONObject catalog =
        new JSONObject(
            "{ruleset:'srd-2024',version:1,classes:[{name:'Mago',recommendedOrder:['intelligence','constitution','dexterity','wisdom','charisma','strength']},{name:'Bárbaro',recommendedOrder:['strength','constitution','dexterity','wisdom','charisma','intelligence']}]}");
    result[0].ok(catalog, true, 0);
    assertEquals(3, model.recommendedOrder()[0]);
    assertEquals(13, model.draft.abilities.score(0));
    model.draft.classIndex = 0;
    result[0].ok(catalog, false, 1);
    assertEquals(0, model.recommendedOrder()[0]);
    assertEquals(13, model.draft.abilities.score(0));
    result[0].error("offline", 0);
    assertNotNull(model.selectedClass());
    assertEquals(0, model.draft.abilities.remaining());
  }

  @Test
  public void unavailableOrInvalidCreationCatalogDoesNotBlockDraft() throws Exception {
    CreationViewModel model = new CreationViewModel();
    model.draft.classIndex = 5;
    model.loadCreation((path, callback) -> callback.error("Not found", 404));
    assertNull(model.recommendedOrder());
    model.draft.step = CharacterDraft.Step.CLASS;
    assertTrue(model.draft.canAdvance());
    model.creation =
        new JSONObject(
            "{classes:[{name:'Mago',recommendedOrder:['strength','strength','dexterity','wisdom','charisma','intelligence']}]}");
    assertNull(model.recommendedOrder());
  }

  @Test
  public void catalogFailureNeverMarksSubmissionUncertainAndAuthExpires() {
    CreationViewModel model = new CreationViewModel();
    com.miapp.dndcompanion.fragua.GameRepository.Result[] callback =
        new com.miapp.dndcompanion.fragua.GameRepository.Result[1];
    model.load((path, result) -> callback[0] = result);
    callback[0].error("Sin conexión", 0);
    assertFalse(model.loading);
    assertFalse(model.uncertainSubmission);
    assertEquals(0, model.errorStatus);
    model.load((path, result) -> callback[0] = result);
    callback[0].error("Sesión vencida", 401);
    assertEquals(401, model.errorStatus);
  }
}
