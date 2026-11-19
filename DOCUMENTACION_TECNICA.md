# DOCUMENTACIÓN TÉCNICA — CaféOrigen (Tienda Virtual de Café)

**Proyecto:** CaféOrigen — Aplicación Android nativa de e-commerce de café de origen
**Entrega:** Actividad 1 (documentación técnica actualizada a la implementación final)
**Fecha:** 2 de noviembre de 2026
**Versión documentada:** `versionName 1.0.0` · `versionCode 1`

---

## 1. Introducción

CaféOrigen es una aplicación Android nativa desarrollada en **Java** que implementa una tienda virtual de café de especialidad colombiano. El proyecto modela la cadena comercial completa entre tres actores reales: un **administrador** de la plataforma, los **vendedores caficultores** (fincas) y los **compradores finales**. Toda la experiencia de compra —catálogo, detalle de producto, carrito, checkout y pedidos— está implementada íntegramente en la app, con una capa de datos en memoria (mock) que permite ejecutar el flujo completo sin necesidad de backend.

Este documento corresponde a la Actividad 1 y ha sido **actualizado para coincidir con la implementación real**: cada sección describe lo que está efectivamente construido en el repositorio, verificado con compilación y Android Lint (informe completo en `INFORME_AUDITORIA.md`).

## 2. Objetivos

**Objetivo general:** aplicar los conceptos de arquitectura de apps Android (Activities, Fragments, MVVM, recursos XML) construyendo una tienda virtual funcional de tres roles en la misma aplicación.

**Objetivos específicos:**
1. Implementar interfaces completas para los tres roles (Administrador, Vendedor, Comprador) con navegación por roles.
2. Adoptar el patrón MVVM: `Fragment → ViewModel → Repositorio → Datos en memoria`, con `LiveData` como único canal de estado hacia la UI.
3. Resolver el ciclo de vida correctamente: ViewBinding sin fugas (binding nulo en `onDestroyView`), refresh en `onResume`, eventos de operación de una sola entrega.
4. Simular el backend con un mock en memoria con latencia y validaciones, manteniendo la separación de permisos por rol en la capa de datos (`vendorId`, `buyerId`, `role`).
5. Implementar autenticación local persistente (correo/contraseña, recuperación) con soporte de biometría mediante `androidx.biometric`.
6. Construir un sistema visual propio (tema Material 3 personalizado, tipografía Poppins, componentes XML reutilizables con atributos personalizados).
7. Elegir una pasarela de pago demostrativa (sin transacciones reales ni proveedor de pagos) documentada como tal.
8. Verificar calidad con compilación reproducible y Android Lint (0 errores).

## 3. Descripción de la aplicación

| Aspecto | Detalle |
|---|---|
| Qué hace | Tienda virtual de café: los vendedores publican sus cafés y gestionan pedidos; los administradores moderan y consultan reportes; los compradores exploran, compran y siguen sus pedidos. |
| Datos | Seed mock en memoria (`MockDataProvider`): 5 usuarios, 8 productos, 6 pedidos iniciales. El estado se regenera en cada arranque (solo la sesión del usuario persiste en `SharedPreferences`). |
| Pagos | Pasarela **demostrativa**: tarjeta demo `•••• 4242`, PSE simulado y contraentrega. No procesa transacciones reales ni solicita datos bancarios. |
| Regla de negocio | Checkout agrupa el carrito por finca (**un pedido por vendedor**), valida existencias, descuenta stock y los pedidos quedan visibles de inmediato en la sesión del vendedor. |
| Identidad visual | Paleta café oro (`cafe_200…900`, `accent_gold`), tipografía **Poppins**, componentes con esquinas y badges propios. |

**Cuentas demo del mock** (incluidas a propósito como datos semilla, para evaluación):

| Rol | Correo | Contraseña |
|---|---|---|
| Administrador | `admin@cafeorigen.co` | `admin1234` |
| Vendedor (principal) | `vendedor@cafeorigen.co` | `vend1234` |
| Vendedor (2.ª finca) | `betania@cafeorigen.co` | `betania1234` |
| Comprador | `comprador@cafeorigen.co` | `cafe1234` |
| Comprador inactivo (login rechazado) | `a.plata@micorreo.co` | `plata1234` |

## 4. Arquitectura

Patrón **MVVM por capas** con navegación manual por `FragmentManager`:

```
┌────────────────── UI (presentación) ───────────────────┐
│ Activity (Splash, Auth, Main) · Fragment por pantalla   │
│ View Binding · Observa LiveData del ViewModel           │
├───────── Presentación lógica (ViewModel / LiveData) ────┤
│ ViewModel por pantalla · Observer · Eventos de op       │
├────────────── Dominio / datos (repositorios) ───────────┤
│ Repos por rol (Admin/Vendor/Buyer/User/CartRepo)        │
│ Acceso con latencia simulada · Validaciones             │
├────────────────── Fuente de datos ──────────────────────┤
│ MockDataProvider (listas en memoria) · SessionManager   │
└────────────────────────────────────────────────────────┘
```

- **Un LiveData por responsabilidad** en cada ViewModel (lista visible, loading, resultado de operación `OpsState`).
- **Repositorios asíncronos**: devuelven `Result<T>` (`UserRepository.Result`). La fuente se elige en ejecución: `FirebaseBootstrap.isConfigured()` → `core/data/firebase/*` (Auth/Firestore/Storage reales) o, en su defecto, el mock con latencia simulada de 350 ms. Esto permite alternar mock y backend real sin tocar la UI (§17).
- **CartRepository** singleton: carrito en memoria validado contra stock; con Firebase se respalda en `carts/{uid}` (escrituras write-through) y se repinta por listener.
- **Permisos en la capa de datos**: `ordersForBuyer(buyerId)` para el comprador, filtrado por `vendorId` para el vendedor; se filtra al devolver los resultados, no en la UI. Con Firebase el aislamiento se refuerza en las reglas (§17).

## 5. Tecnologías utilizadas

| Área | Tecnología | Versión |
|---|---|---|
| Lenguaje | Java | 17 (source/target compatibility) |
| Build | Gradle | 8.7 (wrapper incluido) |
| SDK de Android | compileSdk / targetSdk / minSdk | 34 / 34 / **26** (Android 8.0) |
| SDK de Android | AGP (`com.android.application`) | 8.x |
| UI | Google Material Components | 1.12.0 |
| UI | AppCompat | 1.7.0 |
| UI | ConstraintLayout | 2.1.4 |
| UI | RecyclerView (ListAdapter + DiffUtil) | 1.3.2 |
| UI | CardView | 1.0.0 |
| Arquitectura | Lifecycle (ViewModel + LiveData) | 2.x |
| Seguridad | `androidx.biometric:biometric` | 1.1.0 |
| Backend | Firebase BoM (`auth`, `firestore`, `storage`) | 33.16.0 |
| Backend | Plugin `com.google.gms:google-services` | 4.4.4 (condicional) |
| Binding | ViewBinding (`buildFeatures.viewBinding`) | habilitado |
| Tipografía | Poppins (Regular/Medium/Semibold/Bold) | recurso local `res/font/` |

## 6. Estructura del proyecto Android

```
CafeOrigen/
├── app/
│   ├── build.gradle                      # Config del módulo (minSdk 26, ViewBinding)
│   └── src/main/
│       ├── AndroidManifest.xml           # 3 Activities, LAUNCHER = SplashActivity
│       ├── java/com/cafeorigen/          # 84 archivos .java
│       │   ├── SplashActivity.java  MainActivity.java
│       │   ├── auth/        (5: AuthActivity, AuthViewModel, Login/Register/Recovery)
│       │   ├── core/model/  (8: User, Product, Order, OrderItem, enums…)
│       │   ├── core/data/   (7: MockDataProvider, 5 repos, SessionManager, CartRepository)
│       │   │   └── firebase/ (12: Bootstrap, Schema, Mappers, Auth/User/Product/
│       │   │                  Order/Cart/Report data sources, cache, guard…)
│       │   ├── core/ui/     (5 componentes propios + RemoteImageLoader)
│       │   ├── core/util/   (1: Validators)
│       │   ├── admin/       (15)
│       │   ├── vendor/      (12)
│       │   └── buyer/       (15)
│       └── res/
│           ├── layout/    (40 layouts)
│           ├── drawable/  (55 XML vectoriales/formas)
│           ├── color/     (3 selectores de estado)
│           ├── font/      (poppins.xml, family con 4 pesos)
│           ├── anim/      (2: fade_in, slide_up)
│           ├── menu/      (6 menús de nav/toolbar)
│           └── values/    (attrs, colors, dimens, strings, styles, themes)
├── gradle/wrapper/                       # Gradle wrapper reproducible
├── firebase/                             # Reglas Firestore/Storage, firebase.json y seed
├── README.md · INFORME_AUDITORIA.md
├── FIREBASE_CONFIGURACION.md             # Checklist y verificación real de Firebase
└── .gitignore                            # excluye build/, .gradle/, local.properties, google-services.json…
```

## 7. Descripción de Activities y Fragments

### Activities (3)

| Activity | Rol en la app | Alta en Manifest |
|---|---|---|
| `SplashActivity` | Splash de marca; enruta a `AuthActivity` (sin sesión) o `MainActivity` (con sesión). **Única `LAUNCHER`** | `exported="true"` + `Theme.CafeOrigen.Splash` |
| `AuthActivity` | Huésped de `LoginFragment`, `RegisterFragment` y `PasswordRecoveryFragment`. Declarado `exported="false"` | `exported="false"` |
| `MainActivity` | Shell de la app logueada: toolbar propia, `BottomNavigationView` con menú por rol, `FrameLayout` contenedor de Fragment, ruteo por rol (§13) y cierre de sesión | `exported="false"` |

### Fragments (23, por paquete)

| Paquete | Fragment | Qué implementa |
|---|---|---|
| `auth/` | `LoginFragment` · `RegisterFragment` · `PasswordRecoveryFragment` | Login con validaciones, biometría (§14), registro, recuperación con código demo de 6 dígitos |
| `admin/` | `AdminDashboardFragment` · `AdminUserManagementFragment` · `AdminUserListFragment` · `UserFormFragment` · `AdminProductListFragment` · `AdminReportFragment` · `VisualDemoFragment` | Dashboard con KPIs, gráfico de barras y alertas reales; hub de usuarios con resumen por estado; CRUD de usuarios (crear, editar, desactivar y reactivar) con reglas de último admin; moderación de productos con confirmación; reporte de ventas 7/30/365 días con exportación CSV confirmada |
| `vendor/` | `VendorDashboardFragment` · `VendorProductsFragment` · `VendorProductFormFragment` · `VendorOrdersFragment` · `VendorOrderDetailFragment` · `VendorProfileFragment` | Panel con alerta de stock<3; CRUD de productos (doble publicación borrador/solicitar moderación, imagen por `ACTION_GET_CONTENT`); pedidos con transición de estados y detalle; perfil editable |
| `buyer/` | `BuyerHomeFragment` · `BuyerCatalogFragment` · `BuyerProductDetailFragment` · `BuyerCartFragment` · `BuyerCheckoutFragment` · `BuyerOrdersFragment` · `BuyerProfileFragment` | Inicio con chips por categoría y destacados; catálogo con búsqueda en vivo; detalle con stepper de cantidad; carrito con stepper total; pasarela demo de pago (§9, C5); historial filtrable; perfil con logout |

## 8. Diseño de layouts XML

40 layouts organizados con la convención `fragment_<rol>_<pantalla>.xml`, `item_<rol>_<elemento>.xml`, `view_<componente>.xml`:

| Grupo | Archivos | Diseño |
|---|---|---|
| Transversales (7) | `activity_main`, `activity_auth`, `activity_splash`, `view_kpi_card`, `view_row_item`, `view_empty_state`, `item_product` | Shell con toolbar + BottomNav + contenedor; componentes personalizados (§9) |
| Auth (3) | `fragment_login`, `fragment_register`, `fragment_password_recovery` | `TextInputLayout` con `startIcon` y `counterMaxLength`, botón principal, enlace de recuperación |
| Admin (9) | `fragment_admin_dashboard/user_management/user_list/user_form/products/report`, `item_admin_product/user`, `item_report_row` | Dashboard con `KpiCard` en grid y `BarChartView`; hub de usuarios con resumen total/activas/inactivas; listas con badges de estado semafórico (bg_tag_*) |
| Vendedor (10) | `fragment_vendor_*` (6) + `item_vendor_*` (3) + `dialog_vendor_edit_profile` | Formulario largo de producto en `ScrollView`; líneas de pedido en detalle; chips de filtro por estado |
| Comprador (9) | `fragment_buyer_*` (7) + `item_buyer_product/cart/order` | `ChipGroup` horizontal para filtros; footer elevado fijo en el carrito; checkout con `RadioGroup` de métodos demo y overlay de éxito |

Principios aplicados: raíces `ScrollView`/`LinearLayout` con `weights` para adaptación (sin `px` en ningún layout — verificado), medidas en `dp`/`sp` vía `dimens.xml`, textos siempre desde `strings.xml`, `contentDescription` en iconos interactivos, `clipToPadding`+`paddingBottom` para el scroll de las listas.

## 9. Componentes utilizados

Del **SDK oficial**:
- `BottomNavigationView` con `setOnItemSelectedListener` y menús por rol.
- `TextInputLayout` / `TextInputEditText` (OutlinedBox con iconos y validación `setError`).
- `MaterialButton` en 5 variantes de estilo; `Chip` / `ChipGroup` (`singleSelection` + `setOnCheckedStateChangeListener`); `MaterialCardView`; `RadioButton` con `drawableStart`; `MaterialToolbar`/`AppBarLayout`; `MaterialAlertDialogBuilder`; `Snackbar`/`Toast`.
- `RecyclerView` + `ListAdapter<T, VH>` con `DiffUtil.ItemCallback` (comparación null-safe con `Objects.equals`, verificada con Lint).
- `LiveData`/`MutableLiveData`/`ViewModelProvider` (scope por pantalla con `this` o por Activity con `requireActivity()` según pantallas hermanas).

**Componentes personalizados propios** (parametrizables desde XML con `attrs.xml`):
| Componente | Archivo Java | Atributos |
|---|---|---|
| `KpiCardView` | `core/ui/KpiCardView.java` | `kpiValue`, `kpiLabel`, `kpiIcon`, `kpiAccentColor` |
| `RowItemView` | `core/ui/RowItemView.java` | `rowIcon`, `rowTitle`, `rowSubtitle`, `rowShowChevron` |
| `BarChartView` | `core/ui/BarChartView.java` | `barColor`, `barTrackColor` (dibujo por `Canvas`/`Paint`) |
| `EmptyStateView` | `core/ui/EmptyStateView.java` | título/mensaje por código o attrs |

## 10. Drawables

55 drawables XML en `res/drawable/`, + 3 selectores de estado en `res/color/`:

| Categoría | Ejemplos | Uso |
|---|---|---|
| Logos/splash | `ic_logo_cafeorigen`, `splash_background` (gradiente), `banner_club` | LAUNCHER y splash screen |
| Iconos vectoriales | `ic_dashboard`, `ic_inventory`, `ic_shopping_cart`, `ic_receipt`, `ic_credit_card`, `ic_account_balance`, `ic_fingerprint`, `ic_logout`, … (≈45) | Menús de navegación, formularios, botones; coloreados con `app:tint` (verificado Lint `UseAppTint`) |
| Badges/estado | `bg_tag_info/warning/success/danger` | Píldoras de estado de pedidos/moderación |
| Formas/bg | `shape_avatar`, `shape_card`, `shape_icon_badge`, `shape_product_thumb`, `shape_search`, `shape_divider`, `bg_navbar`, `bg_nav_active` | Fondos coherentes con el tema |
| Selectores (`res/color/`) | `selector_box_stroke`, `chip_background`, `selector_nav_item` | Estados opcionales según activación |

## 11. Styles y themes

Definidos en `res/values/themes.xml` y `styles.xml` (≈35 estilos):

- **`Theme.CafeOrigen`** — hereda de `Theme.Material3.DayNight.NoActionBar`. Mapea la identidad a los colores de Material 3 (`colorPrimary=color_primary → cafe_700`, `colorSecondary=accent_gold`, con `colorSurface`, `colorOutline`), aplica **Poppins como `fontFamily` global**, barra de estado `cafe_900` y barra de navegación `cafe_700`, y define los estilos de componente por defecto (`materialButtonStyle`, `textInputStyle`, `bottomNavigationStyle`, `materialCardViewStyle`).
- **`Theme.CafeOrigen.Splash`** — fondo `splash_background`, barra translúcida `cafe_900`, `windowNoTitle`.
- **`TextAppearance.CafeOrigen.*`** (10): `Display`, `Headline`, `Title`, `Body`, `Subtitle`, `Caption`, `Price`, `Badge`, `Nav`, `Button` — jerarquía tipográfica única compartida por los 3 roles.
- **`Widget.CafeOrigen.*`**: familias `Button` (`Primary`, `Gold`, `Secondary`, `Destructive`, `.Soft`, `Inline`), `TextInputLayout` (+`ExposedDropdown`), `Card` (`Product`, `Kpi`, `Banner`), `Toolbar`, `BottomNav` (+`.Nav.ActiveIndicator`), `Chip` (`.Regular`), `SearchBar`.

## 12. Recursos values

| Archivo | Contenido |
|---|---|
| `strings.xml` | **347 strings** en español de Colombia, con plurales y parámetros de formato (`%1$s`, `%1$d`) para precios, líneas de pedido, pedidos y validaciones. Los avisos Lint `HardcodedText` residuales son solo de pantallas demo (documentados en §15). |
| `colors.xml` | **37 colores**: escalas de marca →(`cafe_50…900`, `gold_container`, `accent_gold`), colores de Material (`color_primary`, `color_surface`, `color_background`, `text_on_dark`), semáforo de estados (`warning`, `info`, `success`, `danger`, `color_error`) y acentos de badges. |
| `dimens.xml` | **42 dimensiones**: escala de espaciado `spacing_4/…/64` y tamaños de control (`size_field_height`, radios). Nada de `px` hardcode en layouts. |
| `attrs.xml` | **3 declare-styleable** (`KpiCard`, `RowItem`, `BarChart`) con **10 atributos** que parametrizan los componentes propios desde XML. |

## 13. Navegación por roles

Flujo de arranque: `SplashActivity` consulta `SessionManager`; sin sesión → `AuthActivity` (login/registro/recuperación); con sesión → `MainActivity` enruta al primer tab **según el rol**:

| Rol | Menú (`menu_nav_*.xml`) | Primer tab |
|---|---|---|
| ADMIN | `nav_dashboard`, `nav_usuarios`, `nav_productos`, `nav_reporte` | `AdminDashboardFragment` |
| VENDEDOR | `nav_inicio`, `nav_productos`, `nav_pedidos`, `nav_perfil` | `VendorDashboardFragment` |
| COMPRADOR | `nav_inicio`, `nav_catalogo`, `nav_carrito`, `nav_pedidos`, `nav_perfil` | `BuyerHomeFragment` |

Modelo de navegación (`MainActivity`):
- `showRoot(fragment, navId)`: reemplaza el fragment del contenedor (nivel superior, sin `addToBackStack`) y sincroniza la selección del tab actual.
- `openSub(fragment)`: apila encima (`addToBackStack`), para pantallas de detalle/formulario (detalle de producto, checkout, formularios, etc.).
- Tres handlers de bottom-nav **aislados por rol**: `handleAdminNav`, `handleVendorNav`, `handleBuyerNav`. Cada uno **primero valida** `SessionManager.getRole()` y solo acepta los ids de su propio menú: un tab de otro rol no llega al handler de un rol distinto (defensa ante cambios de sesión en caliente).
- **Guarda transversal por paquete** en `canAccess(Fragment)`: antes de abrir cualquier pantalla se compara el paquete del fragment (`buyer`/`vendor`/`admin`) con el rol de la sesión, aunque la llamada venga de código. Si no coincide, se deniega con snackbar (`access_denied_role`) y no se navega. Aplica a `openSub()` y a los saltos programáticos.
- **Barra superior funcional por rol**: buscar abre el listado del rol (catálogo, productos del vendedor o moderación del admin), carrito respeta el control de acceso y notificaciones muestra el estado vacío real (`notifications_empty`), sin pantallas simuladas.
- Saltos programáticos: `openRootCatalog(category)` (prefiltro C1→C2), `openRootCart()` ("Ver carrito"), `openRootOrders()` ("Ver mis pedidos" del checkout), `openRootProducts()` (alerta de stock bajo del panel del vendedor) y `requestLogout()` (confirmación de cierre de sesión); todos verifican rol.
- `popBackStack()` en las `openSub` (los formularios y el detalle regresan al fragment que los abrió).
- En autenticación, el botón **atrás** del sistema regresa a Login desde Registro/Recuperación y sale del flujo desde Login (`OnBackPressedCallback` en `AuthActivity`).

### 13.1 Módulo Administrador (A1–A8)

Las ocho pantallas del rol se resuelven con tres destinos de menú inferior (Dashboard, Usuarios, Productos) más el Reporte como cuarto tab, y pantallas internas apiladas con `openSub`:

| # | Pantalla | Implementación | Puntos clave |
|---|---|---|---|
| A1 | Dashboard | `AdminDashboardFragment` + `AdminDashboardViewModel` | KPIs de ventas/usuarios/productos, gráfico de barras propio y alertas operativas calculadas con datos reales (moderación pendiente, stock bajo, cuentas inactivas). |
| A2 | Gestión de usuarios | `AdminUserManagementFragment` | Hub con resumen total/activas/inactivas y accesos al listado y a la creación; refresca al volver de una operación. |
| A3 | Listado de usuarios | `AdminUserListFragment` + `AdminUsersViewModel` | Búsqueda en vivo, filtro por rol (Todos/Compradores/Vendedores/Admins), badges de estado, menú por fila (editar, desactivar/eliminar, reactivar). |
| A4 | Creación de usuarios | `UserFormFragment` (modo crear) | Validación de nombre, correo (formato y duplicados), teléfono y contraseña; selector de rol. |
| A5 | Edición de usuarios | `UserFormFragment` (modo editar) | Carga la cuenta desde `AdminDataRepository.getUser`; el correo queda bloqueado (identifica la cuenta) y la contraseña se oculta con aviso de restablecimiento desde el login. |
| A6 | Eliminación de usuarios | Diálogo en `AdminUserListFragment` | Borrado lógico con confirmación y mensaje de resultado; la cuenta puede reactivarse después. |
| A7 | Gestión de productos | `AdminProductListFragment` + `AdminProductsViewModel` | Filtros por estado, aprobar, rechazar y eliminar con confirmación; estado vacío distinto por filtro. |
| A8 | Reporte de ventas | `AdminReportFragment` + `AdminReportViewModel` | Periodos 7/30/365 días, ingresos, unidades, productos más vendidos y exportación CSV con confirmación. |

Reglas de negocio del módulo, centralizadas en `AdminUsersViewModel`/`AdminDataRepository` (nunca en las Activities):

- **Auto-protección:** un administrador no puede desactivar su propia cuenta ni cambiar su propio rol.
- **Último administrador:** no se permite desactivar o degradar a la última cuenta ADMIN activa; la UI muestra `admin_last_admin_error`.
- **Reactivación:** las cuentas inactivas conservan su historial y pueden reactivarse (`action_user_activate`).
- **Errores explícitos:** las cargas distinguen `LOAD_ERROR` de una lista vacía y las operaciones fallidas muestran `admin_op_error`, sin estados falsos de "sin datos".
- **Doble fuente:** el repositorio usa el mock en memoria o Firestore según exista `google-services.json`; la UI no conoce la diferencia.

## 14. Autenticación biométrica

Implementada en `LoginFragment` (título T4) con `androidx.biometric:biometric:1.1.0`:

```java
int canAuthenticate = BiometricManager.from(requireContext())
        .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK);

switch (canAuthenticate) {
    case BiometricManager.BIOMETRIC_SUCCESS:
        showBiometricPrompt();          // lanza BiometricPrompt con el diálogo oficial del sistema
        break;
    case BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED:
        // Toast: no hay huella registrada → instrucciones
        break;
    case BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE:
    case BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE:
        // Estados no compatibles: continuar por la vía convencional
        break;
    case BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED:
        // El equipo exige actualización de seguridad
        break;
}
```

Estados del `BiometricPrompt.AuthenticationCallback` cubiertos:

| Callback / código | Comportamiento |
|---|---|
| `onAuthenticationSucceeded` | Restaura el último usuario **solo si hay sesión previa real** (mock) o sesión vigente del SDK (Firebase); luego entra a `MainActivity` |
| `onAuthenticationFailed` | Aviso "huella no reconocida"; el diálogo del sistema sigue abierto para reintentar |
| `ERROR_NEGATIVE_BUTTON` | "Usar contraseña": cierra el diálogo y deja el login convencional listo |
| `ERROR_USER_CANCELED` / `ERROR_CANCELED` | Aviso de cancelación; permanece en login |
| `ERROR_LOCKOUT` | Aviso de bloqueo temporal (30 s) y alternativa con contraseña |
| `ERROR_LOCKOUT_PERMANENT` | Aviso de bloqueo permanente: desbloquear el dispositivo o usar contraseña |
| `ERROR_NO_BIOMETRICS` | Deriva al mensaje de huella no registrada |
| Otros (`HW_UNAVAILABLE`, timeout…) | Mensaje del sistema, sin bloquear el login |

Decisiones de diseño: en `showBiometricPrompt()` se exige **una sesión previa local con contraseña** (`SessionManager.getLastUserId()`); al autenticar con éxito se restaura el último usuario real consultándolo en el mock — **nunca se simula huella** ni se usa biometría para "inventar" una sesión (sin CryptoObject en este alcance). En modo Firebase se exige además que el SDK conserve sesión (`hasSession()`); si el token expiró se pide correo y contraseña. El executor es `ContextCompat.getMainExecutor(requireContext())` y ante cualquier error la app regresa sin bloqueos al login convencional. El permiso `USE_BIOMETRIC` lo declara la propia librería `androidx.biometric` (se fusiona automáticamente al manifest en el build).

## 15. Pruebas realizadas

Metodología honesta: **verificación estática + compilación + Lint**, más las sesiones de runtime puntuales registradas abajo. El detalle completo, con evidencia y el listado de lo **no** probado, está en `INFORME_AUDITORIA.md`.

| Verificación | Método | Resultado |
|---|---|---|
| Compilación completa | `gradle clean assembleDebug` | ✅ BUILD SUCCESSFUL |
| APK generado | output-metadata.json (§16) | ✅ válido, metadata correcta |
| Android Lint | `gradle lintDebug` | ✅ **0 errores** / 83 warnings diferidos |
| Referencias a recursos | Cruce `@type/nombre` + `R.*` en todo `res/` y 69 Java | ✅ 0 rotas reales |
| IDs duplicados | Barrido `@\+id` en los 39 layouts | ✅ 0 |
| Layouts válidos | Parse XML de los 39 | ✅ 39/39 |
| Botones sin acción | Cruce ids interactivos ↔ Java | ✅ 0 |
| Fragmentos huérfanos | Cruce de nombres de clase ↔ referencias en el código | ✅ 0 (22/22 usados) |
| px hardcodeados | Regex en 39 layouts | ✅ 0 |
| Permisos por rol | Inspección de handlers/repositorios | ✅ aislados (buyerId/vendorId/rol) |
| **Login por los 3 roles + logout (runtime)** | Emulador AVD API 37, `logcat crash` | ✅ registro + admin + vendedor con sesión estable (el recorrido detectó 3 defectos, corregidos en commits `fdfd8c0`/`647029b`/vendor-NPE) |
| **Navegación por tabs de los 3 roles (runtime)** | `adb shell input tap` + `uiautomator dump` | ✅ selección y fragmentos correctos, proceso estable, 0 crashes |
| **Moderación admin (runtime)** | Tap "Aprobar" en Pendientes | ✅ café pasa de Pendientes a Activos |
| **Transición de pedido vendedor (runtime)** | Detalle → "Confirmar" | ✅ badge Por confirmar → Confirmado propagado a la lista |
| **Biometría sin huella registrada (runtime)** | Emulador API 37, tap "Ingresar con huella"; `logcat` de `BiometricService` | ✅ estado `NONE_ENROLLED` detectado, aviso al usuario y login convencional disponible (sin simular huella) |
| **Atrás del sistema en autenticación (runtime)** | Recuperación → botón atrás | ✅ regresa al Login en lugar de cerrar la app |
| **Menús por rol (runtime)** | Login comprador y vendedor + `uiautomator dump` | ✅ comprador 5 tabs, vendedor 4 tabs; ningún tab de otro rol presente |
| **Acciones del toolbar (runtime)** | Overflow "Ver carrito" como vendedor, buscar, notificaciones | ✅ acceso denegado con snackbar; buscar abre el listado del rol; notificaciones muestra estado vacío real |
| Compilación completa con Firebase | `gradle :app:assembleDebug` (18/11/2026) | ✅ BUILD SUCCESSFUL, modo mock por defecto |
| Conexión real a Firebase | Requiere `google-services.json` del proyecto | ⚠️ pendiente (`FIREBASE_CONFIGURACION.md` §3 y §6) |
| Checkout completo cross-role, CRUD admin/vendedor, rotación, biometría | — | ❌ runtime pendiente ([ver informe](INFORME_AUDITORIA.md) §16) |

## 16. Generación del APK

```bash
./gradlew clean assembleDebug      # Windows: gradlew.bat clean assembleDebug
```

| Dato | Valor |
|---|---|
| **Ruta exacta** | `C:\Users\kaval\Documents\Default Project\CafeOrigen\app\build\outputs\apk\debug\app-debug.apk` |
| Tamaño | 11.829.511 bytes (≈ 11,8 MB con los SDK de Firebase incluidos) |
| applicationId | `com.cafeorigen` |
| versionName / versionCode | `1.0.0` / `1` |
| minSdk | 26 |
| Certificado | Firma de debug (`debug.keystore` del SDK; **no** incluida en el repositorio) |

## 17. Integración con Firebase (Auth + Cloud Firestore + Storage)

La entrega incorpora una capa de persistencia real **opcional y no invasiva**: la
app detecta si existe `app/google-services.json` y elige la fuente de datos en
tiempo de ejecución. Sin el archivo, todo sigue funcionando con el mock; con el
archivo, los mismos repositorios (mismas firmas, mismos ViewModels, misma UI)
delegan en Firebase.

### 17.1 Selección de fuente

```
Fragment → ViewModel → Repositorio ─┬─ FirebaseBootstrap.isConfigured() == true
                                    │     → core/data/firebase/*
                                    └─ false
                                          → MockDataProvider (mock)
```

`FirebaseBootstrap` es el único punto que conoce el SDK: expone `auth()`,
`db()`, `storage()`, `currentUid()` e `isConfigured()`. `FirestoreSchema`
centraliza los nombres de colecciones/campos y `FirebaseMappers` traduce entre
`DocumentSnapshot` y los modelos existentes (`User`, `Product`, `Order`).

### 17.2 Colecciones

| Colección | Documento | Uso |
|---|---|---|
| `users` | `{uid}` | Perfil, `role` y `active` (autorización) |
| `products` | `{productId}` | Catálogo, `vendorId`, stock, `status` de moderación |
| `orders` | `{orderId}` | Pedidos con `buyerId`, `vendorId`, `items`, `total` |
| `carts` | `{uid}` | Carrito persistente del comprador |
| `categories` | `{clave del enum}` | Catálogo de categorías |

No se requieren índices compuestos: las consultas usan un solo filtro de
igualdad y el orden se resuelve en el cliente (`firestore.indexes.json` vacío).

### 17.3 Reglas de seguridad por rol

Ambos archivos (`firebase/firestore.rules`, `firebase/storage.rules`) son
**deny-by-default** y leen el rol de `users/{uid}`, nunca de un campo enviado por
el cliente:

- **users**: registro público solo crea su propio perfil como `COMPRADOR`
  activo con el correo de su token; la autoedición no puede tocar `role`,
  `active` ni `email`; solo un ADMIN activo cambia roles/estado.
- **products**: el vendedor crea solo a su nombre y solo en `BORRADOR`/`PENDIENTE`
  (no puede autoaprobarse); actualiza únicamente productos con
  `vendorId == request.auth.uid` y sin cambiar de dueño; el ADMIN modera y
  elimina.
- **orders**: el comprador solo lee los suyos y **no tiene ninguna rama de
  escritura** (salvo crear con `buyerId` propio y estado `POR_CONFIRMAR`); el
  vendedor dueño solo avanza el estado sin tocar líneas, total ni contraparte;
  `delete` siempre denegado (histórico).
- **carts**: solo el dueño del `uid`.
- **Storage** (`products/{vendorId}/{archivo}`): lectura con sesión; escritura
  solo del vendedor activo dueño, `image/*` y < 5 MB; cualquier otra ruta
  denegada.

**Limitación declarada:** como el checkout corre en el cliente, la regla de
`products` permite al comprador modificar **únicamente** el campo `stock` y
**solo para reducirlo**. Es una concesión acotada para validar stock sin
backend; la solución definitiva es mover la transacción a una Cloud Function y
eliminar esa rama (documentado en `FIREBASE_CONFIGURACION.md` §5 y §9).

### 17.4 Persistencia, concurrencia y errores

- **Firestore con caché local persistente** (`setPersistenceEnabled(true)`).
- **Checkout transaccional**: valida stock, descuenta, crea un pedido por finca,
  borra el carrito y actualiza la caché en una sola transacción atómica.
- **Carrito real**: `carts/{uid}` con escrituras write-through colapsadas y
  listener de cambios; el carrito sobrevive al cierre de la app y a la rotación.
- **Anti duplicados**: `InFlightGuard` + botones deshabilitados durante login,
  registro, guardados y pago.
- **Errores de conexión**: `FirebaseErrorMapper` traduce `UNAVAILABLE`,
  `PERMISSION_DENIED`, `UNAUTHENTICATED`, `ALREADY_EXISTS`, etc. a mensajes en
  español mostrados en snackbar/campo, sin perder la pantalla.
- **Sesión**: el SDK mantiene la sesión; `SessionManager.restoreLastUser()`
  rehidrata el perfil al entrar con biometría.
- **Imágenes**: `RemoteImageLoader` (`HttpURLConnection` + `LruCache`) muestra
  URLs de Storage sin agregar dependencias (Glide/Coil).

### 17.5 Flujos de correo y datos demo

La recuperación de contraseña usa `sendPasswordResetEmail` real (con el
centinela `RECOVERY_EMAIL_SENT` para que la UI no avance al paso del código
simulado). `firebase/seed/seed-firestore.js` recrea las 5 cuentas demo, las
categorías, los 8 productos y los 6 pedidos mediante `firebase-admin`; la clave
de servicio y `google-services.json` están excluidos de Git.

### 17.6 Estado de verificación (18/11/2026)

| Ítem | Resultado |
|---|---|
| Compilación con el SDK incluido (sin JSON) | ✅ `compileDebugJavaWithJavac` y `assembleDebug` OK |
| Modo mock intacto | ✅ la app sigue arrancando con `MockDataProvider` |
| Plugin google-services aplicado | ⚠️ no verificable sin `google-services.json` |
| Conexión real a Auth/Firestore/Storage | ⚠️ pendiente de credenciales del proyecto de la estudiante |
| Reglas y seed desplegados | ⚠️ pendiente de proyecto + Firebase CLI |

La verificación real (crear proyecto, descargar el JSON, habilitar Email/Password,
publicar reglas, ejecutar el seed y comprobar Auth/Firestore/Storage desde la
app) está detallada paso a paso en `FIREBASE_CONFIGURACION.md`, junto con la
matriz de 20 pruebas de reglas.

## 18. Repositorio GitHub

| Aspecto | Estado |
|---|---|
| Local | ✅ Repositorio Git `main` con historia descriptiva por capas (`chore` config → `feat(core)` → `feat(auth)` → `feat(shell)` → `feat(admin)` → `feat(vendor)` → `feat(buyer)` → `feat(ui)` → `feat(firebase)` → `docs`). |
| Archivos sensibles | ✅ Sin secretos reales: `local.properties` (ruta del SDK del equipo), `build/`, `.gradle/`, keystores, `app/google-services.json`, `.firebaserc` y `firebase/seed/serviceAccount.json` quedan excluidos por `.gitignore`. Las únicas credenciales presentes son las cuentas **demo** (mock y seed), intencionales para la evaluación (§3). |
| Remoto | ✅ Cuenta GitHub de la estudiante: `kimaleja` — repositorio público **`CafeOrigen1`** → <https://github.com/kimaleja/CafeOrigen1>. `origin` configurado y `main` sincronizado con el remoto (`git ls-remote origin refs/heads/main` coincide con el HEAD local). |
| Verificación del push | ✅ Contrastado con `git ls-remote origin refs/heads/main` y vía API `https://api.github.com/repos/kimaleja/CafeOrigen1/branches`. |
| ZIP de entrega | `..\CafeOrigen-entrega-v1.0.0.zip` — generado con `git archive HEAD` (solo archivos committeados; sin `build/`, `.gradle/`, `local.properties`, credenciales ni APK). |

## 19. Conclusiones

1. **La arquitectura se sostiene en la práctica**: el patrón MVVM con `LiveData` fue suficiente para separar la lógica de presentación de cada rol y desarrollar pantalla por pantalla sin duplicar estado; la integración de Firebase lo confirmó: la capa de datos cambió de fuente (mock → Auth/Firestore/Storage) conservando firmas de repositorios, ViewModels y UI.
2. **Recursos con identidad coherente**: el sistema visual (Material 3 + Poppins + 55 drawables + 347 strings) mantiene una identidad única entre los 3 roles, y los componentes propios (`KpiCard`, `RowItem`, `BarChart`, `EmptyState`) redujeron la repetición de XML en dashboards y listas.
3. **El ciclo de calidad funcionó**: Android Lint detectó errores reales (25× `UseAppTint`, 7× `DiffUtilEquals`, duplicados de `app:tint`) que fueron corregidos; el build limpio tras las correcciones (`clean assembleDebug` + `lintDebug` OK) evidencia el proceso de QA aplicado a la entrega.
4. **La seguridad se diseñó por rol, no por pantalla**: las reglas de Firestore/Storage son deny-by-default y leen el rol de `users/{uid}`; un vendedor no puede tocar productos de otra finca, un comprador no puede modificar pedidos y nadie puede autoasignarse ADMIN desde el cliente.

### Limitaciones conocidas (documentadas, no errores)
- **Datos por defecto en mock**: sin `app/google-services.json` (que no se versiona) la capa de datos es el mock en memoria y los datos se regeneran en cada arranque. Con el archivo de configuración, la app usa Firebase y persiste; el procedimiento y la verificación pendiente están en `FIREBASE_CONFIGURACION.md` §1 y §17.6.
- **Checkout en cliente**: para validar stock sin backend, la regla de `products` permite al comprador reducir `stock`; la solución definitiva es una Cloud Function transaccional. Igual criterio para el alta de usuarios desde el panel admin (hoy con `FirebaseApp` secundaria).
- **Pruebas de runtime parcial**: el 04/11/2026 se ejecutó una sesión en emulador (registro + auto-login, arranque del comprador, navegación por tabs) que detectó y corrigió 2 defectos reales; el checkout cruzado entre roles, el CRUD admin/vendedor, la rotación y la biometría quedan sin recorrer en device — ver `INFORME_AUDITORIA.md` §16.
- **Pasarela demo**: no procesa pagos reales ni solicita datos bancarios de ningún tipo.
- **Sin variantes de pantalla**: no hay `values-sw*dp` ni layouts landscape/tablet — la UI escala, pero no está afinada para pantallas grandes.
- **VM compartidos**: los ViewModels compartidos entre lista y formulario (`requireActivity()`) mantienen eventos sin consumir: snackbars/toasts se repiten al rotar; el consumo single-shot solo se aplicó en `BuyerCheckoutViewModel`.
