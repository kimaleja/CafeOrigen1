package com.cafeorigen.buyer;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.cafeorigen.core.data.BuyerDataRepository;
import com.cafeorigen.core.data.UserRepository;
import com.cafeorigen.core.model.Order;
import com.cafeorigen.core.model.OrderStatus;

import java.util.ArrayList;
import java.util.List;

/**
 * CafeOrigen — ViewModel del historial de compras del Comprador (C6).
 * Pedidos propios filtrados por buyerId, con filtro por estado.
 */
public class BuyerOrdersViewModel extends ViewModel {

    private final BuyerDataRepository repository = BuyerDataRepository.get();

    private final MutableLiveData<List<Order>> visibleOrders =
            new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);

    private List<Order> fullList = new ArrayList<>();
    private OrderStatus statusFilter; // null = Todos
    private String buyerId;

    public LiveData<List<Order>> getVisibleOrders() {
        return visibleOrders;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public void load(String buyerId) {
        this.buyerId = buyerId;
        loading.setValue(true);
        repository.buyerOrders(buyerId, result -> {
            loading.setValue(false);
            if (result.isSuccess()) {
                fullList = new ArrayList<>(result.data);
                fullList.sort((a, b) -> Integer.compare(a.daysAgo, b.daysAgo));
                applyFilter();
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
}
