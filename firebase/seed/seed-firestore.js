/*
 * ============================================================================
 * CafeOrigen — Carga inicial de datos en Cloud Firestore
 *
 * Recrea en Firebase las cuentas demo y el catálogo que hoy viven en
 * MockDataProvider, para que la app se vea igual al activar Firebase.
 *
 * USO (nunca contra producción sin revisar):
 *   1) npm install            (dentro de firebase/seed)
 *   2) Descargar la clave de servicio desde la consola de Firebase:
 *      Configuración del proyecto → Cuentas de servicio → Generar nueva clave.
 *      Guardarla como firebase/seed/serviceAccount.json (está en .gitignore).
 *   3) node seed-firestore.js
 *
 * Requiere el SDK de administrador (firebase-admin). Este script NO forma
 * parte del APK y NO se ejecuta desde la app: es una herramienta de puesta
 * en marcha. Las contraseñas son las cuentas demo documentadas en el README.
 * ============================================================================
 */

const admin = require('firebase-admin');
const path = require('path');
const fs = require('fs');

const serviceAccountPath = process.env.FIREBASE_SERVICE_ACCOUNT
  || path.join(__dirname, 'serviceAccount.json');

if (!fs.existsSync(serviceAccountPath)) {
  console.error('\n[ERROR] No se encontró la clave de servicio en: ' + serviceAccountPath);
  console.error('Descárgala desde la consola de Firebase y guárdala allí,');
  console.error('o define FIREBASE_SERVICE_ACCOUNT con la ruta absoluta.\n');
  process.exit(1);
}

admin.initializeApp({
  credential: admin.credential.cert(require(path.resolve(serviceAccountPath))),
});

const auth = admin.auth();
const db = admin.firestore();

const DAY_MS = 24 * 60 * 60 * 1000;
const daysAgo = (days) => admin.firestore.Timestamp.fromDate(new Date(Date.now() - days * DAY_MS));

// ---------------------------------------------------------------------------
// Usuarios demo (mismos correos y contraseñas que el mock)
// ---------------------------------------------------------------------------
const USERS = [
  { name: 'Camila Restrepo', email: 'comprador@cafeorigen.co', phone: '3001112233',
    password: 'cafe1234', role: 'COMPRADOR', active: true },
  { name: 'Diego Torres · Finca El Mirador', email: 'vendedor@cafeorigen.co', phone: '3004445566',
    password: 'vend1234', role: 'VENDEDOR', active: true },
  { name: 'Laura Gómez', email: 'admin@cafeorigen.co', phone: '3007778899',
    password: 'admin1234', role: 'ADMINISTRADOR', active: true },
  { name: 'Andrés Plata', email: 'a.plata@micorreo.co', phone: '3012345678',
    password: 'plata1234', role: 'COMPRADOR', active: false },
  { name: 'Marta Ruiz · Finca Betania', email: 'betania@cafeorigen.co', phone: '3005556677',
    password: 'betania1234', role: 'VENDEDOR', active: true },
];

// ---------------------------------------------------------------------------
// Productos (vendorKey se resuelve al uid real de la cuenta)
// ---------------------------------------------------------------------------
const PRODUCTS = [
  { id: 'P-101', vendorKey: 'vendedor@cafeorigen.co', vendorName: 'Finca El Mirador · Diego Torres',
    name: 'Huila · Supremo Lavado', region: 'Huila', variety: 'Caturra', process: 'Lavado',
    altitudeM: 1750, price: 38000, stock: 82, category: 'CAFE_GRANO', status: 'ACTIVO',
    description: 'Notas a chocolate blanco, panela y naranja. Taza dulce y balanceada, ideal para espresso y métodos de filtrado.' },
  { id: 'P-102', vendorKey: 'betania@cafeorigen.co', vendorName: 'Finca Betania · Marta Ruiz',
    name: 'Nariño · Reserva Natural', region: 'Nariño', variety: 'Pink Bourbon', process: 'Natural',
    altitudeM: 1950, price: 52000, stock: 34, category: 'CAFE_GRANO', status: 'ACTIVO',
    description: 'Fermentación natural de 48 horas. Cuerpo cremoso con notas a fruta roja y cacao.' },
  { id: 'P-103', vendorKey: 'vendedor@cafeorigen.co', vendorName: 'Vereda La Chapa · Luis Ortega',
    name: 'Cauca · Honey Selecto', region: 'Cauca', variety: 'Castillo', process: 'Honey',
    altitudeM: 1680, price: 44500, stock: 56, category: 'CAFE_MOLIDO', status: 'PENDIENTE',
    description: 'Mucílago conservado en secado lento. Perfil a panela, melocotón y acidez cítrica suave.' },
  { id: 'P-104', vendorKey: 'betania@cafeorigen.co', vendorName: 'Finca El Sol · Grupo Andino',
    name: 'Sierra Nevada · Rainforest', region: 'Magdalena', variety: 'Typica', process: 'Lavado',
    altitudeM: 1450, price: 47000, stock: 18, category: 'CAFE_VERDE', status: 'PENDIENTE',
    description: 'Cultivado bajo sombra nativa certificada. Notas a almendra tostada y caña de azúcar.' },
  { id: 'P-105', vendorKey: 'betania@cafeorigen.co', vendorName: 'Finca Betania · Marta Ruiz',
    name: 'Nariño · Geisha Edición Limitada', region: 'Nariño', variety: 'Geisha', process: 'Lavado',
    altitudeM: 2050, price: 88000, stock: 9, category: 'CAFE_GRANO', status: 'ACTIVO',
    description: 'Lote micro de Geisha. Jasmín, bergamota y azúcar moreno; potencia aromática de puntuación 90+.' },
  { id: 'P-106', vendorKey: 'vendedor@cafeorigen.co', vendorName: 'Finca El Mirador · Diego Torres',
    name: 'Huila · Peaberry Panela', region: 'Huila', variety: 'Java', process: 'Natural',
    altitudeM: 1800, price: 41000, stock: 12, category: 'CAFE_GRANO', status: 'BORRADOR',
    description: 'Granos pepita de calidad café. Secado en marquesina, perfil dulce de panela y maní.' },
  { id: 'P-107', vendorKey: 'vendedor@cafeorigen.co', vendorName: 'Café Tolima Verde · Rubén Lozano',
    name: 'Tolima · Descafeinado', region: 'Tolima', variety: 'Castillo', process: 'Lavado',
    altitudeM: 1600, price: 39500, stock: 0, category: 'CAFE_MOLIDO', status: 'RECHAZADO',
    description: 'Descafeinado con agua de caña, sin solventes químicos. Base de caramelo y nueces.' },
  { id: 'P-108', vendorKey: 'vendedor@cafeorigen.co', vendorName: 'Finca El Mirador · Diego Torres',
    name: 'Kit Cata: Vaso + Filtro', region: '', variety: '', process: '',
    altitudeM: 0, price: 25000, stock: 40, category: 'ACCESORIOS', status: 'ACTIVO',
    description: 'Kit de degustación con vaso cámara y 50 filtros de papel instruidos.' },
];

// ---------------------------------------------------------------------------
// Pedidos demo (buyerId demo conserva los ids del mock para las cuentas que
// no existen en Auth; las cuentas reales usan su uid)
// ---------------------------------------------------------------------------
const ORDERS = [
  { id: 'O-201', buyerKey: 'comprador@cafeorigen.co', buyerName: 'Camila Restrepo',
    vendorKey: 'vendedor@cafeorigen.co', address: 'Cra 45 #23-10, Bogotá', days: 0,
    status: 'POR_CONFIRMAR',
    items: [
      { productId: 'P-101', productName: 'Huila · Supremo Lavado', units: 4, subtotal: 152000 },
      { productId: 'P-108', productName: 'Kit Cata: Vaso + Filtro', units: 1, subtotal: 25000 },
    ] },
  { id: 'O-202', buyerKey: 'demo:U-2001', buyerName: 'Julián Ossa',
    vendorKey: 'vendedor@cafeorigen.co', address: 'Calle 8 #15-22, Medellín', days: 1,
    status: 'CONFIRMADO',
    items: [{ productId: 'P-101', productName: 'Huila · Supremo Lavado', units: 2, subtotal: 76000 }] },
  { id: 'O-203', buyerKey: 'demo:U-2002', buyerName: 'Natalia Pardo',
    vendorKey: 'vendedor@cafeorigen.co', address: 'Av 68 #102-31, Cali', days: 3,
    status: 'DESPACHADO',
    items: [{ productId: 'P-106', productName: 'Huila · Peaberry Panela', units: 6, subtotal: 246000 }] },
  { id: 'O-204', buyerKey: 'comprador@cafeorigen.co', buyerName: 'Camila Restrepo',
    vendorKey: 'vendedor@cafeorigen.co', address: 'Cra 45 #23-10, Bogotá', days: 6,
    status: 'ENTREGADO',
    items: [
      { productId: 'P-101', productName: 'Huila · Supremo Lavado', units: 3, subtotal: 114000 },
      { productId: 'P-108', productName: 'Kit Cata: Vaso + Filtro', units: 2, subtotal: 50000 },
    ] },
  { id: 'O-205', buyerKey: 'demo:U-2003', buyerName: 'Óscar Vélez',
    vendorKey: 'vendedor@cafeorigen.co', address: 'Cll 70 #4-15, Barranquilla', days: 9,
    status: 'CANCELADO',
    items: [{ productId: 'P-107', productName: 'Tolima · Descafeinado', units: 5, subtotal: 197500 }] },
  { id: 'O-206', buyerKey: 'comprador@cafeorigen.co', buyerName: 'Camila Restrepo',
    vendorKey: 'betania@cafeorigen.co', address: 'Cra 45 #23-10, Bogotá', days: 1,
    status: 'CONFIRMADO',
    items: [{ productId: 'P-102', productName: 'Nariño · Reserva Natural', units: 2, subtotal: 104000 }] },
];

const CATEGORIES = [
  { id: 'CAFE_GRANO', label: 'Café en grano', order: 1 },
  { id: 'CAFE_MOLIDO', label: 'Café molido', order: 2 },
  { id: 'CAFE_VERDE', label: 'Café verde', order: 3 },
  { id: 'ACCESORIOS', label: 'Accesorios', order: 4 },
];

async function upsertUser(user) {
  let record;
  try {
    record = await auth.getUserByEmail(user.email);
  } catch (error) {
    if (error.code !== 'auth/user-not-found') {
      throw error;
    }
    record = await auth.createUser({
      email: user.email,
      password: user.password,
      displayName: user.name,
      emailVerified: true,
      disabled: false,
    });
    console.log('  + Auth creado: ' + user.email);
  }
  await db.collection('users').doc(record.uid).set({
    name: user.name,
    email: user.email,
    phone: user.phone,
    role: user.role,
    active: user.active,
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
    seededAt: admin.firestore.FieldValue.serverTimestamp(),
  }, { merge: true });
  console.log('  = Perfil: ' + user.email + ' (' + user.role + ')');
  return record.uid;
}

async function main() {
  console.log('\nCafeOrigen — carga inicial de Firestore\n');

  const uidByEmail = new Map();
  for (const user of USERS) {
    uidByEmail.set(user.email, await upsertUser(user));
  }

  console.log('\nCategorías:');
  for (const category of CATEGORIES) {
    await db.collection('categories').doc(category.id).set({
      label: category.label,
      order: category.order,
    }, { merge: true });
    console.log('  = ' + category.id);
  }

  console.log('\nProductos:');
  for (const product of PRODUCTS) {
    const vendorId = uidByEmail.get(product.vendorKey);
    if (!vendorId) {
      throw new Error('Vendedor sin uid: ' + product.vendorKey);
    }
    const data = {
      vendorId,
      vendorName: product.vendorName,
      name: product.name,
      region: product.region,
      variety: product.variety,
      process: product.process,
      altitudeM: product.altitudeM,
      price: product.price,
      stock: product.stock,
      description: product.description,
      category: product.category,
      status: product.status,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
      seededAt: admin.firestore.FieldValue.serverTimestamp(),
    };
    await db.collection('products').doc(product.id).set(data, { merge: true });
    console.log('  = ' + product.id + ' · ' + product.name);
  }

  console.log('\nPedidos:');
  for (const order of ORDERS) {
    const buyerId = uidByEmail.get(order.buyerKey) || order.buyerKey;
    const vendorId = uidByEmail.get(order.vendorKey);
    if (!vendorId) {
      throw new Error('Vendedor sin uid: ' + order.vendorKey);
    }
    const total = order.items.reduce((sum, item) => sum + item.subtotal, 0);
    await db.collection('orders').doc(order.id).set({
      buyerId,
      vendorId,
      buyerName: order.buyerName,
      address: order.address,
      status: order.status,
      items: order.items,
      total,
      createdAt: daysAgo(order.days),
      updatedAt: daysAgo(order.days),
      seededAt: admin.firestore.FieldValue.serverTimestamp(),
    }, { merge: true });
    console.log('  = ' + order.id + ' · ' + order.buyerName + ' · ' + order.status);
  }

  console.log('\nListo. Vuelve a abrir la app: el catálogo y los pedidos ya vienen de Firestore.\n');
}

main().catch((error) => {
  console.error('\n[ERROR] La carga falló: ' + error.message + '\n');
  process.exit(1);
});
