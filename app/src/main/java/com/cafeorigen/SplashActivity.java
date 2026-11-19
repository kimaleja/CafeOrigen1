package com.cafeorigen;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.cafeorigen.auth.AuthActivity;
import com.cafeorigen.core.data.SessionManager;
import com.cafeorigen.core.data.firebase.FirebaseBootstrap;

/**
 * CafeOrigen — Splash (T1). Tras la animación de marca enruta según sesion:
 * con sesión activa → MainActivity (respeta biometría en el próximo arranque
 * desde la propia pantalla de login); sin sesión → AuthActivity (Login).
 */
public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DURATION_MS = 1400L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    /** Evita enrutar dos veces si el handler y el ciclo de vida se cruzan. */
    private boolean navigated;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        // Arranque de Firebase (idempotente): detecta google-services.json y
        // deja operativa la capa de datos. Sin el archivo, modo mock.
        FirebaseBootstrap.init(this);
        handler.postDelayed(this::navigateAccordingToSession, SPLASH_DURATION_MS);
    }

    private void navigateAccordingToSession() {
        if (navigated || isFinishing()) {
            return;
        }
        navigated = true;
        Intent destination;
        if (SessionManager.get(this).isLoggedIn()) {
            destination = new Intent(this, MainActivity.class);
        } else {
            destination = new Intent(this, AuthActivity.class);
        }
        startActivity(destination);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Sin esto, una rotación o cierre durante el splash dejaría un enrutado
        // pendiente sobre una Activity destruida.
        handler.removeCallbacksAndMessages(null);
    }
}
