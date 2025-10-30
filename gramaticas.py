# -*- coding: utf-8 -*-
"""
Created on Wed Oct 22 20:55:26 2025

@author: Efrain Santos Luna
@mail: efrain.santos.consultor@gmail.com
@phone:55-66-18-62-95
"""

# ============================================
# Clase que representa un nodo del árbol de derivación
# ============================================
class Nodo:
    def __init__(self, estado_str, padre=None, no_regla=None):
        self.estado_str = estado_str          # Cadena actual (forma sentencial)
        self.padre = padre                    # Referencia al nodo padre
        self.no_regla = no_regla              # Número de la regla aplicada para llegar a este nodo
        self.hijos = []                       # Lista de nodos hijos generados desde este nodo


# ============================================
# Clase que gestiona la frontera de búsqueda (lista de nodos a explorar)
# ============================================
class Frontera:
    def __init__(self, metodo):
        self.metodo = metodo                  # Método de expansión: 'izquierda' o 'derecha'
        self.nodos_frontera = []              # Lista que almacena los nodos por explorar

    def agregar(self, nodos):
        # Agrega nuevos nodos al principio de la lista (LIFO), para búsqueda en profundidad (DFS)
        self.nodos_frontera = nodos + self.nodos_frontera

    def extraer(self):
        # Extrae el siguiente nodo a procesar
        if self.metodo == 'izquierda':
            return self.nodos_frontera.pop(0)   # Saca el primero (expansión por la izquierda)
        else:
            return self.nodos_frontera.pop()    # Saca el último (expansión por la derecha)

    def es_vacia(self):
        # Retorna True si la frontera está vacía
        return len(self.nodos_frontera) == 0


# ============================================
# Clase que genera nuevos estados a partir de un nodo (expansión de reglas)
# ============================================
class GeneraEstados:
    def __init__(self, reglas_produccion):
        self.reglas = reglas_produccion         # Diccionario con las reglas de producción de la gramática

    def expandir(self, padre, metodo):
        # Expande un nodo aplicando todas las posibles reglas según la cabeza encontrada en la cadena
        cadena = padre.estado_str
        ocurrencias = []                        # Lista de ocurrencias de no terminales en la cadena

        # Buscar todas las posiciones en la cadena actual donde se puede aplicar una regla 
        for cabeza in self.reglas.keys():
            print(cabeza)
            print(cadena)
            start = 0
            while True:
                i = cadena.find(cabeza, start)   # Buscar posición de la cabeza
                if i == -1:
                    break #termino de recorrer la cadena
                ocurrencias.append((i, cabeza))  # Guardar posición y no terminal encontrado
                start = i + 1

        # Si no hay ocurrencias, la cadena es completamente terminal (no se puede expandir más)
        if not ocurrencias:
            return []

        # Elegir una ocurrencia según el método: izquierda o derecha
        if metodo == 'izquierda':
            i, cabeza = min(ocurrencias, key=lambda t: t[0])  # Más a la izquierda sobre el indice
        else:  # 'derecha'
            i, cabeza = max(ocurrencias, key=lambda t: t[0])  # Más a la derecha sobre el indice

        hijos = []  # Lista de nodos hijos que se generarán

        # Aplicar todas las reglas posibles para esa cabeza encontrada
        for no_regla, cola in self.reglas[cabeza]:
            # Crear una nueva cadena reemplazando la cabeza por la cola de la producción
            nueva = cadena[:i] + cola + cadena[i+len(cabeza):]
            # Crear un nuevo nodo con la cadena generada
            hijo = Nodo(estado_str=nueva, padre=padre, no_regla=no_regla)
            hijos.append(hijo)

        padre.hijos = hijos  # Guardar los hijos en el nodo padre
        return hijos          # Retornar la lista de hijos generados


# ============================================
# Clase principal que realiza la búsqueda de derivaciones
# ============================================
class Buscador:
    def __init__(self, metodo, estado_inicial, estado_final):
        self.metodo = metodo                 # Método de expansión ('izquierda' o 'derecha')
        self.frontera = Frontera(metodo)     # Inicializa la frontera
        self.estado_final = estado_final     # Cadena terminal objetivo
        self.raiz = Nodo(estado_str=estado_inicial)  # Nodo raíz con el símbolo inicial
        self.MAX_N = 25                      # Límite máximo de profundidad
        self.visitados = set()               # Conjunto de cadenas ya visitadas (evita bucles)
        self.soluciones = []                 # Lista de nodos que alcanzan el estado final
        
    def buscar(self, generador):
        # Agregar el nodo raíz a la frontera
        self.frontera.agregar([self.raiz])

        # Bucle principal: mientras haya nodos en la frontera
        while not self.frontera.es_vacia():
            nodo = self.frontera.extraer()   # Tomar el siguiente nodo

            # Si la cadena del nodo coincide con el estado final, guardar como solución
            if nodo.estado_str == self.estado_final:
                self.soluciones.append(nodo)

            # Si ya se visitó esta cadena, continuar con el siguiente
            if nodo.estado_str in self.visitados:
                continue
            
            # Marcar esta cadena como visitada
            self.visitados.add(nodo.estado_str)

            # Si la profundidad supera el máximo permitido, no seguir expandiendo
            if self.profundidad(nodo) > self.MAX_N:
                continue

            # Expandir el nodo aplicando las reglas gramaticales
            hijos = generador.expandir(nodo, self.metodo)
            if hijos:
                # Agregar los hijos a la frontera para seguir buscando
                self.frontera.agregar(hijos)
                # Actualizar el número de regla aplicada en el nodo padre (solo para mostrar)
                nodo.no_regla = hijos[0].no_regla
                
        # Retornar todas las soluciones encontradas
        return self.soluciones

    def profundidad(self, nodo):
        # Calcula la profundidad de un nodo contando cuántos padres tiene
        d = 0
        while nodo and nodo.padre:
            d += 1
            nodo = nodo.padre
        return d


# ============================================
# Funciones auxiliares para reconstruir los caminos de derivación
# ============================================
def reconstruir_todos_los_caminos(soluciones):
    # Reconstruye el camino de derivación para cada solución encontrada
    caminos = []
    for solucion in soluciones:
        camino = reconstruir_camino(solucion)
        caminos.append(camino)
    return caminos


def reconstruir_camino(nodo):
    # Reconstruye el camino desde la raíz hasta el nodo final
    cabezas = []   # Guarda las formas sentenciales
    reglas = []    # Guarda las reglas aplicadas
    while nodo is not None:
        cabezas.append(nodo.estado_str)
        reglas.append(nodo.no_regla)
        nodo = nodo.padre
    
    # Invertir las listas para que queden de la raíz al final
    cabezas.reverse()
    reglas.reverse()
    reglas = reglas[:-1]       # Quitar la última regla (no se aplica en el estado final)
    reglas.append(None)
    
    # Construir una cadena representando la secuencia de derivación
    camino = '->'
    for cabeza, regla in zip(cabezas, reglas):
        if not regla:
            camino += f'({cabeza})'                 # Estado final
        else:
            camino += f'({cabeza},{regla})->'      # Estado intermedio con número de regla
    return camino


# ============================================
# Definición de las reglas de producción y prueba del sistema
# ============================================

# Diccionario de reglas: cada no terminal se asocia a una lista de tuplas (número_regla, producción)
reglas_produccion = {
    'S': [(1, 'ABC')],
    'E': [(2,'b')],
    'aaA':[(3,'aaBB')],
    'B':[(4,'d')],
    'A':[(5, 'aE')],
    'C':[(6,'dcd')]
}

# Cadena terminal que queremos obtener a partir del símbolo inicial
estado_final = 'abddcd'

# Ejemplo alternativo de gramática (comentado)
# reglas_produccion = {
#     'S': [(1, 'AA')],
#     'A': [(2, 'aSa'), (3, 'a')],
# }
# estado_final = 'aaaaa'

# Símbolo inicial y método de derivación
estado_inicial = 'S'
METODO = 'derecha'        # Puede ser 'izquierda' o 'derecha'

# Crear el generador de estados y el buscador
generador = GeneraEstados(reglas_produccion)
buscador = Buscador(METODO, estado_inicial, estado_final)

# Buscar todas las posibles derivaciones
soluciones = buscador.buscar(generador)

# Reconstruir los caminos completos para todas las soluciones
todos_los_caminos = reconstruir_todos_los_caminos(soluciones)

# ============================================
# Mostrar los resultados
# ============================================

# Imprimir todos los caminos de derivación encontrados
for solucion in todos_los_caminos:
    print(solucion)

# Determinar si la gramática es ambigua (más de un camino válido)
if len(todos_los_caminos) > 1:
    print(f'La Gramatica ingresada es ambigua para la cadena: {estado_final}')
else:
    print(f'La Gramatica ingresada no es ambigua para la cadena: {estado_final}')
