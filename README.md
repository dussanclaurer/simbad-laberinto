# Simulador de Laberinto con Pledge

Aplicación de escritorio en Java que simula un robot resolviendo un laberinto mediante el algoritmo de Pledge. Utiliza Simbad para la simulación, Java 3D para la escena y Swing para los diálogos y la ventana que permite cargar una imagen como textura del piso.

## Requisitos de desarrollo y ejecución

| Componente | Versión o configuración | Uso |
| --- | --- | --- |
| Java Development Kit (JDK) | Java 8; plataforma configurada como `JDK_1.8` | Compilar y ejecutar. `javac.source` y `javac.target` están definidos como `1.8`. |
| Apache Ant | Mínimo `1.8.0`, según `nbproject/build-impl.xml` | Compilar, ejecutar y generar el JAR mediante `build.xml`. |
| NetBeans | Proyecto Java SE; sin versión mínima declarada | Abrir el proyecto, configurar el JDK y administrar las bibliotecas. El empaquetado utiliza su tarea `CopyLibs`. |
| Entorno gráfico | Escritorio con soporte OpenGL para la configuración Java 3D incluida | Mostrar las ventanas y renderizar la simulación 3D. |
| Biblioteca nativa de Java 3D | `j3dcore-ogl.dll`, incluida en la raíz | Backend OpenGL de Java 3D para Windows. Debe ser compatible con los JAR de Java 3D y la arquitectura de la JVM. |

La configuración existente corresponde a Windows y las rutas de Java 3D contienen `j3d64`. Al preparar otra máquina, comprobar que el JDK y las bibliotecas nativas tengan la misma arquitectura. Para usar otro sistema operativo se requieren las bibliotecas nativas correspondientes.

## Bibliotecas externas

Estas son las cuatro dependencias JAR declaradas en `javac.classpath` dentro de [nbproject/project.properties](nbproject/project.properties). También forman parte del classpath de ejecución.

| Biblioteca | Archivo requerido | Versión identificable en el proyecto | Función y clases utilizadas |
| --- | --- | --- | --- |
| Simbad | `simbad-1.4.jar` | `1.4`, indicada por el nombre del archivo | Simulador de robótica. Se utilizan `simbad.gui.Simbad` y las clases `Agent`, `RobotFactory`, `RangeSensorBelt`, `EnvironmentDescription`, `Box` y `World` de `simbad.sim`. |
| Java 3D Core | `j3dcore.jar` | No especificada | Grafo de escena, geometría, apariencia y texturas. El código utiliza `javax.media.j3d`, por ejemplo `VirtualUniverse`, `Locale`, `QuadArray`, `Shape3D`, `BranchGroup`, `Texture2D` e `ImageComponent2D`. |
| Java 3D Utils | `j3dutils.jar` | No especificada | Utilidades de Java 3D declaradas en el classpath para el entorno de Simbad. Los archivos fuente propios no importan directamente sus clases. |
| Vecmath | `vecmath.jar` | No especificada | Vectores, puntos y coordenadas de textura mediante `javax.vecmath`: `Vector3d`, `Vector3f`, `Point3d`, `Point3f` y `TexCoord2f`. |

Los cuatro JAR se referencian desde ubicaciones externas y **no están incluidos en esta carpeta del proyecto**. No hay un gestor Maven o Gradle configurado para descargarlos automáticamente. Las versiones exactas de Java 3D y Vecmath deben comprobarse en los JAR de la instalación utilizada; los archivos de configuración no las fijan.

`javax.vecmath` y `javax.media.j3d` son bibliotecas externas, aunque sus nombres comiencen con `javax`.

### Dependencia nativa

El archivo `j3dcore-ogl.dll` acompaña al backend OpenGL de Java 3D. Su presencia en la raíz no configura automáticamente su búsqueda en todos los modos de ejecución: la carpeta que lo contiene debe estar disponible en `java.library.path` o mediante la instalación de las bibliotecas nativas de Java 3D.

El proyecto no declara otros JAR externos. Cualquier requisito adicional de la distribución concreta de Simbad o Java 3D debe verificarse en esa distribución.

## Bibliotecas estándar de Java

Las siguientes API ya forman parte de Java 8 y no necesitan descargarse como dependencias separadas:

| Paquete o API | Uso en el proyecto |
| --- | --- |
| `javax.swing` | Ventanas, paneles, botones, etiquetas y diálogos, incluidos `JFrame`, `JPanel`, `JButton`, `JLabel`, `JFileChooser` y `JOptionPane`. |
| `javax.swing.filechooser` | `FileNameExtensionFilter` para filtrar imágenes en el selector de archivos. |
| `java.awt` | Diseño de ventanas, colores, fuentes, cursores y dibujo 2D. |
| `java.awt.image` | `BufferedImage` para almacenar y actualizar las imágenes del piso. |
| `javax.imageio` | `ImageIO` para leer las imágenes seleccionadas por el usuario. |
| `java.io` | `File` para representar los archivos de imagen. |
| `java.util` | `Random` para generar el laberinto y seleccionar su salida. |
| `java.lang.reflect` | `Field` para acceder a los campos internos de Simbad y modificar la escena 3D. |
| `java.lang` | Clases disponibles automáticamente, como `Thread`, `System`, `Math`, `String` e `Integer`, para concurrencia, cálculos y control de la aplicación. |

## Configuración de las dependencias

1. Instalar un JDK 8 y configurar la plataforma `JDK_1.8` en NetBeans, o seleccionar el JDK instalado en las propiedades del proyecto.
2. Disponer de `simbad-1.4.jar`, `j3dcore.jar`, `j3dutils.jar` y `vecmath.jar`, junto con las bibliotecas nativas compatibles de Java 3D.
3. Abrir esta carpeta como proyecto en NetBeans. En las propiedades del proyecto, corregir las referencias de las bibliotecas para que apunten a los archivos de la máquina actual.
4. Configurar la carpeta de las bibliotecas nativas en las opciones de la JVM cuando sea necesario.
5. Compilar y ejecutar la clase principal `simulacion.Main`.

Las propiedades que deben apuntar a los JAR son:

```properties
file.reference.simbad-1.4.jar=lib/simbad-1.4.jar
file.reference.j3dcore.jar=lib/j3dcore.jar
file.reference.j3dutils.jar=lib/j3dutils.jar
file.reference.vecmath.jar=lib/vecmath.jar
```

Este ejemplo supone que se crea una carpeta `lib/` y se colocan allí los cuatro JAR. Esa carpeta no viene incluida. Las rutas relativas se resuelven desde la raíz del proyecto y pueden sustituirse por las ubicaciones reales de los archivos.

Para usar la DLL situada en la raíz, se puede establecer en las opciones de ejecución de NetBeans o en `nbproject/project.properties`:

```properties
run.jvmargs=-Djava.library.path=.
```

Esto requiere ejecutar con la raíz del proyecto como directorio de trabajo. Si las bibliotecas nativas están en otra carpeta, utilizar su ruta; si contiene espacios, encerrarla entre comillas.

### Compilación y ejecución con Ant

Con el JDK, los JAR y las propiedades de NetBeans correctamente configurados, ejecutar desde la raíz:

```powershell
ant compile
ant run
ant jar
```

El JAR generado se configura como `dist/SimuladorLaberintoPledge.jar`. El empaquetado con bibliotecas depende de la tarea de NetBeans `org.netbeans.modules.java.j2seproject.copylibstask.CopyLibs`, cuyo classpath se proporciona mediante `libs.CopyLibs.classpath`. Una instalación de Ant aislada necesita tener esa propiedad configurada para generar la distribución.

Si Ant no encuentra la plataforma `JDK_1.8`, se puede indicar su ubicación explícitamente, sustituyendo esta ruta de ejemplo:

```powershell
ant "-Dplatforms.JDK_1.8.home=C:\ruta\al\jdk8" compile
```

Para ejecutar las clases compiladas directamente en Windows con los JAR en `lib/` y la DLL en la raíz:

```powershell
java -Djava.library.path=. -cp "build/classes;lib/*" simulacion.Main
```

## Estructura del código

| Archivo | Responsabilidad |
| --- | --- |
| `src/simulacion/Main.java` | Solicita el tiempo límite e inicia la aplicación. |
| `src/simulacion/GeneradorLaberinto.java` | Genera la matriz del laberinto y su salida. |
| `src/simulacion/EntornoLaberinto.java` | Construye el entorno de Simbad y añade las paredes y el robot. |
| `src/simulacion/RobotPledge.java` | Controla la navegación del robot, sus sensores y el algoritmo de Pledge. |
| `src/simulacion/HiloFondo.java` | Presenta la ventana de selección y visualización de imágenes. |
| `src/simulacion/GestorFondo.java` | Crea y actualiza la textura del piso en la escena Java 3D. |
| `build.xml` y `nbproject/` | Configuración de compilación, classpath y ejecución con Ant y NetBeans. |

## Problemas de dependencias frecuentes

- **`package simbad... does not exist`**: revisar la ruta de `simbad-1.4.jar`.
- **`package javax.media.j3d... does not exist`**: revisar `j3dcore.jar`.
- **`package javax.vecmath... does not exist`**: revisar `vecmath.jar`.
- **`NoClassDefFoundError`**: comprobar que los cuatro JAR estén también en el classpath de ejecución.
- **`UnsatisfiedLinkError`**: comprobar la ubicación de las bibliotecas nativas de Java 3D, su compatibilidad con los JAR y la arquitectura de la JVM.
- **Error al localizar `JDK_1.8` o `CopyLibs`**: revisar las propiedades de la plataforma Java y las herramientas de compilación de NetBeans.

Este inventario se basa en los imports de `src/simulacion/`, `nbproject/project.properties`, `nbproject/build-impl.xml` y los archivos presentes en la raíz.
