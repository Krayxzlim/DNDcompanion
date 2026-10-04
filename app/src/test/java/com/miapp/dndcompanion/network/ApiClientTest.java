package com.miapp.dndcompanion.network;

import static org.junit.Assert.*;

import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class ApiClientTest {
  @Test
  public void validationExplainsWhichFieldFailed() throws Exception {
    JSONObject error =
        new JSONObject(
            "{\"error\":\"Datos"
                + " inválidos\",\"details\":[{\"path\":\"name\",\"message\":\"Required\"}]}");
    assertEquals("Datos inválidos\nname: Required", ApiClient.errorMessage(error, 400));
  }

  @Test
  public void databaseErrorPreservesServerExplanation() throws Exception {
    assertEquals(
        "Base pendiente",
        ApiClient.errorMessage(new JSONObject("{\"error\":\"Base pendiente\"}"), 503));
    assertEquals("Error HTTP 404", ApiClient.errorMessage(new JSONObject(), 404));
  }
}
