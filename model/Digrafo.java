package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public final class Digrafo {
    private final List<Vertice> vertices = new ArrayList<>();
    private final List<Aresta> arestas = new ArrayList<>();
    private final Map<Vertice, List<Vertice>> sucessores = new IdentityHashMap<>();

    public Digrafo() {
    }

    public Digrafo(int quantidadeVertices) {
        if (quantidadeVertices < 0) {
            throw new IllegalArgumentException("A quantidade de vertices nao pode ser negativa.");
        }
        for (int i = 0; i < quantidadeVertices; i++) {
            addVertice(new Vertice(String.valueOf(i)));
        }
    }

    public List<Vertice> getVertices() {
        return Collections.unmodifiableList(vertices);
    }

    public List<Aresta> getArestas() {
        return Collections.unmodifiableList(arestas);
    }

    public List<Vertice> getSucessores(Vertice vertice) {
        List<Vertice> adjacentes = sucessores.get(vertice);
        if (adjacentes == null) {
            throw new IllegalArgumentException("O vertice nao pertence ao digrafo.");
        }
        return Collections.unmodifiableList(adjacentes);
    }

    public void addVertice(Vertice vertice) {
        if (vertice == null) {
            throw new IllegalArgumentException("O vertice nao pode ser nulo.");
        }
        if (!sucessores.containsKey(vertice)) {
            vertices.add(vertice);
            sucessores.put(vertice, new ArrayList<Vertice>());
        }
    }

    public void addAresta(Vertice origem, Vertice destino) {
        addAresta(1.0, origem, destino);
    }

    public void addAresta(double peso, Vertice origem, Vertice destino) {
        if (origem == null || destino == null) {
            throw new IllegalArgumentException("Os vertices da aresta nao podem ser nulos.");
        }
        Aresta aresta = new Aresta(peso, origem, destino);
        addVertice(origem);
        addVertice(destino);
        arestas.add(aresta);
        sucessores.get(origem).add(destino);
    }
}
