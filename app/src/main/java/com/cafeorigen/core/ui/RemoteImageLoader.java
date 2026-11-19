package com.cafeorigen.core.ui;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.widget.ImageView;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * CafeOrigen — Carga de imágenes remotas (Firebase Storage) sin dependencias
 * externas.
 *
 * Las pantallas existentes usaban ImageView.setImageURI con URIs locales de
 * galería. Con Storage la imagen es una URL https y setImageURI no la abre:
 * este cargador descarga, cachea en memoria y aplica el bitmap solo si la
 * vista sigue mostrando la misma URL (evita imágenes cruzadas al reciclar
 * filas del RecyclerView).
 */
public final class RemoteImageLoader {

    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(3);
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final LruCache<String, Bitmap> CACHE = new LruCache<String, Bitmap>(
            (int) (Runtime.getRuntime().maxMemory() / 8)) {
        @Override
        protected int sizeOf(String key, Bitmap value) {
            return value.getByteCount();
        }
    };

    private RemoteImageLoader() {
    }

    public static boolean isRemote(String url) {
        return url != null && (url.startsWith("https://") || url.startsWith("http://"));
    }

    public static void load(final ImageView view, final String url) {
        if (view == null || url == null) {
            return;
        }
        view.setTag(url);
        Bitmap cached = CACHE.get(url);
        if (cached != null) {
            view.setImageBitmap(cached);
            return;
        }
        EXECUTOR.execute(() -> {
            Bitmap bitmap = download(url);
            if (bitmap == null) {
                return;
            }
            CACHE.put(url, bitmap);
            MAIN.post(() -> {
                if (url.equals(view.getTag())) {
                    view.setImageBitmap(bitmap);
                }
            });
        });
    }

    private static Bitmap download(String url) {
        HttpURLConnection connection = null;
        InputStream input = null;
        try {
            connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setConnectTimeout(10_000);
            connection.setReadTimeout(10_000);
            connection.setInstanceFollowRedirects(true);
            input = connection.getInputStream();
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 2; // las miniaturas no necesitan resolución completa
            return BitmapFactory.decodeStream(input, null, options);
        } catch (Exception e) {
            return null;
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (Exception ignored) {
                    // sin acción
                }
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
