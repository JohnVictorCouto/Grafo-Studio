package algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import model.Grafo;
import model.Vertice;

public final class BuscaEmProfundidade {
    private BuscaEmProfundidade() {
    }

    public static List<Vertice> percorrer(Grafo grafo, Vertice inicio) {
        List<Vertice> vertices = validarGrafoEInicio(grafo, inicio);
        boolean[][] adjacencias = grafo.gerarMatrizAdjacencia();
        int indiceInicio = vertices.indexOf(inicio);
        boolean[] visitados = new boolean[vertices.size()];
        List<Vertice> percurso = new ArrayList<>();
        percorrerComPilha(vertices, adjacencias, indiceInicio, visitados, percurso);
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
                percorrerComPilha(vertices, adjacencias, i, visitados, percurso);
            }
        }
        return Collections.unmodifiableList(percurso);
    }

    private static void percorrerComPilha(List<Vertice> vertices, boolean[][] adjacencias,
            int inicio, boolean[] visitados, List<Vertice> percurso) {
        int[] proximoVizinho = new int[vertices.size()];
        List<Integer> pilha = new ArrayList<>();
        visitados[inicio] = true;
        percurso.add(vertices.get(inicio));
        pilha.add(inicio);
        while (!pilha.isEmpty()) {
            int atual = pilha.get(pilha.size() - 1);
            while (proximoVizinho[atual] < vertices.size()
                    && (!adjacencias[atual][proximoVizinho[atual]]
                    || visitados[proximoVizinho[atual]])) {
                proximoVizinho[atual]++;
            }
            if (proximoVizinho[atual] == vertices.size()) {
                pilha.remove(pilha.size() - 1);
            } else {
                int vizinho = proximoVizinho[atual]++;
                visitados[vizinho] = true;
                percurso.add(vertices.get(vizinho));
                pilha.add(vizinho);
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
}
