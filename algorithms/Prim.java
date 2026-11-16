package algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import model.Aresta;
import model.Grafo;
import model.Vertice;

public final class Prim {
    private Prim() {
    }

    public static Optional<List<Aresta>> encontrarArvoreGeradoraMinima(Grafo grafo) {
        Contexto dados = criarContexto(grafo);
        if (dados.vertices.isEmpty()) {
            return Optional.empty();
        }
        return encontrarArvoreGeradoraMinima(grafo, dados.vertices.get(0));
    }

    public static Optional<List<Aresta>> encontrarArvoreGeradoraMinima(
            Grafo grafo, Vertice inicio) {
        Contexto dados = criarContexto(grafo);
        int n = dados.vertices.size();
        if (n == 0) {
            return Optional.empty();
        }
        int indiceInicio = dados.indiceDe(inicio);
        boolean[] naArvore = new boolean[n];
        double[] menorPeso = new double[n];
        Aresta[] ligacao = new Aresta[n];
        for (int i = 0; i < n; i++) {
            menorPeso[i] = Double.POSITIVE_INFINITY;
        }
        menorPeso[indiceInicio] = 0.0;

        List<Aresta> resultado = new ArrayList<>();
        for (int contagem = 0; contagem < n; contagem++) {
            int atual = -1;
            for (int i = 0; i < n; i++) {
                if (!naArvore[i] && (atual == -1 || menorPeso[i] < menorPeso[atual])) {
                    atual = i;
                }
            }
            if (atual == -1 || Double.isInfinite(menorPeso[atual])) {
                return Optional.empty();
            }
            naArvore[atual] = true;
            if (ligacao[atual] != null) {
                resultado.add(ligacao[atual]);
            }
            for (int vizinho = 0; vizinho < n; vizinho++) {
                Aresta aresta = dados.arestas[atual][vizinho];
                if (!naArvore[vizinho] && aresta != null
                        && aresta.peso < menorPeso[vizinho]) {
                    menorPeso[vizinho] = aresta.peso;
                    ligacao[vizinho] = aresta;
                }
            }
        }
        return Optional.of(Collections.unmodifiableList(resultado));
    }

    private static Contexto criarContexto(Grafo grafo) {
        if (grafo == null) {
            throw new IllegalArgumentException("O grafo nao pode ser nulo.");
        }
        List<Vertice> vertices = grafo.getVertices();
        Map<Vertice, Integer> indices = new IdentityHashMap<>();
        for (int i = 0; i < vertices.size(); i++) {
            indices.put(vertices.get(i), i);
        }
        Aresta[][] arestas = new Aresta[vertices.size()][vertices.size()];
        for (Aresta aresta : grafo.getArestas()) {
            if (!Double.isFinite(aresta.peso)) {
                throw new IllegalArgumentException("Os pesos das arestas devem ser finitos.");
            }
            Integer u = indices.get(aresta.origem);
            Integer v = indices.get(aresta.destino);
            if (u == null || v == null) {
                throw new IllegalArgumentException("Uma aresta referencia um vertice fora do grafo.");
            }
            if (!u.equals(v) && (arestas[u][v] == null
                    || Double.compare(aresta.peso, arestas[u][v].peso) < 0)) {
                arestas[u][v] = aresta;
                arestas[v][u] = aresta;
            }
        }
        return new Contexto(vertices, indices, arestas);
    }

    private static final class Contexto {
        private final List<Vertice> vertices;
        private final Map<Vertice, Integer> indices;
        private final Aresta[][] arestas;

        private Contexto(List<Vertice> vertices, Map<Vertice, Integer> indices,
                Aresta[][] arestas) {
            this.vertices = vertices;
            this.indices = indices;
            this.arestas = arestas;
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
