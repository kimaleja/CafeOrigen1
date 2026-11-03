package com.cafeorigen.auth;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.cafeorigen.R;

/**
 * CafeOrigen — Contenedor del flujo de autenticación (T2/T3/T6).
 * Aloja Login, Registro y Recuperación de contraseña con reemplazos de
 * fragmento; la visión completa con nav_graph se activa al integrar
 * Navigation Component en la fase de pantallas por rol.
 */
public class AuthActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);
    }

    /** Reemplaza el fragmento actual del contenedor de autenticación. */
    public void showFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.auth_fragment_container, fragment)
                .commit();
    }
}
