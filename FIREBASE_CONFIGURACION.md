# CafeOrigen — Configuración y verificación de Firebase

Documento de puesta en marcha de **Authentication + Cloud Firestore + Storage**.
La app funciona en dos modos y el cambio entre ellos es automático:

| Modo | Condición | Fuente de datos |
|---|---|---|
| **Mock** (por defecto hoy) | No existe `app/google-services.json` | `MockDataProvider` en memoria |
| **Firebase** | Existe `app/google-services.json` | Auth + Firestore + Storage reales |

No hay que modificar código ni Gradle para pasar de un modo a otro: el plugin
`com.google.gms.google-services` se aplica de forma condicional en
`app/build.gradle` y `FirebaseBootstrap.isConfigured()` decide en tiempo de
ejecución qué repositorio responde.

---

## 1. Estado actual (verificado el 18/11/2026)

| Elemento | Estado |
|---|---|
| `google-services.json` en `app/` | ❌ **No existe** → la app corre en modo mock |
| Firebase CLI (`firebase`) | ❌ No instalado en el equipo |
| Node.js / npm | ✅ v24.16.0 / 11.13.0 (permite ejecutar el seed) |
| Dependencias Firebase | ✅ Resueltas desde Google Maven (BoM 33.16.0) |
| Compilación con SDK incluido | ✅ `:app:compileDebugJavaWithJavac` y `:app:assembleDebug` OK (APK ~11,6 MB) |
| Compilación **con** el plugin google-services | ⚠️ No verificada (requiere el JSON real) |
| Conexión real contra el proyecto Firebase | ⚠️ **No verificada** — falta la configuración del proyecto de la cuenta de la estudiante |
| Reglas desplegadas | ⚠️ Pendiente (requieren proyecto y CLI) |

> **Motivo de la limitación:** `google-services.json` lo genera la consola de
> Firebase de la cuenta dueña del proyecto y contiene el `api_key`, el
> `project_id` y los `client_id` de las apps registradas. No se puede fabricar,
> ni deducir, ni versionar (además de que sería una credencial). Por eso el
> repositorio incluye la integración completa pero **sin** credenciales: la
> verificación real la ejecuta quien tenga acceso a la consola siguiendo el §3.

---

## 2. Qué se implementó

### 2.1 Capa de acceso a Firebase

```
app/src/main/java/com/cafeorigen/core/data/firebase/
├── FirebaseBootstrap.java        # Detección de modo, instancias de Auth/DB/Storage
├── FirestoreSchema.java          # Nombres de colecciones y campos (fuente única)
├── FirebaseMappers.java          # Modelo (User/Product/Order) ↔ DocumentSnapshot
├── FirebaseErrorMapper.java      # Excepciones del SDK → mensajes de la UI
├── InFlightGuard.java            # Anti doble envío (login, guardados, checkout)
├── FirestoreCache.java           # Espejo en memoria de lo leído (sync para la UI)
├── FirebaseAuthDataSource.java   # Login, registro, recuperación, logout
├── FirebaseUserDataSource.java   # Perfiles + alta de usuarios desde admin
├── FirebaseProductDataSource.java# Catálogo, CRUD, moderación, imágenes
├── FirebaseOrderDataSource.java  # Checkout transaccional + estados
├── FirebaseCartDataSource.java   # Carrito persistente carts/{uid}
└── FirebaseReportDataSource.java # Dashboard y reportes desde orders
```

Los repositorios que ya existían conservan **exactamente** sus firmas y son los
que deciden:

```java
if (FirebaseBootstrap.isConfigured()) {
    FirebaseProductDataSource.get().catalog(callback);
} else {
    // ruta mock de siempre
}
```

Así los ViewModels, adaptadores, layouts y navegación no cambiaron de
contrato: la UI desconocía el origen de los datos antes y lo sigue
desconociendo.

### 2.2 Dependencias (justificadas)

| Artefacto | Versión | Por qué |
|---|---|---|
| `com.google.gms:google-services` | 4.4.4 | Plugin que lee el JSON y genera los recursos `google_app_id`, `firebase_database_url`, etc. |
| `firebase-bom` | 33.16.0 | Alinea todos los SDK; es la última línea que no exige `compileSdk 35` (proyecto fijado en 34) |
| `firebase-auth` | vía BoM | Email/contraseña + sesión persistente del SDK |
| `firebase-firestore` | vía BoM | Base de datos con caché local persistente |
| `firebase-storage` | vía BoM | Imágenes de producto |

`minSdk 26` cumple el mínimo de Firestore (19) y Storage; no se subió el SDK ni
se introdujeron librerías nuevas (la carga de imágenes remotas se resolvió con
`HttpURLConnection` + `LruCache` en `core/ui/RemoteImageLoader.java`).

### 2.3 Persistencia y comportamiento

- **Firestore con caché local** (`setPersistenceEnabled(true)`): las lecturas
  sobreviven al cierre de la app y hay respaldo offline básico.
- **Carrito real** en `carts/{uid}`: escrituras *write-through* colapsadas (una
  escritura por ráfaga de cambios) y un listener que repinta la pantalla cuando
  el documento llega desde el servidor.
- **Sesión**: el SDK conserva la sesión; `SessionManager.restoreLastUser()`
  rehidrata el perfil desde Firestore al entrar con biometría.
- **Checkout transaccional**: una transacción de Firestore valida stock,
  descuenta unidades, crea **un pedido por vendedor**, borra el carrito y
  actualiza la caché; si algo falla no se escribe nada.
- **Anti duplicados**: `InFlightGuard` bloquea el segundo toque en login,
  registro, formularios y pago; los botones se deshabilitan mientras la
  operación está en curso.
- **Errores de red**: `FirebaseErrorMapper` traduce `UNAVAILABLE`,
  `PERMISSION_DENIED`, `UNAUTHENTICATED`, `ALREADY_EXISTS`, etc. a mensajes en
  español mostrados en snackbar/campo, sin cerrar pantallas.

---

## 3. Checklist de puesta en marcha (manual, ~15 min)

### Paso 1 — Crear el proyecto

1. Entrar a <https://console.firebase.google.com> con la cuenta de la
   estudiante.
2. **Agregar proyecto** → nombre sugerido `CafeOrigen` (el `project_id` será
   algo como `cafeorigen-xxxxx`).
3. Google Analytics: opcional (no se usa en la app).

### Paso 2 — Registrar la app Android

1. En el proyecto → **Agregar app** → Android.
2. **Nombre del paquete:** `com.cafeorigen` (exacto; debe coincidir con
   `applicationId`).
3. Apodo: `CafeOrigen Android`.
4. **SHA-1:** obligatorio sólo si se usa Google Sign-In o teléfono; con
   email/contraseña no hace falta. Si se quiere añadir:
   `keytool -list -v -keystore %USERPROFILE%\.android\debug.keystore -alias androiddebugkey -storepass android -keypass android`
5. Descargar `google-services.json` y guardarlo **en `CafeOrigen/app/`**, no en
   la raíz del repositorio.

> El archivo está excluido en `.gitignore`. **No debe subirse** al repositorio
> ni incluirse en el ZIP de entrega: contiene la `api_key` del proyecto.

### Paso 3 — Habilitar Authentication

1. **Compilación → Authentication → Comenzar**.
2. **Sign-in method → Correo electrónico/contraseña → Habilitar** (la opción de
   "vínculo de correo" puede quedar desactivada).
3. En **Plantillas**, personalizar el remitente y el texto del correo de
   restablecimiento (la app usa `sendPasswordResetEmail`).

### Paso 4 — Crear Cloud Firestore

1. **Compilación → Firestore Database → Crear base de datos**.
2. Ubicación sugerida: `southamerica-east1` (São Paulo, la más cercana a
   Colombia) o `us-central1`.
3. Empezar en **modo producción** (reglas cerradas). Las reglas reales se
   despliegan en el paso 6; **no** dejar el modo prueba.

### Paso 5 — Crear Storage

1. **Compilación → Storage → Comenzar**.
2. Misma ubicación que Firestore.
3. Modo producción (se reemplaza con `firebase/storage.rules`).

### Paso 6 — Desplegar reglas

Con Firebase CLI (`npm install -g firebase-tools`, luego `firebase login`):

```bash
cd CafeOrigen/firebase
firebase use --add          # seleccionar el proyecto creado (crea .firebaserc, ignorado por git)
firebase deploy --only firestore:rules,storage
```

Sin CLI también se puede: copiar el contenido de `firebase/firestore.rules` y
`firebase/storage.rules` en las pestañas **Reglas** de cada producto y
publicar. Recordar publicar las dos: Firestore y Storage.

### Paso 7 — Cargar los datos demo (opcional pero recomendado)

```bash
cd CafeOrigen/firebase/seed
npm install
# consola de Firebase → Configuración del proyecto → Cuentas de servicio
# → Generar nueva clave privada → guardar como serviceAccount.json aquí
node seed-firestore.js
```

El script crea las 5 cuentas demo en Auth (mismos correos y contraseñas del
mock), sus perfiles en `users`, las 4 categorías, los 8 productos y los 6
pedidos. Es idempotente: se puede volver a ejecutar (usa `merge`).

> La clave `serviceAccount.json` equivale a una contraseña de administrador del
> proyecto: está en `.gitignore` y **nunca** debe salir del equipo.

### Paso 8 — Compilar y ejecutar

```bash
./gradlew clean assembleDebug          # Windows: gradlew.bat clean assembleDebug
./gradlew installDebug                 # con emulador/dispositivo conectado
```

Al existir `app/google-services.json`, Gradle aplica el plugin y el APK queda
configurado. En el primer arranque el log debe mostrar la inicialización de
Firebase sin errores.

---

## 4. Esquema de datos

### `users/{uid}`

| Campo | Tipo | Notas |
|---|---|---|
| `name` | string | Nombre visible |
| `email` | string | Igual al de Auth |
| `phone` | string | Teléfono |
| `role` | string | `ADMINISTRADOR` \| `VENDEDOR` \| `COMPRADOR` |
| `active` | boolean | Borrado lógico; `false` bloquea el acceso |
| `createdAt` | timestamp | |

### `products/{productId}`

| Campo | Tipo | Notas |
|---|---|---|
| `vendorId` | string | **uid** del vendedor dueño (aislamiento) |
| `vendorName` | string | Nombre de la finca |
| `name`, `region`, `variety`, `process`, `description` | string | Ficha de origen |
| `altitudeM` | number | msnm |
| `price`, `stock` | number | Precio en COP, stock en lb |
| `category` | string | Enum `ProductCategory` |
| `status` | string | `BORRADOR` \| `PENDIENTE` \| `ACTIVO` \| `RECHAZADO` |
| `imageUri` | string | URL de Storage (o URI local de galería) |
| `createdAt` | timestamp | |

### `orders/{orderId}`

| Campo | Tipo | Notas |
|---|---|---|
| `buyerId` | string | uid del comprador |
| `vendorId` | string | uid del vendedor destino |
| `buyerName`, `address` | string | Datos de entrega |
| `status` | string | `POR_CONFIRMAR` \| `CONFIRMADO` \| `DESPACHADO` \| `ENTREGADO` \| `CANCELADO` |
| `items` | array | `{productId, productName, units, subtotal}` |
| `total` | number | Suma de subtotales |
| `createdAt`, `updatedAt` | timestamp | |

### `carts/{uid}`

```json
{ "items": [ { "productId": "P-101", "units": 2 } ] }
```

### `categories/{key}`

```json
{ "label": "Café en grano", "order": 1 }
```

**Índices:** no se requieren índices compuestos. Todas las consultas usan un
solo filtro de igualdad (`status`, `vendorId`, `email`) y el orden se resuelve
en el cliente; por eso `firebase/firestore.indexes.json` va vacío. Si más
adelante se añade `orderBy` junto a un filtro, Firestore devolverá un enlace
directo para crear el índice y habrá que declararlo en ese archivo.

### Mapeo con los modelos existentes

`FirebaseMappers` es el único punto de traducción. Los `id` de documento se
conservan como `id` del modelo (`P-101`, `O-201`, uid para usuarios), de modo
que las referencias cruzadas (`order.items[].productId`) siguen funcionando y
la UI no necesita conocer Firestore.

---

## 5. Reglas de seguridad

Ambos archivos siguen el principio **deny-by-default**: toda ruta no declarada
queda denegada y ninguna regla depende de datos que el cliente pueda falsear
(el rol se lee de `users/{uid}`, no de un campo del request).

### `firestore.rules`

| Colección | Lectura | Escritura |
|---|---|---|
| `users` | El propio usuario o ADMIN | Registro público solo crea **su** perfil como `COMPRADOR` activo; autoedición sin tocar `role`/`active`/`email`; ADMIN puede todo |
| `products` | Cualquier cuenta activa | VENDEDOR crea solo a su nombre y solo en `BORRADOR`/`PENDIENTE` (no puede autoaprobarse); actualiza solo sus productos y no puede cambiar el `vendorId`; ADMIN modera y elimina |
| `orders` | Comprador dueño, vendedor dueño o ADMIN | Comprador crea solo con `buyerId` propio y estado `POR_CONFIRMAR`; VENDEDOR dueño solo avanza estado sin tocar líneas/totales; **borrar: nunca** |
| `carts` | Solo el dueño (`uid`) | Solo el dueño |
| `categories` | Cuentas activas | Solo ADMIN |

Puntos que responden directamente a los requisitos:

- Un **vendedor no puede editar productos de otro**: se exige
  `resource.data.vendorId == request.auth.uid` y que el `vendorId` no cambie.
- Un **comprador no puede modificar pedidos ajenos**: no tiene ninguna rama de
  `update` en `orders`; sólo crea pedidos con su propio `buyerId`.
- **Nadie se autoasigna ADMIN**: `users.update` exige `unchanged('role')` para
  el dueño del perfil; solo un ADMIN activo puede cambiar roles.
- Cuenta desactivada (`active == false`): `activeProfile()` falla y **todas**
  las operaciones quedan denegadas, incluso leer el propio perfil.

### `storage.rules`

```
products/{vendorId}/{fileName}
  read   → cualquier usuario autenticado
  write  → vendedor activo con uid == vendorId, < 5 MB, content-type image/*
  delete → dueño
```

Sin sesión no se puede leer ninguna imagen del bucket, y ninguna ruta distinta
de `products/...` está permitida.

### Limitación conocida y recomendada

El checkout ocurre en el cliente, así que el comprador necesita descontar
stock. Para no exigir un backend, `products.update` le permite **solo** el campo
`stock`, **solo** para reducirlo y **solo** con valor ≥ 0:

```
request.resource.data.diff(resource.data).affectedKeys().hasOnly(['stock'])
&& request.resource.data.stock < resource.data.stock
```

Es una concesión deliberada y acotada. La solución definitiva es mover el
checkout a una **Cloud Function** con Admin SDK (valida stock, crea pedidos y
descuenta en una transacción de servidor) y eliminar esa rama de la regla. Se
documenta como trabajo futuro, igual que la creación de usuarios desde el panel
admin (ver §7).

---

## 6. Cómo verificar que la conexión es real (no mock)

Ejecutar en este orden; cada punto tiene una señal observable:

1. **Modo activo:** con el JSON en `app/` y la app instalada, `adb logcat | findstr FirebaseApp` no debe mostrar
   `Default FirebaseApp failed to initialize`. Si aparece, el paquete del JSON
   no coincide con `com.cafeorigen`.
2. **Auth real:** crear una cuenta desde la app (Registro) → en la consola,
   **Authentication → Usuarios** debe aparecer el correo nuevo con su UID.
3. **Perfil real:** en **Firestore → users** debe existir el documento con ese
   mismo UID y `role: "COMPRADOR"`.
4. **Catálogo real:** modificar el `stock` de `P-101` en la consola →
   reabrir Catálogo en la app. El stock mostrado debe cambiar. Si no cambia y el
   mock conserva su valor, Firebase no está activo.
5. **Persistencia real:** cerrar sesión, cerrar la app (quitar de recientes),
   volver a abrir e iniciar sesión con el usuario creado. Debe entrar: el mock
   no conoce esa cuenta.
6. **Escritura real:** con el comprador, agregar productos al carrito → en
   **Firestore → carts → {uid}** debe aparecer el array `items`.
7. **Checkout real:** pagar → aparece un documento nuevo en `orders`, el stock
   del producto baja en Firestore y el pedido se ve en el panel del vendedor.
   En modo mock no se crea ningún documento.
8. **Storage real:** subir imagen desde el formulario del vendedor → en
   **Storage → products/{uid}/** debe estar el archivo y la URL debe cargar en
   el catálogo.

Si los puntos 2–8 fallan pero el 1 pasa, revisar reglas publicadas
(`PERMISSION_DENIED` en logcat) y que la cuenta tenga `active: true`.

---

## 7. Pruebas de reglas (matriz mínima)

Se pueden ejecutar desde el **Simulador de reglas** de la consola o con
`@firebase/rules-unit-testing` en el emulador local. Casos que deben cumplirse:

| # | Actor | Operación | Esperado |
|---|---|---|---|
| 1 | Anónimo | leer `products/P-101` | ❌ denegado |
| 2 | Comprador activo | leer `orders` propio | ✅ permitido |
| 3 | Comprador activo | leer `orders` de otro `buyerId` | ❌ denegado |
| 4 | Comprador activo | `update` de un pedido | ❌ denegado |
| 5 | Comprador activo | crear pedido con `buyerId` ajeno | ❌ denegado |
| 6 | Comprador activo | crear pedido con estado `ENTREGADO` | ❌ denegado |
| 7 | Comprador activo | bajar `stock` de un producto | ✅ permitido (concesión de checkout) |
| 8 | Comprador activo | cambiar `price` o `status` de un producto | ❌ denegado |
| 9 | Vendedor A | `update` de producto de vendedor B | ❌ denegado |
| 10 | Vendedor A | crear producto con `vendorId` = B | ❌ denegado |
| 11 | Vendedor A | crear/actualizar su producto con `status` = `ACTIVO` | ❌ denegado (no autoaprobación) |
| 12 | Vendedor A | `update` de estado de su pedido a `CONFIRMADO` | ✅ permitido |
| 13 | Vendedor A | `update` del `total` de su pedido | ❌ denegado |
| 14 | Cualquiera | `delete` de un pedido | ❌ denegado |
| 15 | Usuario activo | cambiar su propio `role` a `ADMINISTRADOR` | ❌ denegado |
| 16 | Admin activo | cambiar `role`/`active` de otro usuario | ✅ permitido |
| 17 | Cuenta `active: false` | cualquier lectura/escritura | ❌ denegada |
| 18 | Vendedor activo | subir `products/{uid}/x.jpg` (3 MB) | ✅ permitido |
| 19 | Vendedor A | subir a `products/{uidDeB}/x.jpg` | ❌ denegado |
| 20 | Vendedor activo | subir archivo `.pdf` o > 5 MB | ❌ denegado |

Para el emulador local (opcional):

```bash
firebase emulators:start --only firestore,storage,auth
```

y apuntar la app con `FirebaseBootstrap` en modo emulador (requiere cambiar
`useEmulator(...)`; no está activado por defecto para no alterar la entrega).

---

## 8. Solución de problemas

| Síntoma | Causa probable | Solución |
|---|---|---|
| La app sigue mostrando datos del mock | `google-services.json` ausente, mal ubicado o con otro paquete | Debe estar en `app/` y su `package_name` debe ser `com.cafeorigen`; luego `clean assembleDebug` |
| `File google-services.json is missing` | El plugin se aplicó sin el archivo | No ocurre: el plugin es condicional. Si se fuerza, colocar el JSON o quitar la línea |
| `PERMISSION_DENIED` al iniciar sesión | `users/{uid}` inexistente o `active: false` | Ejecutar el seed o crear el perfil; verificar `active` |
| Login correcto pero sin datos | Usuario creado sin perfil en `users` | Ejecutar `node seed-firestore.js` o crear el documento a mano |
| `FAILED_PRECONDITION` con índice | Se añadió una consulta con `orderBy` + filtro | Usar el enlace del error y declarar el índice en `firestore.indexes.json` |
| Imágenes de Storage no cargan | Reglas de Storage sin publicar | `firebase deploy --only storage` |
| El correo de recuperación no llega | Plantilla/remitente sin configurar o correo en spam | Revisar Authentication → Plantillas y la carpeta de spam |
| `ApiException: 12500` al iniciar sesión | Falta habilitar Email/Password | Authentication → Sign-in method |

---

## 9. Limitaciones y trabajo futuro

1. **Verificación real pendiente** por falta de `google-services.json` (§1). El
   resto del documento es el procedimiento exacto para completarla.
2. **Alta de usuarios por el admin**: se implementó con una `FirebaseApp`
   secundaria (`cafeorigen-provisioning`) para crear la cuenta de Auth sin
   cerrar la sesión del administrador. Limitación: en un backend real esto debe
   ser una Cloud Function con Admin SDK (y borrar la clave de la app
   secundaria). Documentado en el código de `FirebaseUserDataSource`.
3. **Checkout en cliente**: la concesión de `stock` descrita en §5 desaparece al
   mover la transacción a Cloud Functions.
4. **Pagos**: la pasarela sigue siendo demostrativa; integración real
   (Wompi/Mercado Pago) queda fuera del alcance académico.
5. **Búsqueda**: el catálogo se filtra en memoria tras descargar los productos
   activos. Con catálogos grandes conviene paginar y usar índices de texto
   (Algolia/Typesense), no Firestore `where` con `>=`/`<=` sobre nombre.
6. **Sin Cloud Messaging**: las notificaciones de cambio de estado de pedido no
   se implementaron (fuera del alcance definido).

---

## 10. Resumen para la entrega

- El proyecto **compila y funciona igual que antes** sin credenciales: modo
  mock, ideal para revisar la app sin depender de una cuenta de Firebase.
- La integración queda **lista y aislada** en `core/data/firebase/`, con reglas
  por rol, script de datos demo y este checklist.
- La única acción pendiente es de la cuenta dueña del proyecto: crear el
  proyecto, descargar `google-services.json`, publicar reglas y ejecutar el
  seed (§3 y §6).
