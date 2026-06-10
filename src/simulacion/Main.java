package simulacion;

import javax.swing.JOptionPane;
import simbad.gui.Simbad;

/**
 * Punto de entrada de la aplicación.
 * <p>
 * Solicita al usuario el tiempo límite de la simulación, lanza el hilo
 * de fondo encargado de actualizar el laberinto y arranca el entorno
 * Simbad con el robot ejecutor del algoritmo de Pledge.
 * </p>
 */
public class Main {

    /**
     * Inicializa y arranca la simulación del laberinto con el algoritmo de Pledge.
     *
     * @param args argumentos de línea de comandos (no utilizados)
     */
    public static void main(String[] args) {

        // Solicitar el tiempo límite al usuario mediante un diálogo modal
        String input = JOptionPane.showInputDialog(
            null,
            "Introduce el tiempo límite de la simulación (en segundos):",
            "Configuración del Temporizador",
            JOptionPane.QUESTION_MESSAGE
        );

        int tiempoElegido = 60; // Valor por defecto si la entrada es inválida o vacía
        try {
            if (input != null && !input.trim().isEmpty()) {
                tiempoElegido = Integer.parseInt(input);
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(null, "Entrada inválida. Usando 60s.");
        }

        // Hilo concurrente que regenera o actualiza el laberinto en segundo plano
        HiloFondo hiloCambiador = new HiloFondo();
        hiloCambiador.start();

        // Crear el entorno del laberinto y abrir la ventana de simulación Simbad
        EntornoLaberinto miEntorno = new EntornoLaberinto(tiempoElegido);
        Simbad frame = new Simbad(miEntorno, false);

        // Inicializar el piso texturizable del laberinto (se conecta a la escena Java3D)
        GestorFondo.inicializar(11);
    }
}