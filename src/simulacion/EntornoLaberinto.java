package simulacion;

import simbad.sim.Box;
import simbad.sim.EnvironmentDescription;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector3f;

/**
 * Entorno de simulación que representa el laberinto generado proceduralmente.
 * <p>
 * Extiende {@link EnvironmentDescription} de Simbad para construir la escena 3D:
 * coloca paredes como objetos {@link Box} según la matriz del laberinto y posiciona
 * el robot {@link RobotPledge} en la celda de inicio {@code (1, 1)}.
 * </p>
 */
public class EntornoLaberinto extends EnvironmentDescription {

    /**
     * Construye el entorno del laberinto y lo puebla con paredes y el robot.
     *
     * @param tiempoLimite tiempo máximo (en segundos de simulación) que el robot
     *                     tiene para resolver el laberinto antes de que se declare
     *                     fracaso.
     */
    public EntornoLaberinto(int tiempoLimite) {
        int tamaño = 11;
        this.setWorldSize(tamaño);

        // Genera la matriz binaria del laberinto (1 = pared, 0 = pasillo)
        int[][] matriz = GeneradorLaberinto.crearLaberinto(tamaño);
        double offset = tamaño / 2.0; // desplazamiento para centrar el mundo en el origen

        // Instancia una caja por cada celda marcada como pared
        for (int i = 0; i < tamaño; i++) {
            for (int j = 0; j < tamaño; j++) {
                if (matriz[i][j] == 1) {
                    double x = i - offset;
                    double z = j - offset;
                    Box pared = new Box(new Vector3d(x, 0, z), new Vector3f(1, 1, 1), this);
                    this.add(pared);
                }
            }
        }

        // Determina en qué borde del laberinto está la salida para orientar al robot
        int si = GeneradorLaberinto.salidaI;
        int sj = GeneradorLaberinto.salidaJ;

        // Codificación del borde: 0=norte, 1=sur, 2=oeste, 3=este
        int paredSalida;
        if      (si == 0)          paredSalida = 0;
        else if (si == tamaño - 1) paredSalida = 1;
        else if (sj == 0)          paredSalida = 2;
        else                       paredSalida = 3;

        // El robot parte desde la celda interior (1,1), convertida a coordenadas del mundo
        double robotX = 1 - offset;
        double robotZ = 1 - offset;

        RobotPledge miRobot = new RobotPledge(
            new Vector3d(robotX, 0, robotZ),
            "PledgeBot",
            tiempoLimite,
            paredSalida,
            tamaño
        );
        this.add(miRobot);
    }
}