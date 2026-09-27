package com.miapp.dndcompanion;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

/** Legacy entry point into the character-scoped Fragua experience. */
public class InventarioActivity extends AppCompatActivity {
  @Override
  protected void onCreate(Bundle state) {
    super.onCreate(state);
    startActivity(new Intent(this, MainActivity.class).putExtra("screen", "inventory"));
    finish();
  }
}
