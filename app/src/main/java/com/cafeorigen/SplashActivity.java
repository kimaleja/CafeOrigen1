package com.cafeorigen;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.cafeorigen.auth.AuthActivity;
import com.cafeorigen.core.data.SessionManager;

/**
 * CafeOrigen — Splash (T1). Tras la animación de marca enruta según sesion:
 * con sesión activa → MainActivity (respeta biometría en el próximo arranque
 * desde la propia pantalla de login); sin sesión → AuthActivity (Login).
 */
public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DURATION_MS = 1400L;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        new Handler(Looper.getMainLooper()).postDelayed(this::navigateAccordingToSession, SPLASH_DURATION_MS);
    }

    private void navigateAccordingToSession() {
        Intent destination;
        if (SessionManager.get(this).isLoggedIn()) {
            destination = new Intent(this, MainActivity.class);
        } else {
            destination = new Intent(this, AuthActivity.class);
        }
        startActivity(destination);
        finish();
    }
}
