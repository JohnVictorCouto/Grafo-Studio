package paths;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import model.Aresta;
import model.Grafo;
import model.Vertice;

public final class Emparelhamento {
    private Emparelhamento() {
    }

    public static List<Aresta> encontrarEmparelhamentoMaximo(Grafo grafo) {
        Contexto contexto = criarContexto(grafo);
        BuscaMaxima busca = new BuscaMaxima(contexto.adjacencias);
        int[] pares = busca.resolver();
        List<Aresta> emparelhamento = new ArrayList<>();

        for (int u = 0; u < pares.length; u++) {
            if (pares[u] > u) {
                for (Aresta aresta : grafo.getArestas()) {
                    if ((aresta.origem == contexto.vertices.get(u)
                            && aresta.destino == contexto.vertices.get(pares[u]))
                            || (aresta.destino == contexto.vertices.get(u)
                            && aresta.origem == contexto.vertices.get(pares[u]))) {
                        emparelhamento.add(aresta);
                        break;
                    }
                }
            }
        }
        return Collections.unmodifiableList(emparelhamento);
    }

    public static List<Aresta> encontrarEmparelhamentoMaximal(Grafo grafo) {
        Contexto contexto = criarContexto(grafo);
        boolean[] ocupado = new boolean[contexto.vertices.size()];
        List<Aresta> emparelhamento = new ArrayList<>();

        for (Aresta aresta : grafo.getArestas()) {
            Integer u = contexto.indices.get(aresta.origem);
            Integer v = contexto.indices.get(aresta.destino);
            if (u != null && v != null && !u.equals(v) && !ocupado[u] && !ocupado[v]) {
                ocupado[u] = true;
                ocupado[v] = true;
                emparelhamento.add(aresta);
            }
        }
        return Collections.unmodifiableList(emparelhamento);
    }

    public static List<Aresta> encontrarEmparelhamentoMaximoBipartido(
            Grafo grafo, Set<Vertice> conjuntoX) {
        if (grafo == null) {
            throw new IllegalArgumentException("O grafo nao pode ser nulo.");
        }
        if (conjuntoX == null) {
            throw new IllegalArgumentException("O conjunto X nao pode ser nulo.");
        }
        List<Vertice> vertices = grafo.getVertices();
        Map<Vertice, Integer> indices = new IdentityHashMap<>();
        for (int i = 0; i < vertices.size(); i++) {
            indices.put(vertices.get(i), i);
        }
        boolean[] ladoX = new boolean[vertices.size()];
        for (Vertice vertice : conjuntoX) {
            Integer indice = indices.get(vertice);
            if (indice == null) {
                throw new IllegalArgumentException("O conjunto X contem um vertice fora do grafo.");
            }
            ladoX[indice] = true;
        }

        List<List<Integer>> adjacencias = new ArrayList<>();
        for (int i = 0; i < vertices.size(); i++) {
            adjacencias.add(new ArrayList<Integer>());
        }
        List<Aresta> arestas = grafo.getArestas();
        for (Aresta aresta : arestas) {
            Integer u = indices.get(aresta.origem);
            Integer v = indices.get(aresta.destino);
            if (u == null || v == null) {
                throw new IllegalArgumentException(
                        "Uma aresta referencia um vertice fora do grafo.");
            }
            if (ladoX[u] == ladoX[v]) {
                throw new IllegalArgumentException("X e seu complemento devem formar uma biparticao.");
            }
            if (ladoX[u]) {
                adjacencias.get(u).add(v);
            } else {
                adjacencias.get(v).add(u);
            }
        }

        int[] parceiroDe = new int[vertices.size()];
        Arrays.fill(parceiroDe, -1);

        for (int equipe = 0; equipe < ladoX.length; equipe++) {
            if (ladoX[equipe]) {
                tentarAumentar(adjacencias, equipe, new boolean[ladoX.length], parceiroDe);
            }
        }

        List<Aresta> resultado = new ArrayList<>();
        boolean[] arestaSelecionada = new boolean[parceiroDe.length];
        for (Aresta aresta : arestas) {
            int u = indices.get(aresta.origem);
            int v = indices.get(aresta.destino);
            if (ladoX[u] && parceiroDe[v] == u && !arestaSelecionada[v]) {
                resultado.add(aresta);
                arestaSelecionada[v] = true;
            } else if (ladoX[v] && parceiroDe[u] == v && !arestaSelecionada[u]) {
                resultado.add(aresta);
                arestaSelecionada[u] = true;
            }
        }
        return Collections.unmodifiableList(resultado);
    }

    public static boolean ehEmparelhamento(Grafo grafo, List<Aresta> emparelhamento) {
        Contexto contexto = criarContexto(grafo);
        if (emparelhamento == null) {
            return false;
        }
        boolean[] ocupado = new boolean[contexto.vertices.size()];
        for (Aresta aresta : emparelhamento) {
            if (aresta == null) {
                return false;
            }
            Integer u = contexto.indices.get(aresta.origem);
            Integer v = contexto.indices.get(aresta.destino);
            if (u == null || v == null || u.equals(v)
                    || !contexto.adjacencias[u][v] || ocupado[u] || ocupado[v]) {
                return false;
            }
            ocupado[u] = true;
            ocupado[v] = true;
        }
        return true;
    }

    public static List<Vertice> verticesLivres(Grafo grafo, List<Aresta> emparelhamento) {
        Contexto contexto = criarContexto(grafo);
        if (!ehEmparelhamento(grafo, emparelhamento)) {
            throw new IllegalArgumentException("A lista informada nao e um emparelhamento do grafo.");
        }
        boolean[] ocupado = verticesEmparelhados(contexto, emparelhamento);
        List<Vertice> livres = new ArrayList<>();
        for (int i = 0; i < ocupado.length; i++) {
            if (!ocupado[i]) {
                livres.add(contexto.vertices.get(i));
            }
        }
        return Collections.unmodifiableList(livres);
    }

    public static boolean ehMaximo(Grafo grafo, List<Aresta> emparelhamento) {
        return ehEmparelhamento(grafo, emparelhamento)
                && encontrarEmparelhamentoMaximo(grafo).size() == emparelhamento.size();
    }

    public static boolean existeCaminhoAumentante(Grafo grafo, List<Aresta> emparelhamento) {
        return ehEmparelhamento(grafo, emparelhamento)
                && encontrarEmparelhamentoMaximo(grafo).size() > emparelhamento.size();
    }

    // A busca exaustiva de um caminho pode ser exponencial; a busca do maximo usa Edmonds.
    public static Optional<List<Vertice>> encontrarCaminhoAumentante(
            Grafo grafo, List<Aresta> emparelhamento) {
        Contexto contexto = criarContexto(grafo);
        if (!ehEmparelhamento(grafo, emparelhamento)) {
            return Optional.empty();
        }
        boolean[][] arestasEmparelhadas = new boolean[contexto.vertices.size()][contexto.vertices.size()];
        boolean[] ocupado = verticesEmparelhados(contexto, emparelhamento);
        for (Aresta aresta : emparelhamento) {
            int u = contexto.indices.get(aresta.origem);
            int v = contexto.indices.get(aresta.destino);
            arestasEmparelhadas[u][v] = true;
            arestasEmparelhadas[v][u] = true;
        }

        for (int inicio = 0; inicio < contexto.vertices.size(); inicio++) {
            if (ocupado[inicio]) {
                continue;
            }
            boolean[] visitados = new boolean[contexto.vertices.size()];
            List<Integer> caminho = new ArrayList<>();
            visitados[inicio] = true;
            caminho.add(inicio);
            if (buscarAumentante(contexto.adjacencias, arestasEmparelhadas, ocupado,
                    inicio, false, visitados, caminho)) {
                return Optional.of(criarPercurso(contexto.vertices, caminho));
            }
        }
        return Optional.empty();
    }

    public static boolean ehMaximal(Grafo grafo, List<Aresta> emparelhamento) {
        if (!ehEmparelhamento(grafo, emparelhamento)) {
            return false;
        }
        Contexto contexto = criarContexto(grafo);
        boolean[] ocupado = verticesEmparelhados(contexto, emparelhamento);
        for (int u = 0; u < contexto.vertices.size(); u++) {
            for (int v = u + 1; v < contexto.vertices.size(); v++) {
                if (contexto.adjacencias[u][v] && !ocupado[u] && !ocupado[v]) {
                    return false;
                }
            }
        }
        return true;
    }

    public static boolean ehPerfeito(Grafo grafo, List<Aresta> emparelhamento) {
        if (!ehEmparelhamento(grafo, emparelhamento)) {
            return false;
        }
        boolean[] ocupado = verticesEmparelhados(criarContexto(grafo), emparelhamento);
        for (boolean verticeOcupado : ocupado) {
            if (!verticeOcupado) {
                return false;
            }
        }
        return true;
    }

    public static boolean temEmparelhamentoCompleto(Grafo grafo, Set<Vertice> conjuntoX) {
        Contexto contexto = criarContexto(grafo);
        boolean[] ladoX = validarParticao(contexto, conjuntoX);
        int quantidadeX = contar(ladoX);
        if (quantidadeX == 0) {
            return true;
        }
        return encontrarEmparelhamentoMaximo(grafo).size() == quantidadeX;
    }

    public static boolean satisfazCondicaoDeHall(Grafo grafo, Set<Vertice> conjuntoX) {
        Contexto contexto = criarContexto(grafo);
        boolean[] ladoX = validarParticao(contexto, conjuntoX);
        List<Integer> indicesX = new ArrayList<>();
        for (int i = 0; i < ladoX.length; i++) {
            if (ladoX[i]) {
                indicesX.add(i);
            }
        }
        return verificarHall(contexto.adjacencias, indicesX, 0, new boolean[ladoX.length], 0);
    }

    public static Set<Vertice> conjuntoVizinhanca(Grafo grafo, Set<Vertice> conjunto) {
        Contexto contexto = criarContexto(grafo);
        boolean[] selecionados = new boolean[contexto.vertices.size()];
        if (conjunto == null) {
            throw new IllegalArgumentException("O conjunto de vertices nao pode ser nulo.");
        }
        for (Vertice vertice : conjunto) {
            Integer indice = contexto.indices.get(vertice);
            if (indice == null) {
                throw new IllegalArgumentException("O conjunto contem um vertice fora do grafo.");
            }
            selecionados[indice] = true;
        }

        Set<Vertice> vizinhanca = new LinkedHashSet<>();
        for (int u = 0; u < selecionados.length; u++) {
            if (!selecionados[u]) {
                continue;
            }
            for (int v = 0; v < selecionados.length; v++) {
                if (contexto.adjacencias[u][v] && !selecionados[v]) {
                    vizinhanca.add(contexto.vertices.get(v));
                }
            }
        }
        return Collections.unmodifiableSet(vizinhanca);
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
            Integer u = indices.get(aresta.origem);
            Integer v = indices.get(aresta.destino);
            if (u != null && v != null && !u.equals(v)) {
                adjacencias[u][v] = true;
                adjacencias[v][u] = true;
            }
        }
        return new Contexto(vertices, indices, adjacencias);
    }

    private static boolean[] validarParticao(Contexto contexto, Set<Vertice> conjuntoX) {
        if (conjuntoX == null) {
            throw new IllegalArgumentException("O conjunto X nao pode ser nulo.");
        }
        boolean[] ladoX = new boolean[contexto.vertices.size()];
        for (Vertice vertice : conjuntoX) {
            Integer indice = contexto.indices.get(vertice);
            if (indice == null) {
                throw new IllegalArgumentException("O conjunto X contem um vertice fora do grafo.");
            }
            ladoX[indice] = true;
        }
        for (int u = 0; u < ladoX.length; u++) {
            for (int v = u + 1; v < ladoX.length; v++) {
                if (contexto.adjacencias[u][v] && ladoX[u] == ladoX[v]) {
                    throw new IllegalArgumentException("X e seu complemento devem formar uma biparticao.");
                }
            }
        }
        return ladoX;
    }

    private static boolean verificarHall(boolean[][] adjacencias, List<Integer> indicesX,
            int proximo, boolean[] selecionados, int tamanho) {
        int quantidadeVizinhos = 0;
        for (int v = 0; v < selecionados.length; v++) {
            boolean vizinho = false;
            for (int u = 0; u < indicesX.size(); u++) {
                if (selecionados[u] && adjacencias[indicesX.get(u)][v]) {
                    vizinho = true;
                    break;
                }
            }
            if (vizinho) {
                quantidadeVizinhos++;
            }
        }
        if (quantidadeVizinhos < tamanho) {
            return false;
        }
        if (proximo == indicesX.size()) {
            return true;
        }
        if (!verificarHall(adjacencias, indicesX, proximo + 1, selecionados, tamanho)) {
            return false;
        }
        selecionados[proximo] = true;
        boolean satisfaz = verificarHall(adjacencias, indicesX, proximo + 1,
                selecionados, tamanho + 1);
        selecionados[proximo] = false;
        return satisfaz;
    }

    private static boolean[] verticesEmparelhados(Contexto contexto, List<Aresta> emparelhamento) {
        boolean[] ocupado = new boolean[contexto.vertices.size()];
        for (Aresta aresta : emparelhamento) {
            ocupado[contexto.indices.get(aresta.origem)] = true;
            ocupado[contexto.indices.get(aresta.destino)] = true;
        }
        return ocupado;
    }

    private static int contar(boolean[] valores) {
        int quantidade = 0;
        for (boolean valor : valores) {
            if (valor) {
                quantidade++;
            }
        }
        return quantidade;
    }

    private static List<Vertice> criarPercurso(List<Vertice> vertices, List<Integer> indices) {
        List<Vertice> percurso = new ArrayList<>();
        for (int indice : indices) {
            percurso.add(vertices.get(indice));
        }
        return Collections.unmodifiableList(percurso);
    }

    private static boolean buscarAumentante(boolean[][] adjacencias, boolean[][] emparelhadas,
            boolean[] ocupado, int atual, boolean proximaEmparelhada,
            boolean[] visitados, List<Integer> caminho) {
        for (int vizinho = 0; vizinho < visitados.length; vizinho++) {
            if (!adjacencias[atual][vizinho]
                    || emparelhadas[atual][vizinho] != proximaEmparelhada
                    || visitados[vizinho]) {
                continue;
            }
            visitados[vizinho] = true;
            caminho.add(vizinho);
            if (!proximaEmparelhada && !ocupado[vizinho]) {
                return true;
            }
            if (buscarAumentante(adjacencias, emparelhadas, ocupado, vizinho,
                    !proximaEmparelhada, visitados, caminho)) {
                return true;
            }
            caminho.remove(caminho.size() - 1);
            visitados[vizinho] = false;
        }
        return false;
    }

    private static boolean tentarAumentar(List<List<Integer>> adjacencias,
            int equipe, boolean[] chamadosVisitados, int[] parceiroDe) {
        for (int chamado : adjacencias.get(equipe)) {
            if (chamadosVisitados[chamado]) {
                continue;
            }
            chamadosVisitados[chamado] = true;
            if (parceiroDe[chamado] == -1
                    || tentarAumentar(adjacencias, parceiroDe[chamado],
                            chamadosVisitados, parceiroDe)) {
                parceiroDe[chamado] = equipe;
                return true;
            }
        }
        return false;
    }

    private static final class Contexto {
        private final List<Vertice> vertices;
        private final Map<Vertice, Integer> indices;
        private final boolean[][] adjacencias;

        private Contexto(List<Vertice> vertices, Map<Vertice, Integer> indices,
                boolean[][] adjacencias) {
            this.vertices = vertices;
            this.indices = indices;
            this.adjacencias = adjacencias;
        }
    }

    private static final class BuscaMaxima {
        private final int quantidadeVertices;
        private final boolean[][] adjacencias;
        private final int[] par;
        private final int[] pai;
        private final int[] base;
        private final boolean[] usado;
        private final boolean[] flor;

        private BuscaMaxima(boolean[][] adjacencias) {
            this.quantidadeVertices = adjacencias.length;
            this.adjacencias = adjacencias;
            this.par = new int[quantidadeVertices];
            this.pai = new int[quantidadeVertices];
            this.base = new int[quantidadeVertices];
            this.usado = new boolean[quantidadeVertices];
            this.flor = new boolean[quantidadeVertices];
            Arrays.fill(par, -1);
        }

        private int[] resolver() {
            for (int raiz = 0; raiz < quantidadeVertices; raiz++) {
                if (par[raiz] == -1) {
                    encontrarAumentante(raiz);
                }
            }
            return par;
        }

        private boolean encontrarAumentante(int raiz) {
            Arrays.fill(usado, false);
            Arrays.fill(pai, -1);
            for (int i = 0; i < quantidadeVertices; i++) {
                base[i] = i;
            }

            int[] fila = new int[quantidadeVertices];
            int inicioFila = 0;
            int fimFila = 0;
            fila[fimFila++] = raiz;
            usado[raiz] = true;

            while (inicioFila < fimFila) {
                int atual = fila[inicioFila++];
                for (int vizinho = 0; vizinho < quantidadeVertices; vizinho++) {
                    if (!adjacencias[atual][vizinho] || base[atual] == base[vizinho]
                            || par[atual] == vizinho) {
                        continue;
                    }
                    if (vizinho == raiz
                            || (par[vizinho] != -1 && pai[par[vizinho]] != -1)) {
                        int baseComum = encontrarBaseComum(atual, vizinho);
                        Arrays.fill(flor, false);
                        marcarCaminho(atual, baseComum, vizinho);
                        marcarCaminho(vizinho, baseComum, atual);
                        for (int i = 0; i < quantidadeVertices; i++) {
                            if (flor[base[i]]) {
                                base[i] = baseComum;
                                if (!usado[i]) {
                                    usado[i] = true;
                                    fila[fimFila++] = i;
                                }
                            }
                        }
                    } else if (pai[vizinho] == -1) {
                        pai[vizinho] = atual;
                        if (par[vizinho] == -1) {
                            aumentar(vizinho);
                            return true;
                        }
                        int parceiro = par[vizinho];
                        usado[parceiro] = true;
                        fila[fimFila++] = parceiro;
                    }
                }
            }
            return false;
        }

        private int encontrarBaseComum(int u, int v) {
            boolean[] noCaminho = new boolean[quantidadeVertices];
            while (true) {
                u = base[u];
                noCaminho[u] = true;
                if (par[u] == -1) {
                    break;
                }
                u = pai[par[u]];
            }
            while (true) {
                v = base[v];
                if (noCaminho[v]) {
                    return v;
                }
                v = pai[par[v]];
            }
        }

        private void marcarCaminho(int vertice, int baseComum, int filho) {
            while (base[vertice] != baseComum) {
                flor[base[vertice]] = true;
                flor[base[par[vertice]]] = true;
                pai[vertice] = filho;
                filho = par[vertice];
                vertice = pai[par[vertice]];
            }
        }

        private void aumentar(int vertice) {
            while (vertice != -1) {
                int anterior = pai[vertice];
                int proximo = anterior == -1 ? -1 : par[anterior];
                par[vertice] = anterior;
                if (anterior != -1) {
                    par[anterior] = vertice;
                }
                vertice = proximo;
            }
        }
    }
}
