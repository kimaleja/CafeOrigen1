# ☕ CaféOrigen

Aplicación Android nativa (Java) para la compra de café de especialidad colombiano directamente de caficultores a compradores, con trazabilidad de origen (región, variedad, proceso, altitud), panel de moderación y una marketplace de tres roles en la misma app.

> Proyecto académico de entrega final. La capa de datos es un **mock en memoria** (no hay backend): las credenciales listadas abajo son datos demo incluidos a propósito en el seed de la app.

## Tecnologías

| Área | Tecnología |
|---|---|
| Lenguaje | Java 17 |
| UI | XML + Material Components 3 (1.12.0), ViewBinding, RecyclerView, ConstraintLayout |
| Arquitectura | MVVM por capas — `Fragment → ViewModel → Repositorio → Datos en memoria` |
| Estado | `LiveData` observable, eventos de operación por ViewModel |
| Datos | Mock en memoria (`MockDataProvider`) con repositorios asíncronos de demostración; carrito singleton |
| Seguridad/UX | `androidx.biometric` (login con huella), sesión persistente con `SharedPreferences` |
| Build | Gradle 8.7 + Android Gradle Plugin, `compileSdk 34`, `minSdk 26`, `targetSdk 34` |
| QA | Android Lint 0 errores (informe en `INFORME_AUDITORIA.md`) |

## Arquitectura

```
app/src/main/java/com/cafeorigen/
├── SplashActivity.java          # Splash (Activity LAUNCHER)
├── MainActivity.java            # Shell: toolbar, bottom-nav y ruteo por rol
├── auth/                        # AuthActivity, AuthViewModel, LoginFragment,
│                                #   RegisterFragment, PasswordRecoveryFragment
├── core/
│   ├── model/                   # User, Product, Order, OrderItem, enums (Role, ProductStatus…)
│   ├── data/                    # MockDataProvider (seed), repos por rol, SessionManager, CartRepository
│   ├── ui/                      # Componentes reutilizables: KpiCard, RowItem, BarChart, EmptyState
│   └── util/                    # Validators (email, contraseña, campos)
├── admin/                       # Rol Administrador (fragment / viewModel / adapter por pantalla)
├── vendor/                      # Rol Vendedor
└── buyer/                       # Rol Comprador
```

Flujo de datos: la UI observa `LiveData` del ViewModel; el ViewModel llama al repositorio (executor + callback con latencia simulada); el repositorio consulta el mock y devuelve `Result<T>`. La navegación es manual (`FragmentManager` con `showRoot`/`openSub`) y `MainActivity` enruta el bottom-nav según el rol en sesión.

## Roles

| Rol | Cuenta demo | Acceso |
|---|---|---|
| 👑 **ADMIN** | `admin@cafeorigen.co / admin1234` | Dashboard, moderación de productos, gestión de usuarios, reportes |
| 🌱 **VENDEDOR** | `vendedor@cafeorigen.co / vend1234` | Panel, CRUD de productos, gestión de pedidos, perfil |
| 🛒 **COMPRADOR** | `comprador@cafeorigen.co / cafe1234` | Inicio, catálogo, detalle, carrito, checkout, historial, perfil |

Cuentas extra para pruebas: vendedor alternativo `betania@cafeorigen.co / betania1234` (U-1005, verifica el aislamiento por finca) y comprador inactivo `a.plata@micorreo.co / plata1234` (U-1004, el login debe rechazarlo con la app en ejecución).

## Funcionalidades

### Administrador (A1–A6)
- Dashboard con KPIs (usuarios, productos, ventas) y gráfico de barras de la semana.
- Moderación de productos publicados por vendedores (aprobar / rechazar / eliminar).
- Gestión de usuarios: crear, editar (rol, estado), activar/desactivar, con validación de email duplicado.
- Reportes con detalle por finca (top productos por ingresos).

### Vendedor (V1–V8)
- Panel KPI con alertas de stock bajo (< 3 lb).
- CRUD de productos: formulario completo con selección de imagen desde galería, doble publicación (borrador / solicitar moderación).
- Estado de pedido en un click (confirmar → despachar → entregar / cancelar), detalle con líneas e ítems.
- Perfil editable y cierre de sesión. Aislamiento por `vendorId`: cada finca solo ve sus productos y pedidos.

### Comprador (C1–C6)
- Inicio con banner, accesos por categoría y cafés destacados.
- Catálogo con búsqueda en vivo (nombre/región) y filtros por categoría; solo productos activos con stock.
- Detalle de producto: origen completo (región, variedad, proceso, altitud) y stepper de cantidad con tope de stock.
- Carrito: línea por producto con stepper +/− (validado), quitar con confirmación, subtotales y total.
- Checkout: resumen de compra (agrupa por vendedor destino), formulario de entrega con validaciones (nombre, teléfono ≥ 7 dígitos, dirección), pasarela **demostrativa** (tarjeta •••• 4242 / PSE / contraentrega) sin datos ni transacciones reales, y pantalla de confirmación con ids de pedido.
- Historial por comprador con chips por estado; perfil con accesos y logout.

Cuando un comprador confirma la compra: la app valida existencias, crea **un pedido por finca**, descuenta stock en el mock y el pedido queda visible de inmediato para el vendedor correspondiente.

## Instrucciones de ejecución

1. Requisito: Android Studio (Koala o superior) con `compileSdk 34` disponible, o Android SDK Platform 34 instalado.
2. Opción A (Android Studio): `File → Open` → carpeta `CafeOrigen` → Run ▶ en un emulador o dispositivo con **Android 8.0 (API 26) o superior**.
3. Opción B (CLI): `./gradlew installDebug` con un dispositivo/emulador conectado.
4. Ingresa con una de las cuentas demo de la tabla anterior. La biometría requiere huella registrada en el dispositivo (si no hay, el botón la ofrece como entrada demo).

Los datos (productos, pedidos, usuarios) se regeneran en cada arranque — es un mock intencional; el estado no persiste entre sesiones (salvo la sesión del usuario logueado).

## Instrucciones de compilación

```bash
# Compilación del APK de debug (requiere JDK 17)
./gradlew clean assembleDebug
# → app/build/outputs/apk/debug/app-debug.apk

# Lint estático (0 errores al cierre de la auditoría)
./gradlew lintDebug
# → app/build/reports/lint-results-debug.html
```

En Windows: `gradlew.bat` en vez de `./gradlew`. Si Gradle no resuelve el SDK, `local.properties` **se genera automáticamente** en la primera apertura del proyecto en Android Studio (apunta al SDK local del equipo; no debe compartirse).

## Estructura del repositorio

```
CafeOrigen/
├── app/                          # Módulo de aplicación (src/main: java + res)
├── build.gradle / settings.gradle/ gradle.properties  # Config de Gradle y módulos
├── gradlew / gradlew.bat / gradle/wrapper/            # Wrapper de Gradle (compilación reproducible)
├── .gitignore                    # Excluye build/, .gradle/, local.properties, keystores
├── INFORME_AUDITORIA.md          # Informe de pruebas: 15 puntos verificables
└── README.md                     # Este documento
```

## Notas de entrega

- Sin backend real: la implementación de red se limita a `Result<T>` simulado para demostrar la arquitectura reactiva.
- Sin credenciales reales, ni claves API, ni keystore de firma en el repositorio.
- Detalle completo de la batería de verificación (y de lo que **no** fue probado en runtime) en `INFORME_AUDITORIA.md`.
