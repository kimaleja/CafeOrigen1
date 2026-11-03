package com.cafeorigen.admin;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.PopupMenu;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.cafeorigen.MainActivity;
import com.cafeorigen.R;
import com.cafeorigen.core.model.Role;
import com.cafeorigen.core.model.User;
import com.cafeorigen.databinding.ItemAdminUserBinding;

/**
 * CafeOrigen — Adaptador del listado de usuarios (A2).
 * Muestra avatar con iniciales, rol y estado; expone menú de acciones.
 */
public class UsersAdapter extends ListAdapter<User, UsersAdapter.Holder> {

    /** Eventos de fila manejados por el Fragment. */
    public interface Listener {
        void onEdit(User user);

        void onDelete(User user);
    }

    private final Listener listener;

    private static final DiffUtil.ItemCallback<User> DIFF = new DiffUtil.ItemCallback<User>() {
        @Override
        public boolean areItemsTheSame(@NonNull User oldItem, @NonNull User newItem) {
            return oldItem.id.equals(newItem.id);
        }

        @Override
        public boolean areContentsTheSame(@NonNull User oldItem, @NonNull User newItem) {
            return java.util.Objects.equals(oldItem.name, newItem.name)
                    && java.util.Objects.equals(oldItem.email, newItem.email)
                    && java.util.Objects.equals(oldItem.role, newItem.role)
                    && oldItem.active == newItem.active;
        }
    };

    public UsersAdapter(Listener listener) {
        super(DIFF);
        this.listener = listener;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemAdminUserBinding binding = ItemAdminUserBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new Holder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        holder.bind(getItem(position));
    }

    class Holder extends RecyclerView.ViewHolder {

        private final ItemAdminUserBinding binding;

        Holder(ItemAdminUserBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(User user) {
            Context context = binding.getRoot().getContext();
            binding.userNameText.setText(user.name);
            binding.userEmailText.setText(user.email);
            binding.userInitialsText.setText(MoneyFormat.initials(user.name));

            // Badge de rol (información) — texto y fondo contextual
            binding.userRoleBadge.setText(rol(user.role));
            int bgRole = user.role == Role.ADMINISTRADOR ? R.drawable.bg_tag_danger
                    : user.role == Role.VENDEDOR ? R.drawable.bg_tag_warning
                    : R.drawable.bg_tag_info;
            int color = user.role == Role.ADMINISTRADOR ? R.color.danger
                    : user.role == Role.VENDEDOR ? R.color.warning : R.color.info;
            binding.userRoleBadge.setBackgroundResource(bgRole);
            binding.userRoleBadge.setTextColor(ContextCompat.getColor(context, color));

            // Estado activo/inactivo (borrado lógico)
            binding.userActiveBadge.setText(context.getString(
                    user.active ? R.string.users_state_active : R.string.users_state_inactive));
            binding.userActiveBadge.setBackgroundResource(user.active
                    ? R.drawable.bg_tag_success : R.drawable.bg_tag_danger);
            binding.userActiveBadge.setTextColor(ContextCompat.getColor(context,
                    user.active ? R.color.success : R.color.danger));

            // Menú contextual por ítem
            binding.userOverflowButton.setOnClickListener(v -> {
                PopupMenu menu = new PopupMenu(context, v);
                menu.getMenuInflater().inflate(
                        R.menu.menu_overflow_user, menu.getMenu());
                menu.setOnMenuItemClickListener(item -> {
                    int id = item.getItemId();
                    if (id == R.id.action_user_edit) {
                        listener.onEdit(user);
                        return true;
                    }
                    if (id == R.id.action_user_delete) {
                        listener.onDelete(user);
                        return true;
                    }
                    return false;
                });
                menu.show();
            });
        }

        private int rol(Role role) {
            switch (role) {
                case VENDEDOR:
                    return R.string.users_role_vendedor_badge;
                case ADMINISTRADOR:
                    return R.string.users_role_admin_badge;
                default:
                    return R.string.users_role_comprador_badge;
            }
        }
    }
}
