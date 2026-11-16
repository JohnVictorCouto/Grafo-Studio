package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public class Grafo {
    ArrayList<Vertice> listaVertices;

    public Grafo(ArrayList<Vertice> listaVertices) {
        this.listaVertices = listaVertices;
    }

    public Grafo() {
        this.listaVertices = new ArrayList<Vertice>();
    }

    public Grafo(int numVertices) {
        this.listaVertices = new ArrayList<Vertice>();
        for (int i = 0; i < numVertices; i++) {
            this.listaVertices.add(new Vertice(String.valueOf(i)));
        }
    }

    public List<Vertice> getVertices() {
        return Collections.unmodifiableList(this.listaVertices);
    }

    public List<Aresta> getArestas() {
        List<Aresta> arestas = new ArrayList<>();
        Map<Vertice, Integer> indices = new IdentityHashMap<>();
        for (int i = 0; i < this.listaVertices.size(); i++) {
            if (!indices.containsKey(this.listaVertices.get(i))) {
                indices.put(this.listaVertices.get(i), i);
            }
        }
        for (int i = 0; i < this.listaVertices.size(); i++) {
            Vertice origem = this.listaVertices.get(i);
            if (origem.listaArestas == null) {
                continue;
            }
            for (Aresta aresta : origem.listaArestas) {
                Integer destino = indices.get(aresta.destino);
                if (destino != null && destino >= i) {
                    arestas.add(aresta);
                }
            }
        }
        return Collections.unmodifiableList(arestas);
    }

    public void addVertice(Vertice v) {
        if (!this.listaVertices.contains(v)) {
            this.listaVertices.add(v);
        }
    }

    // Adiciona aresta não-direcionada entre índices i e j (com suporte a loop)
    public void addAresta(int i, int j) {
        addAresta(i, j, 1.0);
    }

    public void addAresta(int i, int j, double peso) {
        if (!Double.isFinite(peso)) {
            throw new IllegalArgumentException("O peso da aresta deve ser finito.");
        }
        if (i >= 0 && i < listaVertices.size() && j >= 0 && j < listaVertices.size()) {
            Vertice vi = listaVertices.get(i);
            Vertice vj = listaVertices.get(j);
            vi.addAresta(new Aresta(peso, vi, vj));
            if (i != j) {
                vj.addAresta(new Aresta(peso, vj, vi));
            }
        }
    }

    public void addAresta(Vertice u, Vertice v) {
        addAresta(u, v, 1.0);
    }

    public void addAresta(Vertice u, Vertice v, double peso) {
        Aresta aresta = new Aresta(peso, u, v);
        addVertice(u);
        addVertice(v);
        u.addAresta(aresta);
        if (u != v) {
            v.addAresta(new Aresta(peso, v, u));
        }
    }

    public void removeAresta(int i, int j) {
        if (i >= 0 && i < listaVertices.size() && j >= 0 && j < listaVertices.size()) {
            Vertice vi = listaVertices.get(i);
            Vertice vj = listaVertices.get(j);
            vi.listaArestas.removeIf(a -> a.destino == vj);
            vj.listaArestas.removeIf(a -> a.destino == vi);
        }
    }

    public boolean ehAresta(int i, int j) {
        if (i < 0 || i >= listaVertices.size() || j < 0 || j >= listaVertices.size()) {
            return false;
        }
        Vertice vi = listaVertices.get(i);
        Vertice vj = listaVertices.get(j);
        for (Aresta a : vi.listaArestas) {
            if (a.destino == vj) {
                return true;
            }
        }
        return false;
    }

    public boolean ehAresta(Vertice origem, Vertice destino) {
        int i = listaVertices.indexOf(origem);
        int j = listaVertices.indexOf(destino);
        return ehAresta(i, j);
    }

    public boolean[][] gerarMatrizAdjacencia() {
        int n = this.listaVertices.size();
        boolean[][] adjMatrix = new boolean[n][n];

        for (int i = 0; i < n; i++) {
            Vertice vOrigem = this.listaVertices.get(i);
            if (vOrigem.listaArestas != null) {
                for (Aresta a : vOrigem.listaArestas) {
                    int j = this.listaVertices.indexOf(a.destino);
                    if (j != -1) {
                        adjMatrix[i][j] = true;
                    }
                }
            }
        }
        return adjMatrix;
    }

    public String retornaGrafo() {
        boolean[][] adjMatrix = gerarMatrizAdjacencia();
        int numVertices = this.listaVertices.size();
        StringBuilder s = new StringBuilder();

        for (int i = 0; i < numVertices; i++) {
            s.append(i).append(": ");
            for (boolean j : adjMatrix[i]) {
                s.append(j ? 1 : 0).append(" ");
            }
            s.append("\n");
        }
        return s.toString();
    }

    public void imprimeArestasDeVertice(Vertice v) {
        if (!this.listaVertices.contains(v)) {
            System.out.println("Vertice inexistente no grafo...");
        } else {
            if (v.listaArestas.size() == 0) {
                System.out.println("Vertice sem arestas");
            } else {
                int n = v.listaArestas.size();
                for (int i = 0; i < n; i++) {
                    System.out.println(v.listaArestas.get(i).origem.dado + " " + v.listaArestas.get(i).destino.dado);
                }
            }
        }
    }

    public void imprimeVerticesDoGrafo() {
        if (this.listaVertices == null || this.listaVertices.isEmpty()) {
            System.out.println("Grafo sem Vertices...");
        } else {
            int n = this.listaVertices.size();
            for (int i = 0; i < n; i++) {
                System.out.println(this.listaVertices.get(i).dado);
            }
        }
    }

    public Integer retornaGrauVertice(Vertice v) {
        if (v.listaArestas == null) {
            return 0;
        }
        int grau = 0;
        for (Aresta a : v.listaArestas) {
            if (a.origem == a.destino) {
                grau += 2; // Loop conta duas vezes
            } else {
                grau += 1;
            }
        }
        return grau;
    }

    public void imprimeGrauVertice(Vertice v) {
        if (!this.listaVertices.contains(v))
            System.out.println("Vertice inexistente no grafo...");
        else
            System.out.println(this.retornaGrauVertice(v));
    }

    public boolean removeVertice(Vertice v) {
        if (this.listaVertices.contains(v) == false) {
            System.out.println("Vertice nao existe...");
            return false;
        } else {
            this.listaVertices.remove(v);
            int n = listaVertices.size();
            for (int i = 0; i < n; i++) {
                ArrayList<Aresta> listaTrabArestas = listaVertices.get(i).listaArestas;
                for (int j = listaTrabArestas.size() - 1; j >= 0; j--) {
                    if (listaTrabArestas.get(j).origem == v || listaTrabArestas.get(j).destino == v) {
                        listaTrabArestas.remove(j);
                    }
                }
                this.listaVertices.get(i).listaArestas = listaTrabArestas;
            }
            System.out.println("Vertice removido com sucesso...");
            return true;
        }
    }

    public void imprimeGrafo() {
        int n = this.listaVertices.size();
        if (n == 0) {
            System.out.println("Grafo sem vertices...");
        } else {
            for (int i = 0; i < n; i++) {
                System.out.println("--------");
                System.out.println("Vertice: " + this.listaVertices.get(i).dado);
                this.imprimeArestasDeVertice(this.listaVertices.get(i));
            }
        }
    }

    public Set<Vertice> conjuntoVizinhanca(Vertice v) {
        Set<Vertice> vizinhos = new LinkedHashSet<>();
        if (v != null && v.listaArestas != null) {
            for (Aresta a : v.listaArestas) {
                if (a.destino != v) {
                    vizinhos.add(a.destino);
                }
            }
        }
        return vizinhos;
    }

    public boolean ehVerticeIsolado(Vertice v) {
        return retornaGrauVertice(v) == 0;
    }

    public boolean ehVerticeFinal(Vertice v) {
        return retornaGrauVertice(v) == 1;
    }

    public boolean ehVerticePar(Vertice v) {
        return retornaGrauVertice(v) % 2 == 0;
    }

    public boolean ehVerticeImpar(Vertice v) {
        return retornaGrauVertice(v) % 2 != 0;
    }

    public ArrayList<Vertice> getVerticesImpares() {
        ArrayList<Vertice> impares = new ArrayList<>();
        for (Vertice v : this.listaVertices) {
            if (ehVerticeImpar(v)) {
                impares.add(v);
            }
        }
        return impares;
    }

    public ArrayList<Vertice> getVerticesPares() {
        ArrayList<Vertice> pares = new ArrayList<>();
        for (Vertice v : this.listaVertices) {
            if (ehVerticePar(v)) {
                pares.add(v);
            }
        }
        return pares;
    }

    public List<Integer> sequenciaDeGraus() {
        List<Integer> graus = new ArrayList<>();
        for (Vertice v : this.listaVertices) {
            graus.add(retornaGrauVertice(v));
        }
        Collections.sort(graus);
        return graus;
    }

    public int quantidadeArestas() {
        return somaDosGraus() / 2;
    }

    public int somaDosGraus() {
        int soma = 0;
        for (Vertice v : this.listaVertices) {
            soma += retornaGrauVertice(v);
        }
        return soma;
    }

    public boolean ehGrafoNulo() {
        return !this.listaVertices.isEmpty() && quantidadeArestas() == 0;
    }

    // Verifica se possui loops
    public boolean temLoops() {
        for (Vertice v : this.listaVertices) {
            if (v.listaArestas != null) {
                for (Aresta a : v.listaArestas) {
                    if (a.ehLoop()) return true;
                }
            }
        }
        return false;
    }

    // Verifica se possui arestas paralelas
    public boolean temArestasParalelas() {
        for (Vertice v : this.listaVertices) {
            if (v.listaArestas != null) {
                for (int i = 0; i < v.listaArestas.size(); i++) {
                    for (int j = i + 1; j < v.listaArestas.size(); j++) {
                        if (v.listaArestas.get(i).ehParalelaA(v.listaArestas.get(j))) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    // Grafo Simples: não tem loops nem arestas paralelas
    public boolean ehGrafoSimples() {
        return !temLoops() && !temArestasParalelas();
    }

    // Grafo Regular: retorna o grau k se todos tiverem grau k, ou -1 se não for regular
    public int getGrauRegularidade() {
        if (this.listaVertices.isEmpty()) return -1;
        int k = retornaGrauVertice(this.listaVertices.get(0));
        for (Vertice v : this.listaVertices) {
            if (retornaGrauVertice(v) != k) {
                return -1;
            }
        }
        return k;
    }

    public boolean ehRegular() {
        return getGrauRegularidade() != -1;
    }

    // Grafo Completo Kn: simples, com n vértices e n*(n-1)/2 arestas
    public boolean ehCompleto() {
        int n = this.listaVertices.size();
        int arestasEsperadas = (n * (n - 1)) / 2;
        return ehGrafoSimples() && quantidadeArestas() == arestasEsperadas;
    }

    // Grafo Bipartido (verificação por 2-coloração com BFS)
    public boolean ehBipartido() {
        if (temLoops()) return false;
        Map<Vertice, Integer> cor = new HashMap<>();

        for (Vertice inicio : this.listaVertices) {
            if (!cor.containsKey(inicio)) {
                Queue<Vertice> fila = new LinkedList<>();
                fila.add(inicio);
                cor.put(inicio, 0);

                while (!fila.isEmpty()) {
                    Vertice atual = fila.poll();
                    for (Vertice vizinho : conjuntoVizinhanca(atual)) {
                        if (!cor.containsKey(vizinho)) {
                            cor.put(vizinho, 1 - cor.get(atual));
                            fila.add(vizinho);
                        } else if (cor.get(vizinho).equals(cor.get(atual))) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    // Grafo Bipartido Completo Km,n
    public boolean ehBipartidoCompleto() {
        if (!ehGrafoSimples() || !ehBipartido() || this.listaVertices.size() < 2) return false;

        Map<Vertice, Integer> cor = new HashMap<>();
        int tamX = 0, tamY = 0;

        for (Vertice inicio : this.listaVertices) {
            if (!cor.containsKey(inicio)) {
                Queue<Vertice> fila = new LinkedList<>();
                fila.add(inicio);
                cor.put(inicio, 0);
                tamX++;
                while (!fila.isEmpty()) {
                    Vertice atual = fila.poll();
                    for (Vertice vizinho : conjuntoVizinhanca(atual)) {
                        if (!cor.containsKey(vizinho)) {
                            int novaCor = 1 - cor.get(atual);
                            cor.put(vizinho, novaCor);
                            if (novaCor == 0) tamX++; else tamY++;
                            fila.add(vizinho);
                        }
                    }
                }
            }
        }
        return tamX > 0 && tamY > 0 && quantidadeArestas() == (tamX * tamY);
    }

    // Verifica se 'this' (G2) é subgrafo de 'supergrafo' (G1)
    public boolean ehSubgrafoDe(Grafo supergrafo) {
        if (!supergrafo.listaVertices.containsAll(this.listaVertices)) {
            return false;
        }
        for (Vertice vSub : this.listaVertices) {
            for (Aresta aSub : vSub.listaArestas) {
                if (!supergrafo.ehAresta(aSub.origem, aSub.destino)) {
                    return false;
                }
            }
        }
        return true;
    }

    // Verifica os invariantes básicos para Isomorfismo (|V|, |E| e sequência de graus)
    public boolean satisfazInvariantesIsomorfismo(Grafo outro) {
        if (this.listaVertices.size() != outro.listaVertices.size()) return false;
        if (this.quantidadeArestas() != outro.quantidadeArestas()) return false;
        return this.sequenciaDeGraus().equals(outro.sequenciaDeGraus());
    }

    // Gerador de Grafo Completo Kn
    public static Grafo gerarCompleto(int n) {
        Grafo g = new Grafo(n);
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                g.addAresta(i, j);
            }
        }
        return g;
    }

    // Gerador de Grafo Bipartido Completo Km,n
    public static Grafo gerarBipartidoCompleto(int m, int n) {
        Grafo g = new Grafo();
        Vertice[] x = new Vertice[m];
        Vertice[] y = new Vertice[n];
        for (int i = 0; i < m; i++) {
            x[i] = new Vertice("x" + (i + 1));
            g.addVertice(x[i]);
        }
        for (int j = 0; j < n; j++) {
            y[j] = new Vertice("y" + (j + 1));
            g.addVertice(y[j]);
        }
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                g.addAresta(x[i], y[j]);
            }
        }
        return g;
    }
}