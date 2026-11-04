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
- **Repositorios asíncronos**: ejecutor propio + `Handler` del main con latencia de 350 ms, devuelven `Result<T>` (`UserRepository.Result`). Esto permite sustituir el mock por un backend real sin tocar la UI.
- **CartRepository** singleton del carrito en memoria (métodos `synchronized`) con validación de stock contra el mock en cada operación.
- **Permisos en la capa de datos**: `ordersForBuyer(buyerId)` para el comprador, filtrado por `vendorId` para el vendedor; se filtra al devolver los resultados, no en la UI.

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
| Binding | ViewBinding (`buildFeatures.viewBinding`) | habilitado |
| Tipografía | Poppins (Regular/Medium/Semibold/Bold) | recurso local `res/font/` |

## 6. Estructura del proyecto Android

```
CafeOrigen/
├── app/
│   ├── build.gradle                      # Config del módulo (minSdk 26, ViewBinding)
│   └── src/main/
│       ├── AndroidManifest.xml           # 3 Activities, LAUNCHER = SplashActivity
│       ├── java/com/cafeorigen/          # 69 archivos .java
│       │   ├── SplashActivity.java  MainActivity.java
│       │   ├── auth/        (5: AuthActivity, AuthViewModel, Login/Register/Recovery)
│       │   ├── core/model/  (8: User, Product, Order, OrderItem, enums…)
│       │   ├── core/data/   (7: MockDataProvider, 5 repos, SessionManager, CartRepository)
│       │   ├── core/ui/     (4 componentes propios)
│       │   ├── core/util/   (1: Validators)
│       │   ├── admin/       (14)
│       │   ├── vendor/      (13)
│       │   └── buyer/       (14)
│       └── res/
│           ├── layout/    (39 layouts)
│           ├── drawable/  (55 XML vectoriales/formas)
│           ├── color/     (3 selectores de estado)
│           ├── font/      (poppins.xml, family con 4 pesos)
│           ├── anim/      (2: fade_in, slide_up)
│           ├── menu/      (6 menús de nav/toolbar)
│           └── values/    (attrs, colors, dimens, strings, styles, themes)
├── gradle/wrapper/                       # Gradle wrapper reproducible
├── README.md · INFORME_AUDITORIA.md      # Documentos de entrega
└── .gitignore                            # excluye build/, .gradle/, local.properties…
```

## 7. Descripción de Activities y Fragments

### Activities (3)

| Activity | Rol en la app | Alta en Manifest |
|---|---|---|
| `SplashActivity` | Splash de marca; enruta a `AuthActivity` (sin sesión) o `MainActivity` (con sesión). **Única `LAUNCHER`** | `exported="true"` + `Theme.CafeOrigen.Splash` |
| `AuthActivity` | Huésped de `LoginFragment`, `RegisterFragment` y `PasswordRecoveryFragment`. Declarado `exported="false"` | `exported="false"` |
| `MainActivity` | Shell de la app logueada: toolbar propia, `BottomNavigationView` con menú por rol, `FrameLayout` contenedor de Fragment, ruteo por rol (§13) y cierre de sesión | `exported="false"` |

### Fragments (22, por paquete)

| Paquete | Fragment | Qué implementa |
|---|---|---|
| `auth/` | `LoginFragment` · `RegisterFragment` · `PasswordRecoveryFragment` | Login con validaciones, biometría (§14), registro, recuperación con código demo de 6 dígitos |
| `admin/` | `AdminDashboardFragment` · `AdminProductListFragment` · `AdminReportFragment` · `AdminUserListFragment` · `UserFormFragment` · `VisualDemoFragment` | KPIs + gráfico de barras propio; moderación de productos (aprobar/rechazar/eliminar); reportes por finca; CRUD de usuarios con validación de email; catálogo visual del sistema |
| `vendor/` | `VendorDashboardFragment` · `VendorProductsFragment` · `VendorProductFormFragment` · `VendorOrdersFragment` · `VendorOrderDetailFragment` · `VendorProfileFragment` | Panel con alerta de stock<3; CRUD de productos (doble publicación borrador/solicitar moderación, imagen por `ACTION_GET_CONTENT`); pedidos con transición de estados y detalle; perfil editable |
| `buyer/` | `BuyerHomeFragment` · `BuyerCatalogFragment` · `BuyerProductDetailFragment` · `BuyerCartFragment` · `BuyerCheckoutFragment` · `BuyerOrdersFragment` · `BuyerProfileFragment` | Inicio con chips por categoría y destacados; catálogo con búsqueda en vivo; detalle con stepper de cantidad; carrito con stepper total; pasarela demo de pago (§9, C5); historial filtrable; perfil con logout |

## 8. Diseño de layouts XML

39 layouts organizados con la convención `fragment_<rol>_<pantalla>.xml`, `item_<rol>_<elemento>.xml`, `view_<componente>.xml`:

| Grupo | Archivos | Diseño |
|---|---|---|
| Transversales (7) | `activity_main`, `activity_auth`, `activity_splash`, `view_kpi_card`, `view_row_item`, `view_empty_state`, `item_product` | Shell con toolbar + BottomNav + contenedor; componentes personalizados (§9) |
| Auth (3) | `fragment_login`, `fragment_register`, `fragment_password_recovery` | `TextInputLayout` con `startIcon` y `counterMaxLength`, botón principal, enlace de recuperación |
| Admin (8) | `fragment_admin_dashboard/products/report/user_list/user_form`, `item_admin_product/user`, `item_report_row` | Dashboard con `KpiCard` en grid y `BarChartView`; listas con badges de estado semafórico (bg_tag_*) |
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
| ADMIN | `nav_dashboard`, `nav_usuarios`, `nav_productos`, `nav_pedidos`, `nav_perfil` | `AdminDashboardFragment` |
| VENDEDOR | `nav_inicio`, `nav_productos`, `nav_pedidos`, `nav_perfil` | `VendorDashboardFragment` |
| COMPRADOR | `nav_inicio`, `nav_catalogo`, `nav_carrito`, `nav_pedidos`, `nav_perfil` | `BuyerHomeFragment` |

Modelo de navegación (`MainActivity`):
- `showRoot(fragment, navId)`: reemplaza el fragment del contenedor (nivel superior, sin `addToBackStack`) y sincroniza la selección del tab actual.
- `openSub(fragment)`: apila encima (`addToBackStack`), para pantallas de detalle/formulario (detalle de producto, checkout, formularios, etc.).
- Tres handlers de bottom-nav **aislados por rol**: `handleAdminNav`, `handleVendorNav`, `handleBuyerNav`. Cada uno **primero valida** `SessionManager.getRole()` y solo acepta los ids de su propio menú: un tab de otro rol no llega al handler de un rol distinto (defensa ante cambios de sesión en caliente).
- Saltos programáticos: `openRootCatalog(category)` (prefiltro C1→C2), `openRootCart()` ("Ver carrito" en snackbar), `openRootOrders()` ("Ver mis pedidos" del checkout), `openRootProducts()` (alerta de stock bajo del panel del vendedor) y `requestLogout()` (confirmación de cierre de sesión).
- `popBackStack()` en las `openSub` (los formularios y el detalle regresan al fragment que los abrió).

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
}
```

Decisiones de diseño: en `showBiometricPrompt()` se exige **una sesión previa local con contraseña** (`SessionManager.getLastUserId()`); al autenticar con éxito se restaura el último usuario real consultándolo en el mock — **nunca se simula huella** ni se usa biometría para "inventar" una sesión (sin CryptoObject en este alcance). El executor es `ContextCompat.getMainExecutor(requireContext())` y ante `onAuthenticationError` la app regresa sin bloqueos al login convencional. El permiso `USE_BIOMETRIC` lo declara el propia librería `androidx.biometric` (se fusiona automáticamente al manifest en el build).

## 15. Pruebas realizadas

Metodología honesta: **verificación estática + compilación + Lint**; en esta entrega no hay pruebas de runtime (sin dispositivo/emulador conectado — `adb devices` vacío). El detalle completo, con evidencia y el listado de lo **no** probado, está en `INFORME_AUDITORIA.md`.

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
| Checkout completo cross-role, CRUD admin/vendedor, rotación, biometría | — | ❌ runtime pendiente ([ver informe](INFORME_AUDITORIA.md) §16) |

## 16. Generación del APK

```bash
./gradlew clean assembleDebug      # Windows: gradlew.bat clean assembleDebug
```

| Dato | Valor |
|---|---|
| **Ruta exacta** | `C:\Users\kaval\Documents\Default Project\CafeOrigen\app\build\outputs\apk\debug\app-debug.apk` |
| Tamaño | 6.693.196 bytes |
| applicationId | `com.cafeorigen` |
| versionName / versionCode | `1.0.0` / `1` |
| minSdk | 26 |
| Certificado | Firma de debug (`debug.keystore` del SDK; **no** incluida en el repositorio) |

## 17. Repositorio GitHub

| Aspecto | Estado |
|---|---|
| Local | ✅ Repositorio Git `main` con historia descriptiva por capas (`git log`: `chore` config → `feat(core)` → `feat(auth)` → `feat(shell)` → `feat(admin)` → `feat(vendor)` → `feat(buyer)` → `feat(ui)` → `docs`). Working tree limpio. |
| Archivos sensibles | ✅ Sin secretos reales: `local.properties` (ruta del SDK del equipo), `build/`, `.gradle/` y keystores quedan excluidos por `.gitignore`. Las únicas credenciales presentes son las cuentas **demo** del mock, intencionales para la evaluación (§3). |
| Remoto | Cuenta GitHub de la estudiante: `kimaleja` · repositorio público previsto: `cafeorigen`. **Push pendiente de ejecución y verificación** — GitHub no acepta contraseña de cuenta para Git/API: se requiere un PAT con scope `repo`. Procedimiento preparado en `..\publicar-github.ps1` (crea el repo público vía API, agrega `origin`, hace push y compara el HEAD con `origin/main`). |
| Verificación del push | Se declarará verificado únicamente cuando `git ls-remote origin refs/heads/main` coincida con el HEAD local (`git rev-parse HEAD`), o vía API `https://api.github.com/repos/kimaleja/cafeorigen/branches`. |
| ZIP de entrega | `..\CafeOrigen-entrega-v1.0.0.zip` — generado con `git archive HEAD` (solo archivos committeados; sin `build/`, `.gradle/`, `local.properties` ni APK). |

## 18. Conclusiones

1. **La arquitectura se sostiene en la práctica**: el patrón MVVM con `LiveData` fue suficiente para separar la lógica de presentación de cada rol y desarrollar pantalla por pantalla sin duplicar estado; el caso de estudio del repositorio demostró cómo la capa de datos (mock) puede sustituirse por un backend real sin tocar la UI.
2. **Recursos con identidad coherente**: el sistema visual (Material 3 + Poppins + 55 drawables + 347 strings) mantiene una identidad única entre los 3 roles, y los componentes propios (`KpiCard`, `RowItem`, `BarChart`, `EmptyState`) redujeron la repetición de XML en dashboards y listas.
3. **El ciclo de calidad funcionó**: Android Lint detectó errores reales (25× `UseAppTint`, 7× `DiffUtilEquals`, duplicados de `app:tint`) que fueron corregidos; el build limpio tras las correcciones (`clean assembleDebug` + `lintDebug` OK) evidencia el proceso de QA aplicado a la entrega.

### Limitaciones conocidas (documentadas, no errores)
- **Sin backend**: la capa de datos es un mock en memoria; los datos del negocio se regeneran en cada arranque (por diseño, solo persiste la sesión del usuario).
- **Pruebas de runtime parcial**: el 04/11/2026 se ejecutó una sesión en emulador (registro + auto-login, arranque del comprador, navegación por tabs) que detectó y corrigió 2 defectos reales; el checkout cruzado entre roles, el CRUD admin/vendedor, la rotación y la biometría quedan sin recorrer en device — ver `INFORME_AUDITORIA.md` §16.
- **Pasarela demo**: no procesa pagos reales ni solicita datos bancarios de ningún tipo.
- **Sin variantes de pantalla**: no hay `values-sw*dp` ni layouts landscape/tablet — la UI escala, pero no está afinada para pantallas grandes.
- **VM compartidos**: los ViewModels compartidos entre lista y formulario (`requireActivity()`) mantienen eventos sin consumir: snackbars/toasts se repiten al rotar; el consumo single-shot solo se aplicó en `BuyerCheckoutViewModel`.
