import java.util.*;

// Híbrido sencillo basado en tu Python y en la guía de la práctica.
// - Cola (BFS) y Pila (DFS) con LinkedList
// - Clases separadas CyM_SG_BFS y CyM_SG_DFS_Recursivo
// - Nodos simples con hijos = LinkedList<Nodo_*>

public class MisionerosCanibalesHibrido {

    // ===== Estado del problema (mi, ci, bi, md, cd) =====
    static class Estado {
        int mi, ci, bi, md, cd; // misioneros/caníbales izq/dcha y barco izq(1)/dcha(0)

        Estado(int mi, int ci, int bi, int md, int cd) {
            this.mi = mi; this.ci = ci; this.bi = bi; this.md = md; this.cd = cd;
        }
        Estado copiar() { return new Estado(mi, ci, bi, md, cd); }

        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Estado)) return false;
            Estado e = (Estado) o;
            return mi==e.mi && ci==e.ci && bi==e.bi && md==e.md && cd==e.cd;
        }
        @Override public int hashCode() { return Objects.hash(mi,ci,bi,md,cd); }
        @Override public String toString() { return "(" + mi + ", " + ci + ", " + bi + ", " + md + ", " + cd + ")"; }
    }

    // ===== Nodo para BFS =====
    static class Nodo_BFS {
        Estado estado;
        Nodo_BFS padre;
        String operacion; // "op1"... "op5"
        LinkedList<Nodo_BFS> hijos = new LinkedList<>();

        Nodo_BFS(Estado e, Nodo_BFS p, String op) {
            estado = e; padre = p; operacion = op;
        }
    }

    // ===== Nodo para DFS =====
    static class Nodo_DFS {
        Estado estado;
        Nodo_DFS padre;
        String operacion;
        LinkedList<Nodo_DFS> hijos = new LinkedList<>();

        Nodo_DFS(Estado e, Nodo_DFS p, String op) {
            estado = e; padre = p; operacion = op;
        }
    }

    // ===== Cola con LinkedList para BFS =====
    static class ColasGLL {
        private LinkedList<Nodo_BFS> info = new LinkedList<>();
        void encolar(Nodo_BFS n) { info.addLast(n); }     // push back
        Nodo_BFS desencolar() { return info.removeFirst(); } // pop front
        boolean estaVacia() { return info.isEmpty(); }
    }

    // ===== Pila con LinkedList para DFS =====
    static class PilasGLL {
        private LinkedList<Nodo_DFS> info = new LinkedList<>();
        void apilar(Nodo_DFS n) { info.addLast(n); }  // push
        Nodo_DFS desapilar() { return info.removeLast(); } // pop (LIFO)
        boolean estaVacia() { return info.isEmpty(); }
    }

    // ===== Generador de estados (misma lógica que tu Python) =====
    static class GeneraEstados {
        // op -> [misioneros, canibales]
        Map<String,int[]> ops = new LinkedHashMap<>();
        GeneraEstados() {
            ops.put("op1", new int[]{1,1}); // 1 caníbal y 1 misionero
            ops.put("op2", new int[]{0,2}); // 2 caníbales
            ops.put("op3", new int[]{2,0}); // 2 misioneros
            ops.put("op4", new int[]{0,1}); // 1 caníbal
            ops.put("op5", new int[]{1,0}); // 1 misionero
        }

        // ---- Para BFS ----
        LinkedList<Nodo_BFS> expandirBFS(Nodo_BFS padre,
                                          Set<Estado> visitados,
                                          Set<Estado> abiertos) {
            Map<String,Estado> gen = aplicar(padre.estado);
            for (Map.Entry<String,Estado> par : gen.entrySet()) {
                Estado h = par.getValue();
                if (!visitados.contains(h) && !abiertos.contains(h)) {
                    padre.hijos.add(new Nodo_BFS(h, padre, par.getKey()));
                } else {
                    padre.hijos.add(null); // para mantener "lugar" como en tu Python
                }
            }
            return padre.hijos;
        }

        // ---- Para DFS ----
        LinkedList<Nodo_DFS> expandirDFS(Nodo_DFS padre,
                                          Set<Estado> visitados) {
            Map<String,Estado> gen = aplicar(padre.estado);
            for (Map.Entry<String,Estado> par : gen.entrySet()) {
                Estado h = par.getValue();
                if (!visitados.contains(h)) {
                    padre.hijos.add(new Nodo_DFS(h, padre, par.getKey()));
                } else {
                    padre.hijos.add(null);
                }
            }
            return padre.hijos;
        }

        private Map<String,Estado> aplicar(Estado e) {
            Map<String,Estado> hijos = new LinkedHashMap<>();
            int lado = e.bi;
            int signo = (lado==1) ? -1 : 1;

            for (Map.Entry<String,int[]> par : ops.entrySet()) {
                int m = par.getValue()[0];
                int c = par.getValue()[1];
                Estado nvo = e.copiar();
                nvo.mi = nvo.mi + signo*m;
                nvo.ci = nvo.ci + signo*c;
                nvo.md = nvo.md - signo*m;
                nvo.cd = nvo.cd - signo*c;
                nvo.bi = 1 - e.bi;
                if (valido(nvo)) hijos.put(par.getKey(), nvo);
            }
            return hijos;
        }

        private boolean valido(Estado e) {
            int[] v = {e.mi, e.ci, e.bi, e.md, e.cd};
            for (int x : v) if (x < 0 || x > 3) return false;
            if (e.mi > 0 && e.ci > e.mi) return false;
            if (e.md > 0 && e.cd > e.md) return false;
            return true;
        }
    }

    // ===== BFS con Cola =====
    static class CyM_SG_BFS {
        Estado meta;
        Nodo_BFS raiz;
        ColasGLL cola = new ColasGLL();
        Set<Estado> visitados = new HashSet<>();
        Set<Estado> abiertos = new HashSet<>();

        CyM_SG_BFS(Estado meta, Estado inicial) {
            this.meta = meta;
            this.raiz = new Nodo_BFS(inicial, null, null);
        }

        Nodo_BFS buscar(GeneraEstados g) {
            cola.encolar(raiz);
            abiertos.add(raiz.estado);

            while (!cola.estaVacia()) {
                Nodo_BFS n = cola.desencolar();
                abiertos.remove(n.estado);

                if (visitados.contains(n.estado)) continue;
                visitados.add(n.estado);

                if (n.estado.equals(meta)) return n;

                LinkedList<Nodo_BFS> hijos = g.expandirBFS(n, visitados, abiertos);
                for (Nodo_BFS h : hijos) {
                    if (h != null) {
                        cola.encolar(h);
                        abiertos.add(h.estado);
                    }
                }
            }
            return null;
        }
    }

    // ===== DFS Recursivo =====
    static class CyM_SG_DFS_Recursivo {
        Estado meta;
        Nodo_DFS raiz;
        Set<Estado> visitados = new HashSet<>();

        CyM_SG_DFS_Recursivo(Estado meta, Estado inicial) {
            this.meta = meta;
            this.raiz = new Nodo_DFS(inicial, null, null);
        }

        Nodo_DFS buscar(GeneraEstados g) {
            return dfs(raiz, g);
        }

        private Nodo_DFS dfs(Nodo_DFS n, GeneraEstados g) {
            visitados.add(n.estado);
            if (n.estado.equals(meta)) return n;

            LinkedList<Nodo_DFS> hijos = g.expandirDFS(n, visitados);
            for (Nodo_DFS h : hijos) {
                if (h != null && !visitados.contains(h.estado)) {
                    Nodo_DFS r = dfs(h, g);
                    if (r != null) return r;
                }
            }
            return null;
        }
    }

    // ===== Utilidad: reconstruir camino =====
    static class Camino {
        LinkedList<Estado> estados = new LinkedList<>();
        LinkedList<String> ops = new LinkedList<>();
    }

    static Camino reconstruirCaminoBFS(Nodo_BFS meta) {
        Camino c = new Camino();
        for (Nodo_BFS n = meta; n != null; n = n.padre) {
            c.estados.addFirst(n.estado);
            c.ops.addFirst(n.operacion);
        }
        if (!c.ops.isEmpty() && c.ops.getFirst()==null) c.ops.removeFirst();
        return c;
    }

    static Camino reconstruirCaminoDFS(Nodo_DFS meta) {
        Camino c = new Camino();
        for (Nodo_DFS n = meta; n != null; n = n.padre) {
            c.estados.addFirst(n.estado);
            c.ops.addFirst(n.operacion);
        }
        if (!c.ops.isEmpty() && c.ops.getFirst()==null) c.ops.removeFirst();
        return c;
    }

    // ===== MAIN =====
    public static void main(String[] args) {
        Estado inicial = new Estado(3,3,1,0,0);
        Estado meta    = new Estado(0,0,0,3,3);
        GeneraEstados gen = new GeneraEstados();

        boolean RECURSIVO = true; // cámbialo a false para usar BFS
        if (RECURSIVO) {
            CyM_SG_DFS_Recursivo busc = new CyM_SG_DFS_Recursivo(meta, inicial);
            Nodo_DFS sol = busc.buscar(gen);
            if (sol == null) {
                System.out.println("No hay solución (DFS recursivo).");
            } else {
                Camino cam = reconstruirCaminoDFS(sol);
                System.out.println("DFS_Recursivo: estados:");
                for (Estado e : cam.estados) System.out.println("  " + e);
                System.out.println("Operaciones:");
                for (String op : cam.ops) System.out.println("  " + op);
            }
        } else {
            CyM_SG_BFS busc = new CyM_SG_BFS(meta, inicial);
            Nodo_BFS sol = busc.buscar(gen);
            if (sol == null) {
                System.out.println("No hay solución (BFS).");
            } else {
                Camino cam = reconstruirCaminoBFS(sol);
                System.out.println("BFS: estados:");
                for (Estado e : cam.estados) System.out.println("  " + e);
                System.out.println("Operaciones:");
                for (String op : cam.ops) System.out.println("  " + op);
            }
        }
    }
}
