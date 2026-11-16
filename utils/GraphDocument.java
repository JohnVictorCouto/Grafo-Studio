package utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import model.Aresta;
import model.Digrafo;
import model.Grafo;
import model.Vertice;

public final class GraphDocument {
    private final boolean directed;
    private final List<Vertice> vertices = new ArrayList<>();
    private final List<Aresta> edges = new ArrayList<>();
    private int nextVertexNumber = 1;

    public GraphDocument(boolean directed) {
        this.directed = directed;
    }

    public boolean isDirected() {
        return directed;
    }

    public List<Vertice> getVertices() {
        return Collections.unmodifiableList(vertices);
    }

    public List<Aresta> getEdges() {
        return Collections.unmodifiableList(edges);
    }

    public Vertice addVertex() {
        String label;
        do {
            label = "V" + nextVertexNumber++;
        } while (hasLabel(label));
        return addVertex(label);
    }

    public Vertice addVertex(String label) {
        if (label == null || label.trim().isEmpty()) {
            throw new IllegalArgumentException("O rotulo do vertice nao pode ser vazio.");
        }
        if (hasLabel(label)) {
            throw new IllegalArgumentException("Ja existe um vertice com esse rotulo.");
        }
        Vertice vertex = new Vertice(label);
        vertices.add(vertex);
        return vertex;
    }

    public void addEdge(Vertice origin, Vertice destination, double weight) {
        requireVertex(origin);
        requireVertex(destination);
        if (!Double.isFinite(weight)) {
            throw new IllegalArgumentException("O peso da aresta deve ser finito.");
        }
        for (Aresta edge : edges) {
            boolean sameDirection = edge.origem == origin && edge.destino == destination;
            boolean reverseDirection = edge.origem == destination && edge.destino == origin;
            if (sameDirection || (!directed && reverseDirection)) {
                throw new IllegalArgumentException("Ja existe uma aresta entre esses vertices.");
            }
        }
        edges.add(new Aresta(weight, origin, destination));
    }

    public boolean removeVertex(Vertice vertex) {
        if (!vertices.remove(vertex)) {
            return false;
        }
        edges.removeIf(edge -> edge.origem == vertex || edge.destino == vertex);
        return true;
    }

    public boolean removeEdge(Aresta edge) {
        return edges.remove(edge);
    }

    public Grafo toUndirectedGraph() {
        if (directed) {
            throw new IllegalStateException("O documento contem arestas direcionadas.");
        }
        Grafo graph = new Grafo();
        for (Vertice vertex : vertices) {
            graph.addVertice(vertex);
        }
        for (Aresta edge : edges) {
            graph.addAresta(edge.origem, edge.destino, edge.peso);
        }
        return graph;
    }

    public Digrafo toDigraph() {
        if (!directed) {
            throw new IllegalStateException("O documento contem arestas nao direcionadas.");
        }
        Digrafo digraph = new Digrafo();
        for (Vertice vertex : vertices) {
            digraph.addVertice(vertex);
        }
        for (Aresta edge : edges) {
            digraph.addAresta(edge.peso, edge.origem, edge.destino);
        }
        return digraph;
    }

    private void requireVertex(Vertice vertex) {
        if (vertex == null || !vertices.contains(vertex)) {
            throw new IllegalArgumentException("O vertice nao pertence ao documento.");
        }
    }

    private boolean hasLabel(String label) {
        for (Vertice vertex : vertices) {
            if (vertex.toString().equals(label)) {
                return true;
            }
        }
        return false;
    }
}
