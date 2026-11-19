package com.cafeorigen.core.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

/**
 * CafeOrigen — Compresión previa a la subida a Firebase Storage.
 *
 * Las imágenes de galería pueden pesar varios MB; se reescalan a un máximo
 * de 1280 px de lado y se guardan como JPEG 85 antes de subirlas. Si algo
 * falla se devuelve null y el repositorio sube el archivo original (dentro
 * del límite de 5 MB de las reglas de Storage).
 */
public final class ImageCompressor {

    private static final int MAX_SIDE = 1280;
    private static final int QUALITY = 85;

    private ImageCompressor() {
    }

    public static File compress(Context context, Uri uri, String baseName) {
        if (context == null || uri == null) {
            return null;
        }
        InputStream input = null;
        FileOutputStream output = null;
        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            input = context.getContentResolver().openInputStream(uri);
            BitmapFactory.decodeStream(input, null, bounds);
            closeQuietly(input);
            input = null;

            int sample = 1;
            int largest = Math.max(bounds.outWidth, bounds.outHeight);
            while (largest / sample > MAX_SIDE * 2) {
                sample *= 2;
            }

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = sample;
            input = context.getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(input, null, options);
            if (bitmap == null) {
                return null;
            }

            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            int side = Math.max(width, height);
            if (side > MAX_SIDE) {
                float scale = (float) MAX_SIDE / side;
                Bitmap scaled = Bitmap.createScaledBitmap(bitmap,
                        Math.round(width * scale), Math.round(height * scale), true);
                if (scaled != bitmap) {
                    bitmap.recycle();
                }
                bitmap = scaled;
            }

            File out = new File(context.getCacheDir(),
                    "upload_" + (baseName == null ? "img" : baseName) + ".jpg");
            output = new FileOutputStream(out);
            bitmap.compress(Bitmap.CompressFormat.JPEG, QUALITY, output);
            output.flush();
            bitmap.recycle();
            return out;
        } catch (Exception e) {
            return null;
        } finally {
            closeQuietly(input);
            closeQuietly(output);
        }
    }

    private static void closeQuietly(java.io.Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception ignored) {
                // sin acción
            }
        }
    }
}
