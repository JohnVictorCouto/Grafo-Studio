package algorithms;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import model.Aresta;
import model.Grafo;
import model.Vertice;

public final class BuscaEmLargura {
    private BuscaEmLargura() {
    }

    public static List<Vertice> percorrer(Grafo grafo, Vertice inicio) {
        List<Vertice> vertices = validarGrafoEInicio(grafo, inicio);
        boolean[][] adjacencias = grafo.gerarMatrizAdjacencia();
        int indiceInicio = vertices.indexOf(inicio);
        boolean[] visitados = new boolean[vertices.size()];
        List<Vertice> percurso = new ArrayList<>();
        percorrerComFila(vertices, adjacencias, indiceInicio, visitados, percurso);
        return Collections.unmodifiableList(percurso);
    }

    public static List<Vertice> percorrer(Grafo grafo) {
        if (grafo == null) {
            throw new IllegalArgumentException("O grafo nao pode ser nulo.");
        }
        List<Vertice> vertices = grafo.getVertices();
        boolean[][] adjacencias = grafo.gerarMatrizAdjacencia();
        boolean[] visitados = new boolean[vertices.size()];
        List<Vertice> percurso = new ArrayList<>();
        for (int i = 0; i < vertices.size(); i++) {
            if (!visitados[i]) {
                percorrerComFila(vertices, adjacencias, i, visitados, percurso);
            }
        }
        return Collections.unmodifiableList(percurso);
    }

    public static Resultado calcularMenoresCaminhos(Grafo grafo, Vertice origem) {
        if (grafo == null) {
            throw new IllegalArgumentException("O grafo nao pode ser nulo.");
        }
        List<Vertice> vertices = grafo.getVertices();
        Map<Vertice, Integer> indices = new IdentityHashMap<>();
        for (int i = 0; i < vertices.size(); i++) {
            indices.put(vertices.get(i), i);
        }
        Integer indiceOrigem = indices.get(origem);
        if (indiceOrigem == null) {
            throw new IllegalArgumentException("O vertice inicial nao pertence ao grafo.");
        }

        List<List<Integer>> adjacencias = new ArrayList<>();
        for (int i = 0; i < vertices.size(); i++) {
            adjacencias.add(new ArrayList<Integer>());
        }
        for (Aresta aresta : grafo.getArestas()) {
            Integer u = indices.get(aresta.origem);
            Integer v = indices.get(aresta.destino);
            if (u == null || v == null) {
                throw new IllegalArgumentException(
                        "Uma aresta referencia um vertice fora do grafo.");
            }
            if (!u.equals(v)) {
                adjacencias.get(u).add(v);
                adjacencias.get(v).add(u);
            }
        }

        int[] distancias = new int[vertices.size()];
        int[] predecessores = new int[vertices.size()];
        for (int i = 0; i < vertices.size(); i++) {
            distancias[i] = -1;
            predecessores[i] = -1;
        }
        Queue<Integer> fila = new ArrayDeque<>();
        distancias[indiceOrigem] = 0;
        fila.add(indiceOrigem);
        while (!fila.isEmpty()) {
            int atual = fila.remove();
            for (int vizinho : adjacencias.get(atual)) {
                if (distancias[vizinho] == -1) {
                    distancias[vizinho] = distancias[atual] + 1;
                    predecessores[vizinho] = atual;
                    fila.add(vizinho);
                }
            }
        }
        return new Resultado(vertices, indices, distancias, predecessores);
    }

    private static void percorrerComFila(List<Vertice> vertices, boolean[][] adjacencias,
            int inicio, boolean[] visitados, List<Vertice> percurso) {
        Queue<Integer> fila = new ArrayDeque<>();
        visitados[inicio] = true;
        fila.add(inicio);
        while (!fila.isEmpty()) {
            int atual = fila.remove();
            percurso.add(vertices.get(atual));
            for (int vizinho = 0; vizinho < vertices.size(); vizinho++) {
                if (adjacencias[atual][vizinho] && !visitados[vizinho]) {
                    visitados[vizinho] = true;
                    fila.add(vizinho);
                }
            }
        }
    }

    private static List<Vertice> validarGrafoEInicio(Grafo grafo, Vertice inicio) {
        if (grafo == null) {
            throw new IllegalArgumentException("O grafo nao pode ser nulo.");
        }
        if (inicio == null || !grafo.getVertices().contains(inicio)) {
            throw new IllegalArgumentException("O vertice inicial nao pertence ao grafo.");
        }
        return grafo.getVertices();
    }

    public static final class Resultado {
        private final List<Vertice> vertices;
        private final Map<Vertice, Integer> indices;
        private final int[] distancias;
        private final int[] predecessores;

        private Resultado(List<Vertice> vertices, Map<Vertice, Integer> indices,
                int[] distancias, int[] predecessores) {
            this.vertices = vertices;
            this.indices = indices;
            this.distancias = distancias;
            this.predecessores = predecessores;
        }

        public int distanciaAte(Vertice destino) {
            return distancias[indiceDe(destino)];
        }

        public Optional<List<Vertice>> caminhoAte(Vertice destino) {
            int indice = indiceDe(destino);
            if (distancias[indice] == -1) {
                return Optional.empty();
            }
            List<Vertice> caminho = new ArrayList<>();
            for (int atual = indice; atual != -1; atual = predecessores[atual]) {
                caminho.add(vertices.get(atual));
            }
            Collections.reverse(caminho);
            return Optional.of(Collections.unmodifiableList(caminho));
        }

        public Map<Vertice, Integer> distancias() {
            Map<Vertice, Integer> resultado = new LinkedHashMap<>();
            for (int i = 0; i < vertices.size(); i++) {
                resultado.put(vertices.get(i), distancias[i]);
            }
            return Collections.unmodifiableMap(resultado);
        }

        public Map<Vertice, Vertice> predecessores() {
            Map<Vertice, Vertice> resultado = new LinkedHashMap<>();
            for (int i = 0; i < vertices.size(); i++) {
                int indicePai = predecessores[i];
                resultado.put(vertices.get(i),
                        indicePai == -1 ? null : vertices.get(indicePai));
            }
            return Collections.unmodifiableMap(resultado);
        }

        private int indiceDe(Vertice vertice) {
            Integer indice = indices.get(vertice);
            if (indice == null) {
                throw new IllegalArgumentException("O vertice nao pertence ao grafo.");
            }
            return indice;
        }
    }
}
