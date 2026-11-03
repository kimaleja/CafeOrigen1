# Guion técnico — Video demostrativo CaféOrigen (máx. 5 min)

**Grabación:** pantalla (emulador o Android Studio con el proyecto abierto) + voz de la estudiante.
**Cuentas demo para la grabación:** `comprador@cafeorigen.co / cafe1234` → `vendedor@cafeorigen.co / vend1234` → `admin@cafeorigen.co / admin1234`.

> Reglas de la grabación: todo lo que se muestre en el emulador debe estar compilado y en el repo. Si un demo falla al grabar, se repite el bloque — nunca narrar "debería funcionar" sin haberlo visto funcionando.
> Preparación antes de grabar: compilar con `gradlew assembleDebug`, instalar el APK en el emulador y tener abiertos en Android Studio los archivos del guion en tabs separados.

---

## Bloque 1 — Apertura y arquitectura (0:00 – 0:35)

| Elemento | Indicación |
|---|---|
| **Pantalla** | Android Studio con la vista `Project (Java)` de `CafeOrigen` |
| **XML a abrir** | `AndroidManifest.xml` |
| **Código a explicar** | Las 3 Activities: `SplashActivity` (LAUNCHER), `AuthActivity`, `MainActivity`; `minSdk 26` / `targetSdk 34` y `buildFeatures.viewBinding` en `app/build.gradle` |
| **Recurso a señalar** | Carpeta `res/` — señalar con el cursor `layout/` (39), `drawable/` (55), `values/` |
| **Qué demostrar** | De qué trata la app: tienda de café de origen con 3 roles en una misma app |
| **Frase de voz sugerida** | "CaféOrigen es una tienda de café de origen escrita en Java, con arquitectura MVVM por capas y un mock en memoria como fuente de datos" — cerrar este bloque en menos de 35 segundos |

## Bloque 2 — Splash y login con biometría (0:35 – 1:20)

| Elemento | Indicación |
|---|---|
| **Pantalla** | Emulador: arrancar la app de cero (lanzar `SplashActivity`) → pantalla Login |
| **XML a abrir** | `fragment_login.xml` (después volverlo a cerrar) |
| **Código a explicar** | `LoginFragment.onBiometricClick()` (líneas ~125–144): `BiometricManager.canAuthenticate(BIOMETRIC_WEAK)` con sus 3 estados (SUCCESS → prompt, NONE_ENROLLED → aviso, NO_HW/HW_UNAVAILABLE → login convencional) |
| **Recurso a señalar** | `drawable/ic_fingerprint.xml` y el tipográfico del logo `ic_logo_cafeorigen.xml` |
| **Qué demostrar** | Login con la credencial demo `comprador@cafeorigen.co / cafe1234` y validación de campos (mostrar el error por campo vacío); mencionar el flujo de recuperación con código demo |
| **Frase de voz sugerida** | "El login valida email y contraseña contra el repositorio local; la biometría solo reabre la última sesión, nunca simula huella..." |

## Bloque 3 — Flujo completo del COMPRADOR (1:20 – 2:50)

| Elemento | Indicación |
|---|---|
| **Pantalla** | Emulador: rol comprador → **Inicio → Catálogo → Detalle → Carrito → Checkout → Historial** |
| **XML a abrir** | `fragment_buyer_catalog.xml` (chips de filtro + search) y luego `fragment_buyer_checkout.xml` |
| **Código a explicar** | `BuyerCartFragment.confirmRemove()` (confirmación al quitar líneas) y `BuyerDataRepository.checkout()` (agrupar por `vendorId` y descontar stock) |
| **Recurso a señalar** | `res/color/selector_nav_item.xml` y `drawable/bg_tag_info.xml` (tint/estados de los filtros) |
| **Qué demostrar** | 1) Buscar "Huila" en vivo y filtrar por categoría `Café en grano`; 2) agregar ×2 desde el detalle; 3) revisar el carrito (total recalculado); 4) pagar en el checkout demo (tarjeta •••• 4242), pantalla de éxito con id de pedido y "Ver mis pedidos" |
| **Cierre del bloque** | Señalar que el pedido queda `POR CONFIRMAR` y el stock del café bajó |

## Bloque 4 — Flujo VENDEDOR (2:50 – 3:35)

| Elemento | Indicación |
|---|---|
| **Pantalla** | Cerrar sesión → login `vendedor@cafeorigen.co / vend1234` → Panel → Pedidos |
| **XML a abrir** | `fragment_vendor_products.xml` y `item_vendor_product.xml` |
| **Código a explicar** | `VendorOrdersAdapter.Holder.bind()` (badges de estado `bg_tag_*` con semáforo) y `MockDataProvider.updateOrderStatus()` |
| **Recurso a señalar** | `menu_nav_vendedor.xml` (solo 4 tabs del rol) |
| **Qué demostrar** | El pedido `O-3xx` recién creado está en `POR CONFIRMAR`; confirmarlo en un click (transición de estado) y volver al comprador para verlo `CONFIRMADO` en su historial |

## Bloque 5 — Flujo ADMIN y componentes propios (3:35 – 4:25)

| Elemento | Indicación |
|---|---|
| **Pantalla** | Logout → `admin@cafeorigen.co / admin1234` → Dashboard → Productos (moderación) |
| **XML a abrir** | `view_kpi_card.xml` y `attrs.xml` |
| **Código a explicar** | `KpiCardView` leyendo los atributos `kpiValue/kpiLabel/kpiIcon/kpiAccentColor` (mostrar el bloque que obtiene el `TypedArray`) |
| **Recurso a señalar** | `themes.xml` — `Theme.CafeOrigen` con Material 3 + Poppins como `fontFamily` global |
| **Qué demostrar** | KPIs y gráfico de barras (componente propio `BarChartView`); aprobar o rechazar un café pendiente (P-104) y ver el efecto en la lista |

## Bloque 6 — Calidad, APK y cierre (4:25 – 5:00)

| Elemento | Indicación |
|---|---|
| **Pantalla** | Android Studio: terminal con la salida del build |
| **XML a abrir** | `.gitignore` (señalar que excluye `build/`, `local.properties`, keystores) |
| **Código a explicar** | `INFORME_AUDITORIA.md` — resumen: `BUILD SUCCESSFUL`, Lint **0 errores / 83 warnings diferidos**, 0 ids duplicados, 0 referencias rotas y la nota honesta de **sin pruebas de runtime** (sin dispositivo conectado) |
| **Recurso a señalar** | `app/build/outputs/apk/debug/app-debug.apk` (señalar ruta y 6,6 MB) |
| **Qué demostrar** | `git log --oneline` con los commits descriptivos por capas y el repo público `github.com/kimaleja/cafeorigen` (solo si ya está con push verificado) |

---

## Consejos de grabación

1. **Cronómetro**: el guion suma ~4:50. Si un bloque se alarga, recorta el Bloque 5 (mostrar solo el dashboard sin moderación).
2. **Credenciales demo**: aclarar en la toma que las contraseñas mostradas son cuentas demo del seed del mock; no incluir credenciales relacionadas con tu GitHub ni del PAT por ningún motivo.
3. **Evitar distracciones**: no abrir archivos fuera del guion; si se abre algo inadvertido, vuelve a grabar el bloque.
4. **Cierre visual**: mostrar el ZIP de entrega `CafeOrigen-entrega-v1.0.0.zip` en el explorador 3 segundos al final.
5. Si la biometría no está disponible en el emulador, mostrar el mensaje del caso `NONE_ENROLLED` (también es demo válida del flujo) o registrar una huella en el emulador (`Settings → Security → Fingerprint`).
