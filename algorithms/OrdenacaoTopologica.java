package algorithms;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import model.Digrafo;
import model.Vertice;

public final class OrdenacaoTopologica {
    private OrdenacaoTopologica() {
    }

    public static List<Vertice> ordenar(Digrafo digrafo) {
        if (digrafo == null) {
            throw new IllegalArgumentException("O digrafo nao pode ser nulo.");
        }

        List<Vertice> vertices = digrafo.getVertices();
        Map<Vertice, Integer> indices = new IdentityHashMap<>();
        int[] grausEntrada = new int[vertices.size()];
        for (int i = 0; i < vertices.size(); i++) {
            indices.put(vertices.get(i), i);
        }
        for (Vertice origem : vertices) {
            for (Vertice destino : digrafo.getSucessores(origem)) {
                Integer indiceDestino = indices.get(destino);
                if (indiceDestino == null) {
                    throw new IllegalArgumentException(
                            "Uma aresta referencia um vertice fora do digrafo.");
                }
                grausEntrada[indiceDestino]++;
            }
        }

        Queue<Integer> disponiveis = new ArrayDeque<>();
        for (int i = 0; i < grausEntrada.length; i++) {
            if (grausEntrada[i] == 0) {
                disponiveis.add(i);
            }
        }

        List<Vertice> ordem = new ArrayList<>(vertices.size());
        while (!disponiveis.isEmpty()) {
            int atual = disponiveis.remove();
            Vertice origem = vertices.get(atual);
            ordem.add(origem);
            for (Vertice destino : digrafo.getSucessores(origem)) {
                int indiceDestino = indices.get(destino);
                if (--grausEntrada[indiceDestino] == 0) {
                    disponiveis.add(indiceDestino);
                }
            }
        }
        if (ordem.size() != vertices.size()) {
            throw new IllegalStateException("O digrafo possui dependencias circulares.");
        }
        return Collections.unmodifiableList(ordem);
    }
}
