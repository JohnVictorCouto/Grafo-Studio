package utils;

import java.awt.Point;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import model.Aresta;
import model.Vertice;

public final class GraphHistory {
    private static final int MAX_STATES = 50;

    private final Deque<State> undoStates = new ArrayDeque<>();
    private final Deque<State> redoStates = new ArrayDeque<>();

    public void record(GraphDocument document, Map<Vertice, Point> positions) {
        undoStates.push(copyOf(document, positions));
        trim(undoStates);
        redoStates.clear();
    }

    public State undo(GraphDocument current, Map<Vertice, Point> positions) {
        if (undoStates.isEmpty()) {
            return null;
        }
        redoStates.push(copyOf(current, positions));
        return undoStates.pop();
    }

    public State redo(GraphDocument current, Map<Vertice, Point> positions) {
        if (redoStates.isEmpty()) {
            return null;
        }
        undoStates.push(copyOf(current, positions));
        trim(undoStates);
        return redoStates.pop();
    }

    public boolean canUndo() {
        return !undoStates.isEmpty();
    }

    public boolean canRedo() {
        return !redoStates.isEmpty();
    }

    public void clear() {
        undoStates.clear();
        redoStates.clear();
    }

    private State copyOf(GraphDocument document, Map<Vertice, Point> positions) {
        GraphDocument copy = new GraphDocument(document.isDirected());
        Map<Vertice, Vertice> vertices = new IdentityHashMap<>();
        Map<Vertice, Point> copiedPositions = new IdentityHashMap<>();
        for (Vertice vertex : document.getVertices()) {
            Vertice copiedVertex = copy.addVertex(vertex.toString());
            vertices.put(vertex, copiedVertex);
            Point position = positions.get(vertex);
            if (position != null) {
                copiedPositions.put(copiedVertex, new Point(position));
            }
        }
        for (Aresta edge : document.getEdges()) {
            copy.addEdge(vertices.get(edge.origem), vertices.get(edge.destino), edge.peso);
        }
        return new State(copy, copiedPositions);
    }

    private void trim(Deque<State> states) {
        while (states.size() > MAX_STATES) {
            states.removeLast();
        }
    }

    public static final class State {
        private final GraphDocument document;
        private final Map<Vertice, Point> positions;

        private State(GraphDocument document, Map<Vertice, Point> positions) {
            this.document = document;
            Map<Vertice, Point> copy = new IdentityHashMap<>();
            for (Map.Entry<Vertice, Point> entry : positions.entrySet()) {
                copy.put(entry.getKey(), new Point(entry.getValue()));
            }
            this.positions = Collections.unmodifiableMap(copy);
        }

        public GraphDocument getDocument() {
            return document;
        }

        public Map<Vertice, Point> getPositions() {
            return positions;
        }
    }
}
