package model;

public class Aresta {
    public Vertice origem;
    public Vertice destino;
    public String rotulo;
    public double peso;

    public Aresta(Vertice origem, Vertice destino) {
        this("", 1.0, origem, destino);
    }

    public Aresta(String rotulo, Vertice origem, Vertice destino) {
        this(rotulo, 1.0, origem, destino);
    }

    public Aresta(double peso, Vertice origem, Vertice destino) {
        this("", peso, origem, destino);
    }

    public Aresta(String rotulo, double peso, Vertice origem, Vertice destino) {
        if (!Double.isFinite(peso)) {
            throw new IllegalArgumentException("O peso da aresta deve ser finito.");
        }
        this.rotulo = rotulo;
        this.peso = peso;
        this.origem = origem;
        this.destino = destino;
    }

    public boolean ehLoop() {
        return this.origem == this.destino;
    }

    public boolean ehParalelaA(Aresta b) {
        if (this == b) return false;
        return (this.origem == b.origem && this.destino == b.destino) ||
               (this.origem == b.destino && this.destino == b.origem);
    }

    public boolean ehAdjacenteA(Aresta b) {
        if (this == b) return false;
        return this.origem == b.origem || this.origem == b.destino ||
               this.destino == b.origem || this.destino == b.destino;
    }

    @Override
    public String toString() {
        return (rotulo != null && !rotulo.isEmpty()) ? rotulo : "(" + origem.dado + "," + destino.dado + ")";
    }
}