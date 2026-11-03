package com.cafeorigen.buyer;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.cafeorigen.core.data.CartRepository;
import com.cafeorigen.core.data.MockDataProvider;
import com.cafeorigen.core.model.Product;

import java.util.ArrayList;
import java.util.List;

/**
 * CafeOrigen — ViewModel del carrito (C4).
 * Líneas con producto/subtotal para la UI; delega en CartRepository, que
 * valida existencias contra el mock.
 */
public class BuyerCartViewModel extends ViewModel {

    /** Fila de carrito para el adaptador (producto + cantidades). */
    public static final class CartLine {
        public final Product product;
        public final int units;
        public final long subtotal;

        CartLine(Product product, int units) {
            this.product = product;
            this.units = units;
            this.subtotal = (long) product.price * units;
        }
    }

    private final CartRepository cart = CartRepository.get();

    private final MutableLiveData<List<CartLine>> lines =
            new MutableLiveData<>(new ArrayList<>());

    public LiveData<List<CartLine>> getLines() {
        return lines;
    }

    /** Reconstruye las líneas desde el carrito (llamar en onResume/reportes). */
    public void refresh() {
        List<CartLine> rebuilt = new ArrayList<>();
        for (CartRepository.CartItem item : cart.items()) {
            Product product = MockDataProvider.findProductById(item.productId);
            if (product != null) {
                rebuilt.add(new CartLine(product, item.units));
            }
        }
        lines.setValue(rebuilt);
    }

    public long total() {
        return cart.total();
    }

    public boolean add(String productId, int units) {
        boolean ok = cart.add(productId, units);
        if (ok) {
            refresh();
        }
        return ok;
    }

    /** Cambia unidades (por el stepper del carrito). */
    public boolean updateUnits(String productId, int units) {
        boolean ok = cart.updateUnits(productId, units);
        if (ok) {
            refresh();
        }
        return ok;
    }

    public void remove(String productId) {
        cart.remove(productId);
        refresh();
    }
}
