package com.cafeorigen.core.data.firebase;

import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.storage.StorageException;

import java.io.IOException;
import java.util.Locale;

/**
 * CafeOrigen — Traducción de errores de Firebase a los códigos que ya
 * entiende la UI de la app (los mismos que usaba el mock: "invalid_email",
 * "existing_email", "stock_changed"...).
 *
 * La capa de datos nunca expone mensajes técnicos al usuario: los ViewModels
 * reciben un código estable y las pantallas deciden el texto.
 */
public final class FirebaseErrorMapper {

    public static final String NETWORK = "network_error";
    public static final String PERMISSION = "permission_denied";
    public static final String UNAVAILABLE = "service_unavailable";
    public static final String TOO_MANY = "too_many_requests";
    public static final String DISABLED = "account_disabled";
    public static final String AUTH_DISABLED = "auth_method_disabled";
    public static final String PROFILE_MISSING = "profile_missing";
    public static final String UNKNOWN = "unknown_error";
    public static final String IN_PROGRESS = "operation_in_progress";

    private FirebaseErrorMapper() {
    }

    /** Código estable para cualquier fallo de Firebase, IO o inesperado. */
    public static String map(Throwable error) {
        if (error == null) {
            return UNKNOWN;
        }
        if (error instanceof FirebaseAuthException) {
            return mapAuth(((FirebaseAuthException) error).getErrorCode());
        }
        if (error instanceof FirebaseFirestoreException) {
            return mapFirestore(((FirebaseFirestoreException) error).getCode());
        }
        if (error instanceof StorageException) {
            return mapStorage(((StorageException) error).getErrorCode());
        }
        if (error instanceof FirebaseNetworkException || error instanceof IOException) {
            return NETWORK;
        }
        return UNKNOWN;
    }

    public static String mapAuth(String code) {
        if (code == null) {
            return UNKNOWN;
        }
        switch (code.toUpperCase(Locale.US)) {
            case "ERROR_INVALID_EMAIL":
                return "invalid_email";
            case "ERROR_WRONG_PASSWORD":
            case "ERROR_USER_NOT_FOUND":
            case "ERROR_INVALID_CREDENTIAL":
            case "ERROR_INVALID_LOGIN_CREDENTIALS":
                // Mismo mensaje para credenciales y correo inexistente:
                // no se revela si la cuenta existe (enumeración de usuarios).
                return "invalid_credentials";
            case "ERROR_EMAIL_ALREADY_IN_USE":
                return "existing_email";
            case "ERROR_WEAK_PASSWORD":
                return "short_password";
            case "ERROR_NETWORK_REQUEST_FAILED":
                return NETWORK;
            case "ERROR_TOO_MANY_REQUESTS":
                return TOO_MANY;
            case "ERROR_USER_DISABLED":
                return DISABLED;
            case "ERROR_OPERATION_NOT_ALLOWED":
                return AUTH_DISABLED;
            case "ERROR_REQUIRES_RECENT_LOGIN":
                return "requires_recent_login";
            case "ERROR_USER_TOKEN_EXPIRED":
            case "ERROR_INVALID_USER_TOKEN":
                return "session_expired";
            default:
                return UNKNOWN;
        }
    }

    public static String mapFirestore(FirebaseFirestoreException.Code code) {
        if (code == null) {
            return UNKNOWN;
        }
        switch (code) {
            case PERMISSION_DENIED:
                return PERMISSION;
            case UNAUTHENTICATED:
                return "session_expired";
            case UNAVAILABLE:
            case DEADLINE_EXCEEDED:
                return NETWORK;
            case NOT_FOUND:
                return "not_found";
            case ALREADY_EXISTS:
                return "already_exists";
            case ABORTED:
            case FAILED_PRECONDITION:
            case RESOURCE_EXHAUSTED:
                return UNAVAILABLE;
            case CANCELLED:
                return "cancelled";
            default:
                return UNKNOWN;
        }
    }

    public static String mapStorage(int errorCode) {
        switch (errorCode) {
            case StorageException.ERROR_OBJECT_NOT_FOUND:
                return "not_found";
            case StorageException.ERROR_NOT_AUTHENTICATED:
                return "session_expired";
            case StorageException.ERROR_NOT_AUTHORIZED:
                return PERMISSION;
            case StorageException.ERROR_QUOTA_EXCEEDED:
                return UNAVAILABLE;
            case StorageException.ERROR_RETRY_LIMIT_EXCEEDED:
                return NETWORK;
            case StorageException.ERROR_CANCELED:
                return "cancelled";
            default:
                return UNKNOWN;
        }
    }

    /** Los errores de red se pueden reintentar; los de permisos no. */
    public static boolean isRetryable(String code) {
        return NETWORK.equals(code) || UNAVAILABLE.equals(code);
    }
}
