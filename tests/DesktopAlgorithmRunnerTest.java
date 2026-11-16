package tests;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.SwingUtilities;
import model.Aresta;
import model.Vertice;
import utils.AlgorithmRunner;
import utils.GraphDocument;
import utils.GraphHistory;
import utils.GraphIO;
import views.GraphCanvas;

public final class DesktopAlgorithmRunnerTest {
    private DesktopAlgorithmRunnerTest() {
    }

    public static void main(String[] args) {
        testaBfsDirecionada();
        testaDfsPredecessores();
        testaDijkstraComPesos();
        testaTopologicaComCiclo();
        testaArvoresGeradorasMinimas();
        testaEmparelhamento();
        testaPercursosEulerianosEHamiltonianos();
        testaEdicaoDoDocumento();
        testaHistoricoUndoRedo();
        testaPersistenciaDoGrafo();
        testaCanvasSwing();
    }

    private static void testaBfsDirecionada() {
        GraphDocument document = new GraphDocument(true);
        Vertice origem = document.addVertex();
        Vertice meio = document.addVertex();
        Vertice destino = document.addVertex();
        document.addEdge(origem, meio, 7);
        document.addEdge(meio, destino, 2);

        AlgorithmRunner.Trace trace = AlgorithmRunner.run(document,
                AlgorithmRunner.Algorithm.BREADTH_FIRST, origem);
        AlgorithmRunner.Step result = lastStep(trace);
        exige(result.getDistances().get(destino) == 2.0,
                "BFS deve calcular distancia em arestas no grafo direcionado.");

        AlgorithmRunner.Trace reverseTrace = AlgorithmRunner.run(document,
                AlgorithmRunner.Algorithm.BREADTH_FIRST, destino);
        exige(lastStep(reverseTrace).getDistances().get(origem).isInfinite(),
                "BFS deve respeitar a direcao das arestas.");
    }

    private static void testaDijkstraComPesos() {
        GraphDocument document = new GraphDocument(false);
        Vertice origem = document.addVertex();
        Vertice meio = document.addVertex();
        Vertice destino = document.addVertex();
        document.addEdge(origem, meio, 2.5);
        document.addEdge(meio, destino, 1.5);
        document.addEdge(origem, destino, 8);

        AlgorithmRunner.Trace trace = AlgorithmRunner.run(document,
                AlgorithmRunner.Algorithm.DIJKSTRA, origem);
        AlgorithmRunner.Step result = lastStep(trace);
        exige(result.getDistances().get(destino) == 4.0,
                "Dijkstra deve mostrar o menor custo de um caminho.");
        List<Vertice> path = tracePath(result, destino);
        exige(path.equals(Arrays.asList(origem, meio, destino)),
                "Dijkstra deve manter predecessores para reconstruir o caminho.");

        GraphDocument negative = new GraphDocument(true);
        Vertice a = negative.addVertex();
        Vertice b = negative.addVertex();
        negative.addEdge(a, b, -1);
        esperaIllegalArgument(() -> AlgorithmRunner.run(negative,
                AlgorithmRunner.Algorithm.DIJKSTRA, a));
    }

    private static void testaDfsPredecessores() {
        GraphDocument document = new GraphDocument(true);
        Vertice origem = document.addVertex();
        Vertice meio = document.addVertex();
        Vertice destino = document.addVertex();
        document.addEdge(origem, meio, 1);
        document.addEdge(meio, destino, 1);

        AlgorithmRunner.Step result = lastStep(AlgorithmRunner.run(document,
                AlgorithmRunner.Algorithm.DEPTH_FIRST, origem));
        exige(result.getPredecessors().get(destino) == meio,
                "DFS deve preservar os predecessores da arvore de busca.");
    }

    private static void testaTopologicaComCiclo() {
        GraphDocument document = new GraphDocument(true);
        Vertice a = document.addVertex();
        Vertice b = document.addVertex();
        Vertice c = document.addVertex();
        document.addEdge(a, b, 1);
        document.addEdge(b, c, 1);
        document.addEdge(c, a, 1);

        AlgorithmRunner.Trace trace = AlgorithmRunner.run(document,
                AlgorithmRunner.Algorithm.TOPOLOGICAL, null);
        exige(!trace.isSuccessful(), "Ciclo deve ser informado pela execucao topologica.");
        exige(trace.getSummary().contains("Ciclo"), "Resumo deve explicar a dependencia circular.");
    }

    private static void testaArvoresGeradorasMinimas() {
        GraphDocument document = new GraphDocument(false);
        Vertice a = document.addVertex();
        Vertice b = document.addVertex();
        Vertice c = document.addVertex();
        document.addEdge(a, b, 1);
        document.addEdge(b, c, 2);
        document.addEdge(a, c, 10);

        AlgorithmRunner.Trace prim = AlgorithmRunner.run(document,
                AlgorithmRunner.Algorithm.PRIM, a);
        AlgorithmRunner.Trace kruskal = AlgorithmRunner.run(document,
                AlgorithmRunner.Algorithm.KRUSKAL, null);
        exige(prim.isSuccessful() && kruskal.isSuccessful(),
                "Prim e Kruskal devem encontrar uma arvore em grafo conexo.");
        exige(lastStep(prim).getIncludedEdges().size() == 2
                        && lastStep(kruskal).getIncludedEdges().size() == 2,
                "As arvores geradoras devem conter V-1 arestas.");
        exige(prim.getSummary().contains("3")
                        && kruskal.getSummary().contains("3"),
                "Prim e Kruskal devem retornar o peso minimo total.");
    }

    private static void testaEmparelhamento() {
        GraphDocument document = new GraphDocument(false);
        Vertice a = document.addVertex();
        Vertice b = document.addVertex();
        Vertice c = document.addVertex();
        Vertice d = document.addVertex();
        document.addEdge(a, c, 1);
        document.addEdge(a, d, 1);
        document.addEdge(b, c, 1);

        AlgorithmRunner.Trace trace = AlgorithmRunner.run(document,
                AlgorithmRunner.Algorithm.MATCHING, null);
        exige(trace.isSuccessful(), "O emparelhamento bipartido deve ser concluido.");
        exige(lastStep(trace).getIncludedEdges().size() == 2,
                "A busca aumentante deve encontrar o emparelhamento maximo.");
    }

    private static void testaPercursosEulerianosEHamiltonianos() {
        GraphDocument document = new GraphDocument(false);
        Vertice a = document.addVertex();
        Vertice b = document.addVertex();
        Vertice c = document.addVertex();
        document.addEdge(a, b, 1);
        document.addEdge(b, c, 1);
        document.addEdge(c, a, 1);

        AlgorithmRunner.Trace euler = AlgorithmRunner.run(document,
                AlgorithmRunner.Algorithm.EULER_CIRCUIT, null);
        exige(euler.isSuccessful()
                        && lastStep(euler).getIncludedEdges().size() == 3,
                "O circuito euleriano deve percorrer cada aresta uma vez.");

        AlgorithmRunner.Trace hamilton = AlgorithmRunner.run(document,
                AlgorithmRunner.Algorithm.HAMILTON_CYCLE, a);
        exige(hamilton.isSuccessful()
                        && lastStep(hamilton).getRoute().size() == 4,
                "O ciclo hamiltoniano deve visitar cada vertice e retornar a origem.");
    }

    private static void testaEdicaoDoDocumento() {
        GraphDocument document = new GraphDocument(false);
        Vertice a = document.addVertex();
        Vertice b = document.addVertex();
        document.addEdge(a, b, 1);
        Aresta edge = document.getEdges().get(0);
        exige(!document.removeEdge(new Aresta(1, a, b)),
                "A remocao deve exigir a aresta pertencente ao documento.");
        exige(document.removeEdge(edge), "Aresta existente deve ser removida.");
        document.addEdge(a, b, 2);
        exige(document.removeVertex(a), "Vertice existente deve ser removido.");
        exige(document.getEdges().isEmpty(), "A remocao de vertice deve remover arestas incidentes.");
    }

    private static void testaHistoricoUndoRedo() {
        GraphDocument document = new GraphDocument(false);
        Vertice a = document.addVertex("Origem");
        Vertice b = document.addVertex("Destino");
        document.addEdge(a, b, 2);
        Map<Vertice, java.awt.Point> positions = new IdentityHashMap<>();
        positions.put(a, new java.awt.Point(10, 20));
        positions.put(b, new java.awt.Point(30, 40));

        GraphHistory history = new GraphHistory();
        history.record(document, positions);
        document.removeVertex(b);
        GraphHistory.State restored = history.undo(document, positions);
        exige(restored != null && restored.getDocument().getVertices().size() == 2
                        && restored.getDocument().getEdges().size() == 1,
                "Undo deve restaurar vertices e arestas do estado anterior.");
        exige(restored.getPositions().get(restored.getDocument().getVertices().get(0))
                        .equals(new java.awt.Point(10, 20)),
                "Undo deve restaurar as posicoes dos vertices.");

        GraphHistory.State redone = history.redo(restored.getDocument(), restored.getPositions());
        exige(redone != null && redone.getDocument().getVertices().size() == 1
                        && redone.getDocument().getEdges().isEmpty(),
                "Redo deve restaurar o estado editado.");
        history.clear();
        exige(!history.canUndo() && !history.canRedo(),
                "Limpar o historico deve descartar estados de ambos os sentidos.");
    }

    private static void testaPersistenciaDoGrafo() {
        Path path = null;
        try {
            path = Files.createTempFile("grafo-studio-", ".grafo");
            GraphDocument document = new GraphDocument(true);
            Vertice origem = document.addVertex("Início");
            Vertice destino = document.addVertex("Destino");
            document.addEdge(origem, destino, 2.5);
            Map<Vertice, java.awt.Point> positions = new IdentityHashMap<>();
            positions.put(origem, new java.awt.Point(120, 140));
            positions.put(destino, new java.awt.Point(360, 240));

            GraphIO.save(path, document, positions);
            GraphIO.LoadedGraph loaded = GraphIO.load(path);
            exige(loaded.getDocument().isDirected()
                            && loaded.getDocument().getVertices().get(0).toString().equals("Início"),
                    "Salvar e carregar deve preservar direcao e rotulos UTF-8.");
            exige(loaded.getDocument().getEdges().size() == 1
                            && loaded.getDocument().getEdges().get(0).peso == 2.5,
                    "Salvar e carregar deve preservar arestas e pesos.");
            exige(loaded.getPositions().get(loaded.getDocument().getVertices().get(1))
                            .equals(new java.awt.Point(360, 240)),
                    "Salvar e carregar deve preservar as posicoes.");
        } catch (IOException exception) {
            throw new AssertionError("A persistencia do grafo deveria funcionar.", exception);
        } finally {
            if (path != null) {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException exception) {
                    throw new AssertionError("O arquivo temporario deveria ser removido.", exception);
                }
            }
        }
    }

    private static void testaCanvasSwing() {
        GraphDocument document = new GraphDocument(true);
        Vertice origem = document.addVertex();
        Vertice destino = document.addVertex();
        document.addEdge(origem, destino, 2.5);
        try {
            SwingUtilities.invokeAndWait(() -> {
                GraphCanvas canvas = new GraphCanvas(document);
                canvas.setSize(640, 480);
                canvas.setPosition(origem, new java.awt.Point(150, 200));
                canvas.setPosition(destino, new java.awt.Point(450, 200));
                BufferedImage image = new BufferedImage(
                        640, 480, BufferedImage.TYPE_INT_ARGB);
                Graphics2D graphics = image.createGraphics();
                try {
                    canvas.paint(graphics);
                } finally {
                    graphics.dispose();
                }
            });
        } catch (Exception exception) {
            throw new AssertionError("O canvas Swing deveria renderizar sem erros.", exception);
        }
    }

    private static AlgorithmRunner.Step lastStep(AlgorithmRunner.Trace trace) {
        exige(trace.isSuccessful(), "A execucao deveria terminar com sucesso.");
        return trace.getSteps().get(trace.getSteps().size() - 1);
    }

    private static List<Vertice> tracePath(AlgorithmRunner.Step step, Vertice destination) {
        List<Vertice> path = new java.util.ArrayList<>();
        Vertice current = destination;
        int remaining = step.getPredecessors().size();
        while (current != null && remaining-- > 0) {
            path.add(current);
            current = step.getPredecessors().get(current);
        }
        java.util.Collections.reverse(path);
        return path;
    }

    private static void esperaIllegalArgument(Runnable action) {
        try {
            action.run();
        } catch (IllegalArgumentException expected) {
            return;
        }
        throw new AssertionError("Era esperado IllegalArgumentException.");
    }

    private static void exige(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
