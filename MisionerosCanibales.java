import java.util.*;

// ==================================================
// Clase Nodo: representa un estado del problema
// ==================================================
class Nodo {
    int[] estado;          // Estado actual del problema
    Nodo padre;            // Referencia al nodo padre
    String operacion;      // Nombre de la operación aplicada
    List<Nodo> hijos;      // Hijos generados

    public Nodo(int[] estado, Nodo padre, String operacion) {
        this.estado = estado;
        this.padre = padre;
        this.operacion = operacion;
        this.hijos = new ArrayList<>();
    }
}

// ==================================================
// Clase Frontera: maneja la cola o pila (según método)
// ==================================================
class Frontera {
    String metodo;              // "BFS" o "DFS"
    LinkedList<Nodo> nodos;     // Lista enlazada como cola o pila

    public Frontera(String metodo) {
        this.metodo = metodo;
        this.nodos = new LinkedList<>();
    }

    public void agregar(Nodo nodo) {
        nodos.addLast(nodo); // agrega al final
    }

    public Nodo extraer() {
        if (metodo.equals("BFS")) {
            // Búsqueda en amplitud (cola FIFO)
            return nodos.pollFirst();
        } else {
            // Búsqueda en profundidad (pila LIFO)
            return nodos.pollLast();
        }
    }

    public boolean esVacia() {
        return nodos.isEmpty();
    }
}

// ==================================================
// Clase GeneraEstados: aplica operaciones válidas
// ==================================================
class GeneraEstados {
    Map<String, int[]> operaciones;

    public GeneraEstados() {
        operaciones = new LinkedHashMap<>();
        operaciones.put("op1", new int[]{1, 1}); // 1 misionero, 1 caníbal
        operaciones.put("op2", new int[]{0, 2}); // 0 misioneros, 2 caníbales
        operaciones.put("op3", new int[]{2, 0}); // 2 misioneros, 0 caníbales
        operaciones.put("op4", new int[]{0, 1}); // 0 misioneros, 1 caníbal
        operaciones.put("op5", new int[]{1, 0}); // 1 misionero, 0 caníbales
    }

    public List<Nodo> expandir(Nodo padre, Set<String> visitados, Set<String> abiertos) {
        Map<String, int[]> generados = aplicarOperaciones(padre.estado);
        List<Nodo> hijos = new ArrayList<>();

        for (Map.Entry<String, int[]> entry : generados.entrySet()) {
            String operacion = entry.getKey();
            int[] estadoHijo = entry.getValue();

            // Evitar repetir estados ya visitados o abiertos
            if (!estaEnConjunto(estadoHijo, visitados) && !estaEnConjunto(estadoHijo, abiertos)) {
                Nodo hijo = new Nodo(estadoHijo, padre, operacion);
                hijos.add(hijo);
                padre.hijos.add(hijo);
            }
        }
        return hijos;
    }

    private boolean estaEnConjunto(int[] estado, Set<String> conjunto) {
        return conjunto.contains(Arrays.toString(estado));
    }

    private Map<String, int[]> aplicarOperaciones(int[] estado) {
        Map<String, int[]> hijos = new LinkedHashMap<>();
        int lado = estado[2]; // 1 si el barco está a la izquierda
        int signo = (lado == 1) ? -1 : 1; // movimiento según el lado

        for (Map.Entry<String, int[]> entry : operaciones.entrySet()) {
            String op = entry.getKey();
            int m = entry.getValue()[0];
            int c = entry.getValue()[1];

            int[] nuevo = estado.clone();
            nuevo[0] += signo * m;  // misioneros izq
            nuevo[1] += signo * c;  // caníbales izq
            nuevo[3] -= signo * m;  // misioneros der
            nuevo[4] -= signo * c;  // caníbales der
            nuevo[2] = 1 - estado[2]; // cambia el lado del barco

            if (esValido(nuevo)) {
                hijos.put(op, nuevo);
            }
        }
        return hijos;
    }

    private boolean esValido(int[] estado) {
        int mIzq = estado[0];
        int cIzq = estado[1];
        int mDer = estado[3];
        int cDer = estado[4];

        // Rango válido (0 a 3)
        for (int v : estado) {
            if (v < 0 || v > 3) return false;
        }

        // Reglas de equilibrio
        if (mIzq > 0 && cIzq > mIzq) return false;
        if (mDer > 0 && cDer > mDer) return false;

        return true;
    }
}

// ==================================================
// Clase Buscador: búsqueda BFS o DFS
// ==================================================
class Buscador {
    Frontera frontera;
    Set<String> visitados;
    Set<String> abiertos;
    int[] estadoFinal;
    Nodo raiz;

    public Buscador(String metodo, int[] estadoFinal, int[] estadoInicial) {
        this.frontera = new Frontera(metodo);
        this.visitados = new HashSet<>();
        this.abiertos = new HashSet<>();
        this.estadoFinal = estadoFinal;
        this.raiz = new Nodo(estadoInicial, null, null);
    }

    public Nodo buscar(GeneraEstados generador) {
        frontera.agregar(raiz);
        abiertos.add(Arrays.toString(raiz.estado));

        while (!frontera.esVacia()) {
            Nodo actual = frontera.extraer();
            abiertos.remove(Arrays.toString(actual.estado));

            if (visitados.contains(Arrays.toString(actual.estado))) continue;

            visitados.add(Arrays.toString(actual.estado));

            if (Arrays.equals(actual.estado, estadoFinal)) {
                return actual;
            }

            List<Nodo> hijos = generador.expandir(actual, visitados, abiertos);
            for (Nodo h : hijos) {
                frontera.agregar(h);
                abiertos.add(Arrays.toString(h.estado));
            }
        }
        return null;
    }
}

// ==================================================
// Clase BuscadorRecursivo: búsqueda DFS recursiva
// ==================================================
class BuscadorRecursivo {
    int[] estadoFinal;
    Nodo raiz;
    Set<String> visitados;

    public BuscadorRecursivo(int[] estadoFinal, int[] estadoInicial) {
        this.estadoFinal = estadoFinal;
        this.raiz = new Nodo(estadoInicial, null, null);
        this.visitados = new HashSet<>();
    }

    public Nodo buscar(GeneraEstados generador) {
        return dfsRecursivo(raiz, generador);
    }

    private Nodo dfsRecursivo(Nodo nodo, GeneraEstados generador) {
        visitados.add(Arrays.toString(nodo.estado));

        if (Arrays.equals(nodo.estado, estadoFinal)) {
            return nodo;
        }

        List<Nodo> hijos = generador.expandir(nodo, visitados, new HashSet<>());

        for (Nodo hijo : hijos) {
            if (hijo != null && !visitados.contains(Arrays.toString(hijo.estado))) {
                Nodo resultado = dfsRecursivo(hijo, generador);
                if (resultado != null) {
                    return resultado;
                }
            }
        }
        return null;
    }
}

// ==================================================
// Clase Utilidades: reconstruye e imprime la solución
// ==================================================
class Utilidades {
    public static List<int[]> reconstruirCamino(Nodo meta) {
        List<int[]> camino = new ArrayList<>();
        Nodo actual = meta;
        while (actual != null) {
            camino.add(actual.estado);
            actual = actual.padre;
        }
        Collections.reverse(camino);
        return camino;
    }

    public static void imprimirCamino(List<int[]> camino) {
        System.out.println("\nSolución encontrada:");
        for (int[] e : camino) {
            System.out.println(Arrays.toString(e));
        }
        System.out.println("Total de pasos: " + (camino.size() - 1));
    }

    public static void imprimirResultado(String metodo, long tiempo, List<int[]> camino) {
        System.out.println("\n===== RESULTADO DE LA " + metodo + " =====");
        imprimirCamino(camino);
        System.out.println("Tiempo de ejecución: " + tiempo + " ms");
    }
}

// ==================================================
// Clase principal
// ==================================================
public class MisionerosCanibales {
    public static void main(String[] args) {
        int[] estadoInicial = {3, 3, 1, 0, 0};
        int[] estadoFinal = {0, 0, 0, 3, 3};
        String METODO = "DFS_recursivo";  // Cambiar a "DFS" o "DFS_recursivo"

        GeneraEstados generador = new GeneraEstados();
        Nodo solucion = null;

        long inicio = System.currentTimeMillis();

        if (METODO.equals("DFS_recursivo")) {
            BuscadorRecursivo buscador = new BuscadorRecursivo(estadoFinal, estadoInicial);
            solucion = buscador.buscar(generador);
        } else {
            Buscador buscador = new Buscador(METODO, estadoFinal, estadoInicial);
            solucion = buscador.buscar(generador);
        }

        long fin = System.currentTimeMillis();
        long tiempo = fin - inicio;

        if (solucion != null) {
            List<int[]> camino = Utilidades.reconstruirCamino(solucion);
            Utilidades.imprimirResultado(METODO, tiempo, camino);
        } else {
            System.out.println("No se encontró solución.");
        }
    }
}
