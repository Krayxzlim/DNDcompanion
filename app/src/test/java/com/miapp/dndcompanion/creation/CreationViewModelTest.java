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
