package views;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javax.swing.JPanel;
import model.Aresta;
import model.Vertice;
import utils.AlgorithmRunner;
import utils.GraphDocument;

public final class GraphCanvas extends JPanel {
    private static final int VERTEX_RADIUS = 24;
    private static final double EDGE_HIT_TOLERANCE = 9;
    private static final Color EDGE_COLOR = new Color(113, 128, 150);
    private static final Color ACTIVE_COLOR = new Color(228, 161, 27);

    private final Map<Vertice, Point> positions = new IdentityHashMap<>();
    private GraphDocument document;
    private AlgorithmRunner.Step step;
    private Vertice selectedVertex;
    private Vertice pendingVertex;
    private Aresta selectedEdge;
    private Vertice draggingVertex;
    private Point lastDragPoint;
    private Point lastPanPoint;
    private double zoom = 1.0;
    private double panX;
    private double panY;
    private boolean panning;
    private boolean dragged;
    private boolean suppressClick;
    private boolean moveHistoryRecorded;
    private List<Vertice> highlightedPath = Collections.emptyList();
    private Runnable beforeVertexMove = () -> { };
    private Consumer<Point> backgroundClick = point -> { };
    private Consumer<Vertice> vertexClick = vertex -> { };
    private Consumer<Aresta> edgeClick = edge -> { };
    private BiConsumer<Vertice, Point> vertexMoved = (vertex, point) -> { };

    public GraphCanvas(GraphDocument document) {
        this.document = document;
        setMinimumSize(new Dimension(400, 300));
        setPreferredSize(new Dimension(1000, 800));
        setBackground(new Color(247, 249, 252));
        MouseAdapter mouseHandler = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                if (event.getButton() == MouseEvent.BUTTON2) {
                    panning = true;
                    lastPanPoint = event.getPoint();
                } else if (event.getButton() == MouseEvent.BUTTON1) {
                    draggingVertex = findVertex(toWorld(event.getPoint()));
                    if (draggingVertex != null) {
                        lastDragPoint = event.getPoint();
                        dragged = false;
                        moveHistoryRecorded = false;
                    }
                }
            }

            @Override
            public void mouseDragged(MouseEvent event) {
                if (panning && lastPanPoint != null) {
                    Point current = event.getPoint();
                    panX += current.x - lastPanPoint.x;
                    panY += current.y - lastPanPoint.y;
                    lastPanPoint = current;
                    repaint();
                    return;
                }
                if (draggingVertex == null || lastDragPoint == null) {
                    return;
                }
                Point current = event.getPoint();
                if (current.distance(lastDragPoint) > 1) {
                    dragged = true;
                    if (!moveHistoryRecorded) {
                        beforeVertexMove.run();
                        moveHistoryRecorded = true;
                    }
                    Point position = positions.get(draggingVertex);
                    int x = (int) Math.round(position.x
                            + (current.x - lastDragPoint.x) / zoom);
                    int y = (int) Math.round(position.y
                            + (current.y - lastDragPoint.y) / zoom);
                    positions.put(draggingVertex, new Point(x, y));
                    lastDragPoint = current;
                    repaint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent event) {
                panning = false;
                lastPanPoint = null;
                if (draggingVertex != null && dragged) {
                    suppressClick = true;
                    vertexMoved.accept(draggingVertex, new Point(positions.get(draggingVertex)));
                }
                draggingVertex = null;
                lastDragPoint = null;
            }

            @Override
            public void mouseClicked(MouseEvent event) {
                if (event.getButton() != MouseEvent.BUTTON1) {
                    return;
                }
                if (suppressClick) {
                    suppressClick = false;
                    return;
                }
                Point worldPoint = toWorld(event.getPoint());
                Vertice vertex = findVertex(worldPoint);
                if (vertex != null) {
                    vertexClick.accept(vertex);
                    return;
                }
                Aresta edge = findEdge(worldPoint);
                if (edge != null) {
                    edgeClick.accept(edge);
                    return;
                }
                backgroundClick.accept(worldPoint);
            }
        };
        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);
        addMouseWheelListener(event -> {
            double factor = event.getWheelRotation() < 0 ? 1.12 : 1 / 1.12;
            zoomAt(event.getPoint(), factor);
        });
    }

    public void setHandlers(Consumer<Point> backgroundClick,
            Consumer<Vertice> vertexClick, Consumer<Aresta> edgeClick,
            Runnable beforeVertexMove, BiConsumer<Vertice, Point> vertexMoved) {
        this.backgroundClick = backgroundClick;
        this.vertexClick = vertexClick;
        this.edgeClick = edgeClick;
        this.beforeVertexMove = beforeVertexMove;
        this.vertexMoved = vertexMoved;
    }

    public void setDocument(GraphDocument document) {
        this.document = document;
        positions.clear();
        selectedVertex = null;
        pendingVertex = null;
        selectedEdge = null;
        step = null;
        highlightedPath = Collections.emptyList();
        repaint();
    }

    public void setPosition(Vertice vertex, Point position) {
        positions.put(vertex, new Point(position));
        repaint();
    }

    public void setPositions(Map<Vertice, Point> restoredPositions) {
        positions.clear();
        for (Map.Entry<Vertice, Point> entry : restoredPositions.entrySet()) {
            if (document.getVertices().contains(entry.getKey())) {
                positions.put(entry.getKey(), new Point(entry.getValue()));
            }
        }
        repaint();
    }

    public Map<Vertice, Point> snapshotPositions() {
        Map<Vertice, Point> snapshot = new IdentityHashMap<>();
        for (Map.Entry<Vertice, Point> entry : positions.entrySet()) {
            snapshot.put(entry.getKey(), new Point(entry.getValue()));
        }
        return Collections.unmodifiableMap(snapshot);
    }

    public void removePosition(Vertice vertex) {
        positions.remove(vertex);
        if (selectedVertex == vertex) {
            selectedVertex = null;
        }
        if (pendingVertex == vertex) {
            pendingVertex = null;
        }
        repaint();
    }

    public void setSelection(Vertice selectedVertex, Vertice pendingVertex, Aresta selectedEdge) {
        this.selectedVertex = selectedVertex;
        this.pendingVertex = pendingVertex;
        this.selectedEdge = selectedEdge;
        repaint();
    }

    public void setStep(AlgorithmRunner.Step step) {
        this.step = step;
        repaint();
    }

    public void setHighlightedPath(List<Vertice> path) {
        highlightedPath = path == null
                ? Collections.<Vertice>emptyList()
                : Collections.unmodifiableList(new java.util.ArrayList<>(path));
        repaint();
    }

    public void clearStep() {
        step = null;
        highlightedPath = Collections.emptyList();
        repaint();
    }

    public void layoutVertices() {
        List<Vertice> vertices = document.getVertices();
        double centerX = getWidth() > 0 ? getWidth() / 2.0 : getPreferredSize().width / 2.0;
        double centerY = getHeight() > 0 ? getHeight() / 2.0 : getPreferredSize().height / 2.0;
        double radius = Math.max(80, Math.min(centerX, centerY) * 0.62);
        for (int i = 0; i < vertices.size(); i++) {
            double angle = -Math.PI / 2 + 2 * Math.PI * i / vertices.size();
            positions.put(vertices.get(i), new Point(
                    (int) Math.round(centerX + radius * Math.cos(angle)),
                    (int) Math.round(centerY + radius * Math.sin(angle))));
        }
        repaint();
    }

    public double getZoom() {
        return zoom;
    }

    public void zoomIn() {
        zoomAt(new Point(getWidth() / 2, getHeight() / 2), 1.15);
    }

    public void zoomOut() {
        zoomAt(new Point(getWidth() / 2, getHeight() / 2), 1 / 1.15);
    }

    public void centerGraph() {
        List<Vertice> vertices = document.getVertices();
        if (vertices.isEmpty()) {
            panX = 0;
            panY = 0;
            repaint();
            return;
        }
        ensurePositions();
        double minX = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        for (Vertice vertex : vertices) {
            Point point = positions.get(vertex);
            minX = Math.min(minX, point.x);
            maxX = Math.max(maxX, point.x);
            minY = Math.min(minY, point.y);
            maxY = Math.max(maxY, point.y);
        }
        panX = getWidth() / 2.0 - ((minX + maxX) / 2.0) * zoom;
        panY = getHeight() / 2.0 - ((minY + maxY) / 2.0) * zoom;
        repaint();
    }

    private void zoomAt(Point screenPoint, double factor) {
        double newZoom = Math.max(0.35, Math.min(2.5, zoom * factor));
        if (newZoom == zoom) {
            return;
        }
        double worldX = (screenPoint.x - panX) / zoom;
        double worldY = (screenPoint.y - panY) / zoom;
        zoom = newZoom;
        panX = screenPoint.x - worldX * zoom;
        panY = screenPoint.y - worldY * zoom;
        repaint();
    }

    private Point toWorld(Point screenPoint) {
        return new Point(
                (int) Math.round((screenPoint.x - panX) / zoom),
                (int) Math.round((screenPoint.y - panY) / zoom));
    }

    public void refresh() {
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        ensurePositions();
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        drawGrid(g);
        g.translate(panX, panY);
        g.scale(zoom, zoom);
        for (Aresta edge : document.getEdges()) {
            drawEdge(g, edge);
        }
        for (Vertice vertex : document.getVertices()) {
            drawVertex(g, vertex);
        }
        g.dispose();
        drawOverlay((Graphics2D) graphics.create());
    }

    private void drawGrid(Graphics2D g) {
        g.setColor(new Color(226, 232, 240));
        int spacing = 28;
        for (int x = 0; x < getWidth(); x += spacing) {
            g.drawLine(x, 0, x, getHeight());
        }
        for (int y = 0; y < getHeight(); y += spacing) {
            g.drawLine(0, y, getWidth(), y);
        }
    }

    private void drawOverlay(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if (document.getVertices().isEmpty()) {
            String title = "Crie seu primeiro grafo";
            String instruction = "Clique no canvas para adicionar vertices";
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
            FontMetrics metrics = g.getFontMetrics();
            g.setColor(new Color(52, 64, 84));
            g.drawString(title, (getWidth() - metrics.stringWidth(title)) / 2,
                    getHeight() / 2 - 8);
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
            metrics = g.getFontMetrics();
            g.setColor(new Color(102, 112, 133));
            g.drawString(instruction, (getWidth() - metrics.stringWidth(instruction)) / 2,
                    getHeight() / 2 + 20);
        }

        int x = 16;
        int y = getHeight() - 18;
        drawLegendItem(g, x, y, new Color(255, 224, 138), "Atual");
        drawLegendItem(g, x + 90, y, new Color(190, 227, 248), "Fila");
        drawLegendItem(g, x + 170, y, new Color(183, 228, 199), "Processado");
        drawLegendEdge(g, x + 270, y, ACTIVE_COLOR, "Aresta ativa");
        drawLegendEdge(g, x + 380, y, new Color(22, 163, 74), "Solucao");
        drawLegendEdge(g, x + 465, y, new Color(14, 116, 144), "Caminho");
        g.setColor(new Color(71, 84, 103));
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        g.drawString(String.format("Zoom %d%%", Math.round(zoom * 100)),
                Math.max(16, getWidth() - 92), getHeight() - 18);
        g.dispose();
    }

    private void drawLegendItem(Graphics2D g, int x, int y, Color color, String label) {
        g.setColor(color);
        g.fillOval(x, y - 10, 12, 12);
        g.setColor(new Color(71, 84, 103));
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        g.drawString(label, x + 17, y);
    }

    private void drawLegendEdge(Graphics2D g, int x, int y, Color color, String label) {
        g.setColor(color);
        g.setStroke(new BasicStroke(3f));
        g.drawLine(x, y - 5, x + 13, y - 5);
        g.setColor(new Color(71, 84, 103));
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        g.drawString(label, x + 17, y);
    }

    private void ensurePositions() {
        List<Vertice> vertices = document.getVertices();
        double centerX = getWidth() / 2.0;
        double centerY = getHeight() / 2.0;
        double radius = Math.max(80, Math.min(getWidth(), getHeight()) * 0.32);
        for (int i = 0; i < vertices.size(); i++) {
            Vertice vertex = vertices.get(i);
            if (!positions.containsKey(vertex)) {
                double angle = -Math.PI / 2 + 2 * Math.PI * i / vertices.size();
                positions.put(vertex, new Point(
                        (int) Math.round(centerX + radius * Math.cos(angle)),
                        (int) Math.round(centerY + radius * Math.sin(angle))));
            }
        }
        positions.keySet().retainAll(vertices);
    }

    private void drawEdge(Graphics2D g, Aresta edge) {
        Point from = positions.get(edge.origem);
        Point to = positions.get(edge.destino);
        boolean selected = edge == selectedEdge;
        boolean active = step != null && step.getEdgeFrom() != null
                && ((step.getEdgeFrom() == edge.origem && step.getEdgeTo() == edge.destino)
                || (!document.isDirected() && step.getEdgeFrom() == edge.destino
                && step.getEdgeTo() == edge.origem));
        boolean included = step != null && step.getIncludedEdges().contains(edge);
        boolean path = isPathEdge(edge);
        Color color = selected ? new Color(128, 90, 213)
                : active ? ACTIVE_COLOR
                : path ? new Color(14, 116, 144)
                : included ? new Color(22, 163, 74) : EDGE_COLOR;
        g.setColor(color);
        g.setStroke(new BasicStroke(selected || active || path || included ? 3.5f : 2f));

        if (edge.origem == edge.destino) {
            g.draw(new Ellipse2D.Double(from.x - 12,
                    from.y - VERTEX_RADIUS - 24, 24, 24));
            drawEdgeLabel(g, edge, from.x, from.y - VERTEX_RADIUS - 28, color);
            return;
        }

        double dx = to.x - from.x;
        double dy = to.y - from.y;
        double length = Math.max(1, Math.hypot(dx, dy));
        double ux = dx / length;
        double uy = dy / length;
        double startX = from.x + ux * VERTEX_RADIUS;
        double startY = from.y + uy * VERTEX_RADIUS;
        double endX = to.x - ux * (VERTEX_RADIUS + (document.isDirected() ? 9 : 0));
        double endY = to.y - uy * (VERTEX_RADIUS + (document.isDirected() ? 9 : 0));
        g.draw(new Line2D.Double(startX, startY, endX, endY));
        if (document.isDirected()) {
            drawArrowHead(g, endX, endY, ux, uy, color);
        }
        drawEdgeLabel(g, edge, (from.x + to.x) / 2.0,
                (from.y + to.y) / 2.0 - 7, color);
    }

    private boolean isPathEdge(Aresta edge) {
        for (int i = 1; i < highlightedPath.size(); i++) {
            Vertice from = highlightedPath.get(i - 1);
            Vertice to = highlightedPath.get(i);
            if (edge.origem == from && edge.destino == to
                    || !document.isDirected() && edge.origem == to && edge.destino == from) {
                return true;
            }
        }
        return false;
    }

    private void drawVertex(Graphics2D g, Vertice vertex) {
        Point position = positions.get(vertex);
        Color fill = Color.WHITE;
        if (step != null && step.getProcessed().contains(vertex)) {
            fill = new Color(183, 228, 199);
        } else if (step != null && step.getFrontier().contains(vertex)) {
            fill = new Color(190, 227, 248);
        } else if (step != null && step.getDiscovered().contains(vertex)) {
            fill = new Color(217, 234, 253);
        }
        if (step != null && step.getCurrent() == vertex) {
            fill = new Color(255, 224, 138);
        }

        g.setColor(fill);
        g.fill(new Ellipse2D.Double(position.x - VERTEX_RADIUS,
                position.y - VERTEX_RADIUS, VERTEX_RADIUS * 2, VERTEX_RADIUS * 2));
        g.setColor(vertex == selectedVertex || vertex == pendingVertex
                ? new Color(128, 90, 213) : new Color(52, 64, 84));
        g.setStroke(new BasicStroke(vertex == selectedVertex || vertex == pendingVertex
                ? 3f : 1.8f));
        g.draw(new Ellipse2D.Double(position.x - VERTEX_RADIUS,
                position.y - VERTEX_RADIUS, VERTEX_RADIUS * 2, VERTEX_RADIUS * 2));

        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        FontMetrics metrics = g.getFontMetrics();
        String label = vertex.toString();
        g.setColor(new Color(24, 34, 50));
        g.drawString(label, position.x - metrics.stringWidth(label) / 2,
                position.y + (metrics.getAscent() - metrics.getDescent()) / 2);

        if (step != null && !step.getDistances().isEmpty()) {
            Double distance = step.getDistances().get(vertex);
            String text = distance == null || distance.isInfinite()
                    ? "\u221e" : formatDistance(distance);
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
            metrics = g.getFontMetrics();
            g.setColor(new Color(71, 84, 103));
            g.drawString(text, position.x - metrics.stringWidth(text) / 2,
                    position.y + VERTEX_RADIUS + 14);
        }
    }

    private void drawArrowHead(Graphics2D g, double tipX, double tipY,
            double directionX, double directionY, Color color) {
        double perpendicularX = -directionY;
        double perpendicularY = directionX;
        double baseX = tipX - directionX * 10;
        double baseY = tipY - directionY * 10;
        Polygon arrow = new Polygon(
                new int[] {
                    (int) Math.round(tipX),
                    (int) Math.round(baseX + perpendicularX * 5),
                    (int) Math.round(baseX - perpendicularX * 5)
                },
                new int[] {
                    (int) Math.round(tipY),
                    (int) Math.round(baseY + perpendicularY * 5),
                    (int) Math.round(baseY - perpendicularY * 5)
                }, 3);
        g.setColor(color);
        g.fillPolygon(arrow);
    }

    private void drawEdgeLabel(Graphics2D g, Aresta edge, double x, double y, Color color) {
        String text = formatDistance(edge.peso);
        Font oldFont = g.getFont();
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        FontMetrics metrics = g.getFontMetrics();
        int textX = (int) Math.round(x - metrics.stringWidth(text) / 2.0);
        int textY = (int) Math.round(y);
        g.setColor(getBackground());
        g.fillRoundRect(textX - 3, textY - metrics.getAscent(),
                metrics.stringWidth(text) + 6, metrics.getHeight(), 5, 5);
        g.setColor(color);
        g.drawString(text, textX, textY);
        g.setFont(oldFont);
    }

    private Vertice findVertex(Point point) {
        List<Vertice> vertices = document.getVertices();
        for (int i = vertices.size() - 1; i >= 0; i--) {
            Vertice vertex = vertices.get(i);
            if (positions.get(vertex).distance(point) <= VERTEX_RADIUS + 5) {
                return vertex;
            }
        }
        return null;
    }

    private Aresta findEdge(Point point) {
        List<Aresta> edges = document.getEdges();
        for (int i = edges.size() - 1; i >= 0; i--) {
            Aresta edge = edges.get(i);
            Point from = positions.get(edge.origem);
            Point to = positions.get(edge.destino);
            if (edge.origem == edge.destino) {
                double centerY = from.y - VERTEX_RADIUS - 12;
                if (Math.abs(Math.hypot(point.x - from.x, point.y - centerY) - 12)
                        <= EDGE_HIT_TOLERANCE) {
                    return edge;
                }
            } else if (distanceToSegment(point.x, point.y, from, to)
                    <= EDGE_HIT_TOLERANCE) {
                return edge;
            }
        }
        return null;
    }

    private double distanceToSegment(double x, double y, Point start, Point end) {
        double dx = end.x - start.x;
        double dy = end.y - start.y;
        double lengthSquared = dx * dx + dy * dy;
        if (lengthSquared == 0) {
            return Point.distance(x, y, start.x, start.y);
        }
        double fraction = Math.max(0, Math.min(1,
                ((x - start.x) * dx + (y - start.y) * dy) / lengthSquared));
        return Point.distance(x, y, start.x + fraction * dx, start.y + fraction * dy);
    }

    private String formatDistance(double distance) {
        if (distance == Math.rint(distance) && Math.abs(distance) < Long.MAX_VALUE) {
            return Long.toString((long) distance);
        }
        return Double.toString(distance);
    }
}
