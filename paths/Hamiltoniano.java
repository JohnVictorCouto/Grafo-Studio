package paths;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import model.Aresta;
import model.Grafo;
import model.Vertice;

public final class Hamiltoniano {
    private Hamiltoniano() {
    }

    public static boolean temCaminhoHamiltoniano(Grafo grafo) {
        return encontrarCaminhoHamiltoniano(grafo).isPresent();
    }

    public static boolean ehHamiltoniano(Grafo grafo) {
        return encontrarCicloHamiltoniano(grafo).isPresent();
    }

    public static Optional<List<Vertice>> encontrarCaminhoHamiltoniano(Grafo grafo) {
        Contexto contexto = criarContexto(grafo);
        int n = contexto.vertices.size();
        if (n == 0) {
            return Optional.empty();
        }
        if (n == 1) {
            return Optional.of(Collections.singletonList(contexto.vertices.get(0)));
        }
        if (!ehConexo(contexto.adjacencias) || temVerticeIsolado(contexto.graus)) {
            return Optional.empty();
        }

        Integer[] inicios = new Integer[n];
        for (int i = 0; i < n; i++) {
            inicios[i] = i;
        }
        Arrays.sort(inicios, (a, b) -> Integer.compare(contexto.graus[a], contexto.graus[b]));

        boolean[] visitados = new boolean[n];
        List<Integer> caminho = new ArrayList<>();
        for (int inicio : inicios) {
            visitados[inicio] = true;
            caminho.add(inicio);
            if (buscarCaminho(contexto.adjacencias, inicio, visitados, caminho)) {
                return Optional.of(criarPercurso(contexto.vertices, caminho));
            }
            caminho.remove(caminho.size() - 1);
            visitados[inicio] = false;
        }
        return Optional.empty();
    }

    public static Optional<List<Vertice>> encontrarCicloHamiltoniano(Grafo grafo) {
        Contexto contexto = criarContexto(grafo);
        int n = contexto.vertices.size();
        if (n < 3 || !ehConexo(contexto.adjacencias)) {
            return Optional.empty();
        }
        for (int grau : contexto.graus) {
            if (grau < 2) {
                return Optional.empty();
            }
        }

        boolean[] visitados = new boolean[n];
        List<Integer> ciclo = new ArrayList<>();
        visitados[0] = true;
        ciclo.add(0);
        if (!buscarCiclo(contexto.adjacencias, 0, 0, visitados, ciclo)) {
            return Optional.empty();
        }
        ciclo.add(0);
        return Optional.of(criarPercurso(contexto.vertices, ciclo));
    }

    public static boolean satisfazTeoremaDeDirac(Grafo grafo) {
        Contexto contexto = criarContexto(grafo);
        int n = contexto.vertices.size();
        if (n < 3 || !grafo.ehGrafoSimples()) {
            return false;
        }
        for (int grau : contexto.graus) {
            if (2 * grau < n) {
                return false;
            }
        }
        return true;
    }

    public static boolean satisfazTeoremaDeOre(Grafo grafo) {
        Contexto contexto = criarContexto(grafo);
        int n = contexto.vertices.size();
        if (n < 3 || !grafo.ehGrafoSimples()) {
            return false;
        }
        for (int u = 0; u < n; u++) {
            for (int v = u + 1; v < n; v++) {
                if (!contexto.adjacencias[u][v]
                        && contexto.graus[u] + contexto.graus[v] < n) {
                    return false;
                }
            }
        }
        return true;
    }

    public static boolean fechamentoEhCompleto(Grafo grafo) {
        Contexto contexto = criarContexto(grafo);
        int n = contexto.vertices.size();
        if (n < 3 || !grafo.ehGrafoSimples()) {
            return false;
        }
        boolean[][] fechamento = copiar(contexto.adjacencias);
        int[] graus = contexto.graus.clone();
        boolean alterado;
        do {
            alterado = false;
            for (int u = 0; u < n && !alterado; u++) {
                for (int v = u + 1; v < n; v++) {
                    if (!fechamento[u][v] && graus[u] + graus[v] >= n) {
                        fechamento[u][v] = true;
                        fechamento[v][u] = true;
                        graus[u]++;
                        graus[v]++;
                        alterado = true;
                        break;
                    }
                }
            }
        } while (alterado);

        for (int u = 0; u < n; u++) {
            for (int v = u + 1; v < n; v++) {
                if (!fechamento[u][v]) {
                    return false;
                }
            }
        }
        return true;
    }

    public static boolean ehNaoHamiltonianoMaximal(Grafo grafo) {
        Contexto contexto = criarContexto(grafo);
        int n = contexto.vertices.size();
        if (n < 3 || !grafo.ehGrafoSimples()
                || !ehConexo(contexto.adjacencias)
                || possuiCicloHamiltoniano(contexto.adjacencias)) {
            return false;
        }

        for (int u = 0; u < n; u++) {
            for (int v = u + 1; v < n; v++) {
                if (!contexto.adjacencias[u][v]) {
                    boolean[][] supergrafo = copiar(contexto.adjacencias);
                    supergrafo[u][v] = true;
                    supergrafo[v][u] = true;
                    if (!possuiCicloHamiltoniano(supergrafo)) {
                        return false;
                    }
                }
            }
        }
        return true;
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

        boolean[][] adjacencias = new boolean[vertices.size()][vertices.size()];
        for (Aresta aresta : grafo.getArestas()) {
            Integer origem = indices.get(aresta.origem);
            Integer destino = indices.get(aresta.destino);
            if (origem != null && destino != null && !origem.equals(destino)) {
                adjacencias[origem][destino] = true;
                adjacencias[destino][origem] = true;
            }
        }

        int[] graus = new int[vertices.size()];
        for (int u = 0; u < vertices.size(); u++) {
            for (int v = 0; v < vertices.size(); v++) {
                if (adjacencias[u][v]) {
                    graus[u]++;
                }
            }
        }
        return new Contexto(vertices, adjacencias, graus);
    }

    private static boolean buscarCaminho(boolean[][] adjacencias, int atual,
            boolean[] visitados, List<Integer> caminho) {
        if (caminho.size() == visitados.length) {
            return true;
        }
        for (int proximo : vizinhosPorGrau(adjacencias, atual, visitados)) {
            visitados[proximo] = true;
            caminho.add(proximo);
            if (buscarCaminho(adjacencias, proximo, visitados, caminho)) {
                return true;
            }
            caminho.remove(caminho.size() - 1);
            visitados[proximo] = false;
        }
        return false;
    }

    private static boolean buscarCiclo(boolean[][] adjacencias, int atual, int inicio,
            boolean[] visitados, List<Integer> ciclo) {
        if (ciclo.size() == visitados.length) {
            return adjacencias[atual][inicio];
        }
        for (int proximo : vizinhosPorGrau(adjacencias, atual, visitados)) {
            visitados[proximo] = true;
            ciclo.add(proximo);
            if (buscarCiclo(adjacencias, proximo, inicio, visitados, ciclo)) {
                return true;
            }
            ciclo.remove(ciclo.size() - 1);
            visitados[proximo] = false;
        }
        return false;
    }

    private static List<Integer> vizinhosPorGrau(boolean[][] adjacencias, int atual,
            boolean[] visitados) {
        List<Integer> vizinhos = new ArrayList<>();
        for (int i = 0; i < visitados.length; i++) {
            if (adjacencias[atual][i] && !visitados[i]) {
                vizinhos.add(i);
            }
        }
        vizinhos.sort((a, b) -> {
            int grauA = contarVizinhosNaoVisitados(adjacencias, a, visitados);
            int grauB = contarVizinhosNaoVisitados(adjacencias, b, visitados);
            return Integer.compare(grauA, grauB);
        });
        return vizinhos;
    }

    private static int contarVizinhosNaoVisitados(boolean[][] adjacencias, int vertice,
            boolean[] visitados) {
        int quantidade = 0;
        for (int i = 0; i < visitados.length; i++) {
            if (adjacencias[vertice][i] && !visitados[i]) {
                quantidade++;
            }
        }
        return quantidade;
    }

    private static boolean ehConexo(boolean[][] adjacencias) {
        if (adjacencias.length == 0) {
            return false;
        }
        boolean[] visitados = new boolean[adjacencias.length];
        Deque<Integer> fila = new ArrayDeque<>();
        fila.add(0);
        visitados[0] = true;
        while (!fila.isEmpty()) {
            int atual = fila.remove();
            for (int proximo = 0; proximo < adjacencias.length; proximo++) {
                if (adjacencias[atual][proximo] && !visitados[proximo]) {
                    visitados[proximo] = true;
                    fila.add(proximo);
                }
            }
        }
        for (boolean visitado : visitados) {
            if (!visitado) {
                return false;
            }
        }
        return true;
    }

    private static boolean temVerticeIsolado(int[] graus) {
        for (int grau : graus) {
            if (grau == 0) {
                return true;
            }
        }
        return false;
    }

    private static boolean possuiCicloHamiltoniano(boolean[][] adjacencias) {
        int n = adjacencias.length;
        if (n < 3 || !ehConexo(adjacencias)) {
            return false;
        }
        for (int u = 0; u < n; u++) {
            int grau = 0;
            for (int v = 0; v < n; v++) {
                if (adjacencias[u][v]) {
                    grau++;
                }
            }
            if (grau < 2) {
                return false;
            }
        }
        boolean[] visitados = new boolean[n];
        List<Integer> ciclo = new ArrayList<>();
        visitados[0] = true;
        ciclo.add(0);
        if (!buscarCiclo(adjacencias, 0, 0, visitados, ciclo)) {
            return false;
        }
        return true;
    }

    private static boolean[][] copiar(boolean[][] matriz) {
        boolean[][] copia = new boolean[matriz.length][];
        for (int i = 0; i < matriz.length; i++) {
            copia[i] = matriz[i].clone();
        }
        return copia;
    }

    private static List<Vertice> criarPercurso(List<Vertice> vertices, List<Integer> indices) {
        List<Vertice> percurso = new ArrayList<>();
        for (int indice : indices) {
            percurso.add(vertices.get(indice));
        }
        return Collections.unmodifiableList(percurso);
    }

    private static final class Contexto {
        private final List<Vertice> vertices;
        private final boolean[][] adjacencias;
        private final int[] graus;

        private Contexto(List<Vertice> vertices, boolean[][] adjacencias, int[] graus) {
            this.vertices = vertices;
            this.adjacencias = adjacencias;
            this.graus = graus;
        }
    }
}
