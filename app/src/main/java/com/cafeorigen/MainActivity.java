package com.cafeorigen;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.IdRes;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.cafeorigen.admin.AdminDashboardFragment;
import com.cafeorigen.admin.AdminProductListFragment;
import com.cafeorigen.admin.AdminUserListFragment;
import com.cafeorigen.admin.VisualDemoFragment;
import com.cafeorigen.auth.AuthActivity;
import com.cafeorigen.buyer.BuyerCartFragment;
import com.cafeorigen.buyer.BuyerCatalogFragment;
import com.cafeorigen.buyer.BuyerHomeFragment;
import com.cafeorigen.buyer.BuyerOrdersFragment;
import com.cafeorigen.buyer.BuyerProfileFragment;
import com.cafeorigen.core.data.SessionManager;
import com.cafeorigen.core.model.ProductCategory;
import com.cafeorigen.core.model.Role;
import com.cafeorigen.databinding.ActivityMainBinding;
import com.cafeorigen.vendor.VendorDashboardFragment;
import com.cafeorigen.vendor.VendorOrdersFragment;
import com.cafeorigen.vendor.VendorProductsFragment;
import com.cafeorigen.vendor.VendorProfileFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

/**
 * CafeOrigen — Actividad principal (T5) y control de acceso por rol (T6).
 * Ruta según el rol de la sesión; en ADMIN conecta el dashboard real con sus
 * sub-pantallas (usuarios, productos, reporte). Comprador y Vendedor muestran
 * la demo del sistema visual hasta su fase.
 */
public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    /**
     * Guarda contra re-entrada: setSelectedItemId vuelve a disparar el listener
     * incluso cuando el tab ya está seleccionado (crash real detectado en
     * emulador: StackOverflowError por recursión showRoot ↔ handleRoleNav).
     * Cuando showRoot/los saltos programáticos marcan el tab por código, se
     * suprime el callback para que no vuelva a navegar.
     */
    private boolean suppressNavCallback;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Guardia anti acceso sin sesión
        SessionManager session = SessionManager.get(this);
        if (!session.isLoggedIn()) {
            startActivity(new Intent(this, AuthActivity.class));
            finish();
            return;
        }

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        bindToolbarActions();
        applyRoleNavigation(session.getRole());

        // Primer destino por rol
        if (session.getRole() == Role.ADMINISTRADOR) {
            showRoot(new AdminDashboardFragment(), R.id.nav_dashboard);
        } else if (session.getRole() == Role.VENDEDOR) {
            showRoot(new VendorDashboardFragment(), R.id.nav_inicio);
        } else if (session.getRole() == Role.COMPRADOR) {
            showRoot(new BuyerHomeFragment(), R.id.nav_inicio);
        } else {
            showRoot(new VisualDemoFragment(), 0);
            Snackbar.make(binding.bottomNavView,
                    getString(R.string.demo_pending_role, session.getRole().getLabel()),
                    Snackbar.LENGTH_LONG).show();
        }
    }

    // ============================ Navegación por rol ============================

    private void applyRoleNavigation(Role role) {
        int menuRes;
        switch (role) {
            case ADMINISTRADOR:
                menuRes = R.menu.menu_nav_admin;
                break;
            case VENDEDOR:
                menuRes = R.menu.menu_nav_vendedor;
                break;
            case COMPRADOR:
            default:
                menuRes = R.menu.menu_nav_comprador;
                break;
        }
        binding.bottomNavView.getMenu().clear();
        binding.bottomNavView.inflateMenu(menuRes);
        binding.bottomNavView.setOnItemSelectedListener(item -> {
            if (suppressNavCallback) {
                // Selección aplicada por código (showRoot / openRoot*): el item
                // queda marcado pero NO se re-dispara el ruteo (evita el bucle).
                return true;
            }
            @IdRes int id = item.getItemId();
            Role currentRole = SessionManager.get(this).getRole();
            // El ruteo depende del rol de la sesión (control de acceso T6).
            if (currentRole == Role.VENDEDOR) {
                return handleVendorNav(id, String.valueOf(item.getTitle()));
            }
            if (currentRole == Role.COMPRADOR) {
                return handleBuyerNav(id, String.valueOf(item.getTitle()));
            }
            return handleAdminNav(id, String.valueOf(item.getTitle()));
        });
    }

    /**
     * Ruteo de la navegación inferior del Comprador (C1–C6).
     * Nunca contiene ids de admin (nav_dashboard/nav_usuarios) ni entra a las
     * pantallas de vendedor; el historial usa su propio tab de pedidos.
     */
    private boolean handleBuyerNav(int itemId, String title) {
        if (SessionManager.get(this).getRole() != Role.COMPRADOR) {
            showModuleInProgress(title);
            return false;
        }
        if (itemId == R.id.nav_inicio) {
            showRoot(new BuyerHomeFragment(), itemId);
            return true;
        }
        if (itemId == R.id.nav_catalogo) {
            showRoot(BuyerCatalogFragment.newInstance(null), itemId);
            return true;
        }
        if (itemId == R.id.nav_carrito) {
            showRoot(new BuyerCartFragment(), itemId);
            return true;
        }
        if (itemId == R.id.nav_pedidos) {
            showRoot(new BuyerOrdersFragment(), itemId);
            return true;
        }
        if (itemId == R.id.nav_perfil) {
            showRoot(new BuyerProfileFragment(), itemId);
            return true;
        }
        showModuleInProgress(title);
        return false;
    }

    /**
     * Ruteo de la navegación inferior del Vendedor (V1–V8).
     * Los ids de admin (nav_dashboard/nav_usuarios) nunca llegan aquí: el menú
     * del vendedor no los contiene, y esta función ignora cualquier id extra.
     */
    private boolean handleVendorNav(int itemId, String title) {
        if (SessionManager.get(this).getRole() != Role.VENDEDOR) {
            showModuleInProgress(title);
            return false;
        }
        if (itemId == R.id.nav_inicio) {
            showRoot(new VendorDashboardFragment(), itemId);
            return true;
        }
        if (itemId == R.id.nav_productos) {
            showRoot(new VendorProductsFragment(), itemId);
            return true;
        }
        if (itemId == R.id.nav_pedidos) {
            showRoot(new VendorOrdersFragment(), itemId);
            return true;
        }
        if (itemId == R.id.nav_perfil) {
            showRoot(new VendorProfileFragment(), itemId);
            return true;
        }
        showModuleInProgress(title);
        return false;
    }

    /** Ruteo de la navegación inferior del Administrador (A1–A8). */
    private boolean handleAdminNav(int itemId, String title) {
        if (SessionManager.get(this).getRole() != Role.ADMINISTRADOR) {
            showModuleInProgress(title);
            return false;
        }
        if (itemId == R.id.nav_dashboard) {
            showRoot(new AdminDashboardFragment(), itemId);
            return true;
        }
        if (itemId == R.id.nav_usuarios) {
            showRoot(new AdminUserListFragment(), itemId);
            return true;
        }
        if (itemId == R.id.nav_productos) {
            showRoot(new AdminProductListFragment(), itemId);
            return true;
        }
        // Pedidos y Perfil del Admin: fases siguientes (el ítem no queda huérfano)
        showModuleInProgress(title);
        return false;
    }

    /** Cambia a un fragmento raíz (limpia la pila de sub-pantallas). */
    private void showRoot(Fragment fragment, @IdRes int checkedItem) {
        getSupportFragmentManager()
                .popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.main_fragment_container, fragment)
                .commit();
        if (checkedItem != 0) {
            selectTab(checkedItem);
        }
    }

    /**
     * Marca el tab por código sin re-disparar el listener de navegación.
     * Único punto donde se usa setSelectedItemId (guarda anti re-entrada).
     */
    private void selectTab(@IdRes int itemId) {
        suppressNavCallback = true;
        binding.bottomNavView.setSelectedItemId(itemId);
        suppressNavCallback = false;
    }

    /** Abre sub-pantallas (formulario, reporte) manteniendo el tab actual. */
    public void openSub(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.main_fragment_container, fragment)
                .addToBackStack(null)
                .commit();
    }

    /** Título central de la app bar, actualizado por cada fragmento. */
    public void setToolbarTitle(CharSequence title) {
        binding.mainToolbar.setTitle(title);
    }

    /** Salto programático al tab de productos (alerta de stock del panel V1). */
    public void openRootProducts() {
        selectTab(R.id.nav_productos);
    }

    /** Catálogo del comprador prefiltrado por categoría (C1 → C2). */
    public void openRootCatalog(ProductCategory category) {
        showRoot(BuyerCatalogFragment.newInstance(category), R.id.nav_catalogo);
    }

    /** Carrito del comprador (acciones "Ver carrito"). */
    public void openRootCart() {
        selectTab(R.id.nav_carrito);
    }

    /** Historial del comprador (tras confirmar la compra). */
    public void openRootOrders() {
        selectTab(R.id.nav_pedidos);
    }

    /** Cierre de sesión pedido desde el perfil (V8), con confirmación. */
    public void requestLogout() {
        confirmLogout();
    }

    // ============================ Acciones transversales ============================

    private void bindToolbarActions() {
        binding.mainToolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_logout) {
                confirmLogout();
                return true;
            }
            showModuleInProgress(String.valueOf(item.getTitle()));
            return true;
        });
    }

    private void showModuleInProgress(String title) {
        Snackbar.make(binding.bottomNavView,
                getString(R.string.module_in_progress, title), Snackbar.LENGTH_SHORT).show();
    }

    private void confirmLogout() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.logout_confirm_title)
                .setMessage(R.string.logout_confirm_message)
                .setPositiveButton(R.string.logout_confirm_positive, (dialog, which) -> logout())
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void logout() {
        SessionManager.get(this).clearSession();
        Intent intent = new Intent(this, SplashActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
