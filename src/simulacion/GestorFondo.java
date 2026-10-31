package simulacion;

import javax.media.j3d.*;
import javax.vecmath.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import simbad.gui.Simbad;
import simbad.sim.World;

/**
 * Gestiona el fondo texturizable del laberinto en tiempo real.
 * <p>
 * Mediante reflexión accede al grafo de escena Java3D de Simbad y añade
 * un plano (QuadArray) con coordenadas de textura justo sobre el piso gris
 * original. La textura se puede actualizar dinámicamente mientras la
 * simulación corre, sin detener ni reiniciar el simulador.
 * </p>
 */
public class GestorFondo {

    /** Tamaño de la textura: potencia de 2, requerido por Java3D. */
    private static final int TEX_SIZE = 512;

    /** Componente de imagen actualizable en vivo (ALLOW_IMAGE_WRITE). */
    private static ImageComponent2D imageComp;

    /** Indica si la inicialización del piso texturizado fue exitosa. */
    private static volatile boolean listo = false;

    // Constructor privado: clase utilitaria, no instanciable
    private GestorFondo() {}

    /**
     * Inicializa el piso texturizable inyectándolo en la escena Java3D de Simbad.
     * <p>
     * Este método debe llamarse <em>después</em> de crear el frame de Simbad.
     * Corre en un hilo daemon para esperar que Simbad termine de compilar
     * su grafo de escena antes de modificarlo.
     * </p>
     *
     * @param tamañoMundo lado del mundo cuadrado (p. ej. {@code 11} para un grid de 11x11).
     */
    public static void inicializar(int tamañoMundo) {
        Thread t = new Thread(() -> {
            try {
                // Reintentar hasta obtener la instancia de Simbad (máximo 5 s)
                Simbad simbad = null;
                for (int i = 0; i < 25 && simbad == null; i++) {
                    Thread.sleep(200);
                    simbad = Simbad.getSimbadInstance();
                }
                if (simbad == null) {
                    System.err.println("GestorFondo: No se pudo obtener la instancia de Simbad.");
                    return;
                }

                // Acceder a World (campo package-private) mediante reflexión
                Field worldField = Simbad.class.getDeclaredField("world");
                worldField.setAccessible(true);
                World world = (World) worldField.get(simbad);

                // Acceder al VirtualUniverse de World mediante reflexión
                Field univField = World.class.getDeclaredField("universe");
                univField.setAccessible(true);
                VirtualUniverse universe = (VirtualUniverse) univField.get(world);

                // Obtener el primer Locale del universo
                Locale locale = (Locale) universe.getAllLocales().nextElement();

                // ── Preparar la textura dinámica ──────────────────────────────
                BufferedImage imagenInicial = crearImagenGris();
                imageComp = new ImageComponent2D(ImageComponent2D.FORMAT_RGB, imagenInicial);
                imageComp.setCapability(ImageComponent2D.ALLOW_IMAGE_WRITE);

                Texture2D textura = new Texture2D(Texture.BASE_LEVEL, Texture.RGB, TEX_SIZE, TEX_SIZE);
                textura.setImage(0, imageComp);
                textura.setMinFilter(Texture.BASE_LEVEL_LINEAR);
                textura.setMagFilter(Texture.BASE_LEVEL_LINEAR);

                TextureAttributes ta = new TextureAttributes();
                ta.setTextureMode(TextureAttributes.REPLACE);

                // Sin culling para que sea visible desde arriba y abajo
                PolygonAttributes pa = new PolygonAttributes();
                pa.setCullFace(PolygonAttributes.CULL_NONE);

                Appearance app = new Appearance();
                app.setTexture(textura);
                app.setTextureAttributes(ta);
                app.setPolygonAttributes(pa);

                // ── Crear el plano del piso con coordenadas UV ────────────────
                // y = 0.005 → justo por encima del piso gris de Simbad (y = 0)
                float h = tamañoMundo / 2.0f + 0.5f;
                float y = 0.005f;

                QuadArray quad = new QuadArray(4,
                    GeometryArray.COORDINATES | GeometryArray.TEXTURE_COORDINATE_2);

                quad.setCoordinate(0, new Point3f(-h, y,  h));
                quad.setCoordinate(1, new Point3f( h, y,  h));
                quad.setCoordinate(2, new Point3f( h, y, -h));
                quad.setCoordinate(3, new Point3f(-h, y, -h));

                quad.setTextureCoordinate(0, 0, new TexCoord2f(0f, 0f));
                quad.setTextureCoordinate(0, 1, new TexCoord2f(1f, 0f));
                quad.setTextureCoordinate(0, 2, new TexCoord2f(1f, 1f));
                quad.setTextureCoordinate(0, 3, new TexCoord2f(0f, 1f));

                Shape3D pisoShape = new Shape3D(quad, app);

                // BranchGroup con ALLOW_DETACH para poder reemplazarlo si es necesario
                BranchGroup bgPiso = new BranchGroup();
                bgPiso.setCapability(BranchGroup.ALLOW_DETACH);
                bgPiso.addChild(pisoShape);

                // ── Inyectar en la escena viva de Simbad ─────────────────────
                locale.addBranchGraph(bgPiso);

                listo = true;
                System.out.println("GestorFondo: Piso texturizable inicializado correctamente.");

            } catch (Exception e) {
                System.err.println("GestorFondo: Error al inicializar: " + e.getMessage());
                e.printStackTrace();
            }
        });
        t.setDaemon(true);
        t.setName("GestorFondo-Init");
        t.start();
    }

    /**
     * Actualiza la imagen del piso en tiempo real sin interrumpir la simulación.
     *
     * @param imagen imagen cargada por el usuario; se escala a {@value #TEX_SIZE}×{@value #TEX_SIZE} px.
     */
    public static void actualizarImagen(BufferedImage imagen) {
        if (!listo || imageComp == null) {
            System.err.println("GestorFondo: Todavía no está listo. Intenta en unos segundos.");
            return;
        }

        // Escalar la imagen al tamaño de textura requerido por Java3D (potencia de 2)
        BufferedImage escalada = new BufferedImage(TEX_SIZE, TEX_SIZE, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = escalada.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(imagen, 0, 0, TEX_SIZE, TEX_SIZE, null);
        g.dispose();

        // Actualización en vivo: Java3D renderizará el nuevo contenido en el próximo frame
        imageComp.set(escalada);
        System.out.println("GestorFondo: Imagen del piso del laberinto actualizada.");
    }

    /** Crea una imagen gris por defecto igual al color del piso original de Simbad. */
    private static BufferedImage crearImagenGris() {
        BufferedImage img = new BufferedImage(TEX_SIZE, TEX_SIZE, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(190, 190, 190));
        g.fillRect(0, 0, TEX_SIZE, TEX_SIZE);
        g.dispose();
        return img;
    }
}
