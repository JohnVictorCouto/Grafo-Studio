package utils;

import java.awt.Point;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import model.Aresta;
import model.Vertice;

public final class GraphIO {
    private static final String FORMAT = "grafo-studio-1";
    private static final int MAX_VERTICES = 10000;
    private static final int MAX_EDGES = 1000000;

    private GraphIO() {
    }

    public static void save(Path path, GraphDocument document,
            Map<Vertice, Point> positions) throws IOException {
        if (path == null || document == null || positions == null) {
            throw new IllegalArgumentException("Caminho, grafo e posicoes sao obrigatorios.");
        }
        Properties data = new Properties();
        data.setProperty("format", FORMAT);
        data.setProperty("directed", Boolean.toString(document.isDirected()));
        List<Vertice> vertices = document.getVertices();
        data.setProperty("vertex.count", Integer.toString(vertices.size()));
        Map<Vertice, Integer> indices = new IdentityHashMap<>();
        for (int i = 0; i < vertices.size(); i++) {
            Vertice vertex = vertices.get(i);
            indices.put(vertex, i);
            String prefix = "vertex." + i + ".";
            data.setProperty(prefix + "label", encode(vertex.toString()));
            Point point = positions.get(vertex);
            data.setProperty(prefix + "x", Integer.toString(point == null ? 0 : point.x));
            data.setProperty(prefix + "y", Integer.toString(point == null ? 0 : point.y));
        }
        List<Aresta> edges = document.getEdges();
        data.setProperty("edge.count", Integer.toString(edges.size()));
        for (int i = 0; i < edges.size(); i++) {
            Aresta edge = edges.get(i);
            Integer origin = indices.get(edge.origem);
            Integer destination = indices.get(edge.destino);
            if (origin == null || destination == null) {
                throw new IllegalArgumentException("Uma aresta referencia um vertice inexistente.");
            }
            String prefix = "edge." + i + ".";
            data.setProperty(prefix + "from", origin.toString());
            data.setProperty(prefix + "to", destination.toString());
            data.setProperty(prefix + "weight", Double.toString(edge.peso));
        }
        try (OutputStream output = Files.newOutputStream(path)) {
            data.store(output, "Grafo Studio");
        }
    }

    public static LoadedGraph load(Path path) throws IOException {
        if (path == null) {
            throw new IllegalArgumentException("O caminho do arquivo nao pode ser nulo.");
        }
        Properties data = new Properties();
        try (InputStream input = Files.newInputStream(path)) {
            data.load(input);
        }
        if (!FORMAT.equals(data.getProperty("format"))) {
            throw new IOException("Formato de arquivo Grafo Studio desconhecido.");
        }

        boolean directed = parseBoolean(data, "directed");
        int vertexCount = parseCount(data, "vertex.count", MAX_VERTICES);
        int edgeCount = parseCount(data, "edge.count", MAX_EDGES);
        GraphDocument document = new GraphDocument(directed);
        List<Vertice> vertices = new ArrayList<>();
        Map<Vertice, Point> positions = new IdentityHashMap<>();
        try {
            for (int i = 0; i < vertexCount; i++) {
                String prefix = "vertex." + i + ".";
                String label = decode(required(data, prefix + "label"));
                Vertice vertex = document.addVertex(label);
                vertices.add(vertex);
                int x = parseInteger(data, prefix + "x");
                int y = parseInteger(data, prefix + "y");
                if (Math.abs((long) x) > 1000000 || Math.abs((long) y) > 1000000) {
                    throw new IOException("Uma posicao de vertice esta fora dos limites.");
                }
                positions.put(vertex, new Point(x, y));
            }
            for (int i = 0; i < edgeCount; i++) {
                String prefix = "edge." + i + ".";
                int origin = parseInteger(data, prefix + "from");
                int destination = parseInteger(data, prefix + "to");
                double weight = parseDouble(data, prefix + "weight");
                if (origin < 0 || origin >= vertexCount
                        || destination < 0 || destination >= vertexCount) {
                    throw new IOException("Uma aresta referencia um indice de vertice invalido.");
                }
                document.addEdge(vertices.get(origin), vertices.get(destination), weight);
            }
        } catch (IllegalArgumentException exception) {
            throw new IOException("O arquivo contem dados de grafo invalidos: "
                    + exception.getMessage(), exception);
        }
        return new LoadedGraph(document, positions);
    }

    private static String encode(String text) {
        return Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8));
    }

    private static String decode(String encoded) throws IOException {
        try {
            return new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException exception) {
            throw new IOException("Rotulo de vertice invalido.", exception);
        }
    }

    private static String required(Properties data, String key) throws IOException {
        String value = data.getProperty(key);
        if (value == null) {
            throw new IOException("Campo obrigatorio ausente: " + key + ".");
        }
        return value;
    }

    private static boolean parseBoolean(Properties data, String key) throws IOException {
        String value = required(data, key);
        if (!"true".equals(value) && !"false".equals(value)) {
            throw new IOException("Valor booleano invalido: " + key + ".");
        }
        return Boolean.parseBoolean(value);
    }

    private static int parseCount(Properties data, String key, int maximum) throws IOException {
        int count = parseInteger(data, key);
        if (count < 0 || count > maximum) {
            throw new IOException("Quantidade fora dos limites: " + key + ".");
        }
        return count;
    }

    private static int parseInteger(Properties data, String key) throws IOException {
        try {
            return Integer.parseInt(required(data, key));
        } catch (NumberFormatException exception) {
            throw new IOException("Numero inteiro invalido: " + key + ".", exception);
        }
    }

    private static double parseDouble(Properties data, String key) throws IOException {
        try {
            double value = Double.parseDouble(required(data, key));
            if (!Double.isFinite(value)) {
                throw new IOException("Peso invalido: " + key + ".");
            }
            return value;
        } catch (NumberFormatException exception) {
            throw new IOException("Numero decimal invalido: " + key + ".", exception);
        }
    }

    public static final class LoadedGraph {
        private final GraphDocument document;
        private final Map<Vertice, Point> positions;

        private LoadedGraph(GraphDocument document, Map<Vertice, Point> positions) {
            this.document = document;
            Map<Vertice, Point> copies = new IdentityHashMap<>();
            for (Map.Entry<Vertice, Point> entry : positions.entrySet()) {
                copies.put(entry.getKey(), new Point(entry.getValue()));
            }
            this.positions = Collections.unmodifiableMap(copies);
        }

        public GraphDocument getDocument() {
            return document;
        }

        public Map<Vertice, Point> getPositions() {
            return positions;
        }
    }
}
