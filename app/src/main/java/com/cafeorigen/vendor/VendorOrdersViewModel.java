package com.cafeorigen.vendor;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.cafeorigen.core.data.UserRepository;
import com.cafeorigen.core.data.VendorDataRepository;
import com.cafeorigen.core.model.Order;
import com.cafeorigen.core.model.OrderStatus;

import java.util.ArrayList;
import java.util.List;

/**
 * CafeOrigen — ViewModel del historial de pedidos recibidos (V6).
 * Filtrado por vendorId y por estado; actualización de estado (V7).
 */
public class VendorOrdersViewModel extends ViewModel {

    public enum OpsStatus { IDLE, CONFIRMED, DISPATCHED, DELIVERED, CANCELED, ERROR }

    public static class OpsState {
        public final OpsStatus status;
        public final String orderId;

        OpsState(OpsStatus status, String orderId) {
            this.status = status;
            this.orderId = orderId;
        }
    }

    private final VendorDataRepository repository = VendorDataRepository.get();

    private final MutableLiveData<List<Order>> visibleOrders =
            new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<OpsState> opsState =
            new MutableLiveData<>(new OpsState(OpsStatus.IDLE, null));
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);

    private List<Order> fullList = new ArrayList<>();
    private OrderStatus statusFilter; // null = Todos
    private String vendorId;

    public LiveData<List<Order>> getVisibleOrders() {
        return visibleOrders;
    }

    public LiveData<OpsState> getOpsState() {
        return opsState;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public void load(String vendorId) {
        this.vendorId = vendorId;
        loading.setValue(true);
        repository.listOrders(vendorId, result -> {
            loading.setValue(false);
            if (result.isSuccess()) {
                fullList = new ArrayList<>(result.data);
                // Más recientes primero
                fullList.sort((a, b) -> Integer.compare(a.daysAgo, b.daysAgo));
                applyFilter();
            } else {
                opsState.setValue(new OpsState(OpsStatus.ERROR, null));
            }
        });
    }

    public void setStatusFilter(OrderStatus status) {
        statusFilter = status;
        applyFilter();
    }

    private void applyFilter() {
        List<Order> filtered = new ArrayList<>();
        for (Order order : fullList) {
            if (statusFilter == null || order.status == statusFilter) {
                filtered.add(order);
            }
        }
        visibleOrders.setValue(filtered);
    }

    /** Acciones del detalle (V7): transiciones válidas por estado. */
    public void changeStatus(Order order, OrderStatus target) {
        if (vendorId == null || !order.vendorId.equals(vendorId)) {
            opsState.setValue(new OpsState(OpsStatus.ERROR, order.id));
            return;
        }
        OpsStatus onOk;
        switch (target) {
            case CONFIRMADO:
                onOk = OpsStatus.CONFIRMED;
                break;
            case DESPACHADO:
                onOk = OpsStatus.DISPATCHED;
                break;
            case CANCELADO:
                onOk = OpsStatus.CANCELED;
                break;
            case ENTREGADO:
                onOk = OpsStatus.DELIVERED;
                break;
            default:
                onOk = OpsStatus.ERROR;
        }
        repository.updateOrderStatus(order.id, target, result -> {
            if (result.isSuccess()) {
                opsState.setValue(new OpsState(onOk, order.id));
                load(vendorId);
            } else {
                opsState.setValue(new OpsState(OpsStatus.ERROR, order.id));
            }
        });
    }
}
