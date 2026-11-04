import javax.swing.*;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Desktop operations workspace for the emergency room simulation (dark / light themes). */
public class HospitalGUI extends JFrame {

    /* ------------------------------------------------------------------ */
    /*  Theme                                                              */
    /* ------------------------------------------------------------------ */
    private static Color c(int r, int g, int b) { return new Color(r, g, b); }

    static final class Theme {
        final Color bg, sidebarTop, sidebarBottom, card, border, text, muted, accent, accent2,
                inputBg, rowAlt, selection, sky, amber, violet, green, red;

        Theme(Color[] k) {
            bg = k[0]; sidebarTop = k[1]; sidebarBottom = k[2]; card = k[3]; border = k[4];
            text = k[5]; muted = k[6]; accent = k[7]; accent2 = k[8]; inputBg = k[9];
            rowAlt = k[10]; selection = k[11]; sky = k[12]; amber = k[13]; violet = k[14];
            green = k[15]; red = k[16];
        }

        static final Theme DARK = new Theme(new Color[]{
                c(11, 18, 32), c(16, 26, 46), c(9, 14, 26), c(18, 28, 47), c(36, 51, 76),
                c(232, 238, 247), c(142, 159, 183), c(45, 212, 191), c(56, 189, 248), c(12, 20, 36),
                c(21, 32, 53), c(29, 62, 82), c(56, 189, 248), c(251, 191, 36), c(167, 139, 250),
                c(74, 222, 128), c(248, 113, 113)});

        static final Theme LIGHT = new Theme(new Color[]{
                c(241, 245, 250), c(18, 38, 70), c(10, 22, 45), c(255, 255, 255), c(222, 229, 239),
                c(18, 31, 51), c(94, 110, 134), c(13, 148, 136), c(2, 132, 199), c(244, 247, 251),
                c(249, 251, 253), c(220, 242, 239), c(2, 132, 199), c(202, 138, 4), c(124, 58, 237),
                c(22, 163, 74), c(220, 38, 38)});
    }

    // must stay the first instance field: custom components read it while being built
    private Theme t = Theme.DARK;

    private static final String FONT = "Segoe UI";
    private static final int TREATMENT_ROOMS = 2;
    private static final int ROLE_TEXT = 0, ROLE_MUTED = 1, ROLE_ACCENT = 2;
    private static final int BTN_PRIMARY = 0, BTN_GHOST = 1, BTN_FLAT = 2;

    private enum Kind { READY, RUN, DONE, ERROR }

    /* ------------------------------------------------------------------ */
    /*  State / widgets                                                    */
    /* ------------------------------------------------------------------ */
    private final Stepper capacity = new Stepper(5, 1, 100);
    private final Stepper doctors = new Stepper(3, 1, 50);
    private final Stepper patients = new Stepper(12, 1, 200);
    private final RoundButton start = new RoundButton("▶  Start simulation", BTN_PRIMARY, 14, 30);
    private final RoundButton clear = new RoundButton("Clear log", BTN_GHOST, 10, 18);
    private final RoundButton themeBtn = new RoundButton("☀  Light mode", BTN_GHOST, 9, 16);
    private final Pill statusPill = new Pill();
    private final JLabel pageTitle = new TLabel("Operations overview", ROLE_TEXT, Font.BOLD, 30);
    private final JLabel waitingValue = new TLabel("0", ROLE_TEXT, Font.BOLD, 36);
    private final JLabel treatingValue = new TLabel("0 / " + TREATMENT_ROOMS, ROLE_TEXT, Font.BOLD, 36);
    private final JLabel dischargedValue = new TLabel("0", ROLE_TEXT, Font.BOLD, 36);
    private final JLabel elapsedValue = new TLabel("00:00", ROLE_TEXT, Font.BOLD, 36);
    private final JLabel treatedLabel = new TLabel("0 of 0 treated", ROLE_MUTED, Font.BOLD, 13);
    private final JLabel eventCount = new TLabel("0 events", ROLE_MUTED, Font.PLAIN, 15);
    private final RoundProgress progress = new RoundProgress();
    private final JTextPane activity = new JTextPane();
    private final DefaultTableModel patientModel = model("ID", "PATIENT STATUS", "CARE TEAM");
    private final DefaultTableModel doctorModel = model("DOCTOR", "STATUS", "PATIENT");
    private final JTable patientTable = new JTable(patientModel);
    private final JTable doctorTable = new JTable(doctorModel);
    private final List<JScrollPane> scrolls = new ArrayList<>();
    private final List<String[]> logEntries = new ArrayList<>();
    private final Map<String, Integer> patientRows = new HashMap<>();
    private final Map<Integer, Integer> doctorRows = new HashMap<>();
    private final CardLayout pageLayout = new CardLayout();
    private final JPanel pages = new JPanel(pageLayout);
    private final NavButton operationsNav = new NavButton("▦", "Operations", true);
    private final NavButton activityNav = new NavButton("≡", "Event log", false);

    private Timer elapsedTimer;
    private long startedAt;
    private int totalPatients;
    private int treatedPatients;
    private int waitingCount;
    private int roomsInUse;
    private boolean allDischarged;
    private String statusText = "Ready";
    private Kind statusKind = Kind.READY;

    /* ------------------------------------------------------------------ */
    /*  Construction                                                       */
    /* ------------------------------------------------------------------ */
    public HospitalGUI() {
        super("Emergency Room | Operations");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1100, 720));
        setSize(1320, 880);
        setLocationRelativeTo(null);

        JPanel shell = new JPanel(new BorderLayout());
        setContentPane(shell);
        shell.add(buildSidebar(), BorderLayout.WEST);

        JPanel workspace = new BgPanel(new BorderLayout());
        workspace.add(buildTopbar(), BorderLayout.NORTH);
        workspace.add(buildMainPages(), BorderLayout.CENTER);
        shell.add(workspace, BorderLayout.CENTER);

        themeBtn.addActionListener(e -> applyTheme(t == Theme.DARK ? Theme.LIGHT : Theme.DARK));
        applyTheme(Theme.DARK);
    }

    /* ------------------------------------------------------------------ */
    /*  Sidebar / topbar                                                   */
    /* ------------------------------------------------------------------ */
    private JPanel buildSidebar() {
        JPanel sidebar = new GradientPanel(new BorderLayout());
        sidebar.setPreferredSize(new Dimension(248, 1));
        sidebar.setBorder(new EmptyBorder(26, 16, 20, 16));

        JPanel top = new JPanel(new BorderLayout(0, 34));
        top.setOpaque(false);

        JPanel brand = new JPanel(new BorderLayout(12, 0));
        brand.setOpaque(false);
        JPanel brandText = new JPanel(new GridLayout(2, 1, 0, 1));
        brandText.setOpaque(false);
        JLabel brandName = new JLabel("DoctorX");
        brandName.setForeground(c(240, 245, 250));
        brandName.setFont(font(Font.BOLD, 15));
        JLabel brandSub = new JLabel("Emergency room");
        brandSub.setForeground(c(140, 160, 184));
        brandSub.setFont(font(Font.PLAIN, 12));
        brandText.add(brandName);
        brandText.add(brandSub);
        brand.add(new Logo(), BorderLayout.WEST);
        brand.add(brandText, BorderLayout.CENTER);
        top.add(brand, BorderLayout.NORTH);

        JPanel nav = new JPanel(new GridLayout(0, 1, 0, 6));
        nav.setOpaque(false);
        JLabel section = new JLabel("WORKSPACE");
        section.setForeground(c(120, 140, 165));
        section.setFont(font(Font.BOLD, 11));
        section.setBorder(new EmptyBorder(0, 14, 6, 0));
        nav.add(section);
        operationsNav.addActionListener(e -> showPage("Operations"));
        activityNav.addActionListener(e -> showPage("Event log"));
        nav.add(operationsNav);
        nav.add(activityNav);
        top.add(nav, BorderLayout.CENTER);
        sidebar.add(top, BorderLayout.NORTH);

        JPanel footer = new JPanel(new GridLayout(2, 1, 0, 3));
        footer.setOpaque(false);
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(255, 255, 255, 30)),
                new EmptyBorder(14, 4, 0, 4)));
        JLabel product = new JLabel("HOSPITAL SIMULATOR");
        product.setFont(font(Font.BOLD, 11));
        product.setForeground(c(215, 226, 236));
        JLabel version = new JLabel("Operating systems lab");
        version.setFont(font(Font.PLAIN, 12));
        version.setForeground(c(140, 160, 184));
        footer.add(product);
        footer.add(version);
        sidebar.add(footer, BorderLayout.SOUTH);
        return sidebar;
    }

    private JPanel buildTopbar() {
        JPanel topbar = new JPanel(new BorderLayout());
        topbar.setOpaque(false);
        topbar.setBorder(new EmptyBorder(26, 36, 6, 36));

        JPanel heading = new JPanel(new GridLayout(2, 1, 0, 2));
        heading.setOpaque(false);
        heading.add(new TLabel("EMERGENCY DEPARTMENT", ROLE_ACCENT, Font.BOLD, 12));
        heading.add(pageTitle);
        topbar.add(heading, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        right.setOpaque(false);
        right.add(themeBtn);
        right.add(statusPill);
        topbar.add(right, BorderLayout.EAST);
        return topbar;
    }

    private JPanel buildMainPages() {
        pages.setOpaque(false);
        pages.add(buildOperationsPage(), "Operations");
        pages.add(buildActivityPage(), "Event log");
        return pages;
    }

    /* ------------------------------------------------------------------ */
    /*  Operations page                                                    */
    /* ------------------------------------------------------------------ */
    private JPanel buildOperationsPage() {
        JPanel page = new JPanel(new BorderLayout(0, 18));
        page.setOpaque(false);
        page.setBorder(new EmptyBorder(18, 36, 32, 36));
        page.add(buildSetup(), BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(0, 18));
        body.setOpaque(false);
        body.add(buildMetricStrip(), BorderLayout.NORTH);
        body.add(buildTables(), BorderLayout.CENTER);
        page.add(body, BorderLayout.CENTER);
        return page;
    }

    private JPanel buildSetup() {
        Card setup = new Card(new BorderLayout(28, 0));
        setup.bar(() -> t.accent);
        setup.setBorder(new EmptyBorder(22, 26, 20, 26));

        JPanel fields = new JPanel(new GridLayout(1, 3, 28, 0));
        fields.setOpaque(false);
        fields.add(field("WAITING ROOM", "Capacity", capacity));
        fields.add(field("CARE TEAM", "Doctors", doctors));
        fields.add(field("ARRIVALS", "Patients", patients));
        setup.add(fields, BorderLayout.CENTER);

        start.addActionListener(e -> startSimulation());
        JPanel startWrap = new JPanel(new GridBagLayout());
        startWrap.setOpaque(false);
        startWrap.add(start);
        setup.add(startWrap, BorderLayout.EAST);
        return setup;
    }

    private JPanel buildMetricStrip() {
        JPanel strip = new JPanel(new GridLayout(1, 4, 16, 0));
        strip.setOpaque(false);
        strip.add(metric("WAITING", waitingValue, "patients in queue", () -> t.amber));
        strip.add(metric("IN TREATMENT", treatingValue, "rooms occupied", () -> t.accent));
        strip.add(metric("DISCHARGED", dischargedValue, "patients treated", () -> t.green));
        strip.add(metric("ELAPSED", elapsedValue, "run time", () -> t.sky));
        return strip;
    }

    private JPanel buildTables() {
        JPanel region = new JPanel(new GridBagLayout());
        region.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1;
        gbc.gridy = 0;

        gbc.gridx = 0;
        gbc.weightx = 0.58;
        gbc.insets = new Insets(0, 0, 0, 8);
        region.add(tablePanel("Patient flow", "Arrival status and assigned doctor", patientModel, patientTable), gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.42;
        gbc.insets = new Insets(0, 8, 0, 0);
        region.add(tablePanel("Care team", "Doctor availability and active assignment", doctorModel, doctorTable), gbc);
        return region;
    }

    private JPanel tablePanel(String title, String subtitle, DefaultTableModel model, JTable table) {
        Card panel = new Card(new BorderLayout(0, 12));
        panel.setBorder(new EmptyBorder(18, 18, 16, 18));

        JPanel heading = new JPanel(new BorderLayout(8, 0));
        heading.setOpaque(false);
        JPanel labels = new JPanel(new GridLayout(2, 1, 0, 3));
        labels.setOpaque(false);
        labels.add(new TLabel(title, ROLE_TEXT, Font.BOLD, 19));
        labels.add(new TLabel(subtitle, ROLE_MUTED, Font.PLAIN, 13));
        heading.add(labels, BorderLayout.WEST);
        if (model == patientModel) heading.add(treatedLabel, BorderLayout.EAST);
        panel.add(heading, BorderLayout.NORTH);

        styleTable(table);
        JScrollPane scroll = new JScrollPane(table);
        styleScroll(scroll);
        panel.add(scroll, BorderLayout.CENTER);

        if (model == patientModel) panel.add(progress, BorderLayout.SOUTH);
        return panel;
    }

    /* ------------------------------------------------------------------ */
    /*  Event log page                                                     */
    /* ------------------------------------------------------------------ */
    private JPanel buildActivityPage() {
        JPanel page = new JPanel(new BorderLayout(0, 16));
        page.setOpaque(false);
        page.setBorder(new EmptyBorder(18, 36, 32, 36));

        JPanel heading = new JPanel(new BorderLayout());
        heading.setOpaque(false);
        JPanel labels = new JPanel(new GridLayout(2, 1, 0, 3));
        labels.setOpaque(false);
        labels.add(new TLabel("Chronological simulation events", ROLE_TEXT, Font.BOLD, 17));
        labels.add(eventCount);
        heading.add(labels, BorderLayout.WEST);
        clear.addActionListener(e -> {
            logEntries.clear();
            renderLog();
        });
        heading.add(clear, BorderLayout.EAST);
        page.add(heading, BorderLayout.NORTH);

        activity.setEditable(false);
        activity.setOpaque(true);
        activity.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        activity.setBorder(new EmptyBorder(18, 22, 18, 22));
        JScrollPane scroll = new JScrollPane(activity);
        styleScroll(scroll);

        Card card = new Card(new BorderLayout());
        card.setBorder(new EmptyBorder(6, 6, 6, 6));
        card.add(scroll, BorderLayout.CENTER);
        page.add(card, BorderLayout.CENTER);
        return page;
    }

    /* ------------------------------------------------------------------ */
    /*  Simulation                                                         */
    /* ------------------------------------------------------------------ */
    private void startSimulation() {
        int roomCapacity = capacity.getValue();
        int doctorCount = doctors.getValue();
        totalPatients = patients.getValue();
        resetRunView(doctorCount, totalPatients);
        setInputsEnabled(false);
        allDischarged = false;
        setStatus("Simulation running", Kind.RUN);
        startedAt = System.nanoTime();
        elapsedTimer = new Timer(1000, e -> updateElapsed());
        elapsedTimer.start();

        Thread simulation = new Thread(() -> {
            try {
                Hospital.runSimulation(roomCapacity, doctorCount, totalPatients,
                        message -> SwingUtilities.invokeLater(() -> addEvent(message)));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                SwingUtilities.invokeLater(() -> {
                    addEvent("Simulation was interrupted.");
                    setStatus("Simulation interrupted", Kind.ERROR);
                });
            } catch (RuntimeException e) {
                SwingUtilities.invokeLater(() -> addEvent("Simulation error: " + e.getMessage()));
            } finally {
                SwingUtilities.invokeLater(() -> {
                    if (elapsedTimer != null) elapsedTimer.stop();
                    setInputsEnabled(true);
                    if (!allDischarged && statusKind != Kind.ERROR) setStatus("Simulation finished", Kind.DONE);
                });
            }
        }, "hospital-simulation");
        simulation.start();
    }

    private void resetRunView(int doctorCount, int patientCount) {
        logEntries.clear();
        renderLog();
        patientModel.setRowCount(0);
        doctorModel.setRowCount(0);
        patientRows.clear();
        doctorRows.clear();
        waitingCount = roomsInUse = treatedPatients = 0;
        waitingValue.setText("0");
        treatingValue.setText("0 / " + TREATMENT_ROOMS);
        dischargedValue.setText("0");
        elapsedValue.setText("00:00");
        treatedLabel.setText("0 of " + patientCount + " treated");
        progress.set(0, patientCount);
        for (int i = 1; i <= doctorCount; i++) {
            doctorRows.put(i, doctorModel.getRowCount());
            doctorModel.addRow(new Object[]{"Doctor " + i, "Available", "—"});
        }
        for (int i = 1; i <= patientCount; i++) {
            String id = "P" + i;
            patientRows.put(id, patientModel.getRowCount());
            patientModel.addRow(new Object[]{id, "Not arrived", "—"});
        }
    }

    private void addEvent(String message) {
        String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        logEntries.add(new String[]{time, message});
        if (logEntries.size() == 1) renderLog();
        else {
            appendEntry(activity.getStyledDocument(), time, message);
            eventCount.setText(logEntries.size() + " events");
            activity.setCaretPosition(activity.getDocument().getLength());
        }

        if (message.contains("arrived and joined")) updatePatient(patientId(message), "Arrived", null);
        if (message.contains("is waiting for a doctor")) {
            waitingCount++;
            waitingValue.setText(String.valueOf(waitingCount));
            updatePatient(patientId(message), "Waiting", null);
        }
        if (message.contains("is assigned to")) {
            int doctorId = doctorId(message);
            String patient = patientId(message);
            waitingCount = Math.max(0, waitingCount - 1);
            waitingValue.setText(String.valueOf(waitingCount));
            setDoctor(doctorId, "Preparing", patient);
            updatePatient(patient, "Assigned", "Doctor " + doctorId);
        }
        if (message.contains("begins treating")) {
            roomsInUse = Math.min(TREATMENT_ROOMS, roomsInUse + 1);
            treatingValue.setText(roomsInUse + " / " + TREATMENT_ROOMS);
            int doctorId = doctorId(message);
            String patient = patientId(message);
            setDoctor(doctorId, "Treating", patient);
            updatePatient(patient, "In treatment", "Doctor " + doctorId);
        }
        if (message.contains("finished treating")) {
            treatedPatients++;
            dischargedValue.setText(String.valueOf(treatedPatients));
            treatedLabel.setText(treatedPatients + " of " + totalPatients + " treated");
            progress.set(treatedPatients, totalPatients);
            roomsInUse = Math.max(0, roomsInUse - 1);
            treatingValue.setText(roomsInUse + " / " + TREATMENT_ROOMS);
            updatePatient(patientId(message), "Treated", null);
        }
        if (message.contains("is now available")) setDoctor(doctorId(message), "Available", "—");
        if (message.startsWith("Simulation complete")) {
            allDischarged = true;
            setStatus("All patients discharged", Kind.DONE);
        }
        if (message.startsWith("Simulation error:")) setStatus("Simulation error", Kind.ERROR);
    }

    private void updatePatient(String id, String state, String assignedDoctor) {
        Integer row = patientRows.get(id);
        if (row == null) return;
        patientModel.setValueAt(state, row, 1);
        if (assignedDoctor != null) patientModel.setValueAt(assignedDoctor, row, 2);
    }

    private void setDoctor(int id, String state, String patient) {
        Integer row = doctorRows.get(id);
        if (row == null) return;
        doctorModel.setValueAt(state, row, 1);
        doctorModel.setValueAt(patient, row, 2);
    }

    private static String patientId(String message) {
        Matcher matcher = Pattern.compile("P\\d+").matcher(message);
        return matcher.find() ? matcher.group() : "";
    }

    private static int doctorId(String message) {
        Matcher matcher = Pattern.compile("Doctor (\\d+)").matcher(message);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : -1;
    }

    private void updateElapsed() {
        long seconds = Duration.ofNanos(System.nanoTime() - startedAt).toSeconds();
        elapsedValue.setText(String.format("%02d:%02d", seconds / 60, seconds % 60));
    }

    private void setInputsEnabled(boolean enabled) {
        capacity.setEnabled(enabled);
        doctors.setEnabled(enabled);
        patients.setEnabled(enabled);
        start.setEnabled(enabled);
    }

    /* ------------------------------------------------------------------ */
    /*  Log rendering                                                      */
    /* ------------------------------------------------------------------ */
    private void renderLog() {
        activity.setText("");
        StyledDocument doc = activity.getStyledDocument();
        if (logEntries.isEmpty()) {
            appendText(doc, "Events appear here when the simulation starts.\n\n"
                    + "Configure the run in Operations, then press Start simulation.", t.muted, false);
            eventCount.setText("0 events");
        } else {
            for (String[] e : logEntries) appendEntry(doc, e[0], e[1]);
            eventCount.setText(logEntries.size() + " events");
            activity.setCaretPosition(doc.getLength());
        }
    }

    private void appendEntry(StyledDocument doc, String time, String message) {
        appendText(doc, "[" + time + "]  ", alpha(t.muted, 200), false);
        appendText(doc, message + "\n", eventColor(message), message.startsWith("Simulation"));
    }

    private void appendText(StyledDocument doc, String s, Color color, boolean bold) {
        SimpleAttributeSet a = new SimpleAttributeSet();
        StyleConstants.setForeground(a, color);
        StyleConstants.setBold(a, bold);
        StyleConstants.setFontFamily(a, Font.MONOSPACED);
        StyleConstants.setFontSize(a, 14);
        try {
            doc.insertString(doc.getLength(), s, a);
        } catch (BadLocationException ignored) {
        }
    }

    private Color eventColor(String m) {
        if (m.startsWith("Simulation error") || m.contains("interrupted")) return t.red;
        if (m.startsWith("Simulation complete")) return t.green;
        if (m.contains("finished treating")) return t.green;
        if (m.contains("begins treating")) return t.accent;
        if (m.contains("is assigned to")) return t.violet;
        if (m.contains("is waiting for a doctor")) return t.amber;
        if (m.contains("arrived and joined")) return t.sky;
        if (m.contains("is now available")) return t.muted;
        return t.text;
    }

    /* ------------------------------------------------------------------ */
    /*  Theme / status / navigation                                        */
    /* ------------------------------------------------------------------ */
    private void applyTheme(Theme next) {
        t = next;
        themeBtn.setText(t == Theme.DARK ? "☀  Light mode" : "☾  Dark mode");
        activity.setBackground(t.card);
        for (JScrollPane s : scrolls) s.getViewport().setBackground(t.card);
        for (JTable tb : new JTable[]{patientTable, doctorTable}) {
            tb.setBackground(t.card);
            tb.setGridColor(t.border);
        }
        renderLog();
        refreshStatus();
        repaint();
    }

    private void setStatus(String message, Kind kind) {
        statusText = message;
        statusKind = kind;
        refreshStatus();
    }

    private void refreshStatus() {
        Color color = t.accent;
        if (statusKind == Kind.RUN) color = t.amber;
        else if (statusKind == Kind.DONE) color = t.green;
        else if (statusKind == Kind.ERROR) color = t.red;
        statusPill.set(statusText, color, statusKind == Kind.RUN);
    }

    private void showPage(String page) {
        pageLayout.show(pages, page);
        boolean overview = page.equals("Operations");
        operationsNav.setActive(overview);
        activityNav.setActive(!overview);
        pageTitle.setText(overview ? "Operations overview" : "Event log");
        setTitle(overview ? "Emergency Room | Operations" : "Emergency Room | Event log");
    }

    /* ------------------------------------------------------------------ */
    /*  Builders / helpers                                                 */
    /* ------------------------------------------------------------------ */
    private JPanel metric(String label, JLabel value, String unit, Supplier<Color> bar) {
        Card panel = new Card(new BorderLayout(0, 4));
        panel.bar(bar);
        panel.setBorder(new EmptyBorder(20, 22, 18, 22));
        panel.add(new TLabel(label, ROLE_MUTED, Font.BOLD, 12), BorderLayout.NORTH);
        panel.add(value, BorderLayout.CENTER);
        panel.add(new TLabel(unit, ROLE_MUTED, Font.PLAIN, 13), BorderLayout.SOUTH);
        return panel;
    }

    private JPanel field(String eyebrow, String label, Stepper stepper) {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setOpaque(false);
        JPanel labels = new JPanel(new GridLayout(2, 1, 0, 2));
        labels.setOpaque(false);
        labels.add(new TLabel(eyebrow, ROLE_ACCENT, Font.BOLD, 11));
        labels.add(new TLabel(label, ROLE_TEXT, Font.PLAIN, 14));
        JPanel wrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        wrap.setOpaque(false);
        wrap.add(stepper);
        panel.add(labels, BorderLayout.NORTH);
        panel.add(wrap, BorderLayout.CENTER);
        return panel;
    }

    private void styleScroll(JScrollPane s) {
        s.setBorder(null);
        s.setOpaque(false);
        s.getViewport().setOpaque(true);
        s.getVerticalScrollBar().setUI(new SlimScrollBarUI());
        s.getHorizontalScrollBar().setUI(new SlimScrollBarUI());
        s.getVerticalScrollBar().setOpaque(false);
        s.getHorizontalScrollBar().setOpaque(false);
        s.getVerticalScrollBar().setPreferredSize(new Dimension(12, 12));
        s.getHorizontalScrollBar().setPreferredSize(new Dimension(12, 12));
        scrolls.add(s);
    }

    private void styleTable(JTable table) {
        table.setRowHeight(46);
        table.setFillsViewportHeight(true);
        table.setShowGrid(false);
        table.setShowHorizontalLines(true);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setResizingAllowed(false);
        table.getTableHeader().setPreferredSize(new Dimension(1, 38));
        table.getTableHeader().setDefaultRenderer(new HeaderRenderer());
        CellRenderer plain = new CellRenderer();
        BadgeRenderer badge = new BadgeRenderer();
        for (int i = 0; i < table.getColumnModel().getColumnCount(); i++)
            table.getColumnModel().getColumn(i).setCellRenderer(i == 1 ? badge : plain);
        table.getColumnModel().getColumn(0).setPreferredWidth(90);
        table.getColumnModel().getColumn(1).setPreferredWidth(190);
        table.getColumnModel().getColumn(2).setPreferredWidth(130);
    }

    private static DefaultTableModel model(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
    }

    private static Font font(int style, int size) { return new Font(FONT, style, size); }

    private static Color alpha(Color c, int a) { return new Color(c.getRed(), c.getGreen(), c.getBlue(), a); }

    private static void aa(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }

    private Color statusColor(String s) {
        if (s.equals("Treating") || s.equals("In treatment")) return t.accent;
        if (s.equals("Treated") || s.equals("Available")) return t.green;
        if (s.equals("Waiting")) return t.amber;
        if (s.equals("Assigned") || s.equals("Preparing")) return t.violet;
        if (s.equals("Arrived")) return t.sky;
        return t.muted;
    }

    /* ------------------------------------------------------------------ */
    /*  Custom components                                                  */
    /* ------------------------------------------------------------------ */
    private class BgPanel extends JPanel {
        BgPanel(LayoutManager lm) { super(lm); }
        @Override protected void paintComponent(Graphics g) {
            g.setColor(t.bg);
            g.fillRect(0, 0, getWidth(), getHeight());
        }
    }

    private class GradientPanel extends JPanel {
        GradientPanel(LayoutManager lm) { super(lm); }
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setPaint(new GradientPaint(0, 0, t.sidebarTop, 0, getHeight(), t.sidebarBottom));
            g.fillRect(0, 0, getWidth(), getHeight());
            g.dispose();
        }
    }

    private class Card extends JPanel {
        private Supplier<Color> bar;
        Card(LayoutManager lm) { super(lm); setOpaque(false); }
        Card bar(Supplier<Color> s) { bar = s; return this; }
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            aa(g);
            int w = getWidth(), h = getHeight();
            Shape shape = new RoundRectangle2D.Float(0, 0, w - 1, h - 1, 20, 20);
            g.setColor(t.card);
            g.fill(shape);
            if (bar != null) {
                Shape old = g.getClip();
                g.clip(shape);
                g.setColor(bar.get());
                g.fillRect(0, 0, w, 3);
                g.setClip(old);
            }
            g.setColor(t.border);
            g.draw(shape);
            g.dispose();
        }
    }

    /** Label whose colour follows the active theme. */
    private class TLabel extends JLabel {
        private final int role;
        TLabel(String text, int role, int style, int size) {
            super(text);
            this.role = role;
            setFont(font(style, size));
        }
        @Override public Color getForeground() {
            if (t == null) return super.getForeground();
            if (role == ROLE_ACCENT) return t.accent;
            if (role == ROLE_MUTED || !isEnabled()) return t.muted;
            return t.text;
        }
    }

    private class Logo extends JComponent {
        Logo() { setPreferredSize(new Dimension(42, 42)); }
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            aa(g);
            g.setPaint(new GradientPaint(0, 0, Theme.DARK.accent, 42, 42, Theme.DARK.accent2));
            g.fillRoundRect(0, 0, 42, 42, 14, 14);
            g.setColor(Color.WHITE);
            g.fillRoundRect(18, 9, 6, 24, 3, 3);
            g.fillRoundRect(9, 18, 24, 6, 3, 3);
            g.dispose();
        }
    }

    private class RoundButton extends JButton {
        private final int kind;
        private boolean hover;

        RoundButton(String text, int kind, int vp, int hp) {
            super(text);
            this.kind = kind;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setBorder(new EmptyBorder(vp, hp, vp, hp));
            setFont(font(Font.BOLD, 14));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hover = false; repaint(); }
            });
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            aa(g);
            int w = getWidth(), h = getHeight();
            Color fg;
            if (kind == BTN_PRIMARY) {
                if (isEnabled()) {
                    Color a = hover ? t.accent.brighter() : t.accent;
                    g.setPaint(new GradientPaint(0, 0, a, w, h, t.accent2));
                    fg = t == Theme.DARK ? c(5, 24, 32) : Color.WHITE;
                } else {
                    g.setColor(alpha(t.muted, 60));
                    fg = alpha(t.muted, 200);
                }
                g.fillRoundRect(0, 0, w, h, 14, 14);
            } else {
                int a = hover && isEnabled() ? 30 : (kind == BTN_GHOST ? 12 : 0);
                g.setColor(alpha(t.text, a));
                g.fillRoundRect(0, 0, w, h, 14, 14);
                if (kind == BTN_GHOST) {
                    g.setColor(t.border);
                    g.drawRoundRect(0, 0, w - 1, h - 1, 14, 14);
                }
                fg = isEnabled() ? t.text : alpha(t.muted, 120);
            }
            g.setFont(getFont());
            FontMetrics fm = g.getFontMetrics();
            g.setColor(fg);
            g.drawString(getText(), (w - fm.stringWidth(getText())) / 2,
                    (h - fm.getHeight()) / 2 + fm.getAscent());
            g.dispose();
        }
    }

    private class NavButton extends JButton {
        private boolean selected, hover;
        private final String glyph;

        NavButton(String glyph, String label, boolean selected) {
            super(label);
            this.glyph = glyph;
            this.selected = selected;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setHorizontalAlignment(SwingConstants.LEFT);
            setBorder(new EmptyBorder(13, 46, 13, 12));
            setFont(font(Font.BOLD, 14));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hover = false; repaint(); }
            });
        }

        void setActive(boolean s) { selected = s; repaint(); }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            aa(g);
            int w = getWidth(), h = getHeight();
            if (selected) {
                g.setColor(new Color(255, 255, 255, 26));
                g.fillRoundRect(0, 0, w, h, 14, 14);
                g.setColor(Theme.DARK.accent);
                g.fillRoundRect(0, 10, 4, h - 20, 4, 4);
            } else if (hover) {
                g.setColor(new Color(255, 255, 255, 12));
                g.fillRoundRect(0, 0, w, h, 14, 14);
            }
            g.setFont(font(Font.BOLD, 16));
            FontMetrics gm = g.getFontMetrics();
            g.setColor(selected ? Theme.DARK.accent : c(140, 160, 184));
            g.drawString(glyph, 18, (h - gm.getHeight()) / 2 + gm.getAscent());
            g.setFont(getFont());
            FontMetrics fm = g.getFontMetrics();
            g.setColor(selected ? Color.WHITE : c(190, 204, 218));
            g.drawString(getText(), 46, (h - fm.getHeight()) / 2 + fm.getAscent());
            g.dispose();
        }
    }

    /** Compact − value + control replacing JSpinner. */
    private class Stepper extends JPanel {
        private int value;
        private final int min, max;
        private final TLabel label = new TLabel("", ROLE_TEXT, Font.BOLD, 19);
        private final RoundButton minus = new RoundButton("−", BTN_FLAT, 5, 15);
        private final RoundButton plus = new RoundButton("+", BTN_FLAT, 5, 15);

        Stepper(int value, int min, int max) {
            super(new BorderLayout(4, 0));
            this.min = min;
            this.max = max;
            setOpaque(false);
            setBorder(new EmptyBorder(4, 4, 4, 4));
            label.setHorizontalAlignment(SwingConstants.CENTER);
            minus.setFont(font(Font.BOLD, 20));
            plus.setFont(font(Font.BOLD, 20));
            add(minus, BorderLayout.WEST);
            add(label, BorderLayout.CENTER);
            add(plus, BorderLayout.EAST);
            minus.addActionListener(e -> set(this.value - 1));
            plus.addActionListener(e -> set(this.value + 1));
            setPreferredSize(new Dimension(176, 50));
            set(value);
        }

        int getValue() { return value; }

        void set(int v) {
            value = Math.max(min, Math.min(max, v));
            label.setText(String.valueOf(value));
        }

        @Override public void setEnabled(boolean e) {
            super.setEnabled(e);
            if (minus == null) return;
            minus.setEnabled(e);
            plus.setEnabled(e);
            label.setEnabled(e);
            repaint();
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            aa(g);
            g.setColor(t.inputBg);
            g.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
            g.setColor(t.border);
            g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
            g.dispose();
        }
    }

    private class Pill extends JLabel {
        private Color color;
        private boolean pulse;
        private float phase;
        private final Timer timer = new Timer(60, e -> { phase += 0.18f; repaint(); });

        Pill() {
            setFont(font(Font.BOLD, 13));
            setBorder(new EmptyBorder(9, 34, 9, 18));
        }

        void set(String text, Color c, boolean pulsing) {
            setText(text);
            color = c;
            pulse = pulsing;
            if (pulsing) timer.start(); else timer.stop();
            repaint();
        }

        @Override public Color getForeground() { return color == null ? super.getForeground() : color; }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            aa(g);
            int w = getWidth(), h = getHeight();
            Color col = color == null ? Color.GRAY : color;
            g.setColor(alpha(col, 34));
            g.fillRoundRect(0, 0, w, h, h, h);
            g.setColor(alpha(col, 90));
            g.drawRoundRect(0, 0, w - 1, h - 1, h, h);
            int cx = 18, cy = h / 2;
            if (pulse) {
                int halo = (int) (6 + 4 * Math.sin(phase));
                g.setColor(alpha(col, 50));
                g.fillOval(cx - halo, cy - halo, halo * 2, halo * 2);
            }
            g.setColor(col);
            g.fillOval(cx - 4, cy - 4, 8, 8);
            g.dispose();
            super.paintComponent(g0);
        }
    }

    private class RoundProgress extends JComponent {
        private int value, max = 1;
        RoundProgress() { setPreferredSize(new Dimension(1, 22)); }
        void set(int v, int m) { value = v; max = Math.max(1, m); repaint(); }
        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            aa(g);
            int w = getWidth(), h = 8, y = getHeight() - h;
            g.setColor(alpha(t.muted, 50));
            g.fillRoundRect(0, y, w, h, h, h);
            int fill = (int) (w * Math.min(1.0, value / (double) max));
            if (fill > 0) {
                g.setPaint(new GradientPaint(0, 0, t.accent, w, 0, t.accent2));
                g.fillRoundRect(0, y, Math.max(fill, h), h, h, h);
            }
            g.dispose();
        }
    }

    private class SlimScrollBarUI extends BasicScrollBarUI {
        private JButton zero() {
            JButton b = new JButton();
            Dimension d = new Dimension(0, 0);
            b.setPreferredSize(d);
            b.setMinimumSize(d);
            b.setMaximumSize(d);
            return b;
        }
        @Override protected JButton createDecreaseButton(int o) { return zero(); }
        @Override protected JButton createIncreaseButton(int o) { return zero(); }
        @Override protected void paintTrack(Graphics g, JComponent c, Rectangle r) { }
        @Override protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
            if (r.isEmpty()) return;
            Graphics2D g2 = (Graphics2D) g.create();
            aa(g2);
            g2.setColor(alpha(t.muted, isDragging ? 170 : 90));
            g2.fillRoundRect(r.x + 3, r.y + 2, r.width - 6, r.height - 4, 8, 8);
            g2.dispose();
        }
    }

    /* ------------------------------------------------------------------ */
    /*  Table renderers                                                    */
    /* ------------------------------------------------------------------ */
    private class HeaderRenderer extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(JTable tb, Object v, boolean sel,
                                                                 boolean focus, int row, int col) {
            super.getTableCellRendererComponent(tb, v, false, false, row, col);
            setBackground(t.card);
            setForeground(t.muted);
            setFont(font(Font.BOLD, 11));
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, t.border), new EmptyBorder(0, 12, 0, 8)));
            return this;
        }
    }

    private class CellRenderer extends DefaultTableCellRenderer {
        @Override public Component getTableCellRendererComponent(JTable tb, Object v, boolean sel,
                                                                 boolean focus, int row, int col) {
            super.getTableCellRendererComponent(tb, v, sel, false, row, col);
            setBorder(new EmptyBorder(0, 12, 0, 8));
            setBackground(sel ? t.selection : (row % 2 == 0 ? t.card : t.rowAlt));
            String s = v == null ? "" : v.toString();
            setForeground(s.equals("—") ? alpha(t.muted, 150) : t.text);
            setFont(font(col == 0 ? Font.BOLD : Font.PLAIN, 14));
            return this;
        }
    }

    private class BadgeRenderer extends DefaultTableCellRenderer {
        private Color badge = Color.GRAY;

        @Override public Component getTableCellRendererComponent(JTable tb, Object v, boolean sel,
                                                                 boolean focus, int row, int col) {
            super.getTableCellRendererComponent(tb, v, sel, false, row, col);
            String s = v == null ? "" : v.toString();
            setText(s);
            badge = statusColor(s);
            setBackground(sel ? t.selection : (row % 2 == 0 ? t.card : t.rowAlt));
            return this;
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            aa(g);
            g.setColor(getBackground());
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setFont(font(Font.BOLD, 12));
            FontMetrics fm = g.getFontMetrics();
            String s = getText();
            int bw = fm.stringWidth(s) + 36, bh = 26, x = 10, y = (getHeight() - bh) / 2;
            g.setColor(alpha(badge, 38));
            g.fillRoundRect(x, y, bw, bh, bh, bh);
            g.setColor(badge);
            g.fillOval(x + 11, y + bh / 2 - 3, 6, 6);
            g.drawString(s, x + 24, y + (bh - fm.getHeight()) / 2 + fm.getAscent());
            g.dispose();
        }
    }

    /* ------------------------------------------------------------------ */
    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        SwingUtilities.invokeLater(() -> new HospitalGUI().setVisible(true));
    }
}