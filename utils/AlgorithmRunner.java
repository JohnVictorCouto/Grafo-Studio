package utils;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import model.Aresta;
import model.Grafo;
import model.Vertice;
import paths.Euleriano;
import paths.Hamiltoniano;

public final class AlgorithmRunner {
    private AlgorithmRunner() {
    }

    public enum Algorithm {
        BREADTH_FIRST("Busca em largura (BFS)"),
        DEPTH_FIRST("Busca em profundidade (DFS)"),
        DIJKSTRA("Dijkstra"),
        TOPOLOGICAL("Ordenacao topologica"),
        PRIM("Arvore geradora minima (Prim)"),
        KRUSKAL("Arvore geradora minima (Kruskal)"),
        MATCHING("Emparelhamento maximo"),
        EULER_TRAIL("Trilha euleriana"),
        EULER_CIRCUIT("Circuito euleriano"),
        HAMILTON_PATH("Caminho hamiltoniano"),
        HAMILTON_CYCLE("Ciclo hamiltoniano");

        private final String label;

        Algorithm(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public static Trace run(GraphDocument document, Algorithm algorithm, Vertice start) {
        if (document == null || algorithm == null) {
            throw new IllegalArgumentException("Documento e algoritmo sao obrigatorios.");
        }
        if (document.getVertices().isEmpty()) {
            throw new IllegalArgumentException("Adicione vertices antes de executar um algoritmo.");
        }
        if (algorithm == Algorithm.TOPOLOGICAL && !document.isDirected()) {
            throw new IllegalArgumentException(
                    "A ordenacao topologica requer um grafo direcionado.");
        }
        if (requiresUndirected(algorithm) && document.isDirected()) {
            throw new IllegalArgumentException(
                    "Este algoritmo requer um grafo nao direcionado.");
        }
        if (requiresStart(algorithm)
                && (start == null || !document.getVertices().contains(start))) {
            throw new IllegalArgumentException("Selecione um vertice inicial valido.");
        }

        Context context = createContext(document);
        if (algorithm == Algorithm.BREADTH_FIRST) {
            return breadthFirst(context, start);
        }
        if (algorithm == Algorithm.DEPTH_FIRST) {
            return depthFirst(context, start);
        }
        if (algorithm == Algorithm.DIJKSTRA) {
            return dijkstra(context, start);
        }
        if (algorithm == Algorithm.TOPOLOGICAL) {
            return topological(context);
        }
        if (algorithm == Algorithm.PRIM) {
            return prim(context, start);
        }
        if (algorithm == Algorithm.KRUSKAL) {
            return kruskal(context);
        }
        if (algorithm == Algorithm.MATCHING) {
            return matching(context);
        }
        if (algorithm == Algorithm.EULER_TRAIL || algorithm == Algorithm.EULER_CIRCUIT) {
            return euler(context, algorithm == Algorithm.EULER_CIRCUIT);
        }
        return hamilton(context, start, algorithm == Algorithm.HAMILTON_CYCLE);
    }

    private static boolean requiresStart(Algorithm algorithm) {
        return algorithm == Algorithm.BREADTH_FIRST
                || algorithm == Algorithm.DEPTH_FIRST
                || algorithm == Algorithm.DIJKSTRA
                || algorithm == Algorithm.PRIM
                || algorithm == Algorithm.HAMILTON_PATH
                || algorithm == Algorithm.HAMILTON_CYCLE;
    }

    private static boolean requiresUndirected(Algorithm algorithm) {
        return algorithm == Algorithm.PRIM
                || algorithm == Algorithm.KRUSKAL
                || algorithm == Algorithm.MATCHING
                || algorithm == Algorithm.EULER_TRAIL
                || algorithm == Algorithm.EULER_CIRCUIT
                || algorithm == Algorithm.HAMILTON_PATH
                || algorithm == Algorithm.HAMILTON_CYCLE;
    }

    private static Context createContext(GraphDocument document) {
        List<Vertice> vertices = document.getVertices();
        Map<Vertice, Integer> indices = new IdentityHashMap<>();
        for (int i = 0; i < vertices.size(); i++) {
            indices.put(vertices.get(i), i);
        }
        List<List<Arc>> adjacency = new ArrayList<>();
        for (int i = 0; i < vertices.size(); i++) {
            adjacency.add(new ArrayList<Arc>());
        }
        for (Aresta edge : document.getEdges()) {
            Integer origin = indices.get(edge.origem);
            Integer destination = indices.get(edge.destino);
            if (origin == null || destination == null) {
                throw new IllegalArgumentException("Uma aresta referencia um vertice inexistente.");
            }
            adjacency.get(origin).add(new Arc(destination, edge));
            if (!document.isDirected() && !origin.equals(destination)) {
                adjacency.get(destination).add(new Arc(origin, edge));
            }
        }
        return new Context(document, vertices, indices, adjacency,
                new ArrayList<>(document.getEdges()));
    }

    private static Trace breadthFirst(Context context, Vertice start) {
        int count = context.vertices.size();
        int source = context.indices.get(start);
        double[] distances = new double[count];
        int[] predecessors = new int[count];
        Arrays.fill(distances, Double.POSITIVE_INFINITY);
        Arrays.fill(predecessors, -1);

        Set<Vertice> discovered = new LinkedHashSet<>();
        Set<Vertice> processed = new LinkedHashSet<>();
        Queue<Integer> queue = new ArrayDeque<>();
        List<Step> steps = new ArrayList<>();
        distances[source] = 0;
        discovered.add(start);
        queue.add(source);
        steps.add(snapshot(context, "Inicio em " + start, start, null, null,
                discovered, processed, queue, distances, predecessors, null));

        while (!queue.isEmpty()) {
            int current = queue.remove();
            Vertice vertex = context.vertices.get(current);
            processed.add(vertex);
            steps.add(snapshot(context, "Visitando " + vertex, vertex, null, null,
                    discovered, processed, queue, distances, predecessors, null));

            for (Arc arc : context.adjacency.get(current)) {
                Vertice neighbor = context.vertices.get(arc.destination);
                String description;
                if (!discovered.contains(neighbor)) {
                    discovered.add(neighbor);
                    distances[arc.destination] = distances[current] + 1;
                    predecessors[arc.destination] = current;
                    queue.add(arc.destination);
                    description = "Descoberto " + neighbor + " a "
                            + formatNumber(distances[arc.destination]) + " aresta(s) da origem.";
                } else {
                    description = "Aresta para " + neighbor + "; o vertice ja foi descoberto.";
                }
                steps.add(snapshot(context, description, vertex, vertex, neighbor,
                        discovered, processed, queue, distances, predecessors, null));
            }
        }
        return new Trace(Algorithm.BREADTH_FIRST, steps,
                "BFS concluida. Distancias representam quantidade de arestas.", true);
    }

    private static Trace depthFirst(Context context, Vertice start) {
        int[] predecessors = new int[context.vertices.size()];
        Arrays.fill(predecessors, -1);
        Set<Vertice> discovered = new LinkedHashSet<>();
        Set<Vertice> processed = new LinkedHashSet<>();
        Deque<Integer> stack = new ArrayDeque<>();
        List<Step> steps = new ArrayList<>();
        int source = context.indices.get(start);
        depthFirstVisit(context, source, discovered, processed, stack, predecessors, steps);
        return new Trace(Algorithm.DEPTH_FIRST, steps,
                "DFS concluida a partir de " + start + ".", true);
    }

    private static void depthFirstVisit(Context context, int current,
            Set<Vertice> discovered, Set<Vertice> processed, Deque<Integer> stack,
            int[] predecessors, List<Step> steps) {
        Vertice vertex = context.vertices.get(current);
        discovered.add(vertex);
        stack.push(current);
        steps.add(snapshot(context, "Visitando " + vertex + ".", vertex, null, null,
                discovered, processed, stack, null, predecessors, null));

        for (Arc arc : context.adjacency.get(current)) {
            Vertice neighbor = context.vertices.get(arc.destination);
            if (!discovered.contains(neighbor)) {
                predecessors[arc.destination] = current;
                steps.add(snapshot(context, "Explorando " + vertex + " -> " + neighbor + ".",
                        vertex, vertex, neighbor, discovered, processed, stack,
                        null, predecessors, null));
                depthFirstVisit(context, arc.destination, discovered, processed,
                        stack, predecessors, steps);
            } else {
                steps.add(snapshot(context, "Aresta para " + neighbor + " ja descoberta.",
                        vertex, vertex, neighbor, discovered, processed, stack,
                        null, predecessors, null));
            }
        }

        processed.add(vertex);
        stack.pop();
        steps.add(snapshot(context, "Finalizando " + vertex + ".", vertex, null, null,
                discovered, processed, stack, null, predecessors, null));
    }

    private static Trace dijkstra(Context context, Vertice start) {
        for (List<Arc> arcs : context.adjacency) {
            for (Arc arc : arcs) {
                if (arc.edge.peso < 0) {
                    throw new IllegalArgumentException(
                            "Dijkstra nao aceita arestas com peso negativo.");
                }
            }
        }

        int count = context.vertices.size();
        int source = context.indices.get(start);
        double[] distances = new double[count];
        int[] predecessors = new int[count];
        Arrays.fill(distances, Double.POSITIVE_INFINITY);
        Arrays.fill(predecessors, -1);
        Set<Vertice> discovered = new LinkedHashSet<>();
        Set<Vertice> processed = new LinkedHashSet<>();
        PriorityQueue<QueueEntry> queue = new PriorityQueue<>(
                Comparator.comparingDouble(entry -> entry.distance));
        List<Step> steps = new ArrayList<>();
        distances[source] = 0;
        discovered.add(start);
        queue.add(new QueueEntry(source, 0));
        steps.add(snapshot(context, "Inicio em " + start + " com custo zero.",
                start, null, null, discovered, processed, queueVertices(queue),
                distances, predecessors, null));

        while (!queue.isEmpty()) {
            QueueEntry entry = queue.remove();
            if (Double.compare(entry.distance, distances[entry.vertex]) != 0
                    || processed.contains(context.vertices.get(entry.vertex))) {
                continue;
            }
            int current = entry.vertex;
            Vertice vertex = context.vertices.get(current);
            processed.add(vertex);
            steps.add(snapshot(context, "Fixado o menor custo de " + vertex + ".",
                    vertex, null, null, discovered, processed, queueVertices(queue),
                    distances, predecessors, null));

            for (Arc arc : context.adjacency.get(current)) {
                Vertice neighbor = context.vertices.get(arc.destination);
                double candidate = distances[current] + arc.edge.peso;
                String description;
                if (!processed.contains(neighbor) && candidate < distances[arc.destination]) {
                    distances[arc.destination] = candidate;
                    predecessors[arc.destination] = current;
                    discovered.add(neighbor);
                    queue.add(new QueueEntry(arc.destination, candidate));
                    description = "Relaxada a aresta " + vertex + " -> " + neighbor
                            + "; novo custo " + formatNumber(candidate) + ".";
                } else {
                    description = "Aresta " + vertex + " -> " + neighbor
                            + " nao melhora o custo conhecido.";
                }
                steps.add(snapshot(context, description, vertex, vertex, neighbor,
                        discovered, processed, queueVertices(queue), distances, predecessors, null));
            }
        }
        return new Trace(Algorithm.DIJKSTRA, steps,
                "Dijkstra concluido. Custos exibidos sao relativos a " + start + ".", true);
    }

    private static Trace topological(Context context) {
        int count = context.vertices.size();
        int[] indegrees = new int[count];
        for (List<Arc> arcs : context.adjacency) {
            for (Arc arc : arcs) {
                indegrees[arc.destination]++;
            }
        }

        Queue<Integer> queue = new ArrayDeque<>();
        Set<Vertice> discovered = new LinkedHashSet<>();
        Set<Vertice> processed = new LinkedHashSet<>();
        List<Vertice> order = new ArrayList<>();
        List<Step> steps = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            if (indegrees[i] == 0) {
                queue.add(i);
                discovered.add(context.vertices.get(i));
            }
        }
        steps.add(snapshot(context, "Fila inicial: vertices com grau de entrada zero.",
                null, null, null, discovered, processed, queue, null, null, order));

        while (!queue.isEmpty()) {
            int current = queue.remove();
            Vertice vertex = context.vertices.get(current);
            processed.add(vertex);
            order.add(vertex);
            steps.add(snapshot(context, "Incluido " + vertex + " na ordem topologica.",
                    vertex, null, null, discovered, processed, queue, null, null, order));

            for (Arc arc : context.adjacency.get(current)) {
                Vertice neighbor = context.vertices.get(arc.destination);
                indegrees[arc.destination]--;
                if (indegrees[arc.destination] == 0) {
                    queue.add(arc.destination);
                    discovered.add(neighbor);
                }
                steps.add(snapshot(context, "Removida a dependencia " + vertex + " -> "
                                + neighbor + "; grau de entrada restante: "
                                + indegrees[arc.destination] + ".",
                        vertex, vertex, neighbor, discovered, processed, queue,
                        null, null, order));
            }
        }

        if (order.size() != count) {
            Set<Vertice> bloqueados = new LinkedHashSet<>(context.vertices);
            bloqueados.removeAll(processed);
            String summary = "Ciclo detectado. Vertices que permaneceram bloqueados: "
                    + joinVertices(bloqueados) + ".";
            steps.add(snapshot(context, summary, null, null, null,
                    discovered, processed, queue, null, null, order));
            return new Trace(Algorithm.TOPOLOGICAL, steps, summary, false);
        }
        return new Trace(Algorithm.TOPOLOGICAL, steps,
                "Ordenacao topologica: " + joinVertices(order) + ".", true);
    }

    private static Trace prim(Context context, Vertice start) {
        int source = context.indices.get(start);
        Set<Vertice> includedVertices = new LinkedHashSet<>();
        Set<Aresta> includedEdges = new LinkedHashSet<>();
        List<Step> steps = new ArrayList<>();
        PriorityQueue<CandidateEdge> candidates = new PriorityQueue<>(
                Comparator.comparingDouble(candidate -> candidate.edge.peso));
        includedVertices.add(start);
        addCandidates(context, source, includedVertices, candidates);
        steps.add(snapshot(context, "Prim inicia em " + start + ".",
                start, null, null, includedVertices, includedVertices,
                candidateVertices(context, candidates), null, null, null,
                includedEdges, Collections.<Vertice>emptyList()));

        while (!candidates.isEmpty() && includedEdges.size() < context.vertices.size() - 1) {
            CandidateEdge candidate = candidates.remove();
            Vertice from = context.vertices.get(candidate.from);
            Vertice to = context.vertices.get(candidate.to);
            if (includedVertices.contains(to)) {
                steps.add(snapshot(context, "Ignorando a aresta " + from + " -- " + to
                                + ": ela formaria um ciclo.",
                        from, from, to, includedVertices, includedVertices,
                        candidateVertices(context, candidates), null, null, null,
                        includedEdges, Collections.<Vertice>emptyList()));
                continue;
            }

            includedEdges.add(candidate.edge);
            includedVertices.add(to);
            addCandidates(context, candidate.to, includedVertices, candidates);
            steps.add(snapshot(context, "Incluida " + from + " -- " + to
                            + " com peso " + formatNumber(candidate.edge.peso) + ".",
                    to, from, to, includedVertices, includedVertices,
                    candidateVertices(context, candidates), null, null, null,
                    includedEdges, Collections.<Vertice>emptyList()));
        }

        boolean connected = includedEdges.size() == context.vertices.size() - 1;
        String summary = connected
                ? "Arvore geradora minima encontrada. Peso total: "
                        + formatNumber(totalWeight(includedEdges)) + "."
                : "O grafo e desconexo; nao existe uma arvore geradora para todos os vertices.";
        return new Trace(Algorithm.PRIM, steps, summary, connected);
    }

    private static void addCandidates(Context context, int origin,
            Set<Vertice> includedVertices, PriorityQueue<CandidateEdge> candidates) {
        for (Arc arc : context.adjacency.get(origin)) {
            Vertice destination = context.vertices.get(arc.destination);
            if (!includedVertices.contains(destination)) {
                candidates.add(new CandidateEdge(origin, arc.destination, arc.edge));
            }
        }
    }

    private static Collection<Integer> candidateVertices(
            Context context, Collection<CandidateEdge> candidates) {
        List<CandidateEdge> ordered = new ArrayList<>(candidates);
        ordered.sort(Comparator.comparingDouble(candidate -> candidate.edge.peso));
        List<Integer> vertices = new ArrayList<>();
        for (CandidateEdge candidate : ordered) {
            if (!vertices.contains(candidate.to)) {
                vertices.add(candidate.to);
            }
        }
        return vertices;
    }

    private static Trace kruskal(Context context) {
        List<Aresta> candidates = new ArrayList<>(context.edges);
        candidates.sort(Comparator.comparingDouble(edge -> edge.peso));
        DisjointSet components = new DisjointSet(context.vertices.size());
        Set<Aresta> includedEdges = new LinkedHashSet<>();
        Set<Vertice> includedVertices = new LinkedHashSet<>();
        List<Step> steps = new ArrayList<>();
        steps.add(snapshot(context, "Kruskal processa as arestas em ordem crescente de peso.",
                null, null, null, includedVertices, includedVertices,
                Collections.<Integer>emptyList(), null, null, null,
                includedEdges, Collections.<Vertice>emptyList()));
        for (Aresta edge : candidates) {
            int origin = context.indices.get(edge.origem);
            int destination = context.indices.get(edge.destino);
            Vertice from = context.vertices.get(origin);
            Vertice to = context.vertices.get(destination);
            if (components.union(origin, destination)) {
                includedEdges.add(edge);
                includedVertices.add(from);
                includedVertices.add(to);
                steps.add(snapshot(context, "Incluida " + from + " -- " + to
                                + " com peso " + formatNumber(edge.peso) + ".",
                        to, from, to, includedVertices, includedVertices,
                        Collections.singletonList(destination), null, null, null,
                        includedEdges, Collections.<Vertice>emptyList()));
            } else {
                steps.add(snapshot(context, "Ignorada " + from + " -- " + to
                                + ": os vertices ja estao conectados.",
                        from, from, to, includedVertices, includedVertices,
                        Collections.singletonList(destination), null, null, null,
                        includedEdges, Collections.<Vertice>emptyList()));
            }
            if (includedEdges.size() == context.vertices.size() - 1) {
                break;
            }
        }

        boolean connected = includedEdges.size() == context.vertices.size() - 1;
        String summary = connected
                ? "Arvore geradora minima encontrada. Peso total: "
                        + formatNumber(totalWeight(includedEdges)) + "."
                : "O grafo e desconexo; nao existe uma arvore geradora para todos os vertices.";
        return new Trace(Algorithm.KRUSKAL, steps, summary, connected);
    }

    private static double totalWeight(Collection<Aresta> edges) {
        double total = 0;
        for (Aresta edge : edges) {
            total += edge.peso;
        }
        return total;
    }

    private static Trace matching(Context context) {
        int[] colors = bipartition(context);
        List<Step> steps = new ArrayList<>();
        if (colors == null) {
            String summary = "O grafo nao e bipartido; nao e possivel calcular este emparelhamento.";
            steps.add(snapshot(context, summary, null, null, null,
                    Collections.<Vertice>emptySet(), Collections.<Vertice>emptySet(),
                    Collections.<Integer>emptyList(), null, null, null));
            return new Trace(Algorithm.MATCHING, steps, summary, false);
        }

        int[] partnerOfRight = new int[context.vertices.size()];
        Arrays.fill(partnerOfRight, -1);
        Set<Vertice> processed = new LinkedHashSet<>();
        Set<Vertice> discovered = new LinkedHashSet<>();
        for (int left = 0; left < colors.length; left++) {
            if (colors[left] != 0) {
                continue;
            }
            Vertice vertex = context.vertices.get(left);
            tryAugment(context, left, colors, new boolean[colors.length],
                    partnerOfRight, discovered, processed, steps);
            processed.add(vertex);
        }

        Set<Aresta> resultEdges = matchingEdges(context, colors, partnerOfRight);
        int size = resultEdges.size();
        String summary = "Emparelhamento maximo encontrado: " + size + " aresta(s).";
        steps.add(snapshot(context, summary, null, null, null,
                matchedVertices(context, resultEdges), processed,
                Collections.<Integer>emptyList(), null, null, null,
                resultEdges, Collections.<Vertice>emptyList()));
        return new Trace(Algorithm.MATCHING, steps, summary, true);
    }

    private static int[] bipartition(Context context) {
        int[] colors = new int[context.vertices.size()];
        Arrays.fill(colors, -1);
        Queue<Integer> queue = new ArrayDeque<>();
        for (int start = 0; start < colors.length; start++) {
            if (colors[start] != -1) {
                continue;
            }
            colors[start] = 0;
            queue.add(start);
            while (!queue.isEmpty()) {
                int current = queue.remove();
                for (Arc arc : context.adjacency.get(current)) {
                    if (colors[arc.destination] == -1) {
                        colors[arc.destination] = 1 - colors[current];
                        queue.add(arc.destination);
                    } else if (colors[arc.destination] == colors[current]) {
                        return null;
                    }
                }
            }
        }
        return colors;
    }

    private static boolean tryAugment(Context context, int left, int[] colors,
            boolean[] visitedRight, int[] partnerOfRight, Set<Vertice> discovered,
            Set<Vertice> processed, List<Step> steps) {
        Vertice leftVertex = context.vertices.get(left);
        for (Arc arc : context.adjacency.get(left)) {
            int right = arc.destination;
            if (colors[right] != 1 || visitedRight[right]) {
                continue;
            }
            visitedRight[right] = true;
            Vertice rightVertex = context.vertices.get(right);
            steps.add(snapshot(context, "Testando compatibilidade "
                            + leftVertex + " -- " + rightVertex + ".",
                    leftVertex, leftVertex, rightVertex, discovered, processed,
                    Collections.singletonList(right), null, null, null,
                    matchingEdges(context, colors, partnerOfRight),
                    Collections.<Vertice>emptyList()));
            if (partnerOfRight[right] == -1
                    || tryAugment(context, partnerOfRight[right], colors,
                            visitedRight, partnerOfRight, discovered, processed, steps)) {
                partnerOfRight[right] = left;
                discovered.add(leftVertex);
                discovered.add(rightVertex);
                steps.add(snapshot(context, "Atribuido " + leftVertex + " a "
                                + rightVertex + ".",
                        leftVertex, leftVertex, rightVertex, discovered, processed,
                        Collections.<Integer>emptyList(), null, null, null,
                        matchingEdges(context, colors, partnerOfRight),
                        Collections.<Vertice>emptyList()));
                return true;
            }
        }
        return false;
    }

    private static Set<Aresta> matchingEdges(Context context, int[] colors, int[] partnerOfRight) {
        Set<Aresta> selected = new LinkedHashSet<>();
        for (Aresta edge : context.edges) {
            int from = context.indices.get(edge.origem);
            int to = context.indices.get(edge.destino);
            if (colors[from] == 0 && colors[to] == 1 && partnerOfRight[to] == from
                    || colors[to] == 0 && colors[from] == 1 && partnerOfRight[from] == to) {
                selected.add(edge);
            }
        }
        return selected;
    }

    private static Set<Vertice> matchedVertices(Context context, Set<Aresta> edges) {
        Set<Vertice> matched = new LinkedHashSet<>();
        for (Aresta edge : edges) {
            matched.add(edge.origem);
            matched.add(edge.destino);
        }
        return matched;
    }

    private static Trace euler(Context context, boolean circuit) {
        Grafo graph = context.document.toUndirectedGraph();
        java.util.Optional<List<Vertice>> result = circuit
                ? Euleriano.encontrarCircuitoEuleriano(graph)
                : Euleriano.encontrarTrilhaEuleriana(graph);
        List<Step> steps = new ArrayList<>();
        if (!result.isPresent()) {
            String summary = circuit
                    ? "O grafo nao possui circuito euleriano."
                    : "O grafo nao possui trilha euleriana.";
            steps.add(snapshot(context, summary, null, null, null,
                    Collections.<Vertice>emptySet(), Collections.<Vertice>emptySet(),
                    Collections.<Integer>emptyList(), null, null, null));
            return new Trace(circuit ? Algorithm.EULER_CIRCUIT : Algorithm.EULER_TRAIL,
                    steps, summary, false);
        }

        List<Vertice> route = result.get();
        Set<Aresta> usedEdges = new LinkedHashSet<>();
        Set<Vertice> visited = new LinkedHashSet<>();
        if (!route.isEmpty()) {
            visited.add(route.get(0));
            steps.add(snapshot(context, "Inicio da trilha em " + route.get(0) + ".",
                    route.get(0), null, null, visited, visited,
                    Collections.<Integer>emptyList(), null, null, null,
                    usedEdges, route.subList(0, 1)));
        }
        for (int i = 1; i < route.size(); i++) {
            Vertice from = route.get(i - 1);
            Vertice to = route.get(i);
            Aresta edge = findUnusedEdge(context, from, to, usedEdges);
            if (edge == null) {
                String summary = "Nao foi possivel reconstruir uma aresta da trilha.";
                return new Trace(circuit ? Algorithm.EULER_CIRCUIT : Algorithm.EULER_TRAIL,
                        steps, summary, false);
            }
            usedEdges.add(edge);
            visited.add(to);
            steps.add(snapshot(context, "Percorrida a aresta " + from + " -- " + to + ".",
                    to, from, to, visited, visited, Collections.<Integer>emptyList(),
                    null, null, null, usedEdges, route.subList(0, i + 1)));
        }
        String summary = (circuit ? "Circuito" : "Trilha")
                + " euleriano: " + joinVertices(route) + ".";
        return new Trace(circuit ? Algorithm.EULER_CIRCUIT : Algorithm.EULER_TRAIL,
                steps, summary, true);
    }

    private static Aresta findUnusedEdge(Context context, Vertice from,
            Vertice to, Set<Aresta> usedEdges) {
        for (Aresta edge : context.edges) {
            if (usedEdges.contains(edge)) {
                continue;
            }
            if (edge.origem == from && edge.destino == to
                    || edge.origem == to && edge.destino == from) {
                return edge;
            }
        }
        return null;
    }

    private static Trace hamilton(Context context, Vertice start, boolean cycle) {
        int count = context.vertices.size();
        if (count > 12) {
            String summary = "A busca exata foi limitada a grafos com ate 12 vertices.";
            List<Step> steps = new ArrayList<>();
            steps.add(snapshot(context, summary, start, null, null,
                    Collections.singleton(start), Collections.<Vertice>emptySet(),
                    Collections.<Integer>emptyList(), null, null, null));
            return new Trace(cycle ? Algorithm.HAMILTON_CYCLE : Algorithm.HAMILTON_PATH,
                    steps, summary, false);
        }
        if (cycle && count < 3) {
            String summary = "Um ciclo hamiltoniano requer pelo menos 3 vertices.";
            List<Step> steps = new ArrayList<>();
            steps.add(snapshot(context, summary, start, null, null,
                    Collections.singleton(start), Collections.<Vertice>emptySet(),
                    Collections.<Integer>emptyList(), null, null, null));
            return new Trace(Algorithm.HAMILTON_CYCLE, steps, summary, false);
        }

        int source = context.indices.get(start);
        boolean[] visited = new boolean[count];
        List<Integer> path = new ArrayList<>();
        List<Step> steps = new ArrayList<>();
        int[] explored = {0};
        visited[source] = true;
        path.add(source);
        steps.add(snapshot(context, "Busca iniciada em " + start + ".",
                start, null, null, Collections.singleton(start),
                Collections.<Vertice>emptySet(), Collections.<Integer>emptyList(),
                null, null, null, Collections.<Aresta>emptySet(),
                Collections.singletonList(start)));

        boolean found = searchHamilton(context, source, source, cycle, visited,
                path, explored, steps);
        List<Vertice> route = verticesForPath(context, path);
        String summary;
        boolean complete = found || explored[0] <= 5000;
        if (found) {
            summary = (cycle ? "Ciclo" : "Caminho") + " hamiltoniano: "
                    + joinVertices(route) + ".";
        } else if (complete) {
            summary = cycle ? "Nao foi encontrado ciclo hamiltoniano."
                    : "Nao foi encontrado caminho hamiltoniano a partir de " + start + ".";
        } else {
            summary = "Busca interrompida ao atingir o limite de exploracao.";
        }
        steps.add(snapshot(context, summary, null, null, null,
                new LinkedHashSet<>(route), Collections.<Vertice>emptySet(),
                Collections.<Integer>emptyList(), null, null, null,
                routeEdges(context, route), route));
        return new Trace(cycle ? Algorithm.HAMILTON_CYCLE : Algorithm.HAMILTON_PATH,
                steps, summary, found);
    }

    private static boolean searchHamilton(Context context, int current, int start,
            boolean cycle, boolean[] visited, List<Integer> path,
            int[] explored, List<Step> steps) {
        if (path.size() == visited.length) {
            if (!cycle || hasArc(context, current, start)) {
                if (cycle) {
                    path.add(start);
                }
                List<Vertice> route = verticesForPath(context, path);
                steps.add(snapshot(context, "Todos os vertices foram visitados.",
                        context.vertices.get(current), context.vertices.get(current),
                        cycle ? context.vertices.get(start) : null,
                        new LinkedHashSet<>(route), new LinkedHashSet<>(route),
                        Collections.<Integer>emptyList(), null, null, null,
                        routeEdges(context, route), route));
                return true;
            }
            return false;
        }
        if (++explored[0] > 5000) {
            return false;
        }

        for (Arc arc : context.adjacency.get(current)) {
            if (visited[arc.destination]) {
                continue;
            }
            visited[arc.destination] = true;
            path.add(arc.destination);
            List<Vertice> route = verticesForPath(context, path);
            steps.add(snapshot(context, "Avancando para "
                            + context.vertices.get(arc.destination) + ".",
                    context.vertices.get(arc.destination), context.vertices.get(current),
                    context.vertices.get(arc.destination), new LinkedHashSet<>(route),
                    Collections.<Vertice>emptySet(), Collections.<Integer>emptyList(),
                    null, null, null, routeEdges(context, route), route));
            if (searchHamilton(context, arc.destination, start, cycle,
                    visited, path, explored, steps)) {
                return true;
            }
            path.remove(path.size() - 1);
            visited[arc.destination] = false;
            route = verticesForPath(context, path);
            steps.add(snapshot(context, "Retornando de "
                            + context.vertices.get(arc.destination) + ".",
                    context.vertices.get(current), context.vertices.get(current),
                    context.vertices.get(arc.destination), new LinkedHashSet<>(route),
                    Collections.<Vertice>emptySet(), Collections.<Integer>emptyList(),
                    null, null, null, routeEdges(context, route), route));
            if (explored[0] > 5000) {
                return false;
            }
        }
        return false;
    }

    private static boolean hasArc(Context context, int from, int to) {
        for (Arc arc : context.adjacency.get(from)) {
            if (arc.destination == to) {
                return true;
            }
        }
        return false;
    }

    private static List<Vertice> verticesForPath(Context context, List<Integer> path) {
        List<Vertice> vertices = new ArrayList<>();
        for (int index : path) {
            vertices.add(context.vertices.get(index));
        }
        return vertices;
    }

    private static Set<Aresta> routeEdges(Context context, List<Vertice> route) {
        Set<Aresta> used = new LinkedHashSet<>();
        for (int i = 1; i < route.size(); i++) {
            Aresta edge = findUnusedEdge(context, route.get(i - 1), route.get(i), used);
            if (edge != null) {
                used.add(edge);
            }
        }
        return used;
    }

    private static Step snapshot(Context context, String description, Vertice current,
            Vertice edgeFrom, Vertice edgeTo, Collection<Vertice> discovered,
            Collection<Vertice> processed, Collection<Integer> frontier,
            double[] distances, int[] predecessors, List<Vertice> order) {
        return snapshot(context, description, current, edgeFrom, edgeTo,
                discovered, processed, frontier, distances, predecessors, order,
                Collections.<Aresta>emptySet(), Collections.<Vertice>emptyList());
    }

    private static Step snapshot(Context context, String description, Vertice current,
            Vertice edgeFrom, Vertice edgeTo, Collection<Vertice> discovered,
            Collection<Vertice> processed, Collection<Integer> frontier,
            double[] distances, int[] predecessors, List<Vertice> order,
            Collection<Aresta> includedEdges, Collection<Vertice> route) {
        Set<Vertice> frontierVertices = new LinkedHashSet<>();
        List<Vertice> frontierOrder = new ArrayList<>();
        for (int index : frontier) {
            Vertice vertex = context.vertices.get(index);
            frontierVertices.add(vertex);
            frontierOrder.add(vertex);
        }

        Map<Vertice, Double> distanceMap = new LinkedHashMap<>();
        Map<Vertice, Vertice> predecessorMap = new LinkedHashMap<>();
        if (distances != null) {
            for (int i = 0; i < context.vertices.size(); i++) {
                distanceMap.put(context.vertices.get(i), distances[i]);
            }
        }
        if (predecessors != null) {
            for (int i = 0; i < context.vertices.size(); i++) {
                int predecessor = predecessors[i];
                predecessorMap.put(context.vertices.get(i),
                        predecessor == -1 ? null : context.vertices.get(predecessor));
            }
        }
        return new Step(description, current, edgeFrom, edgeTo,
                discovered, processed, frontierVertices, distanceMap,
                predecessorMap, order == null ? Collections.<Vertice>emptyList() : order,
                frontierOrder, includedEdges, route);
    }

    private static String joinVertices(Collection<Vertice> vertices) {
        StringBuilder text = new StringBuilder();
        for (Vertice vertex : vertices) {
            if (text.length() > 0) {
                text.append(", ");
            }
            text.append(vertex);
        }
        return text.toString();
    }

    private static Collection<Integer> queueVertices(Collection<QueueEntry> entries) {
        List<QueueEntry> ordered = new ArrayList<>(entries);
        ordered.sort(Comparator.comparingDouble(entry -> entry.distance));
        List<Integer> vertices = new ArrayList<>();
        for (QueueEntry entry : ordered) {
            vertices.add(entry.vertex);
        }
        return vertices;
    }

    private static String formatNumber(double value) {
        if (value == Math.rint(value)) {
            return Long.toString((long) value);
        }
        return Double.toString(value);
    }

    private static final class Context {
        private final GraphDocument document;
        private final List<Vertice> vertices;
        private final Map<Vertice, Integer> indices;
        private final List<List<Arc>> adjacency;
        private final List<Aresta> edges;

        private Context(GraphDocument document, List<Vertice> vertices,
                Map<Vertice, Integer> indices, List<List<Arc>> adjacency, List<Aresta> edges) {
            this.document = document;
            this.vertices = vertices;
            this.indices = indices;
            this.adjacency = adjacency;
            this.edges = edges;
        }
    }

    private static final class Arc {
        private final int destination;
        private final Aresta edge;

        private Arc(int destination, Aresta edge) {
            this.destination = destination;
            this.edge = edge;
        }
    }

    private static final class QueueEntry {
        private final int vertex;
        private final double distance;

        private QueueEntry(int vertex, double distance) {
            this.vertex = vertex;
            this.distance = distance;
        }
    }

    private static final class CandidateEdge {
        private final int from;
        private final int to;
        private final Aresta edge;

        private CandidateEdge(int from, int to, Aresta edge) {
            this.from = from;
            this.to = to;
            this.edge = edge;
        }
    }

    private static final class DisjointSet {
        private final int[] parent;
        private final int[] rank;

        private DisjointSet(int size) {
            parent = new int[size];
            rank = new int[size];
            for (int i = 0; i < size; i++) {
                parent[i] = i;
            }
        }

        private int find(int value) {
            if (parent[value] != value) {
                parent[value] = find(parent[value]);
            }
            return parent[value];
        }

        private boolean union(int left, int right) {
            int rootLeft = find(left);
            int rootRight = find(right);
            if (rootLeft == rootRight) {
                return false;
            }
            if (rank[rootLeft] < rank[rootRight]) {
                parent[rootLeft] = rootRight;
            } else if (rank[rootLeft] > rank[rootRight]) {
                parent[rootRight] = rootLeft;
            } else {
                parent[rootRight] = rootLeft;
                rank[rootLeft]++;
            }
            return true;
        }
    }

    public static final class Trace {
        private final Algorithm algorithm;
        private final List<Step> steps;
        private final String summary;
        private final boolean successful;

        private Trace(Algorithm algorithm, List<Step> steps, String summary, boolean successful) {
            this.algorithm = algorithm;
            this.steps = Collections.unmodifiableList(new ArrayList<>(steps));
            this.summary = summary;
            this.successful = successful;
        }

        public Algorithm getAlgorithm() {
            return algorithm;
        }

        public List<Step> getSteps() {
            return steps;
        }

        public String getSummary() {
            return summary;
        }

        public boolean isSuccessful() {
            return successful;
        }
    }

    public static final class Step {
        private final String description;
        private final Vertice current;
        private final Vertice edgeFrom;
        private final Vertice edgeTo;
        private final Set<Vertice> discovered;
        private final Set<Vertice> processed;
        private final Set<Vertice> frontier;
        private final Map<Vertice, Double> distances;
        private final Map<Vertice, Vertice> predecessors;
        private final List<Vertice> topologicalOrder;
        private final List<Vertice> frontierOrder;
        private final Set<Aresta> includedEdges;
        private final List<Vertice> route;

        private Step(String description, Vertice current, Vertice edgeFrom, Vertice edgeTo,
                Collection<Vertice> discovered, Collection<Vertice> processed,
                Collection<Vertice> frontier, Map<Vertice, Double> distances,
                Map<Vertice, Vertice> predecessors, Collection<Vertice> topologicalOrder,
                Collection<Vertice> frontierOrder, Collection<Aresta> includedEdges,
                Collection<Vertice> route) {
            this.description = description;
            this.current = current;
            this.edgeFrom = edgeFrom;
            this.edgeTo = edgeTo;
            this.discovered = Collections.unmodifiableSet(new LinkedHashSet<>(discovered));
            this.processed = Collections.unmodifiableSet(new LinkedHashSet<>(processed));
            this.frontier = Collections.unmodifiableSet(new LinkedHashSet<>(frontier));
            this.distances = Collections.unmodifiableMap(new LinkedHashMap<>(distances));
            this.predecessors = Collections.unmodifiableMap(new LinkedHashMap<>(predecessors));
            this.topologicalOrder = Collections.unmodifiableList(new ArrayList<>(topologicalOrder));
            this.frontierOrder = Collections.unmodifiableList(new ArrayList<>(frontierOrder));
            this.includedEdges = Collections.unmodifiableSet(new LinkedHashSet<>(includedEdges));
            this.route = Collections.unmodifiableList(new ArrayList<>(route));
        }

        public String getDescription() {
            return description;
        }

        public Vertice getCurrent() {
            return current;
        }

        public Vertice getEdgeFrom() {
            return edgeFrom;
        }

        public Vertice getEdgeTo() {
            return edgeTo;
        }

        public Set<Vertice> getDiscovered() {
            return discovered;
        }

        public Set<Vertice> getProcessed() {
            return processed;
        }

        public Set<Vertice> getFrontier() {
            return frontier;
        }

        public Map<Vertice, Double> getDistances() {
            return distances;
        }

        public Map<Vertice, Vertice> getPredecessors() {
            return predecessors;
        }

        public List<Vertice> getTopologicalOrder() {
            return topologicalOrder;
        }

        public List<Vertice> getFrontierOrder() {
            return frontierOrder;
        }

        public Set<Aresta> getIncludedEdges() {
            return includedEdges;
        }

        public List<Vertice> getRoute() {
            return route;
        }
    }
}
