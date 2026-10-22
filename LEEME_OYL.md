# OYL Frutos Secos — demo Android (15 pantallas)

Proyecto Kotlin/Jetpack Compose/Material 3 con Room SQLite y arquitectura sencilla con ViewModel.

## Cómo abrir
1. Descomprime el ZIP y abre la carpeta `OYLFrutosSecos` desde Android Studio.
2. Selecciona Gradle JDK 17 y comprueba SDK Platform 35.
3. Sincroniza Gradle (requiere Internet para descargar dependencias) y ejecuta `app`.
4. Si Gradle solicita instalar componentes, permite su descarga.

Configuración base: AGP 8.13.1, Gradle 8.13, Kotlin 2.2.10, KSP 2.2.10-2.0.2.

## Pantallas
1. Bienvenida; 2. Registro; 3. Login; 4. Dashboard; 5. Mapa; 6. Catálogo; 7. Detalle; 8. Escáner QR; 9. Carrito; 10. Confirmación; 11. Gestión productos; 12. Inventario; 13. Pedidos; 14. Notificaciones; 15. Perfil.

## Cuentas de prueba
Administrador: alias `admin`, contraseña `admin123`. Es una credencial ficticia para la evaluación, NO segura para producción.
Cliente: crear una cuenta desde Registro. Los datos locales de carrito y pedidos se separan por ID de cliente.

## Qué funciona offline
- Registro e inicio de sesión local, catálogo, filtros y detalle.
- Carrito separado por cliente, solicitud de pedido local y consulta de estado.
- Gestión de productos, fotografía con cámara, stock local, notificaciones internas.
- QR mediante foto de cámara y ML Kit (código que contenga ID de producto).
- Apertura de ubicación referencial en aplicación de mapas instalada.
- Datos persistidos en Room/SQLite.

## Pendiente de integrar para cumplir el sistema conectado completo
- Backend Spring Boot real, endpoints REST y Retrofit autenticado; el archivo `BackendApi.kt` contiene el contrato inicial, no está conectado a un servidor.
- Reserva/descuento transaccional de stock y validación oficial de precios: NO se ejecutan en la demo local.
- Mapa integrado con marcadores de tienda y puntos de retiro reales; por ahora se abre una aplicación externa con ubicación referencial.
- Escáner QR en tiempo real: la demo procesa una foto tomada con cámara, no un flujo de video continuo.
- Notificaciones push remotas: por ahora hay avisos internos locales, sin Firebase Cloud Messaging.
- Edición completa de perfil, subida de fotografías a backend, recuperación de contraseña, tests de integración y seguridad de credenciales.
- Pruebas de compilación en Android Studio y generación de APK release firmado: no disponibles en este entorno.

## Nota de seguridad
Las contraseñas de clientes se guardan sin hash únicamente en la demo académica. No utilices contraseñas reales ni datos personales auténticos.
