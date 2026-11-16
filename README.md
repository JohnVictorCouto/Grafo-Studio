# Grafo Studio

Implementacoes em Java para modelagem e algoritmos de grafos. Escolha primeiro
os vertices, conexoes, direcao e pesos que representam o problema; depois
selecione o algoritmo compativel com a pergunta e suas restricoes.

## Modelos

- `model.Grafo`: grafo nao direcionado, armazenado por listas de adjacencia.
  Arestas podem ter pesos; as operacoes existentes tambem permitem consultar
  matriz de adjacencia e propriedades estruturais.
- `model.Digrafo`: grafo direcionado com lista de sucessores, apropriado para
  dependencias e ordenacao topologica.
- `model.Vertice` e `model.Aresta`: representam entidades e conexoes. No
  `Grafo`, uma aresta inserida conecta os dois extremos; no `Digrafo`, ela vai
  apenas da origem ao destino.

Use listas quando o grafo for esparso e for importante percorrer vizinhos.
Uma matriz pode ser conveniente para grafos pequenos/densos e consultas
frequentes de adjacencia, ao custo de memoria quadratica.

## Algoritmos

| Necessidade | Implementacao | Observacoes |
| --- | --- | --- |
| Percorrer vertices | `BuscaEmLargura` e `BuscaEmProfundidade` | BFS tambem calcula distancias em arestas e reconstrui caminhos. |
| Menor quantidade de arestas | `BuscaEmLargura.calcularMenoresCaminhos` | Adequada quando cada conexao tem o mesmo custo. Distancia `-1` indica destino inalcançavel. |
| Menor custo acumulado | `Dijkstra` | Requer pesos nao negativos; retorna distancias e caminhos. |
| Ordenar dependencias | `OrdenacaoTopologica` | Kahn em `Digrafo`; informa ciclo com `IllegalStateException`. |
| Maximizar atribuicoes | `Emparelhamento` | Inclui uma busca aumentante para grafos bipartidos e uma implementacao geral. |
| Percorrer todas as arestas | `Euleriano` | Verifica trilhas e circuitos eulerianos. |
| Visitar todos os vertices | `Hamiltoniano` | Busca exata; o tempo pode crescer exponencialmente. |
| Arvore geradora minima | `Prim` e `Kruskal` | Para grafos ponderados e conexos. |

BFS e ordenacao topologica operam em tempo O(V + E) com listas de adjacencia.
Dijkstra usa fila de prioridade, com custo O((V + E) log V). A busca
aumentante bipartida tem custo O(VE). Algoritmos Hamiltonianos exatos nao sao
adequados a grafos grandes sem limites ou heuristicas.

## Exemplo de uso

```java
import algorithms.BuscaEmLargura;
import algorithms.OrdenacaoTopologica;
import java.util.List;
import java.util.Optional;
import model.Digrafo;
import model.Grafo;
import model.Vertice;

Grafo rede = new Grafo();
Vertice origem = new Vertice("Origem");
Vertice destino = new Vertice("Destino");
rede.addAresta(origem, destino, 2.5);

BuscaEmLargura.Resultado niveis =
        BuscaEmLargura.calcularMenoresCaminhos(rede, origem);
Optional<List<Vertice>> caminho = niveis.caminhoAte(destino);

Digrafo dependencias = new Digrafo();
Vertice base = new Vertice("Base");
Vertice aplicacao = new Vertice("Aplicacao");
dependencias.addAresta(base, aplicacao);
List<Vertice> ordem = OrdenacaoTopologica.ordenar(dependencias);
```

Uma implementacao correta ainda depende de um modelo adequado: pesos podem
representar tempo, distancia ou custo, mas um unico peso nao expressa
necessariamente todas as restricoes de uma aplicacao.

## Aplicacao desktop

A aplicacao usa exclusivamente Java Swing e nao exige bibliotecas graficas
adicionais. Requer JDK 8 ou superior e Maven. Na raiz do projeto, compile e execute:

```powershell
mvn package
java -jar target\grafo-studio-1.0.0.jar
```

No canvas, clique em uma area vazia para criar um vertice; selecione dois
vertices em sequencia para adicionar uma aresta com o peso informado. Arraste
vertices para reposiciona-los. Clique em um vertice ou aresta e use **Remover
selecao** para exclui-los. A direcao do grafo pode ser escolhida antes de criar
vertices.

O painel executa BFS, DFS, Dijkstra, ordenacao topologica, Prim, Kruskal,
emparelhamento bipartido, trilha/circuito euleriano e caminho/ciclo
hamiltoniano. A execucao oferece reproduzir, pausar, avancar, voltar e reiniciar,
com velocidade ajustavel. A tabela mostra distancias e predecessores quando
aplicaveis; a fila ou pilha apresenta a fronteira atual. O canvas destaca a
aresta examinada, o caminho encontrado e as arestas escolhidas para uma solucao.

Use os botoes de desfazer/refazer para percorrer ate 50 alteracoes. Salve e abra
arquivos `.grafo` para preservar direcao, rotulos, pesos e posicoes dos vertices.
O canvas oferece zoom pela roda do mouse e centralizacao da visualizacao; arraste
com o botao do meio para mover a area de trabalho.

Dijkstra requer pesos nao negativos. Prim e Kruskal requerem grafos nao
direcionados e conexos para obter uma arvore geradora; o emparelhamento requer
um grafo bipartido nao direcionado. A busca hamiltoniana e exata e limitada a
12 vertices para manter a interface responsiva.
