package views;

import static java.awt.Component.LEFT_ALIGNMENT;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import model.Aresta;
import model.Vertice;
import utils.AlgorithmRunner;
import utils.GraphDocument;
import utils.GraphHistory;
import utils.GraphIO;

public final class MainFrame {
    private static final Color NAVY = new Color(24, 34, 50);
    private static final Color PANEL = new Color(248, 250, 252);
    private static final Color ACCENT = new Color(47, 94, 168);
    private static final Color MUTED = new Color(102, 112, 133);

    private final JFrame frame = new JFrame("Grafo Studio");
    private final JCheckBox directedCheckBox = new JCheckBox("Arestas direcionadas");
    private final JComboBox<AlgorithmRunner.Algorithm> algorithmCombo =
            new JComboBox<>(AlgorithmRunner.Algorithm.values());
    private final JComboBox<Vertice> sourceCombo = new JComboBox<>();
    private final JComboBox<Vertice> destinationCombo = new JComboBox<>();
    private final JTextField weightField = new JTextField("1", 8);
    private final JTextArea stepDescription = new JTextArea(4, 24);
    private final JTextArea frontierArea = new JTextArea(4, 24);
    private final JLabel stepLabel = new JLabel("Nenhuma execucao");
    private final JLabel resultLabel = new JLabel(" ");
    private final JLabel speedLabel = new JLabel("Velocidade: 6");
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[] {"Vertice", "Distancia", "Anterior", "Estado"}, 0) {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return false;
                }
            };
    private final JTable stateTable = new JTable(tableModel);
    private final JButton undoButton = toolbarButton("Desfazer");
    private final JButton redoButton = toolbarButton("Refazer");
    private final JButton previousButton = toolbarButton("Anterior");
    private final JButton nextButton = toolbarButton("Proximo");
    private final JButton playButton = toolbarButton("Reproduzir");
    private final JButton resetButton = toolbarButton("Reiniciar");
    private final JButton runButton = primaryButton("Executar");
    private final JSlider speedSlider = new JSlider(1, 10, 6);
    private final Timer playbackTimer = new Timer(720, event -> advancePlayback());

    private GraphDocument document = new GraphDocument(false);
    private final GraphCanvas canvas;
    private final GraphHistory history = new GraphHistory();
    private AlgorithmRunner.Trace trace;
    private int stepIndex = -1;
    private String currentWeightText = "1";
    private Vertice pendingSource;
    private Vertice selectedVertex;
    private Aresta selectedEdge;
    private Path currentFile;
    private boolean dirty;

    public MainFrame() {
        canvas = new GraphCanvas(document);
        canvas.setHandlers(this::addVertexAt, this::handleVertexClick,
                this::handleEdgeClick, this::recordEdit,
                (vertex, point) -> graphChanged("Vertice " + vertex + " reposicionado."));
        configureLookAndFeel();
        buildWindow();
        connectControls();
        refreshVertexControls();
        updateHistoryButtons();
        updateTitle();
    }

    public void show() {
        frame.setVisible(true);
        canvas.centerGraph();
    }

    private void configureLookAndFeel() {
        UIManager.put("Button.font", new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        UIManager.put("Label.font", new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        UIManager.put("ComboBox.font", new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        UIManager.put("TextField.font", new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        UIManager.put("Table.font", new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        UIManager.put("TableHeader.font", new Font(Font.SANS_SERIF, Font.BOLD, 11));
    }

    private void buildWindow() {
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.setMinimumSize(new Dimension(1180, 720));
        frame.setSize(1520, 920);
        frame.setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Color.WHITE);
        root.add(buildToolbar(), BorderLayout.NORTH);
        root.add(buildEditorPanel(), BorderLayout.WEST);
        root.add(canvas, BorderLayout.CENTER);
        root.add(buildInspectorPanel(), BorderLayout.EAST);
        frame.setContentPane(root);

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                if (confirmDiscardChanges()) {
                    playbackTimer.stop();
                    frame.dispose();
                }
            }
        });
        bindShortcut("control Z", "undo", this::undo);
        bindShortcut("control Y", "redo", this::redo);
        bindShortcut("control S", "save", this::saveGraph);
        bindShortcut("DELETE", "delete", this::deleteSelection);
    }

    private JPanel buildToolbar() {
        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setBackground(Color.WHITE);
        toolbar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(226, 232, 240)),
                new EmptyBorder(10, 16, 10, 16)));

        JPanel brand = new JPanel(new BorderLayout(10, 0));
        brand.setOpaque(false);
        JLabel mark = new JLabel("G");
        mark.setOpaque(true);
        mark.setBackground(ACCENT);
        mark.setForeground(Color.WHITE);
        mark.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 17));
        mark.setHorizontalAlignment(SwingConstants.CENTER);
        mark.setPreferredSize(new Dimension(34, 34));
        JLabel title = new JLabel("Grafo Studio");
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        title.setForeground(NAVY);
        brand.add(mark, BorderLayout.WEST);
        brand.add(title, BorderLayout.CENTER);
        toolbar.add(brand, BorderLayout.WEST);

        JPanel actions = new JPanel(new GridLayout(1, 0, 6, 0));
        actions.setOpaque(false);
        JButton newButton = toolbarButton("Novo");
        JButton openButton = toolbarButton("Abrir");
        JButton saveButton = toolbarButton("Salvar");
        JButton zoomOutButton = toolbarButton("\u2212");
        JButton zoomInButton = toolbarButton("+");
        JButton centerButton = toolbarButton("Centralizar");
        actions.add(newButton);
        actions.add(openButton);
        actions.add(saveButton);
        actions.add(undoButton);
        actions.add(redoButton);
        actions.add(zoomOutButton);
        actions.add(zoomInButton);
        actions.add(centerButton);
        toolbar.add(actions, BorderLayout.EAST);

        newButton.addActionListener(event -> newGraph());
        openButton.addActionListener(event -> openGraph());
        saveButton.addActionListener(event -> saveGraph());
        undoButton.addActionListener(event -> undo());
        redoButton.addActionListener(event -> redo());
        zoomOutButton.addActionListener(event -> canvas.zoomOut());
        zoomInButton.addActionListener(event -> canvas.zoomIn());
        centerButton.addActionListener(event -> canvas.centerGraph());
        return toolbar;
    }

    private JPanel buildEditorPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(PANEL);
        panel.setBorder(new EmptyBorder(18, 16, 16, 16));
        panel.setPreferredSize(new Dimension(270, 800));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.add(sectionTitle("EDITOR DO GRAFO"));

        directedCheckBox.setOpaque(false);
        directedCheckBox.setAlignmentX(LEFT_ALIGNMENT);
        panel.add(directedCheckBox);
        panel.add(Box.createVerticalStrut(8));
        JLabel instructions = new JLabel("<html>Clique no canvas para criar vertices.<br>"
                + "Selecione dois vertices para adicionar uma aresta.</html>");
        instructions.setForeground(MUTED);
        instructions.setAlignmentX(LEFT_ALIGNMENT);
        panel.add(instructions);
        panel.add(Box.createVerticalStrut(14));

        addLabel(panel, "Peso da proxima aresta");
        weightField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        weightField.setAlignmentX(LEFT_ALIGNMENT);
        panel.add(weightField);
        panel.add(Box.createVerticalStrut(12));

        JButton deleteButton = toolbarButton("Remover selecao");
        JButton clearButton = toolbarButton("Limpar grafo");
        deleteButton.setAlignmentX(LEFT_ALIGNMENT);
        clearButton.setAlignmentX(LEFT_ALIGNMENT);
        panel.add(deleteButton);
        panel.add(Box.createVerticalStrut(6));
        panel.add(clearButton);
        panel.add(Box.createVerticalStrut(28));

        panel.add(sectionTitle("ALGORITMO"));
        algorithmCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        algorithmCombo.setAlignmentX(LEFT_ALIGNMENT);
        panel.add(algorithmCombo);
        panel.add(Box.createVerticalStrut(12));
        addLabel(panel, "Vertice inicial");
        sourceCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        sourceCombo.setAlignmentX(LEFT_ALIGNMENT);
        panel.add(sourceCombo);
        panel.add(Box.createVerticalStrut(10));
        addLabel(panel, "Destino para caminho");
        destinationCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        destinationCombo.setAlignmentX(LEFT_ALIGNMENT);
        panel.add(destinationCombo);
        panel.add(Box.createVerticalStrut(12));

        runButton.setAlignmentX(LEFT_ALIGNMENT);
        panel.add(runButton);
        panel.add(Box.createVerticalStrut(14));
        speedLabel.setForeground(MUTED);
        speedLabel.setAlignmentX(LEFT_ALIGNMENT);
        panel.add(speedLabel);
        speedSlider.setOpaque(false);
        speedSlider.setAlignmentX(LEFT_ALIGNMENT);
        speedSlider.setMajorTickSpacing(3);
        speedSlider.setPaintTicks(true);
        panel.add(speedSlider);
        panel.add(Box.createVerticalStrut(10));
        JLabel panningHelp = new JLabel("<html>Roda do mouse: zoom<br>"
                + "Botao do meio: mover o canvas</html>");
        panningHelp.setForeground(MUTED);
        panningHelp.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        panningHelp.setAlignmentX(LEFT_ALIGNMENT);
        panel.add(panningHelp);
        panel.add(Box.createVerticalGlue());

        deleteButton.addActionListener(event -> deleteSelection());
        clearButton.addActionListener(event -> clearGraph());
        directedCheckBox.addActionListener(event -> changeGraphDirection());
        runButton.addActionListener(event -> runAlgorithm());
        return panel;
    }

    private JPanel buildInspectorPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(18, 14, 14, 14));
        panel.setPreferredSize(new Dimension(330, 800));

        JPanel details = new JPanel();
        details.setBackground(Color.WHITE);
        details.setLayout(new BoxLayout(details, BoxLayout.Y_AXIS));
        details.add(sectionTitle("EXECUCAO"));
        stepLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        stepLabel.setForeground(ACCENT);
        stepLabel.setAlignmentX(LEFT_ALIGNMENT);
        details.add(stepLabel);
        details.add(Box.createVerticalStrut(8));

        stepDescription.setEditable(false);
        stepDescription.setLineWrap(true);
        stepDescription.setWrapStyleWord(true);
        stepDescription.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        stepDescription.setMargin(new Insets(8, 8, 8, 8));
        stepDescription.setBackground(PANEL);
        JScrollPane descriptionScroll = new JScrollPane(stepDescription);
        descriptionScroll.setAlignmentX(LEFT_ALIGNMENT);
        descriptionScroll.setPreferredSize(new Dimension(290, 100));
        descriptionScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
        details.add(descriptionScroll);
        details.add(Box.createVerticalStrut(7));

        resultLabel.setForeground(new Color(22, 101, 52));
        resultLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
        resultLabel.setAlignmentX(LEFT_ALIGNMENT);
        details.add(resultLabel);
        details.add(Box.createVerticalStrut(12));
        details.add(sectionTitle("ESTADO DOS VERTICES"));

        stateTable.setRowHeight(25);
        stateTable.setFillsViewportHeight(true);
        stateTable.setShowVerticalLines(false);
        stateTable.setGridColor(new Color(234, 238, 243));
        stateTable.getTableHeader().setReorderingAllowed(false);
        stateTable.getColumnModel().getColumn(0).setPreferredWidth(52);
        stateTable.getColumnModel().getColumn(1).setPreferredWidth(72);
        stateTable.getColumnModel().getColumn(2).setPreferredWidth(65);
        stateTable.getColumnModel().getColumn(3).setPreferredWidth(82);
        stateTable.getColumnModel().getColumn(3)
                .setCellRenderer(new StateRenderer());
        JScrollPane tableScroll = new JScrollPane(stateTable);
        tableScroll.setAlignmentX(LEFT_ALIGNMENT);
        tableScroll.setPreferredSize(new Dimension(300, 240));
        tableScroll.setMinimumSize(new Dimension(250, 140));
        details.add(tableScroll);
        details.add(Box.createVerticalStrut(12));
        details.add(sectionTitle("FILA / PILHA"));

        frontierArea.setEditable(false);
        frontierArea.setLineWrap(true);
        frontierArea.setWrapStyleWord(true);
        frontierArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        frontierArea.setMargin(new Insets(7, 7, 7, 7));
        frontierArea.setBackground(PANEL);
        JScrollPane frontierScroll = new JScrollPane(frontierArea);
        frontierScroll.setAlignmentX(LEFT_ALIGNMENT);
        frontierScroll.setPreferredSize(new Dimension(290, 90));
        frontierScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));
        details.add(frontierScroll);
        panel.add(details, BorderLayout.CENTER);

        JPanel playback = new JPanel(new GridLayout(2, 2, 6, 6));
        playback.setBackground(Color.WHITE);
        playback.add(previousButton);
        playback.add(nextButton);
        playback.add(playButton);
        playback.add(resetButton);
        panel.add(playback, BorderLayout.SOUTH);

        previousButton.addActionListener(event -> showStep(stepIndex - 1));
        nextButton.addActionListener(event -> showStep(stepIndex + 1));
        playButton.addActionListener(event -> togglePlayback());
        resetButton.addActionListener(event -> {
            playbackTimer.stop();
            playButton.setText("Reproduzir");
            if (trace != null && !trace.getSteps().isEmpty()) {
                showStep(0);
            }
        });
        return panel;
    }

    private void connectControls() {
        weightField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent event) {
                currentWeightText = weightField.getText();
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent event) {
                currentWeightText = weightField.getText();
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent event) {
                currentWeightText = weightField.getText();
            }
        });
        algorithmCombo.addActionListener(event -> updateAlgorithmControls());
        speedSlider.addChangeListener(event -> {
            int speed = speedSlider.getValue();
            speedLabel.setText("Velocidade: " + speed);
            playbackTimer.setDelay(1600 - speed * 140);
        });
    }

    private void addVertexAt(Point point) {
        recordEdit();
        Vertice vertex = document.addVertex();
        canvas.setPosition(vertex, point);
        selectedVertex = vertex;
        selectedEdge = null;
        pendingSource = null;
        canvas.setSelection(selectedVertex, null, null);
        graphChanged("Vertice " + vertex + " criado.");
    }

    private void handleVertexClick(Vertice vertex) {
        selectedVertex = vertex;
        selectedEdge = null;
        if (pendingSource == null) {
            pendingSource = vertex;
            canvas.setSelection(selectedVertex, pendingSource, null);
            setStatus("Origem selecionada: " + vertex
                    + ". Clique em outro vertice para criar uma aresta.");
            return;
        }
        if (pendingSource == vertex) {
            pendingSource = null;
            canvas.setSelection(selectedVertex, null, null);
            setStatus("Selecao de aresta cancelada.");
            return;
        }
        try {
            double weight = Double.parseDouble(currentWeightText.trim());
            if (!Double.isFinite(weight)) {
                throw new IllegalArgumentException("O peso deve ser finito.");
            }
            if (hasDuplicateEdge(pendingSource, vertex)) {
                throw new IllegalArgumentException("Ja existe uma aresta entre esses vertices.");
            }
            recordEdit();
            document.addEdge(pendingSource, vertex, weight);
            String description = "Aresta criada: " + pendingSource
                    + (document.isDirected() ? " -> " : " -- ") + vertex + ".";
            pendingSource = null;
            canvas.setSelection(selectedVertex, null, null);
            graphChanged(description);
        } catch (IllegalArgumentException exception) {
            setStatus(exception.getMessage() == null
                    ? "Peso invalido. Informe um numero finito." : exception.getMessage());
        }
    }

    private boolean hasDuplicateEdge(Vertice from, Vertice to) {
        for (Aresta edge : document.getEdges()) {
            if (edge.origem == from && edge.destino == to
                    || !document.isDirected() && edge.origem == to && edge.destino == from) {
                return true;
            }
        }
        return false;
    }

    private void handleEdgeClick(Aresta edge) {
        selectedEdge = edge;
        selectedVertex = null;
        pendingSource = null;
        canvas.setSelection(null, null, selectedEdge);
        setStatus("Aresta selecionada: " + edge.origem + " -> " + edge.destino
                + " (peso " + edge.peso + ").");
    }

    private void deleteSelection() {
        if (selectedVertex != null) {
            recordEdit();
            Vertice removed = selectedVertex;
            document.removeVertex(removed);
            canvas.removePosition(removed);
            selectedVertex = null;
            pendingSource = null;
            selectedEdge = null;
            canvas.setSelection(null, null, null);
            graphChanged("Vertice " + removed + " e suas arestas foram removidos.");
        } else if (selectedEdge != null) {
            recordEdit();
            document.removeEdge(selectedEdge);
            selectedEdge = null;
            canvas.setSelection(null, null, null);
            graphChanged("Aresta removida.");
        } else {
            setStatus("Selecione um vertice ou uma aresta para remover.");
        }
    }

    private void changeGraphDirection() {
        if (!document.getVertices().isEmpty()) {
            directedCheckBox.setSelected(document.isDirected());
            setStatus("Limpe o grafo antes de alterar sua direcao.");
            return;
        }
        recordEdit();
        document = new GraphDocument(directedCheckBox.isSelected());
        canvas.setDocument(document);
        selectedVertex = null;
        selectedEdge = null;
        pendingSource = null;
        graphChanged(document.isDirected()
                ? "Novo grafo direcionado." : "Novo grafo nao direcionado.");
    }

    private void newGraph() {
        if (!confirmDiscardChanges()) {
            return;
        }
        recordEdit();
        document = new GraphDocument(directedCheckBox.isSelected());
        currentFile = null;
        selectedVertex = null;
        selectedEdge = null;
        pendingSource = null;
        canvas.setDocument(document);
        graphChanged("Novo grafo criado.");
        updateTitle();
    }

    private void clearGraph() {
        int answer = JOptionPane.showConfirmDialog(frame,
                "Deseja remover todos os vertices e arestas?", "Limpar grafo",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (answer != JOptionPane.YES_OPTION) {
            return;
        }
        recordEdit();
        document = new GraphDocument(document.isDirected());
        selectedVertex = null;
        selectedEdge = null;
        pendingSource = null;
        canvas.setDocument(document);
        graphChanged("Grafo limpo.");
    }

    private void saveGraph() {
        Path path = currentFile;
        if (path == null) {
            JFileChooser chooser = graphFileChooser();
            if (chooser.showSaveDialog(frame) != JFileChooser.APPROVE_OPTION) {
                return;
            }
            path = ensureGraphExtension(chooser.getSelectedFile().toPath());
        }
        try {
            GraphIO.save(path, document, canvas.snapshotPositions());
            currentFile = path;
            dirty = false;
            updateTitle();
            setStatus("Grafo salvo em " + path.getFileName() + ".");
        } catch (IOException | IllegalArgumentException exception) {
            showError("Nao foi possivel salvar o grafo: " + exception.getMessage());
        }
    }

    private void openGraph() {
        if (!confirmDiscardChanges()) {
            return;
        }
        JFileChooser chooser = graphFileChooser();
        if (chooser.showOpenDialog(frame) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        Path path = chooser.getSelectedFile().toPath();
        try {
            GraphIO.LoadedGraph loaded = GraphIO.load(path);
            history.clear();
            applyState(loaded.getDocument(), loaded.getPositions());
            currentFile = path;
            dirty = false;
            updateTitle();
            setStatus("Grafo carregado de " + path.getFileName() + ".");
        } catch (IOException | IllegalArgumentException exception) {
            showError("Nao foi possivel abrir o grafo: " + exception.getMessage());
        }
    }

    private JFileChooser graphFileChooser() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter(
                "Grafo Studio (*.grafo)", "grafo"));
        return chooser;
    }

    private Path ensureGraphExtension(Path path) {
        if (path.getFileName().toString().toLowerCase(java.util.Locale.ROOT).endsWith(".grafo")) {
            return path;
        }
        return Paths.get(path.toString() + ".grafo");
    }

    private void undo() {
        GraphHistory.State state = history.undo(document, canvas.snapshotPositions());
        if (state == null) {
            return;
        }
        applyState(state.getDocument(), state.getPositions());
        dirty = true;
        setStatus("Alteracao desfeita.");
    }

    private void redo() {
        GraphHistory.State state = history.redo(document, canvas.snapshotPositions());
        if (state == null) {
            return;
        }
        applyState(state.getDocument(), state.getPositions());
        dirty = true;
        setStatus("Alteracao refeita.");
    }

    private void applyState(GraphDocument restored, Map<Vertice, Point> positions) {
        document = restored;
        selectedVertex = null;
        selectedEdge = null;
        pendingSource = null;
        canvas.setDocument(document);
        canvas.setPositions(positions);
        canvas.centerGraph();
        graphChanged("");
        updateTitle();
    }

    private void recordEdit() {
        history.record(document, canvas.snapshotPositions());
        updateHistoryButtons();
    }

    private void runAlgorithm() {
        AlgorithmRunner.Algorithm algorithm =
                (AlgorithmRunner.Algorithm) algorithmCombo.getSelectedItem();
        Vertice source = (Vertice) sourceCombo.getSelectedItem();
        try {
            trace = AlgorithmRunner.run(document, algorithm, source);
            stepIndex = 0;
            showStep(stepIndex);
            updatePlaybackControls();
        } catch (IllegalArgumentException | IllegalStateException exception) {
            resetTrace();
            showError(exception.getMessage());
        }
    }

    private void showStep(int index) {
        if (trace == null || index < 0 || index >= trace.getSteps().size()) {
            return;
        }
        stepIndex = index;
        AlgorithmRunner.Step step = trace.getSteps().get(stepIndex);
        canvas.setStep(step);

        List<Vertice> path = step.getRoute();
        if (path.isEmpty()
                && (!step.getDistances().isEmpty() || !step.getPredecessors().isEmpty())) {
            Vertice destination = (Vertice) destinationCombo.getSelectedItem();
            if (destination != null) {
                Double distance = step.getDistances().get(destination);
                if (distance != null && !distance.isInfinite()
                        || step.getPredecessors().containsKey(destination)
                        && step.getDiscovered().contains(destination)) {
                    path = reconstructPath(step, destination);
                }
            }
        }
        canvas.setHighlightedPath(path);
        stepDescription.setText(step.getDescription());
        updateStateTable(step);
        updateFrontier(step);
        updateResult(step);
        stepLabel.setText("Passo " + (stepIndex + 1) + " de " + trace.getSteps().size());
        stepLabel.setForeground(stepIndex == trace.getSteps().size() - 1
                && !trace.isSuccessful() ? new Color(185, 28, 28) : ACCENT);
        updatePlaybackControls();
    }

    private void updateStateTable(AlgorithmRunner.Step step) {
        tableModel.setRowCount(0);
        for (Vertice vertex : document.getVertices()) {
            Double distance = step.getDistances().get(vertex);
            Vertice predecessor = step.getPredecessors().get(vertex);
            String state;
            if (step.getCurrent() == vertex) {
                state = "Atual";
            } else if (step.getProcessed().contains(vertex)) {
                state = "Processado";
            } else if (step.getFrontier().contains(vertex)) {
                state = "Na fila";
            } else if (step.getDiscovered().contains(vertex)) {
                state = "Descoberto";
            } else {
                state = "-";
            }
            tableModel.addRow(new Object[] {
                vertex.toString(),
                distance == null ? "-" : formatDistance(distance),
                predecessor == null ? "-" : predecessor.toString(),
                state
            });
        }
    }

    private void updateFrontier(AlgorithmRunner.Step step) {
        List<Vertice> frontier = step.getFrontierOrder();
        if (frontier.isEmpty()) {
            frontierArea.setText("(vazia)");
            return;
        }
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < frontier.size(); i++) {
            Vertice vertex = frontier.get(i);
            if (i > 0) {
                text.append("  \u2192  ");
            }
            text.append(vertex);
            Double distance = step.getDistances().get(vertex);
            if (distance != null) {
                text.append(" (").append(formatDistance(distance)).append(")");
            }
        }
        frontierArea.setText(text.toString());
    }

    private void updateResult(AlgorithmRunner.Step step) {
        if (stepIndex != trace.getSteps().size() - 1) {
            resultLabel.setText(" ");
            return;
        }
        resultLabel.setText(trace.getSummary());
        resultLabel.setForeground(trace.isSuccessful()
                ? new Color(22, 101, 52) : new Color(185, 28, 28));
    }

    private List<Vertice> reconstructPath(AlgorithmRunner.Step step, Vertice destination) {
        Map<Vertice, Vertice> predecessors = step.getPredecessors();
        if (!predecessors.containsKey(destination)) {
            return Collections.emptyList();
        }
        List<Vertice> path = new ArrayList<>();
        Vertice current = destination;
        int remaining = predecessors.size();
        while (current != null && remaining-- > 0) {
            path.add(current);
            current = predecessors.get(current);
        }
        Collections.reverse(path);
        return path;
    }

    private void togglePlayback() {
        if (trace == null || trace.getSteps().isEmpty()) {
            return;
        }
        if (playbackTimer.isRunning()) {
            playbackTimer.stop();
            playButton.setText("Reproduzir");
            return;
        }
        if (stepIndex >= trace.getSteps().size() - 1) {
            showStep(0);
        }
        playButton.setText("Pausar");
        playbackTimer.start();
    }

    private void advancePlayback() {
        if (trace == null || stepIndex >= trace.getSteps().size() - 1) {
            playbackTimer.stop();
            playButton.setText("Reproduzir");
            return;
        }
        showStep(stepIndex + 1);
    }

    private void graphChanged(String message) {
        dirty = true;
        resetTrace();
        canvas.refresh();
        refreshVertexControls();
        updateHistoryButtons();
        updateTitle();
        if (!message.isEmpty()) {
            setStatus(message);
        }
    }

    private void resetTrace() {
        playbackTimer.stop();
        trace = null;
        stepIndex = -1;
        playButton.setText("Reproduzir");
        stepLabel.setText("Nenhuma execucao");
        stepLabel.setForeground(ACCENT);
        stepDescription.setText("");
        frontierArea.setText("");
        resultLabel.setText(" ");
        tableModel.setRowCount(0);
        canvas.clearStep();
        canvas.setHighlightedPath(Collections.<Vertice>emptyList());
        updatePlaybackControls();
    }

    private void refreshVertexControls() {
        Vertice oldSource = (Vertice) sourceCombo.getSelectedItem();
        Vertice oldDestination = (Vertice) destinationCombo.getSelectedItem();
        List<Vertice> vertices = new ArrayList<>(document.getVertices());
        sourceCombo.removeAllItems();
        destinationCombo.removeAllItems();
        for (Vertice vertex : vertices) {
            sourceCombo.addItem(vertex);
            destinationCombo.addItem(vertex);
        }
        if (oldSource != null && vertices.contains(oldSource)) {
            sourceCombo.setSelectedItem(oldSource);
        }
        if (oldDestination != null && vertices.contains(oldDestination)) {
            destinationCombo.setSelectedItem(oldDestination);
        }
        directedCheckBox.setSelected(document.isDirected());
        directedCheckBox.setEnabled(vertices.isEmpty());
        updateAlgorithmControls();
    }

    private void updateAlgorithmControls() {
        AlgorithmRunner.Algorithm algorithm =
                (AlgorithmRunner.Algorithm) algorithmCombo.getSelectedItem();
        boolean needsStart = algorithm == AlgorithmRunner.Algorithm.BREADTH_FIRST
                || algorithm == AlgorithmRunner.Algorithm.DEPTH_FIRST
                || algorithm == AlgorithmRunner.Algorithm.DIJKSTRA
                || algorithm == AlgorithmRunner.Algorithm.PRIM
                || algorithm == AlgorithmRunner.Algorithm.HAMILTON_PATH
                || algorithm == AlgorithmRunner.Algorithm.HAMILTON_CYCLE;
        boolean needsDestination = algorithm == AlgorithmRunner.Algorithm.BREADTH_FIRST
                || algorithm == AlgorithmRunner.Algorithm.DIJKSTRA;
        sourceCombo.setEnabled(needsStart);
        destinationCombo.setEnabled(needsDestination);
    }

    private void updatePlaybackControls() {
        boolean available = trace != null && !trace.getSteps().isEmpty();
        previousButton.setEnabled(available && stepIndex > 0);
        nextButton.setEnabled(available && stepIndex < trace.getSteps().size() - 1);
        playButton.setEnabled(available);
        resetButton.setEnabled(available);
    }

    private void updateHistoryButtons() {
        undoButton.setEnabled(history.canUndo());
        redoButton.setEnabled(history.canRedo());
    }

    private void bindShortcut(String key, String name, Runnable action) {
        frame.getRootPane().getInputMap(JPanel.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(key), name);
        frame.getRootPane().getActionMap().put(name,
                new javax.swing.AbstractAction() {
                    @Override
                    public void actionPerformed(java.awt.event.ActionEvent event) {
                        action.run();
                    }
                });
    }

    private boolean confirmDiscardChanges() {
        if (!dirty) {
            return true;
        }
        int answer = JOptionPane.showConfirmDialog(frame,
                "Salvar as alteracoes antes de continuar?", "Alteracoes nao salvas",
                JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        if (answer == JOptionPane.YES_OPTION) {
            saveGraph();
            return !dirty;
        }
        return answer == JOptionPane.NO_OPTION;
    }

    private void updateTitle() {
        String name = currentFile == null ? "Novo grafo" : currentFile.getFileName().toString();
        frame.setTitle("Grafo Studio - " + name + (dirty ? " *" : ""));
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(frame, message, "Grafo Studio",
                JOptionPane.ERROR_MESSAGE);
    }

    private void setStatus(String message) {
        stepDescription.setText(message == null ? "" : message);
    }

    private JPanel sectionTitle(String text) {
        JPanel section = new JPanel(new BorderLayout());
        section.setOpaque(false);
        JLabel label = new JLabel(text);
        label.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 11));
        label.setForeground(ACCENT);
        section.add(label, BorderLayout.WEST);
        section.setAlignmentX(LEFT_ALIGNMENT);
        section.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
        return section;
    }

    private void addLabel(JPanel panel, String text) {
        JLabel label = new JLabel(text);
        label.setForeground(MUTED);
        label.setAlignmentX(LEFT_ALIGNMENT);
        panel.add(label);
        panel.add(Box.createVerticalStrut(4));
    }

    private JButton primaryButton(String text) {
        JButton button = new JButton(text);
        button.setForeground(Color.WHITE);
        button.setBackground(ACCENT);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(9, 14, 9, 14));
        return button;
    }

    private static JButton toolbarButton(String text) {
        JButton button = new JButton(text);
        button.setFocusPainted(false);
        button.setBackground(Color.WHITE);
        button.setForeground(NAVY);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 226, 234)),
                BorderFactory.createEmptyBorder(6, 9, 6, 9)));
        return button;
    }

    private void updateWeightText() {
        currentWeightText = weightField.getText();
    }

    private String formatDistance(double distance) {
        if (distance == Math.rint(distance) && Math.abs(distance) < Long.MAX_VALUE) {
            return Long.toString((long) distance);
        }
        return Double.toString(distance);
    }

    private String joinVertices(List<Vertice> vertices) {
        StringBuilder text = new StringBuilder();
        for (Vertice vertex : vertices) {
            if (text.length() > 0) {
                text.append(" \u2192 ");
            }
            text.append(vertex);
        }
        return text.toString();
    }

    private static final class StateRenderer extends DefaultTableCellRenderer {
        @Override
        public java.awt.Component getTableCellRendererComponent(JTable table, Object value,
                boolean selected, boolean focused, int row, int column) {
            java.awt.Component component = super.getTableCellRendererComponent(
                    table, value, selected, focused, row, column);
            if (!selected) {
                String state = value == null ? "" : value.toString();
                component.setBackground("Atual".equals(state) ? new Color(255, 242, 204)
                        : "Na fila".equals(state) ? new Color(219, 234, 254)
                        : "Processado".equals(state) ? new Color(220, 252, 231)
                        : Color.WHITE);
            }
            return component;
        }
    }
}
