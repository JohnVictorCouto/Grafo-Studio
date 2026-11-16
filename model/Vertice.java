package model;

import java.util.ArrayList;

public class Vertice {
    String dado;
    ArrayList<Aresta> listaArestas;

    public Vertice(String dado, ArrayList<Aresta> listaArestas) {
        this.dado = dado;
        this.listaArestas = listaArestas;
    }

    public Vertice(String dado) {
        this.dado = dado;
        this.listaArestas = new ArrayList<Aresta>();
    }

    public void addAresta(Aresta a) {
        this.listaArestas.add(a);
    }

    public boolean removeAresta(Aresta a) {
        if (this.listaArestas.remove(a))
            return true;
        else
            return false;
    }

    @Override
    public String toString() {
        return dado;
    }
}