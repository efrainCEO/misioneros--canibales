# -*- coding: utf-8 -*-
"""
Created on Sat Oct 18 18:27:44 2025

@author: Efrain Santos Luna

@mail: efrain.santos.consultor@gmail.com

@phone:55-66-18-62-95
"""

import collections
import time


# ----------------------------
# Representación de un nodo en el árbol/grafo de búsqueda
# ----------------------------
class Nodo:
    def __init__(self, estado=(), padre=None, operacion=None):
        self.estado = estado          # Tupla inmutable que describe el estado del problema
        self.hijos = []               #lista para almacenar los hijos generados
        self.padre = padre            # Referencia al nodo padre (para reconstruir el camino)
        self.operacion = operacion    # Etiqueta de la operación aplicada para llegar a este nodo


# ----------------------------
# Estructura de frontera (cola o pila) según el método de búsqueda
# ----------------------------
class Frontera():
    def __init__(self, metodo):
        self.metodo = metodo
        if metodo == 'BFS':
            # Para BFS (búsqueda en anchura), una deque permite popleft() en O(1)
            self.nodos_frontera = collections.deque()
        else:
            # Para otros métodos (ej. DFS), una lista funciona como pila (LIFO)
            self.nodos_frontera = []
        
    def agregar(self, nodo):
        # Inserta al final (en cola para BFS, en tope de pila para DFS que usa .pop())
        self.nodos_frontera.append(nodo)
        
    def extraer(self):
        # Extrae según la política del método
        if self.metodo == 'BFS':
            # BFS: extrae por el frente (FIFO)
            return self.nodos_frontera.popleft()
        else:
            # DFS (u otros): extrae del final (LIFO)
            return self.nodos_frontera.pop()
    
    def es_vacia(self):
        # True si no quedan nodos por explorar
        if len(self.nodos_frontera) == 0:
            return True
        else:
            return False


# ----------------------------
# Generador de estados hijos a partir de un estado padre
# ----------------------------
class GeneraEstados:
    
    def __init__(self):
        self.operaciones = {
            "op1":(1, 1),  # cruzar 1 caníbal y 1 misionero
            "op2":(0, 2),  # cruzar 2 caníbales y 0 misioneros
            "op3":(2, 0),  # cruzar 0 caníbales y 2 misioneros
            "op4":(0, 1),  # cruzar 1 caníbal y 0 misioneros
            "op5":(1, 0)  # cruzar 0 caníbales y 1 misionero
        }


        
    def expandir(self, padre, visitados, abiertos):
        # Aplica todas las operaciones disponibles al estado del nodo padre
        estados_generados = self.aplicar_operaciones(padre.estado)
        # Intenta crear cada hijo (solo si no está en visitados ni en abiertos)
        for operacion, estado in estados_generados.items():
            hijo_ = self.crear_hijo(padre, estado, operacion, visitados, abiertos)
            padre.hijos.append(hijo_)


        # Devuelve la terna de hijos (pueden ser None si fueron filtrados)
        return padre.hijos
    
    def crear_hijo(self, padre, estado_hijo, operacion, visitados, abiertos):
        """
        Crea y devuelve un Nodo hijo solo si 'estado_hijo' es válido (no repetido).
        Se filtra si el estado ya se ha visitado o si ya está en la frontera (abiertos).
        """
        if estado_hijo in visitados or estado_hijo in abiertos:
            # No se crea hijo para evitar ciclos y duplicados en la frontera
            return None
        
        # Nodo hijo válido: se referencia al padre y se registra qué operación lo generó
        return Nodo(estado=estado_hijo, padre=padre, operacion=operacion)

    def aplicar_operaciones(self, estado):
        hijos = {}
        lado = estado[2]  # 1 si el barco está a la izquierda, 0 si está a la derecha
        signo = -1 if lado == 1 else 1  # cambia el sentido del movimiento

        

        for operacion, movimientos in self.operaciones.items():
            misioneros, canibales = movimientos
            nuevo = list(estado)
            nuevo[0] += signo * misioneros  # misioneros izq
            nuevo[1] += signo * canibales  # caníbales izq
            nuevo[3] -= signo * misioneros  # misioneros der
            nuevo[4] -= signo * canibales  # caníbales der
            nuevo[2] = 1 - estado[2]  # cambia el lado del barco

            if self.es_valido(nuevo):
                hijos[operacion] = tuple(nuevo) #importante convertir a tupla por que si marcara error al comparar con el objeto set en la funcion crear_hijo

        return hijos
            
    def es_valido(self, estado):
        # Desempaquetamos los elementos del estado para mayor claridad
        misioneros_izq, canibales_izq, barco_izq, misioneros_der, canibales_der = estado

        # Verificar que los valores estén dentro del rango permitido (0 a 3)
        for valor in estado:
            if valor < 0 or valor > 3:
                return False

        # Regla 1: En la orilla izquierda no pueden haber más caníbales que misioneros
        if misioneros_izq > 0 and canibales_izq > misioneros_izq:
            return False

        # Regla 2: En la orilla derecha tampoco pueden haber más caníbales que misioneros
        if misioneros_der > 0 and canibales_der > misioneros_der:
            return False

        # Si pasa todas las verificaciones, el estado es válido
        return True
    


# ----------------------------
# Motor de búsqueda genérico (BFS o DFS según la frontera)
# ----------------------------
class Buscador:
    def __init__(self, metodo, estado_final, estado_inicial):
        self.frontera = Frontera(metodo)   # Estructura que define el orden de expansión
        self.visitados = set()             # Estados ya extraídos/expandidos
        # Conjunto de estados actualmente en la frontera (para consulta O(1)
        # y evitar list comprehensions costosas)
        self.abiertos = set()
        self.estado_final = estado_final   # Estado objetivo/meta
        self.nodo_raiz = Nodo(estado=estado_inicial, padre=None, operacion=None)
        
    def buscar(self, generador):
        # Inicializa frontera con la raíz y la marca como abierta
        self.frontera.agregar(self.nodo_raiz)
        self.abiertos.add(self.nodo_raiz.estado)

        # Bucle principal de búsqueda
        while not self.frontera.es_vacia():
            # Toma siguiente nodo según la política (BFS/DFS)
            nodo = self.frontera.extraer()
            # Ya no está en abiertos porque lo vamos a procesar
            self.abiertos.discard(nodo.estado)

            # Si el estado ya fue procesado antes, lo saltamos
            if nodo.estado in self.visitados:
                continue
            
            # Marcamos el estado como visitado (expandido)
            self.visitados.add(nodo.estado)

            # ¿Alcanzamos la meta?
            if self.es_meta(nodo.estado):
                # Devuelve el nodo meta; el camino se reconstruye con punteros padre/operación
                return nodo

            # Expandimos el nodo actual y añadimos hijos válidos a la frontera
            hijos = generador.expandir(nodo, self.visitados, self.abiertos)
            for hijo in hijos:
                if hijo:
                    self.frontera.agregar(hijo)
                    self.abiertos.add(hijo.estado)

        # Si se vacía la frontera: no hay solución alcanzable
        return None
    
    def es_meta(self, estado):
        # Criterio de meta: igualdad exacta con el estado objetivo
        return estado == self.estado_final

class Buscador_recursivo:
    def __init__(self, estado_final, estado_inicial):
        self.estado_final = estado_final
        self.nodo_raiz = Nodo(estado=estado_inicial)
        self.visitados = set()

    def buscar(self, generador):
        return self.dfs_recursivo(self.nodo_raiz, generador)

    def dfs_recursivo(self, nodo, generador):
        self.visitados.add(nodo.estado)
        
        if self.es_meta(nodo.estado):
            return nodo

        hijos = generador.expandir(nodo, self.visitados, set())  # abiertos no necesarios, mando un set vacio para no modificar el codigo de esa funcion
        for hijo in hijos:
            if hijo and hijo.estado not in self.visitados:
                res = self.dfs_recursivo(hijo, generador)
                if res is not None:
                    return res
        return None

    def es_meta(self, estado):
        return estado == self.estado_final

# ----------------------------
# Reconstrucción del camino solución desde el nodo meta
# ----------------------------
def reconstruir_camino(nodo_meta):
    """
    Devuelve (estados, operaciones), donde:
      - estados: [estado0, estado1, ..., estadoN]
      - operaciones: [op1, op2, ..., opN] aplicadas para ir de estado0 a estadoN

    Recorre hacia atrás usando los punteros 'padre' y luego invierte para
    obtener el orden desde la raíz a la meta.
    """
    estados = []
    operaciones = []
    n = nodo_meta
    while n is not None:
        estados.append(n.estado)        # Agrega el estado del nodo actual
        operaciones.append(n.operacion) # Operación que generó este nodo desde su padre
        n = n.padre                     # Retrocede al padre

    # Invertimos para quedar en orden cronológico (desde raíz hasta meta)
    estados.reverse()
    operaciones.reverse()

    return estados, operaciones

def construir_mensaje_solucion(camino_solucion, tiempo_ejecucion):
    if METODO == 'BFS':
        msj_metodo = 'Busqueda en Amplitud Finalizada\n'
    elif METODO == 'DFS':
        msj_metodo = 'Busqueda en Profundidad Finalizada\n'
    else:
        msj_metodo = 'Busqueda en Profundidad Recursivo Finalizada\n'
    
    msj_nodos = f'Se generaron {len(camino_solucion)} nodos\n'
    msj_tiempo = f'El tiempo de ejecucion de {METODO} fue de {tiempo_ejecucion}\n'
    msj_solucion = 'La solucion es:\n'
    camino_solucion = '->'.join([str(x) for x in camino_solucion])

    return msj_metodo + msj_nodos + msj_tiempo + msj_solucion + camino_solucion

estado_inicial = (3, 3, 1, 0, 0)
estado_final = (0, 0, 0, 3, 3)
generador = GeneraEstados()
METODO = 'BFS' # BFS, DFS, DFS_recursivo

if METODO == 'BFS' or METODO == 'DFS':
    buscador = Buscador(METODO, estado_final, estado_inicial)
else:
    buscador = Buscador_recursivo(estado_final, estado_inicial)

tiempo_inicial = time.time()
solucion = buscador.buscar(generador)
tiempo_final = time.time()
tiempo_ejecucion = round((tiempo_final - tiempo_inicial)/60,5)
camino_solucion = reconstruir_camino(solucion)
mensaje_resultado = construir_mensaje_solucion(camino_solucion[0], tiempo_ejecucion)
        
