package paths;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import model.Aresta;
import model.Grafo;
import model.Vertice;

public final class Euleriano {
    private Euleriano() {
    }

    public static boolean temTrilhaEuleriana(Grafo grafo) {
        return encontrarTrilhaEuleriana(grafo).isPresent();
    }

    public static boolean ehEuleriano(Grafo grafo) {
        return encontrarCircuitoEuleriano(grafo).isPresent();
    }

    public static Optional<List<Vertice>> encontrarTrilhaEuleriana(Grafo grafo) {
        return encontrarTrilha(grafo, false);
    }

    public static Optional<List<Vertice>> encontrarCircuitoEuleriano(Grafo grafo) {
        return encontrarTrilha(grafo, true);
    }

    private static Optional<List<Vertice>> encontrarTrilha(Grafo grafo, boolean circuito) {
        if (grafo == null) {
            throw new IllegalArgumentException("O grafo nao pode ser nulo.");
        }

        List<Vertice> vertices = grafo.getVertices();
        List<Aresta> arestas = grafo.getArestas();
        if (arestas.isEmpty()) {
            if (vertices.size() != 1) {
                return Optional.empty();
            }
            return Optional.of(Collections.singletonList(vertices.get(0)));
        }

        Map<Vertice, Integer> indices = new IdentityHashMap<>();
        for (int i = 0; i < vertices.size(); i++) {
            indices.put(vertices.get(i), i);
        }

        List<List<Integer>> incidentes = new ArrayList<>();
        for (int i = 0; i < vertices.size(); i++) {
            incidentes.add(new ArrayList<Integer>());
        }
        int[] graus = new int[vertices.size()];
        for (int i = 0; i < arestas.size(); i++) {
            Aresta aresta = arestas.get(i);
            Integer origem = indices.get(aresta.origem);
            Integer destino = indices.get(aresta.destino);
            if (origem == null || destino == null) {
                return Optional.empty();
            }
            incidentes.get(origem).add(i);
            incidentes.get(destino).add(i);
            graus[origem]++;
            graus[destino]++;
        }

        List<Integer> impares = new ArrayList<>();
        int inicioConexo = -1;
        for (int i = 0; i < graus.length; i++) {
            if (graus[i] > 0 && inicioConexo == -1) {
                inicioConexo = i;
            }
            if (graus[i] % 2 != 0) {
                impares.add(i);
            }
        }
        if (circuito ? !impares.isEmpty() : impares.size() != 0 && impares.size() != 2) {
            return Optional.empty();
        }

        boolean[] visitados = new boolean[vertices.size()];
        Deque<Integer> fila = new ArrayDeque<>();
        fila.add(inicioConexo);
        visitados[inicioConexo] = true;
        while (!fila.isEmpty()) {
            int atual = fila.remove();
            for (int arestaId : incidentes.get(atual)) {
                Aresta aresta = arestas.get(arestaId);
                int vizinho = indices.get(aresta.origem) == atual
                        ? indices.get(aresta.destino)
                        : indices.get(aresta.origem);
                if (!visitados[vizinho]) {
                    visitados[vizinho] = true;
                    fila.add(vizinho);
                }
            }
        }
        for (int i = 0; i < graus.length; i++) {
            if (!visitados[i]) {
                return Optional.empty();
            }
        }

        int inicio = impares.size() == 2 ? impares.get(0) : inicioConexo;
        int[] proximoIncidente = new int[vertices.size()];
        boolean[] usada = new boolean[arestas.size()];
        Deque<Integer> pilha = new ArrayDeque<>();
        List<Integer> percursoReverso = new ArrayList<>();
        pilha.push(inicio);
        while (!pilha.isEmpty()) {
            int atual = pilha.peek();
            List<Integer> adjacentes = incidentes.get(atual);
            while (proximoIncidente[atual] < adjacentes.size()
                    && usada[adjacentes.get(proximoIncidente[atual])]) {
                proximoIncidente[atual]++;
            }
            if (proximoIncidente[atual] == adjacentes.size()) {
                percursoReverso.add(pilha.pop());
            } else {
                int arestaId = adjacentes.get(proximoIncidente[atual]++);
                if (usada[arestaId]) {
                    continue;
                }
                usada[arestaId] = true;
                Aresta aresta = arestas.get(arestaId);
                int vizinho = indices.get(aresta.origem) == atual
                        ? indices.get(aresta.destino)
                        : indices.get(aresta.origem);
                pilha.push(vizinho);
            }
        }

        if (percursoReverso.size() != arestas.size() + 1) {
            return Optional.empty();
        }
        Collections.reverse(percursoReverso);
        List<Vertice> percurso = new ArrayList<>();
        for (int indice : percursoReverso) {
            percurso.add(vertices.get(indice));
        }
        return Optional.of(Collections.unmodifiableList(percurso));
    }
}
