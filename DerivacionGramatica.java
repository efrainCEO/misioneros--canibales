import java.util.*;

// =============================================================
// Clase Nodo: representa un nodo en el árbol de derivación
// =============================================================
class Nodo {
    String estadoStr;        // Cadena actual
    Nodo padre;              // Nodo padre
    Integer noRegla;         // Número de la regla aplicada
    List<Nodo> hijos;        // Lista de hijos generados

    public Nodo(String estadoStr, Nodo padre, Integer noRegla) {
        this.estadoStr = estadoStr;
        this.padre = padre;
        this.noRegla = noRegla;
        this.hijos = new ArrayList<>();
    }
}

// =============================================================
// Clase Frontera: maneja los nodos por explorar
// =============================================================
class Frontera {
    String metodo;                  // "izquierda" o "derecha"
    List<Nodo> nodosFrontera;       // Lista de nodos en la frontera

    public Frontera(String metodo) {
        this.metodo = metodo;
        this.nodosFrontera = new ArrayList<>();
    }

    // Agregar nodos a la frontera (al principio)
    public void agregar(List<Nodo> nodos) {
        // Añadir los nuevos nodos al inicio (simula DFS)
        nodosFrontera.addAll(0, nodos);
    }

    // Extraer el siguiente nodo según el método
    public Nodo extraer() {
        if (nodosFrontera.isEmpty()) {
            return null;
        }
        if (metodo.equals("izquierda")) {
            return nodosFrontera.remove(0); // primer elemento
        } else {
            return nodosFrontera.remove(nodosFrontera.size() - 1); // último elemento
        }
    }

    public boolean esVacia() {
        return nodosFrontera.isEmpty();
    }
}

// =============================================================
// Clase GeneraEstados: aplica reglas de producción
// =============================================================
class GeneraEstados {
    Map<String, List<Regla>> reglas;   // Reglas de producción

    public GeneraEstados(Map<String, List<Regla>> reglasProduccion) {
        this.reglas = reglasProduccion;
    }

    public List<Nodo> expandir(Nodo padre, String metodo) {
        String cadena = padre.estadoStr;
        List<Par> ocurrencias = new ArrayList<>();

        // Buscar todas las ocurrencias de las "cabezas" (no terminales)
        for (String cabeza : reglas.keySet()) {
            int start = 0;
            while (true) {
                int i = cadena.indexOf(cabeza, start);
                if (i == -1) break;
                ocurrencias.add(new Par(i, cabeza));
                start = i + 1;
            }
        }

        // Si no hay ocurrencias, la cadena es completamente terminal
        if (ocurrencias.isEmpty()) {
            return new ArrayList<>();
        }

        // Elegir la ocurrencia según el método
        Par elegida;
        if (metodo.equals("izquierda")) {
            elegida = Collections.min(ocurrencias, Comparator.comparingInt(p -> p.posicion));
        } else {
            elegida = Collections.max(ocurrencias, Comparator.comparingInt(p -> p.posicion));
        }

        int i = elegida.posicion;
        String cabeza = elegida.cabeza;

        // Generar todos los hijos posibles aplicando las reglas
        List<Nodo> hijos = new ArrayList<>();
        for (Regla regla : reglas.get(cabeza)) {
            String nueva = cadena.substring(0, i) + regla.produccion + cadena.substring(i + cabeza.length());
            Nodo hijo = new Nodo(nueva, padre, regla.noRegla);
            hijos.add(hijo);
        }

        padre.hijos = hijos;
        return hijos;
    }
}

// =============================================================
// Clase Regla: representa una producción (número + parte derecha)
// =============================================================
class Regla {
    int noRegla;
    String produccion;

    public Regla(int noRegla, String produccion) {
        this.noRegla = noRegla;
        this.produccion = produccion;
    }
}

// =============================================================
// Clase Par: ayuda a guardar posición y cabeza encontrada
// =============================================================
class Par {
    int posicion;
    String cabeza;

    public Par(int posicion, String cabeza) {
        this.posicion = posicion;
        this.cabeza = cabeza;
    }
}

// =============================================================
// Clase Buscador: controla el proceso de derivación
// =============================================================
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

            if (nodo.estadoStr.equals(estadoFinal)) {
                soluciones.add(nodo);
            }

            if (visitados.contains(nodo.estadoStr)) {
                continue;
            }

            visitados.add(nodo.estadoStr);

            if (profundidad(nodo) > MAX_N) {
                continue;
            }

            List<Nodo> hijos = generador.expandir(nodo, metodo);
            if (!hijos.isEmpty()) {
                frontera.agregar(hijos);
                nodo.noRegla = hijos.get(0).noRegla;
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

// =============================================================
// Funciones auxiliares (estilo funciones Python)
// =============================================================
class Utilidades {

    public static List<String> reconstruirTodosLosCaminos(List<Nodo> soluciones) {
        List<String> caminos = new ArrayList<>();
        for (Nodo solucion : soluciones) {
            caminos.add(reconstruirCamino(solucion));
        }
        return caminos;
    }

    public static String reconstruirCamino(Nodo nodo) {
        List<String> cabezas = new ArrayList<>();
        List<Integer> reglas = new ArrayList<>();

        while (nodo != null) {
            cabezas.add(nodo.estadoStr);
            reglas.add(nodo.noRegla);
            nodo = nodo.padre;
        }

        Collections.reverse(cabezas);
        Collections.reverse(reglas);

        if (!reglas.isEmpty()) {
            reglas.remove(reglas.size() - 1);
        }
        reglas.add(null);

        StringBuilder camino = new StringBuilder("->");
        for (int i = 0; i < cabezas.size(); i++) {
            String cabeza = cabezas.get(i);
            Integer regla = reglas.get(i);
            if (regla == null) {
                camino.append("(").append(cabeza).append(")");
            } else {
                camino.append("(").append(cabeza).append(",").append(regla).append(")->");
            }
        }
        return camino.toString();
    }
}

// =============================================================
// Clase principal con el método main
// =============================================================
public class DerivacionGramatica {
    public static void main(String[] args) {
        // Definir las reglas de producción
        Map<String, List<Regla>> reglasProduccion = new HashMap<>();

        reglasProduccion.put("S", Arrays.asList(new Regla(1, "ABC")));
        reglasProduccion.put("E", Arrays.asList(new Regla(2, "b")));
        reglasProduccion.put("aaA", Arrays.asList(new Regla(3, "aaBB")));
        reglasProduccion.put("B", Arrays.asList(new Regla(4, "d")));
        reglasProduccion.put("A", Arrays.asList(new Regla(5, "aE")));
        reglasProduccion.put("C", Arrays.asList(new Regla(6, "dcd")));

        String estadoInicial = "S";
        String estadoFinal = "abddcd";
        String metodo = "derecha"; // puede ser "izquierda"

        // Crear el generador y el buscador
        GeneraEstados generador = new GeneraEstados(reglasProduccion);
        Buscador buscador = new Buscador(metodo, estadoInicial, estadoFinal);

        // Ejecutar la búsqueda
        List<Nodo> soluciones = buscador.buscar(generador);

        // Reconstruir caminos
        List<String> todosLosCaminos = Utilidades.reconstruirTodosLosCaminos(soluciones);

        // Mostrar los resultados
        for (String camino : todosLosCaminos) {
            System.out.println(camino);
        }

        if (todosLosCaminos.size() > 1) {
            System.out.println("La gramática ingresada es ambigua para la cadena: " + estadoFinal);
        } else {
            System.out.println("La gramática ingresada no es ambigua para la cadena: " + estadoFinal);
        }
    }
}
