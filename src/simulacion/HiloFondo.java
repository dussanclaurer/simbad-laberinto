package simulacion;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Hilo concurrente que muestra una ventana auxiliar con un panel de imagen.
 * <p>
 * Corre en paralelo a la simulación principal de Simbad y permite al usuario
 * cargar y cambiar la imagen de fondo en tiempo real mediante el botón
 * "Subir Imagen", sin interrumpir la ejecución del laberinto.
 * </p>
 */
public class HiloFondo extends Thread {

    /** Ventana principal de este hilo. */
    private final JFrame ventana;

    /** Panel personalizado que renderiza la imagen cargada. */
    private final PanelImagen panelImagen;

    /** Etiqueta de estado que indica el nombre del archivo cargado. */
    private final JLabel etiquetaEstado;

    /** Controla el ciclo de vida del hilo. */
    private volatile boolean corriendo = true;

    /**
     * Construye y muestra la ventana de fondo.
     * El bucle del hilo empieza al llamar a {@link #start()}.
     */
    public HiloFondo() {
        ventana = new JFrame("Fondo del Laberinto");
        ventana.setSize(420, 480);
        ventana.setLocation(820, 80);
        ventana.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        ventana.setLayout(new BorderLayout(0, 0));

        // Panel central: muestra la imagen cargada
        panelImagen = new PanelImagen();
        ventana.add(panelImagen, BorderLayout.CENTER);

        // ── Panel inferior: botón + etiqueta de estado ──────────────────
        JPanel panelInferior = new JPanel(new BorderLayout(8, 0));
        panelInferior.setBackground(new Color(25, 25, 35));
        panelInferior.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        JButton btnSubir = crearBoton();
        panelInferior.add(btnSubir, BorderLayout.WEST);

        etiquetaEstado = new JLabel("Sin imagen cargada");
        etiquetaEstado.setForeground(new Color(160, 160, 180));
        etiquetaEstado.setFont(new Font("SansSerif", Font.ITALIC, 11));
        panelInferior.add(etiquetaEstado, BorderLayout.CENTER);

        ventana.add(panelInferior, BorderLayout.SOUTH);
        ventana.setVisible(true);
    }

    /**
     * Crea y configura el botón "Subir Imagen" con su lógica de selección de archivo.
     *
     * @return botón configurado con su {@code ActionListener}.
     */
    private JButton crearBoton() {
        JButton btn = new JButton("  Subir Imagen");
        btn.setFont(new Font("SansSerif", Font.BOLD, 13));
        btn.setBackground(new Color(60, 100, 200));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        btn.addActionListener(e -> {
            // Abrir el selector de archivos filtrando solo formatos de imagen
            JFileChooser selector = new JFileChooser();
            selector.setDialogTitle("Seleccionar imagen de fondo del laberinto");
            selector.setFileFilter(new FileNameExtensionFilter(
                "Imágenes (jpg, png, gif, bmp)", "jpg", "jpeg", "png", "gif", "bmp"
            ));

            if (selector.showOpenDialog(ventana) == JFileChooser.APPROVE_OPTION) {
                File archivo = selector.getSelectedFile();
                try {
                    BufferedImage imagen = ImageIO.read(archivo);
                    if (imagen != null) {
                        // 1. Actualizar el fondo del laberinto en la escena Java3D
                        GestorFondo.actualizarImagen(imagen);

                        // 2. Mostrar vista previa en esta ventana auxiliar
                        SwingUtilities.invokeLater(() -> {
                            panelImagen.setImagen(imagen);
                            etiquetaEstado.setText("✔ " + archivo.getName());
                        });
                    } else {
                        JOptionPane.showMessageDialog(ventana,
                            "El archivo seleccionado no es una imagen válida.",
                            "Error", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(ventana,
                        "No se pudo cargar la imagen:\n" + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        return btn;
    }

    /**
     * Bucle concurrente principal.
     * Mantiene el panel actualizado cada 100 ms mientras la simulación esté activa,
     * demostrando la ejecución paralela del hilo independiente del juego.
     */
    @Override
    public void run() {
        while (corriendo) {
            try {
                // Solicitar repintado periódico del panel desde el EDT de Swing
                SwingUtilities.invokeLater(panelImagen::repaint);
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Detiene el hilo y cierra la ventana.
     * Debe invocarse cuando la simulación concluya.
     */
    public void detener() {
        corriendo = false;
        ventana.dispose();
    }

    // =========================================================================
    // Clase interna: panel que renderiza la imagen seleccionada
    // =========================================================================

    /**
     * Panel personalizado que escala y dibuja la imagen cargada manteniendo
     * sus proporciones. Si no hay imagen, muestra un mensaje de instrucción.
     */
    private static class PanelImagen extends JPanel {

        /** Imagen actualmente mostrada en el panel; {@code null} si no se ha cargado ninguna. */
        private BufferedImage imagen;

        public PanelImagen() {
            setBackground(new Color(15, 15, 25));
        }

        /**
         * Establece una nueva imagen de fondo y fuerza el repintado del panel.
         *
         * @param img imagen a mostrar.
         */
        public void setImagen(BufferedImage img) {
            this.imagen = img;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                                RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            if (imagen != null) {
                // Escalar la imagen para cubrir el panel conservando proporciones
                double escalaX = (double) getWidth()  / imagen.getWidth();
                double escalaY = (double) getHeight() / imagen.getHeight();
                double escala  = Math.max(escalaX, escalaY);

                int ancho = (int) (imagen.getWidth()  * escala);
                int alto  = (int) (imagen.getHeight() * escala);
                int x     = (getWidth()  - ancho) / 2;
                int y     = (getHeight() - alto)  / 2;

                g2.drawImage(imagen, x, y, ancho, alto, null);
            } else {
                // Estado por defecto: instrucción visual al usuario
                g2.setColor(new Color(40, 40, 60));
                g2.fillRect(0, 0, getWidth(), getHeight());

                g2.setColor(new Color(100, 100, 140));
                g2.setFont(new Font("SansSerif", Font.PLAIN, 36));
                g2.drawString("🖼", getWidth() / 2 - 22, getHeight() / 2 - 20);

                g2.setFont(new Font("SansSerif", Font.BOLD, 14));
                g2.setColor(new Color(140, 140, 180));
                g2.drawString("Usa el botón para cargar",
                    centrarTexto(g2, "Usa el botón para cargar"), getHeight() / 2 + 15);
                g2.drawString("una imagen de fondo",
                    centrarTexto(g2, "una imagen de fondo"), getHeight() / 2 + 35);
            }
        }

        /** Calcula la coordenada X para centrar un texto en el panel. */
        private int centrarTexto(Graphics2D g2, String texto) {
            FontMetrics fm = g2.getFontMetrics();
            return (getWidth() - fm.stringWidth(texto)) / 2;
        }
    }
}