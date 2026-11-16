package algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import model.Aresta;
import model.Grafo;
import model.Vertice;

public final class Kruskal {
    private Kruskal() {
    }

    public static Optional<List<Aresta>> encontrarArvoreGeradoraMinima(Grafo grafo) {
        Contexto dados = criarContexto(grafo);
        int n = dados.vertices.size();
        if (n == 0) {
            return Optional.empty();
        }
        if (n == 1) {
            return Optional.of(Collections.<Aresta>emptyList());
        }

        List<Aresta> candidatas = new ArrayList<>();
        for (int u = 0; u < n; u++) {
            for (int v = u + 1; v < n; v++) {
                if (dados.arestas[u][v] != null) {
                    candidatas.add(dados.arestas[u][v]);
                }
            }
        }
        candidatas.sort(Comparator.comparingDouble(aresta -> aresta.peso));

        ConjuntosDisjuntos conjuntos = new ConjuntosDisjuntos(n);
        List<Aresta> resultado = new ArrayList<>();
        for (Aresta aresta : candidatas) {
            int u = dados.indiceDe(aresta.origem);
            int v = dados.indiceDe(aresta.destino);
            if (conjuntos.unir(u, v)) {
                resultado.add(aresta);
                if (resultado.size() == n - 1) {
                    break;
                }
            }
        }
        if (resultado.size() != n - 1) {
            return Optional.empty();
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

    private static final class ConjuntosDisjuntos {
        private final int[] pai;
        private final int[] altura;

        private ConjuntosDisjuntos(int quantidade) {
            pai = new int[quantidade];
            altura = new int[quantidade];
            for (int i = 0; i < quantidade; i++) {
                pai[i] = i;
            }
        }

        private int encontrar(int vertice) {
            if (pai[vertice] != vertice) {
                pai[vertice] = encontrar(pai[vertice]);
            }
            return pai[vertice];
        }

        private boolean unir(int u, int v) {
            int raizU = encontrar(u);
            int raizV = encontrar(v);
            if (raizU == raizV) {
                return false;
            }
            if (altura[raizU] < altura[raizV]) {
                pai[raizU] = raizV;
            } else if (altura[raizU] > altura[raizV]) {
                pai[raizV] = raizU;
            } else {
                pai[raizV] = raizU;
                altura[raizU]++;
            }
            return true;
        }
    }
}
