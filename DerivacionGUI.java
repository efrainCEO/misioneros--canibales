import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.List;

/* ===========================
   MODELO (tu motor existente)
   =========================== */

// ---------------------------
// Clase Nodo
// ---------------------------
class Nodo {
    String estadoStr;
    Nodo padre;
    Integer noRegla;
    List<Nodo> hijos;

    public Nodo(String estadoStr, Nodo padre, Integer noRegla) {
        this.estadoStr = estadoStr;
        this.padre = padre;
        this.noRegla = noRegla;
        this.hijos = new ArrayList<>();
    }
}

// ---------------------------
// Clase Frontera
// ---------------------------
class Frontera {
    String metodo;            // "izquierda" o "derecha"
    List<Nodo> nodosFrontera; // frontera (LIFO o FIFO segun método elegido aquí como DFS-like)

    public Frontera(String metodo) {
        this.metodo = metodo;
        this.nodosFrontera = new ArrayList<>();
    }

    public void agregar(List<Nodo> nodos) {
        // Añade al inicio para simular un recorrido en profundidad (está bien para este ejercicio)
        nodosFrontera.addAll(0, nodos);
    }

    public Nodo extraer() {
        if (nodosFrontera.isEmpty()) return null;
        if (metodo.equals("izquierda")) {
            return nodosFrontera.remove(0);
        } else {
            return nodosFrontera.remove(nodosFrontera.size() - 1);
        }
    }

    public boolean esVacia() {
        return nodosFrontera.isEmpty();
    }
}

// ---------------------------
// Clase Regla
// ---------------------------
class Regla {
    int noRegla;
    String produccion; // cuerpo/derecha

    public Regla(int noRegla, String produccion) {
        this.noRegla = noRegla;
        this.produccion = produccion;
    }
}

// ---------------------------
// Aux: Par(posición, cabeza)
// ---------------------------
class Par {
    int posicion;
    String cabeza;

    public Par(int posicion, String cabeza) {
        this.posicion = posicion;
        this.cabeza = cabeza;
    }
}

// ---------------------------
// GeneraEstados
// ---------------------------
class GeneraEstados {
    Map<String, List<Regla>> reglas;

    public GeneraEstados(Map<String, List<Regla>> reglasProduccion) {
        this.reglas = reglasProduccion;
    }

    public List<Nodo> expandir(Nodo padre, String metodo) {
        String cadena = padre.estadoStr;
        List<Par> ocurrencias = new ArrayList<>();

        // Buscar todas las ocurrencias de TODAS las cabezas (no terminales)
        for (String cabeza : reglas.keySet()) {
            int start = 0;
            while (true) {
                int i = cadena.indexOf(cabeza, start);
                if (i == -1) break;
                ocurrencias.add(new Par(i, cabeza));
                start = i + 1;
            }
        }

        // Si no hay más no terminales que reemplazar
        if (ocurrencias.isEmpty()) return new ArrayList<>();

        // Elegir ocurrencia por izquierda o derecha
        Par elegida;
        if (metodo.equals("izquierda")) {
            elegida = Collections.min(ocurrencias, Comparator.comparingInt(p -> p.posicion));
        } else {
            elegida = Collections.max(ocurrencias, Comparator.comparingInt(p -> p.posicion));
        }

        int i = elegida.posicion;
        String cabeza = elegida.cabeza;

        // Expandir con todas las reglas para esa cabeza
        List<Nodo> hijos = new ArrayList<>();
        for (Regla regla : reglas.get(cabeza)) {
            String produccion = "ε".equals(regla.produccion) ? "" : regla.produccion; // soporta epsilon
            String nueva = cadena.substring(0, i) + produccion + cadena.substring(i + cabeza.length());
            Nodo hijo = new Nodo(nueva, padre, regla.noRegla);
            hijos.add(hijo);
        }

        padre.hijos = hijos;
        return hijos;
    }
}

// ---------------------------
// Buscador
// ---------------------------
class Buscador {
    String metodo;
    Frontera frontera;
    String estadoFinal;
    Nodo raiz;
    int MAX_N = 25;
    Set<String> visitados;
    List<Nodo> soluciones;

    public Buscador(String metodo, String estadoInicial, String estadoFinal) {
        this.metodo = metodo;
        this.frontera = new Frontera(metodo);
        this.estadoFinal = estadoFinal;
        this.raiz = new Nodo(estadoInicial, null, null);
        this.visitados = new HashSet<>();
        this.soluciones = new ArrayList<>();
    }

    public List<Nodo> buscar(GeneraEstados generador) {
        frontera.agregar(Arrays.asList(raiz));

        while (!frontera.esVacia()) {
            Nodo nodo = frontera.extraer();
            if (nodo == null) break;

            if (nodo.estadoStr.equals(estadoFinal)) {
                soluciones.add(nodo);
                // No retornamos de inmediato, seguimos buscando para detectar ambigüedad
            }

            if (visitados.contains(nodo.estadoStr)) continue;
            visitados.add(nodo.estadoStr);

            if (profundidad(nodo) > MAX_N) continue;

            List<Nodo> hijos = generador.expandir(nodo, metodo);
            if (!hijos.isEmpty()) {
                frontera.agregar(hijos);
                nodo.noRegla = hijos.get(0).noRegla; // opcional
            }
        }
        return soluciones;
    }

    public int profundidad(Nodo nodo) {
        int d = 0;
        while (nodo != null && nodo.padre != null) {
            d++;
            nodo = nodo.padre;
        }
        return d;
    }
}

// ---------------------------
// Utilidades (reconstrucción)
// ---------------------------
class Utilidades {
    public static List<String> reconstruirTodosLosCaminos(List<Nodo> soluciones) {
        List<String> caminos = new ArrayList<>();
        for (Nodo solucion : soluciones) caminos.add(reconstruirCamino(solucion));
        return caminos;
    }

    public static String reconstruirCamino(Nodo nodo) {
        List<String> cadenas = new ArrayList<>();
        List<Integer> reglas = new ArrayList<>();

        while (nodo != null) {
            cadenas.add(nodo.estadoStr);
            reglas.add(nodo.noRegla);
            nodo = nodo.padre;
        }
        Collections.reverse(cadenas);
        Collections.reverse(reglas);

        if (!reglas.isEmpty()) reglas.remove(reglas.size() - 1);
        reglas.add(null);

        StringBuilder sb = new StringBuilder("Resultados de la derivación: ");
        for (int i = 0; i < cadenas.size(); i++) {
            String cad = cadenas.get(i);
            Integer r = reglas.get(i);
            if (r == null) sb.append("(").append(cad).append(")");
            else sb.append("(").append(cad).append(",").append(r).append(")->");
        }
        return sb.toString();
    }
}

/* ====================================
   PARSER de gramáticas y utilidades IO
   ==================================== */

class GrammarParser {
    // Formato esperado: "1. S -> AA"
    // Cabeceras y cuerpos pueden ser cadenas (p.ej., "aaA"). Epsilon con "ε".
    public static Map<String, List<Regla>> parseRules(List<String> lines) throws IOException {
        Map<String, List<Regla>> mapa = new LinkedHashMap<>();

        for (String raw : lines) {
            String line = raw.trim();
            if (line.isEmpty()) continue;
            // quita comentarios simples si quieres (opcional)
            // if (line.startsWith("#")) continue;

            // separar índice
            int dotIdx = line.indexOf('.');
            if (dotIdx <= 0) continue; // línea inválida, se ignora
            String numStr = line.substring(0, dotIdx).trim();

            // cabeza y cuerpo
            String rest = line.substring(dotIdx + 1).trim(); // "S -> AA"
            int arrow = rest.indexOf("->");
            if (arrow < 0) continue;

            String head = rest.substring(0, arrow).trim();   // "S"
            String body = rest.substring(arrow + 2).trim();  // "AA"

            int num;
            try { num = Integer.parseInt(numStr); }
            catch (NumberFormatException e) { continue; }

            // guarda
            mapa.computeIfAbsent(head, k -> new ArrayList<>())
                .add(new Regla(num, body));
        }
        // ordena reglas por número de regla, para reproducibilidad
        for (List<Regla> l : mapa.values()) {
            l.sort(Comparator.comparingInt(r -> r.noRegla));
        }
        return mapa;
    }
}

/* ===========================
   VISTA/CONTROLADOR (Swing)
   =========================== */

public class DerivacionGUI extends JFrame {

    // Componentes GUI
    private JTextArea txtReglas;
    private JTextArea txtResultados;
    private JTextField txtCadena;
    private JRadioButton rbIzq, rbDer;
    private JButton btnDerivar;

    // Estado/IO
    private File archivoActual = null;
    private Map<String, List<Regla>> reglas = null;

    // Ajustes
    private static final String ESTADO_INICIAL = "S";   // se asume "S" como axioma
    private static final int MAX_N = 25;                // límite sugerido

    public DerivacionGUI() {
        super("Práctica 3 – Derivación Izquierda/Derecha de una G2");

        // ====== Layout principal ======
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // ====== Barra de menú ======
        JMenuBar menuBar = new JMenuBar();
        JMenu mArchivo = new JMenu("Archivo");

        JMenuItem miAbrir = new JMenuItem("Abrir");
        miAbrir.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        miAbrir.addActionListener(e -> onAbrir());

        JMenuItem miGuardar = new JMenuItem("Guardar Cambios");
        miGuardar.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        miGuardar.addActionListener(e -> onGuardar());

        JMenuItem miSalir = new JMenuItem("Salir");
        miSalir.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F4, InputEvent.ALT_DOWN_MASK));
        miSalir.addActionListener(e -> onSalir());

        mArchivo.add(miAbrir);
        mArchivo.add(miGuardar);
        mArchivo.addSeparator();
        mArchivo.add(miSalir);
        menuBar.add(mArchivo);
        setJMenuBar(menuBar);

        // ====== Panel izquierdo: reglas ======
        JPanel panelCentro = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8,8,8,8);
        gbc.fill = GridBagConstraints.BOTH;

        JLabel lblReglas = new JLabel("REGLAS PRESENTES EN LA GRAMÁTICA");
        lblReglas.setFont(lblReglas.getFont().deriveFont(Font.BOLD));

        txtReglas = new JTextArea();
        txtReglas.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        txtReglas.setLineWrap(false);
        JScrollPane spReglas = new JScrollPane(txtReglas);

        // ====== Panel derecho: controles ======
        JPanel panelControl = new JPanel();
        panelControl.setLayout(new BoxLayout(panelControl, BoxLayout.Y_AXIS));

        JLabel lblCadena = new JLabel("Cadena a Derivar");
        txtCadena = new JTextField();
        txtCadena.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));

        rbIzq = new JRadioButton("Derivación por la Izquierda");
        rbDer = new JRadioButton("Derivación por la Derecha");
        ButtonGroup bg = new ButtonGroup();
        bg.add(rbIzq);
        bg.add(rbDer);

        btnDerivar = new JButton("INICIAR DERIVACIÓN");
        btnDerivar.addActionListener(e -> onDerivar());

        // accesos rápidos
        txtCadena.addActionListener(e -> onDerivar());

        panelControl.add(lblCadena);
        panelControl.add(Box.createVerticalStrut(4));
        panelControl.add(txtCadena);
        panelControl.add(Box.createVerticalStrut(12));
        panelControl.add(rbIzq);
        panelControl.add(rbDer);
        panelControl.add(Box.createVerticalStrut(12));
        panelControl.add(btnDerivar);

        // ====== Resultados ======
        JLabel lblResultados = new JLabel("RESULTADOS DE LA DERIVACIÓN");
        lblResultados.setFont(lblResultados.getFont().deriveFont(Font.BOLD));

        txtResultados = new JTextArea();
        txtResultados.setEditable(false);
        txtResultados.setLineWrap(true);
        txtResultados.setWrapStyleWord(true);
        txtResultados.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        JScrollPane spResultados = new JScrollPane(txtResultados);

        // ====== Ubicar en la grilla ======
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.7; gbc.weighty = 0.0;
        panelCentro.add(lblReglas, gbc);
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 0.3; gbc.weighty = 0.0;
        panelCentro.add(new JLabel(""), gbc); // separador visual

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.7; gbc.weighty = 0.7;
        panelCentro.add(spReglas, gbc);
        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 0.3; gbc.weighty = 0.7;
        panelCentro.add(panelControl, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2; gbc.weightx = 1.0; gbc.weighty = 0.0;
        panelCentro.add(lblResultados, gbc);
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2; gbc.weightx = 1.0; gbc.weighty = 0.3;
        panelCentro.add(spResultados, gbc);

        add(panelCentro, BorderLayout.CENTER);
    }

    /* === Handlers === */

    private void onAbrir() {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileNameExtensionFilter("Texto (*.txt)", "txt"));
        int res = fc.showOpenDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            archivoActual = fc.getSelectedFile();
            try {
                List<String> lines = Files.readAllLines(archivoActual.toPath(), StandardCharsets.UTF_8);
                txtReglas.setText(String.join(System.lineSeparator(), lines));
                // parseamos para validar desde ya
                reglas = GrammarParser.parseRules(lines);
                JOptionPane.showMessageDialog(this, "Archivo cargado y reglas analizadas correctamente.",
                        "Abrir", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                showError("Error al leer el archivo:\n" + ex.getMessage());
            }
        }
    }

    private void onGuardar() {
        try {
            if (archivoActual == null) {
                // “Guardar como…”
                JFileChooser fc = new JFileChooser();
                fc.setFileFilter(new FileNameExtensionFilter("Texto (*.txt)", "txt"));
                int res = fc.showSaveDialog(this);
                if (res != JFileChooser.APPROVE_OPTION) return;
                archivoActual = fc.getSelectedFile();
                if (!archivoActual.getName().toLowerCase().endsWith(".txt")) {
                    archivoActual = new File(archivoActual.getParentFile(), archivoActual.getName() + ".txt");
                }
            }
            Files.write(archivoActual.toPath(), txtReglas.getText().getBytes(StandardCharsets.UTF_8));
            // re-parsear tras guardar
            reglas = GrammarParser.parseRules(Arrays.asList(txtReglas.getText().split("\\R")));
            JOptionPane.showMessageDialog(this, "Cambios guardados correctamente.",
                    "Guardar Cambios", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            showError("Error al guardar:\n" + ex.getMessage());
        }
    }

    private void onSalir() {
        dispose();
    }

    private void onDerivar() {
        // Validaciones (pág. 8)
        if (reglas == null || reglas.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "No se ha abierto un archivo con las reglas de producción.",
                    "Faltan reglas", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String cadena = txtCadena.getText().trim();
        if (cadena.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Ingrese la cadena de caracteres a comprobar.",
                    "Falta cadena", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String metodo = rbIzq.isSelected() ? "izquierda" : rbDer.isSelected() ? "derecha" : null;
        if (metodo == null) {
            JOptionPane.showMessageDialog(this,
                    "Seleccione un método de derivación (Izquierda o Derecha).",
                    "Falta método", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Ejecutar búsqueda con tu motor
        GeneraEstados generador = new GeneraEstados(reglas);
        Buscador buscador = new Buscador(metodo, ESTADO_INICIAL, cadena);
        buscador.MAX_N = MAX_N;

        long t0 = System.nanoTime();
        List<Nodo> soluciones = buscador.buscar(generador);
        long t1 = System.nanoTime();

        List<String> caminos = Utilidades.reconstruirTodosLosCaminos(soluciones);

        // Construir salida en el panel de resultados
        StringBuilder out = new StringBuilder();
        out.append("Resultados de la Derivación por la ").append(metodo.equals("izquierda") ? "Izquierda" : "Derecha").append("\n\n");
        if (caminos.isEmpty()) {
            out.append("No se encontró derivación desde '").append(ESTADO_INICIAL)
               .append("' hasta '").append(cadena).append("'.\n");
        } else {
            for (String c : caminos) {
                out.append(c).append("\n");
            }
            if (caminos.size() > 1) {
                out.append("\nLa Gramática ingresada es **Ambigua** para la cadena: ").append(cadena).append("\n");
            } else {
                out.append("\nLa Gramática ingresada **No es ambigua** para la cadena: ").append(cadena).append("\n");
            }
        }
        double ms = (t1 - t0) / 1_000_000.0;
        out.append(String.format("\nTiempo de ejecución: %.3f ms\n", ms));
        txtResultados.setText(out.toString());
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    /* ====== main ====== */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new DerivacionGUI().setVisible(true));
    }
}
