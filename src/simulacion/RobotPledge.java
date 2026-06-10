package simulacion;

import simbad.sim.Agent;
import simbad.sim.RobotFactory;
import simbad.sim.RangeSensorBelt;
import javax.vecmath.Vector3d;
import javax.swing.JOptionPane;
import javax.vecmath.Point3d;

/**
 * Agente robótico que implementa el <b>Algoritmo de Pledge</b> para escapar de un laberinto.
 *
 * <p>El Algoritmo de Pledge combina dos modos de navegación:</p>
 * <ul>
 *   <li><b>Avance recto</b>: el robot avanza en línea recta hacia el norte hasta encontrar
 *       un obstáculo.</li>
 *   <li><b>Seguimiento de pared</b>: al detectar un obstáculo, el robot sigue su contorno
 *       por la derecha mientras mantiene un contador de giros acumulados. Cuando dicho
 *       contador vuelve a cero <em>y</em> el robot está orientado al norte, abandona el
 *       seguimiento y retoma el avance recto, garantizando que ha rodeado completamente
 *       el obstáculo y no regresa al punto de origen.</li>
 * </ul>
 *
 * <p>Adicionalmente incorpora un mecanismo anti-atasco que detecta si el robot lleva
 * demasiado tiempo sin avanzar y ejecuta una maniobra de retroceso + giro para
 * desbloquearse.</p>
 *
 * @author SimuladorLaberintoPledge
 */
public class RobotPledge extends Agent {

    /* ------------------------------------------------------------------ */
    /* Estados del algoritmo de navegación principal (modo de movimiento)  */
    /* ------------------------------------------------------------------ */
    /** El robot avanza en línea recta buscando la salida. */
    private static final int AVANZANDO_RECTO  = 0;
    /** El robot está bordeando un obstáculo siguiendo su pared derecha. */
    private static final int SIGUIENDO_PARED  = 1;
    private int estadoActual = AVANZANDO_RECTO;

    /* ------------------------------------------------------------------ */
    /* Sub-estados del modo de giro / maniobra especial                    */
    /* ------------------------------------------------------------------ */
    /** Sin giro activo; el robot ejecuta la lógica principal de Pledge. */
    private static final int ESTADO_NORMAL       = 0;
    /** Se está ejecutando un giro de 90° a la izquierda (en sitio). */
    private static final int EJECUTANDO_GIRO_IZQ = 1;
    /** Se está ejecutando un giro de 90° a la derecha (avanzando). */
    private static final int EJECUTANDO_GIRO_DER = 2;
    /** El robot está retrocediendo para salir de un estado de atasco. */
    private static final int RETROCEDIENDO       = 3;
    private int modoGiroActual = ESTADO_NORMAL;

    /* ------------------------------------------------------------------ */
    /* Sensor de distancia y variables del algoritmo de Pledge             */
    /* ------------------------------------------------------------------ */
    /** Cinturón de sónares de 8 posiciones uniformemente distribuidas. */
    private RangeSensorBelt sonares;
    /** Contador acumulado de giros Pledge (izquierdas +1, derechas −1). */
    private int    contadorGirosPledge       = 0;
    /** Radianes girados durante el giro en curso (se resetea al completar). */
    private double radianesGiradosEnEsteGiro = 0;
    /** Constante equivalente a 90° en radianes. */
    private final  double NOVENTA_GRADOS     = Math.PI / 2;

    /* ------------------------------------------------------------------ */
    /* Orientación cardinal del robot (índice 0–3)                         */
    /* ------------------------------------------------------------------ */
    /**
     * Orientación actual del robot codificada como entero:
     * 0 = Norte, 1 = Oeste, 2 = Sur, 3 = Este.
     */
    private int orientacionActual = 0;

    /* ------------------------------------------------------------------ */
    /* Cronómetro y estado de fin de partida                               */
    /* ------------------------------------------------------------------ */
    /** Tiempo restante de la simulación en segundos. */
    private double  tiempoRestante;
    /** Indica si la simulación ya ha concluido (victoria o derrota). */
    private boolean juegoTerminado = false;

    /* ------------------------------------------------------------------ */
    /* Configuración de la pared de salida                                 */
    /* ------------------------------------------------------------------ */
    /** Índice de la pared de salida: 0=izq, 1=der, 2=sup, 3=inf. */
    private final int    paredSalida;
    /** Coordenada umbral a partir de la cual el robot se considera fuera del mapa. */
    private final double umbralSalida;

    /* ------------------------------------------------------------------ */
    /* Parámetros del mecanismo anti-atasco                                */
    /* ------------------------------------------------------------------ */
    /** Última posición registrada en el checkpoint anti-atasco. */
    private Point3d posicionCheckpoint    = new Point3d(Double.MAX_VALUE, 0, Double.MAX_VALUE);
    /** Ticks transcurridos desde el último checkpoint. */
    private int     ticksDesdeCheckpoint  = 0;
    /** Ticks que lleva ejecutándose la maniobra de retroceso. */
    private int     ticksRetrocediendo    = 0;

    /** Número de ticks entre evaluaciones de progreso. */
    private static final int    INTERVALO_CHECK    = 60;
    /** Distancia mínima que debe avanzar el robot entre checkpoints. */
    private static final double AVANCE_MINIMO      = 0.10;
    /** Duración en ticks de la maniobra de retroceso anti-atasco. */
    private static final int    DURACION_RETROCESO = 30;

    // ------------------------------------------------------------------ //

    /**
     * Construye un {@code RobotPledge} y lo configura con los parámetros de la simulación.
     *
     * @param posicion     posición inicial del robot en el mundo 3-D.
     * @param nombre       nombre identificativo del agente.
     * @param tiempoLimite tiempo máximo de simulación en segundos.
     * @param paredSalida  índice de la pared de salida (0=izq, 1=der, 2=sup, 3=inf).
     * @param tamañoMundo  lado del mapa cuadrado en unidades de simulación; se usa
     *                     para calcular el umbral de salida.
     */
    public RobotPledge(Vector3d posicion, String nombre, int tiempoLimite, int paredSalida, int tamañoMundo) {
        super(posicion, nombre);
        sonares = RobotFactory.addSonarBeltSensor(this, 8);
        this.tiempoRestante = tiempoLimite;
        this.paredSalida    = paredSalida;
        this.umbralSalida   = (tamañoMundo / 2.0) - 0.5;
    }

    /**
     * Lógica de comportamiento ejecutada en cada tick de la simulación.
     *
     * <p>El método sigue la secuencia:</p>
     * <ol>
     *   <li>Detiene el robot si la partida ya terminó.</li>
     *   <li>Descuenta el tiempo restante y comprueba Game Over.</li>
     *   <li>Verifica si el robot ha cruzado la pared de salida (victoria).</li>
     *   <li>Ejecuta el mecanismo anti-atasco (checkpoint periódico).</li>
     *   <li>Gestiona las maniobras activas: retroceso, giro izquierda o giro derecha.</li>
     *   <li>Aplica la lógica principal del Algoritmo de Pledge.</li>
     * </ol>
     */
    @Override
    protected void performBehavior() {

        // Si la partida ya concluyó, detener el robot inmediatamente
        if (juegoTerminado) {
            this.setTranslationalVelocity(0.0);
            this.setRotationalVelocity(0.0);
            return;
        }

        double tiempoPaso = 0.05;

        // Cronómetro: descontar el tiempo de este paso de simulación
        tiempoRestante -= tiempoPaso;

        System.out.printf("Modo: %s | Pledge: %d | Orient: %s | TIEMPO: %.1fs%n",
            (estadoActual == AVANZANDO_RECTO ? "BUSCANDO_NORTE" : "EVADIENDO_PARED"),
            contadorGirosPledge,
            orientacionToString(orientacionActual),
            tiempoRestante
        );

        // Comprobar expiración del tiempo → derrota
        if (tiempoRestante <= 0) {
            juegoTerminado = true;
            JOptionPane.showMessageDialog(null,
                "¡GAME OVER!\nEl tiempo se ha agotado y el robot perdió.",
                "Fin de la Simulación", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Obtener posición actual del robot en el mundo
        Point3d posActual = new Point3d();
        this.getCoords(posActual);

        // Comprobar si el robot ha cruzado la pared de salida → victoria
        boolean salio = false;
        switch (paredSalida) {
            case 0: salio = posActual.x < -umbralSalida; break;
            case 1: salio = posActual.x >  umbralSalida; break;
            case 2: salio = posActual.z < -umbralSalida; break;
            case 3: salio = posActual.z >  umbralSalida; break;
        }
        if (salio) {
            juegoTerminado = true;
            String nombrePared = new String[]{"SUPERIOR","INFERIOR","IZQUIERDA","DERECHA"}[paredSalida];
            String mensaje = (contadorGirosPledge == 0)
                ? "¡VICTORIA PERFECTA!\nEl robot escapó por la pared " + nombrePared + " con Pledge = 0."
                : "¡VICTORIA!\nEl robot escapó por la pared " + nombrePared + ".\nContador Pledge final: " + contadorGirosPledge;
            JOptionPane.showMessageDialog(null, mensaje, "Fin de la Simulación", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // Anti-atasco: cada INTERVALO_CHECK ticks verificar si el robot avanzó suficiente
        ticksDesdeCheckpoint++;
        if (ticksDesdeCheckpoint >= INTERVALO_CHECK) {
            ticksDesdeCheckpoint = 0;
            double distanciaAvanzada = posActual.distance(posicionCheckpoint);

            if (distanciaAvanzada < AVANCE_MINIMO && modoGiroActual == ESTADO_NORMAL) {
                System.out.println("⚠ ATASCADO detectado. Iniciando maniobra de escape (retroceso)...");
                modoGiroActual     = RETROCEDIENDO;
                ticksRetrocediendo = 0;
            }
            posicionCheckpoint.set(posActual);
        }

        // Maniobra de escape: retroceder durante DURACION_RETROCESO ticks
        if (modoGiroActual == RETROCEDIENDO) {
            this.setTranslationalVelocity(-0.3);
            this.setRotationalVelocity(0.0);
            ticksRetrocediendo++;

            // Al completar el retroceso, encadenar un giro izquierda para reorientar
            if (ticksRetrocediendo >= DURACION_RETROCESO) {
                System.out.println("✔ Escape completado. Retomando Algoritmo de Pledge...");
                this.setTranslationalVelocity(0.0);
                modoGiroActual            = EJECUTANDO_GIRO_IZQ;
                radianesGiradosEnEsteGiro = 0;
                ticksRetrocediendo        = 0;
                estadoActual = SIGUIENDO_PARED;
            }
            return;
        }

        // Giro izquierda de 90°: rotar en sitio, actualizar contador Pledge y orientación
        if (modoGiroActual == EJECUTANDO_GIRO_IZQ) {
            this.setTranslationalVelocity(0.0);
            this.setRotationalVelocity(0.6);
            radianesGiradosEnEsteGiro += 0.6 * tiempoPaso;
            if (radianesGiradosEnEsteGiro >= NOVENTA_GRADOS) {
                this.setRotationalVelocity(0.0);
                contadorGirosPledge += 1;
                orientacionActual = (orientacionActual + 1) % 4;
                modoGiroActual    = ESTADO_NORMAL;
                radianesGiradosEnEsteGiro = 0;
            }
            return;
        }

        // Giro derecha de 90°: avanzar ligeramente mientras gira, decrementar contador Pledge
        if (modoGiroActual == EJECUTANDO_GIRO_DER) {
            this.setTranslationalVelocity(0.25);
            this.setRotationalVelocity(-0.6);
            radianesGiradosEnEsteGiro += 0.6 * tiempoPaso;
            if (radianesGiradosEnEsteGiro >= NOVENTA_GRADOS) {
                this.setRotationalVelocity(0.0);
                contadorGirosPledge -= 1;
                orientacionActual = (orientacionActual + 3) % 4;
                modoGiroActual    = ESTADO_NORMAL;
                radianesGiradosEnEsteGiro = 0;
            }
            return;
        }

        // Lecturas de los sónares relevantes para la decisión de navegación
        double distFrente      = sonares.hasHit(0) ? sonares.getMeasurement(0) : 10.0;
        double distDerecha     = sonares.hasHit(6) ? sonares.getMeasurement(6) : 10.0;
        double distDiagDerecha = sonares.hasHit(7) ? sonares.getMeasurement(7) : 10.0;

        double limiteSeguro       = 0.45;   // distancia mínima frontal antes de girar
        double limiteParedDerecha = 0.75;   // distancia a la pared derecha para seguirla

        // Decisión del Algoritmo de Pledge: modo avance recto
        if (estadoActual == AVANZANDO_RECTO) {
            this.setTranslationalVelocity(0.5);
            this.setRotationalVelocity(0.0);

            // Obstáculo detectado al frente → pasar a seguimiento de pared
            if (distFrente < limiteSeguro) {
                estadoActual              = SIGUIENDO_PARED;
                modoGiroActual            = EJECUTANDO_GIRO_IZQ;
                radianesGiradosEnEsteGiro = 0;
            }
        }
        // Decisión del Algoritmo de Pledge: modo seguimiento de pared derecha
        else if (estadoActual == SIGUIENDO_PARED) {

            // Condición de retorno al avance recto: contador Pledge en 0 y orientado al norte
            if (contadorGirosPledge == 0 && orientacionActual == 0 && distFrente > limiteSeguro) {
                estadoActual = AVANZANDO_RECTO;
                return;
            }

            // Pared frontal → girar a la izquierda para bordearla
            if (distFrente < limiteSeguro) {
                modoGiroActual            = EJECUTANDO_GIRO_IZQ;
                radianesGiradosEnEsteGiro = 0;
            }
            // Esquina libre a la derecha → girar a la derecha para seguir el contorno
            else if (distDerecha > limiteParedDerecha && distDiagDerecha > limiteParedDerecha) {
                modoGiroActual            = EJECUTANDO_GIRO_DER;
                radianesGiradosEnEsteGiro = 0;
            }
            // Pared derecha presente y frente despejado → avanzar paralelo a la pared
            else {
                this.setTranslationalVelocity(0.4);
                this.setRotationalVelocity(0.0);
            }
        }
    }

    /**
     * Convierte el índice de orientación interna a su nombre cardinal en español.
     *
     * @param orientacion índice entero de orientación (0 = Norte, 1 = Oeste,
     *                    2 = Sur, 3 = Este).
     * @return cadena con el nombre del punto cardinal, o {@code "???"} si el valor
     *         no es reconocido.
     */
    private String orientacionToString(int orientacion) {
        switch (orientacion) {
            case 0:  return "NORTE";
            case 1:  return "OESTE";
            case 2:  return "SUR";
            case 3:  return "ESTE";
            default: return "???";
        }
    }
}