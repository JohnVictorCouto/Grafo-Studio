package algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import model.Aresta;
import model.Vertice;
import model.Grafo;

public final class Dijkstra {
    private Dijkstra() {
    }

    public static Map<Vertice, Double> distancias(Grafo grafo, Vertice origem) {
        Contexto dados = criarContexto(grafo);
        Resultado resultado = calcular(dados, origem);
        Map<Vertice, Double> distancias = new LinkedHashMap<>();
        for (int i = 0; i < dados.vertices.size(); i++) {
            distancias.put(dados.vertices.get(i), resultado.distancias[i]);
        }
        return Collections.unmodifiableMap(distancias);
    }

    public static Optional<List<Vertice>> menorCaminho(
            Grafo grafo, Vertice origem, Vertice destino) {
        Contexto dados = criarContexto(grafo);
        int indiceDestino = dados.indiceDe(destino);
        Resultado resultado = calcular(dados, origem);
        if (Double.isInfinite(resultado.distancias[indiceDestino])) {
            return Optional.empty();
        }

        List<Vertice> caminhoReverso = new ArrayList<>();
        for (int atual = indiceDestino; atual != -1; atual = resultado.pais[atual]) {
            caminhoReverso.add(dados.vertices.get(atual));
        }
        Collections.reverse(caminhoReverso);
        return Optional.of(Collections.unmodifiableList(caminhoReverso));
    }

    private static Resultado calcular(Contexto dados, Vertice origem) {
        int n = dados.vertices.size();
        int indiceOrigem = dados.indiceDe(origem);
        double[] distancias = new double[n];
        int[] pais = new int[n];
        boolean[] finalizados = new boolean[n];
        for (int i = 0; i < n; i++) {
            distancias[i] = Double.POSITIVE_INFINITY;
            pais[i] = -1;
        }
        for (Aresta aresta : dados.arestasOriginais) {
            if (aresta.peso < 0) {
                throw new IllegalArgumentException("Dijkstra nao aceita arestas com peso negativo.");
            }
        }
        distancias[indiceOrigem] = 0.0;
        PriorityQueue<NoDistancia> fila = new PriorityQueue<>();
        fila.add(new NoDistancia(indiceOrigem, 0.0));

        while (!fila.isEmpty()) {
            NoDistancia proximo = fila.remove();
            int atual = proximo.indice;
            if (finalizados[atual] || Double.compare(proximo.distancia, distancias[atual]) != 0) {
                continue;
            }
            finalizados[atual] = true;
            for (Aresta aresta : dados.adjacencias.get(atual)) {
                Integer vizinho = dados.indices.get(aresta.destino);
                if (finalizados[vizinho]) {
                    continue;
                }
                double alternativa = distancias[atual] + aresta.peso;
                if (alternativa < distancias[vizinho]) {
                    distancias[vizinho] = alternativa;
                    pais[vizinho] = atual;
                    fila.add(new NoDistancia(vizinho, alternativa));
                }
            }
        }
        return new Resultado(distancias, pais);
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
        List<List<Aresta>> adjacencias = new ArrayList<>();
        for (int i = 0; i < vertices.size(); i++) {
            adjacencias.add(new ArrayList<Aresta>());
        }
        List<Aresta> arestasOriginais = grafo.getArestas();
        for (Aresta aresta : arestasOriginais) {
            if (!Double.isFinite(aresta.peso)) {
                throw new IllegalArgumentException("Os pesos das arestas devem ser finitos.");
            }
            Integer u = indices.get(aresta.origem);
            Integer v = indices.get(aresta.destino);
            if (u == null || v == null) {
                throw new IllegalArgumentException("Uma aresta referencia um vertice fora do grafo.");
            }
            if (!u.equals(v)) {
                adjacencias.get(u).add(aresta);
                adjacencias.get(v).add(new Aresta(
                        aresta.rotulo, aresta.peso, aresta.destino, aresta.origem));
            }
        }
        return new Contexto(vertices, indices, adjacencias, arestasOriginais);
    }

    private static final class Contexto {
        private final List<Vertice> vertices;
        private final Map<Vertice, Integer> indices;
        private final List<List<Aresta>> adjacencias;
        private final List<Aresta> arestasOriginais;

        private Contexto(List<Vertice> vertices, Map<Vertice, Integer> indices,
                List<List<Aresta>> adjacencias, List<Aresta> arestasOriginais) {
            this.vertices = vertices;
            this.indices = indices;
            this.adjacencias = adjacencias;
            this.arestasOriginais = arestasOriginais;
        }

        private int indiceDe(Vertice vertice) {
            Integer indice = indices.get(vertice);
            if (indice == null) {
                throw new IllegalArgumentException("O vertice nao pertence ao grafo.");
            }
            return indice;
        }
    }

    private static final class Resultado {
        private final double[] distancias;
        private final int[] pais;

        private Resultado(double[] distancias, int[] pais) {
            this.distancias = distancias;
            this.pais = pais;
        }
    }

    private static final class NoDistancia implements Comparable<NoDistancia> {
        private final int indice;
        private final double distancia;

        private NoDistancia(int indice, double distancia) {
            this.indice = indice;
            this.distancia = distancia;
        }

        @Override
        public int compareTo(NoDistancia outro) {
            return Double.compare(distancia, outro.distancia);
        }
    }
}
