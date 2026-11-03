package com.cafeorigen.admin;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.cafeorigen.core.data.AdminDataRepository;
import com.cafeorigen.core.data.UserRepository;
import com.cafeorigen.core.model.Role;
import com.cafeorigen.core.model.User;

import java.util.ArrayList;
import java.util.List;

/**
 * CafeOrigen — ViewModel de gestión de usuarios (A2–A5).
 * Estado de listado (con búsqueda y filtro por rol) y operaciones de
 * guardado/borrado lógico expuestas como LiveData.
 */
public class AdminUsersViewModel extends ViewModel {

    /** Códigos de operación para snacks/mensajes. */
    public enum OpsStatus { IDLE, SAVED, DELETED, EMAIL_TAKEN, ERROR }

    public static class OpsState {
        public final OpsStatus status;
        public final String userName; // para el mensaje con formato

        OpsState(OpsStatus status, String userName) {
            this.status = status;
            this.userName = userName;
        }
    }

    public static class Draft {
        public final String name;
        public final String email;
        public final String phone;
        public final String password; // vacía → sin cambios en edición
        public final Role role;
        public final boolean active;

        public Draft(String name, String email, String phone, String password, Role role, boolean active) {
            this.name = name;
            this.email = email;
            this.phone = phone;
            this.password = password;
            this.role = role;
            this.active = active;
        }
    }

    private final AdminDataRepository repository = AdminDataRepository.get();

    private final MutableLiveData<List<User>> visibleUsers = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<OpsState> opsState = new MutableLiveData<>(new OpsState(OpsStatus.IDLE, null));
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);

    private List<User> fullList = new ArrayList<>();
    private Role roleFilter; // null = Todos
    private String query = "";

    public LiveData<List<User>> getVisibleUsers() {
        return visibleUsers;
    }

    public LiveData<OpsState> getOpsState() {
        return opsState;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public void load() {
        loading.setValue(true);
        repository.listUsers(result -> {
            loading.setValue(false);
            if (result.isSuccess()) {
                fullList = new ArrayList<>(result.data);
                applyFilter();
            } else {
                opsState.setValue(new OpsState(OpsStatus.ERROR, null));
            }
        });
    }

    /** Filtro por rol: null = Todos. */
    public void setRoleFilter(Role role) {
        roleFilter = role;
        applyFilter();
    }

    /** Búsqueda por nombre o correo (insensible a mayúsculas). */
    public void setQuery(String newQuery) {
        query = newQuery == null ? "" : newQuery.trim().toLowerCase();
        applyFilter();
    }

    private void applyFilter() {
        List<User> filtered = new ArrayList<>();
        for (User user : fullList) {
            boolean matchesRole = roleFilter == null || user.role == roleFilter;
            boolean matchesQuery = query.isEmpty()
                    || user.name.toLowerCase().contains(query)
                    || user.normalizedEmail().contains(query);
            if (matchesRole && matchesQuery) {
                filtered.add(user);
            }
        }
        visibleUsers.setValue(filtered);
    }

    /** Guarda (crea o edita). Devuelve el resultado por opsState. */
    public void save(Draft draft, String userId, boolean isNew) {
        loading.setValue(true);
        String password = draft.password == null ? "" : draft.password;
        User persistDraft = new User(userId, draft.name, draft.email, draft.phone,
                password, draft.role, draft.active);
        repository.saveUser(persistDraft, isNew, result -> {
            loading.setValue(false);
            if (result.isSuccess()) {
                opsState.setValue(new OpsState(OpsStatus.SAVED, result.data.name));
                load(); // refresca la lista
            } else if ("existing_email".equals(result.error)) {
                opsState.setValue(new OpsState(OpsStatus.EMAIL_TAKEN, null));
            } else {
                opsState.setValue(new OpsState(OpsStatus.ERROR, null));
            }
        });
    }

    /** Borrado lógico (mantener historial). */
    public void setUserActive(User user, boolean active) {
        loading.setValue(true);
        repository.setUserActive(user.id, active, result -> {
            loading.setValue(false);
            if (result.isSuccess()) {
                opsState.setValue(new OpsState(active ? OpsStatus.SAVED : OpsStatus.DELETED,
                        result.data.name));
                load();
            } else {
                opsState.setValue(new OpsState(OpsStatus.ERROR, null));
            }
        });
    }
}
