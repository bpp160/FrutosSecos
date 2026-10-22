# OYL Frutos Secos

## Descripción del proyecto

**OYL Frutos Secos** es una aplicación móvil Android desarrollada con
**Kotlin**, **Android Studio**, **Jetpack Compose** y **Material 3**. Su
objetivo es presentar un catálogo de frutos secos, permitir simular una
compra y administrar las existencias de los productos desde una interfaz
móvil.

## Integrantes

-   Benjamín Ignacio Pérez Pizarro

## Tecnologías utilizadas

-   **Kotlin:** lenguaje de programación.
-   **Android Studio:** entorno de desarrollo.
-   **Jetpack Compose:** creación de la interfaz de usuario mediante
    funciones Kotlin.
-   **Material 3:** componentes visuales, como botones, campos de texto,
    menús y navegación.
-   **Gradle:** configuración y construcción del proyecto.

## Funcionalidades implementadas

### 1. Catálogo de productos

-   Visualización de los productos disponibles.
-   Presentación de información del producto, como nombre, categoría,
    precio y stock.
-   Opción para agregar productos al carrito.
-   Validaciones para evitar seleccionar unidades que superen las
    existencias disponibles, según la lógica implementada.

### 2. Carrito de compras

-   Visualización de los productos seleccionados.
-   Revisión de las cantidades agregadas.
-   Cálculo del total de la compra a partir de los precios y las
    cantidades.
-   Simulación de la confirmación de un pedido.

### 3. Administración de inventario

-   Selección de un producto mediante un menú desplegable.
-   Modificación de las existencias.
-   Validación de la cantidad ingresada para evitar valores negativos o
    no válidos, según la lógica implementada.

### 4. Interfaz y navegación

-   Navegación entre las secciones de catálogo, carrito y
    administración.
-   Uso de componentes de Jetpack Compose y Material 3.
-   Actualización de la interfaz cuando cambian los estados de la
    aplicación.

## Alcance y limitaciones

La versión actual funciona como una demostración local. Los datos se
mantienen en memoria durante la ejecución y **no se garantiza que
permanezcan guardados al cerrar la aplicación**. No se incluye una base
de datos persistente, un servidor remoto ni procesamiento de pagos
reales. La confirmación de compra es una simulación.

## Requisitos previos

-   Android Studio instalado.
-   JDK compatible con la configuración de Gradle del proyecto; para la
    configuración utilizada en este proyecto, se recomienda JDK 17.
-   Un dispositivo Android físico con depuración USB activada, o un
    emulador configurado.
-   Conexión a Internet la primera vez que se descarguen las
    dependencias de Gradle.

## Pasos para ejecutar el proyecto

### Opción A: Ejecutar en Android Studio

1.  Abre Android Studio.
2.  Selecciona **Open** y elige la carpeta raíz del proyecto
    `OYLFrutosSecos`.
3.  Espera a que termine la sincronización de Gradle.
4.  Si Android Studio solicita instalar componentes o aceptar licencias,
    sigue las instrucciones que muestra.
5.  Selecciona un emulador Android o conecta un teléfono mediante USB.
6.  Si utilizas un teléfono, activa las **Opciones de desarrollador** y
    la **Depuración USB**. Desbloquea el dispositivo y acepta el aviso
    de autorización cuando aparezca.
7.  Selecciona el dispositivo en la lista de dispositivos de Android
    Studio.
8.  Pulsa el botón **Run ▶** para compilar, instalar e iniciar la
    aplicación.

### Opción B: Ejecutar desde la terminal

Desde la carpeta raíz del proyecto, ejecuta:

En Windows (PowerShell o CMD):

``` bat
gradlew.bat assembleDebug
```

Si la compilación termina correctamente, el APK de depuración
normalmente se encuentra en:

``` text
app/build/outputs/apk/debug/app-debug.apk
```

Puedes instalarlo en un dispositivo conectado con ADB, si Android SDK
Platform-Tools está instalado y el dispositivo está autorizado:

``` bat
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Estructura principal del código

-   `app/src/main/java/com/oyl/frutossecos/MainActivity.kt`: actividad
    principal y código de la interfaz de la aplicación.
-   `app/src/main/java/com/oyl/frutossecos/ui/theme/`: archivos
    relacionados con el tema visual, colores y tipografía.

La ubicación exacta de los archivos puede variar según la estructura
actual del proyecto.

## Estado del proyecto

Proyecto académico en desarrollo para practicar el desarrollo de
aplicaciones móviles con Kotlin y Jetpack Compose.

------------------------------------------------------------------------

**OYL Frutos Secos**
