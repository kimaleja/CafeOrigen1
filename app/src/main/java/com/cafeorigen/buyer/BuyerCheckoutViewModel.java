package com.cafeorigen.buyer;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.cafeorigen.core.data.BuyerDataRepository;
import com.cafeorigen.core.data.CartRepository;
import com.cafeorigen.core.data.UserRepository;
import com.cafeorigen.core.model.Order;
import com.cafeorigen.core.model.ProductCategory;

import java.util.List;

/**
 * CafeOrigen — ViewModel de la pasarela de pagos (C5).
 * Pasarela demostrativa: valida la entrega, llama al repositorio (que agrupa
 * por vendedor y descuenta stock) y devuelve los pedidos creados. No hay
 * proveedor de pagos real: los métodos son solo opciones de demo.
 */
public class BuyerCheckoutViewModel extends ViewModel {

    /** Métodos de pago de la interfaz demo (sin datos bancarios reales). */
    public enum PaymentMethod {
        CARD_DEMO("Tarjeta de demo",
                "Preconfigurada en la app de pruebas •••• 4242"),
        PSE_DEMO("Débito PSE (demo)",
                "Simulada, sin acceso a tu banco"),
        CASH_DEMO("Pago contraentrega (demo)",
                "Pagas al recibir el pedido");

        public final String title;
        public final String subtitle;

        PaymentMethod(String title, String subtitle) {
            this.title = title;
            this.subtitle = subtitle;
        }
    }

    public enum OpsStatus { IDLE, SUCCESS, DELIVERY_INVALID, STOCK_CHANGED, ERROR }

    public static class OpsState {
        public final OpsStatus status;
        public final List<Order> createdOrders;

        OpsState(OpsStatus status, List<Order> createdOrders) {
            this.status = status;
            this.createdOrders = createdOrders;
        }
    }

    /** Borrador de entrega validado por el Fragment. */
    public static final class DeliveryDraft {
        public final String recipientName;
        public final String phone;
        public final String address;
        public final PaymentMethod method;

        public DeliveryDraft(String recipientName, String phone, String address,
                             PaymentMethod method) {
            this.recipientName = recipientName;
            this.phone = phone;
            this.address = address;
            this.method = method;
        }
    }

    private final BuyerDataRepository repository = BuyerDataRepository.get();

    private final MutableLiveData<OpsState> opsState =
            new MutableLiveData<>(new OpsState(OpsStatus.IDLE, null));
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);

    public LiveData<OpsState> getOpsState() {
        return opsState;
    }

    /**
     * Consume el evento ya entregado (vuelve a IDLE): los mensajes y la
     * pantalla de éxito no se repiten cuando se rota la pantalla.
     */
    public void consumeOps() {
        opsState.setValue(new OpsState(OpsStatus.IDLE, null));
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    /** Confirma el pedido con el snapshot actual del carrito (C5). */
    public void checkout(DeliveryDraft draft, String buyerId, String buyerName) {
        loading.setValue(true);
        List<CartRepository.CartItem> items = CartRepository.get().items();
        repository.checkout(buyerId, buyerName,
                new BuyerDataRepository.Delivery(draft.recipientName, draft.phone,
                        draft.address),
                items,
                result -> {
                    loading.setValue(false);
                    if (result.isSuccess()) {
                        CartRepository.get().clear();
                        opsState.setValue(new OpsState(OpsStatus.SUCCESS, result.data));
                    } else if ("stock_changed".equals(result.error)) {
                        opsState.setValue(new OpsState(OpsStatus.STOCK_CHANGED, null));
                    } else {
                        opsState.setValue(new OpsState(OpsStatus.ERROR, null));
                    }
                });
    }
}
