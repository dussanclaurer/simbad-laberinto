package simulacion;

import java.util.Random;

/**
 * Genera laberintos cuadrados de forma procedural.
 * <p>
 * Utiliza un algoritmo basado en muros aleatorios con paso 2 para crear
 * estructuras interiores. La salida se abre aleatoriamente en una de las
 * cuatro paredes perimetrales y sus coordenadas quedan expuestas como
 * campos estáticos para uso externo.
 * </p>
 */
public class GeneradorLaberinto {

    /** Fila (coordenada i) de la celda de salida en la matriz del laberinto. */
    public static int salidaI;

    /** Columna (coordenada j) de la celda de salida en la matriz del laberinto. */
    public static int salidaJ;

    /**
     * Crea y devuelve un laberinto cuadrado generado aleatoriamente.
     * <p>
     * El perímetro completo se rellena con muros ({@code 1}). El interior
     * se puebla con muros en posiciones pares, prolongando cada uno en una
     * dirección aleatoria para crear pasajes. Finalmente se abre una única
     * salida en una pared perimetral escogida al azar.
     * </p>
     *
     * @param tamaño dimensión del laberinto (número de filas y columnas)
     * @return matriz {@code tamaño × tamaño} donde {@code 0} = pasillo y
     *         {@code 1} = muro
     */
    public static int[][] crearLaberinto(int tamaño) {
        int[][] laberinto = new int[tamaño][tamaño];
        Random random = new Random();

        // Inicializar: perímetro = muro (1), interior = pasillo (0)
        for (int i = 0; i < tamaño; i++) {
            for (int j = 0; j < tamaño; j++) {
                if (i == 0 || i == tamaño - 1 || j == 0 || j == tamaño - 1) {
                    laberinto[i][j] = 1;
                } else {
                    laberinto[i][j] = 0;
                }
            }
        }

        // Colocar muros interiores en posiciones pares y extender uno en dirección aleatoria
        for (int i = 2; i < tamaño - 2; i += 2) {
            for (int j = 2; j < tamaño - 2; j += 2) {
                laberinto[i][j] = 1;
                int direccion = random.nextInt(4); // 0=arriba, 1=abajo, 2=izquierda, 3=derecha
                if (direccion == 0) laberinto[i-1][j] = 1;
                if (direccion == 1) laberinto[i+1][j] = 1;
                if (direccion == 2) laberinto[i][j-1] = 1;
                if (direccion == 3) laberinto[i][j+1] = 1;
            }
        }

        // Seleccionar pared perimetral y posición aleatoria para abrir la salida
        int pared = random.nextInt(4);
        int pos   = 1 + random.nextInt(tamaño - 2); // dentro del rango interior [1, tamaño-2]

        switch (pared) {
            case 0: salidaI = 0;         salidaJ = pos;        break; // pared superior
            case 1: salidaI = tamaño-1;  salidaJ = pos;        break; // pared inferior
            case 2: salidaI = pos;       salidaJ = 0;          break; // pared izquierda
            default: salidaI = pos;      salidaJ = tamaño - 1; break; // pared derecha
        }

        laberinto[salidaI][salidaJ] = 0; // Abrir la celda de salida

        System.out.printf("Salida generada en matriz [%d,%d] → pared: %s%n",
            salidaI, salidaJ,
            new String[]{"SUPERIOR", "INFERIOR", "IZQUIERDA", "DERECHA"}[pared]
        );

        return laberinto;
    }
}