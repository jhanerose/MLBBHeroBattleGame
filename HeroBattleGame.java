import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.List;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

public class HeroBattleGame extends JFrame {

    // List to store heroes from the CSV file
    private java.util.List<Hero> heroes = new ArrayList<>();

    // UI components
    private JComboBox<String> hero1Dropdown;
    private JComboBox<String> hero2Dropdown;
    private JPanel hero1StatsPanel;
    private JPanel hero2StatsPanel;
    private JLabel hero1ProbabilityLabel;
    private JLabel hero2ProbabilityLabel;
    private JProgressBar probabilityProgressBar;
    private BackgroundPanel backgroundPanel;

    // Default background image path (if needed)
    private String defaultBackgroundPath = "your-default-background-image.jpg";

    // Flag to avoid showing the dialog repeatedly for the same selection.
    private String lastWinner = "";

    public HeroBattleGame() {
        setTitle("MLBB Hero Battle Game");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 750);
        setLocationRelativeTo(null);

        // Use a custom background panel as the content pane.
        backgroundPanel = new BackgroundPanel();
        backgroundPanel.setLayout(new BorderLayout());
        // Set a very dark purple background.
        backgroundPanel.setBackground(new Color(30, 0, 30));
        setContentPane(backgroundPanel);

        // ----- Top Panel: File Upload Button and New Durable Hero Button -----
        JPanel topPanel = new JPanel(new FlowLayout());
        topPanel.setOpaque(false);
        // Using the previous button style (purple-to-gold gradient)
        JButton uploadButton = createStyledButton("Upload MLBB Hero Dataset");
        JButton showDurableButton = createStyledButton("Show Most Durable Hero");
        topPanel.add(uploadButton);
        topPanel.add(showDurableButton);
        backgroundPanel.add(topPanel, BorderLayout.NORTH);

        // ----- Center Panel: Two Hero Selection Panels -----
        JPanel centerPanel = new JPanel(new GridLayout(1, 2, 10, 10));
        centerPanel.setOpaque(false);
        // Use a dim gold for borders and titles.
        Color titleColor = new Color(184, 134, 11); // Dark Goldenrod
        Border lineBorder = BorderFactory.createLineBorder(titleColor, 2);
        Font titleFont = new Font("Arial", Font.BOLD, 14);

        // --- Left: Hero 1 Panel ---
        JPanel hero1Panel = new JPanel(new BorderLayout());
        hero1Panel.setOpaque(false);
        hero1Panel.setBorder(BorderFactory.createTitledBorder(lineBorder, "Hero 1",
                TitledBorder.LEADING, TitledBorder.TOP, titleFont, titleColor));
        hero1Dropdown = new JComboBox<>();
        hero1Dropdown.addItem("-- Select Hero 1 --");
        hero1Panel.add(hero1Dropdown, BorderLayout.NORTH);
        hero1StatsPanel = new JPanel();
        hero1StatsPanel.setOpaque(false);
        hero1StatsPanel.setLayout(new BoxLayout(hero1StatsPanel, BoxLayout.Y_AXIS));
        hero1Panel.add(hero1StatsPanel, BorderLayout.CENTER);

        // --- Right: Hero 2 Panel ---
        JPanel hero2Panel = new JPanel(new BorderLayout());
        hero2Panel.setOpaque(false);
        hero2Panel.setBorder(BorderFactory.createTitledBorder(lineBorder, "Hero 2",
                TitledBorder.LEADING, TitledBorder.TOP, titleFont, titleColor));
        hero2Dropdown = new JComboBox<>();
        hero2Dropdown.addItem("-- Select Hero 2 --");
        hero2Panel.add(hero2Dropdown, BorderLayout.NORTH);
        hero2StatsPanel = new JPanel();
        hero2StatsPanel.setOpaque(false);
        hero2StatsPanel.setLayout(new BoxLayout(hero2StatsPanel, BoxLayout.Y_AXIS));
        hero2Panel.add(hero2StatsPanel, BorderLayout.CENTER);

        centerPanel.add(hero1Panel);
        centerPanel.add(hero2Panel);
        backgroundPanel.add(centerPanel, BorderLayout.CENTER);

        // ----- Bottom Panel: Battle Prediction -----
        JPanel bottomPanel = new JPanel();
        bottomPanel.setOpaque(false);
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel battleTitle = new JLabel("Battle Prediction");
        battleTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        battleTitle.setFont(new Font("Arial", Font.BOLD, 18));
        battleTitle.setForeground(titleColor);
        bottomPanel.add(battleTitle);

        // Panel for the probability labels.
        JPanel probLabelsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 5));
        probLabelsPanel.setOpaque(false);
        hero1ProbabilityLabel = new JLabel("");
        hero1ProbabilityLabel.setForeground(titleColor);
        hero2ProbabilityLabel = new JLabel("");
        hero2ProbabilityLabel.setForeground(titleColor);
        probLabelsPanel.add(hero1ProbabilityLabel);
        probLabelsPanel.add(hero2ProbabilityLabel);
        bottomPanel.add(probLabelsPanel);

        // A progress bar representing hero1's probability.
        probabilityProgressBar = new JProgressBar(0, 100);
        probabilityProgressBar.setValue(0);
        probabilityProgressBar.setStringPainted(true);
        probabilityProgressBar.setForeground(titleColor);
        bottomPanel.add(probabilityProgressBar);

        // --- Add the formula label ---
        JLabel formulaLabel = new JLabel("<html><body style='text-align:center;'>"
                + "<h2>How is this Calculated?</h2>"
                + "<p><strong>The battle prediction uses a weighted scoring system that considers:</p>"
                + "<p><strong>Survival Score</strong> = (HP × 0.35) + (Physical Defense × 0.25) + (Magic Defense × 0.25) + (HP Regen × 0.15)</p>"
                + "<p><strong>Attack Score</strong> = (Physical Attack × 0.4) + (Attack Speed × 0.3) + (Skill Effectiveness × 0.3)</p>"
                + "<p><strong>Utility Score</strong> = (Movement Speed × 0.4) + (Mana Regen × 0.3) + (Win Rate × 0.3)</p>"
                + "</body></html>");
        formulaLabel.setForeground(titleColor);
        formulaLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        formulaLabel.setHorizontalAlignment(SwingConstants.CENTER);
        formulaLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        bottomPanel.add(formulaLabel);

        backgroundPanel.add(bottomPanel, BorderLayout.SOUTH);

        // ----- Action Listeners -----
        uploadButton.addActionListener(e -> uploadCSV());
        showDurableButton.addActionListener(e -> showMostDurableHero());

        hero1Dropdown.addActionListener(e -> {
            String selected = (String) hero1Dropdown.getSelectedItem();
            Hero hero = getHeroByName(selected);
            updateHeroStats(hero, hero1StatsPanel);
            showBattlePrediction();
        });

        hero2Dropdown.addActionListener(e -> {
            String selected = (String) hero2Dropdown.getSelectedItem();
            Hero hero = getHeroByName(selected);
            updateHeroStats(hero, hero2StatsPanel);
            showBattlePrediction();
        });
    }

    // Opens a file chooser to select a CSV file.
    private void uploadCSV() {
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(this);
        if(result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            loadCSV(file);
        }
    }

    // Reads the CSV file and populates the heroes list.
    private void loadCSV(File file) {
        heroes.clear();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String headerLine = br.readLine(); // Read header row.
            if(headerLine == null) return;
            String[] headers = headerLine.split(",");
            int heroNameIdx    = indexOf(headers, "hero_name");
            int roleIdx        = indexOf(headers, "role");
            int hpIdx          = indexOf(headers, "hp");
            int physicalAtkIdx = indexOf(headers, "physical_atk");
            int physicalDefIdx = indexOf(headers, "physical_defense");
            int magicDefIdx    = indexOf(headers, "magic_defense");
            int winRateIdx     = indexOf(headers, "win_rate");

            String line;
            while((line = br.readLine()) != null) {
                if(line.trim().isEmpty()) continue;
                String[] values = line.split(",");
                String heroName = heroNameIdx >= 0 && heroNameIdx < values.length ? values[heroNameIdx].trim() : "";
                String role = roleIdx >= 0 && roleIdx < values.length ? values[roleIdx].trim() : "";
                double hp = hpIdx >= 0 && hpIdx < values.length ? parseDouble(values[hpIdx].trim()) : 0;
                double physicalAtk = physicalAtkIdx >= 0 && physicalAtkIdx < values.length ? parseDouble(values[physicalAtkIdx].trim()) : 0;
                double physicalDefense = physicalDefIdx >= 0 && physicalDefIdx < values.length ? parseDouble(values[physicalDefIdx].trim()) : 0;
                double magicDefense = magicDefIdx >= 0 && magicDefIdx < values.length ? parseDouble(values[magicDefIdx].trim()) : 0;
                double winRate = winRateIdx >= 0 && winRateIdx < values.length ? parseDouble(values[winRateIdx].trim()) : 0;
                Hero hero = new Hero(heroName, role, hp, physicalAtk, physicalDefense, magicDefense, winRate);
                heroes.add(hero);
            }
            updateDropdowns();
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading CSV file: " + ex.getMessage());
        }
    }

    // Returns the index of a key in the headers array (case-insensitive).
    private int indexOf(String[] headers, String key) {
        for (int i = 0; i < headers.length; i++) {
            if(headers[i].trim().equalsIgnoreCase(key)) {
                return i;
            }
        }
        return -1;
    }

    // Safely parses a double value; returns 0 if parsing fails.
    private double parseDouble(String s) {
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // Updates both dropdowns with hero names from the CSV, sorted alphabetically.
    private void updateDropdowns() {
        hero1Dropdown.removeAllItems();
        hero2Dropdown.removeAllItems();
        hero1Dropdown.addItem("-- Choose Hero 1 --");
        hero2Dropdown.addItem("-- Choose Hero 2 --");

        List<String> heroNames = new ArrayList<>();
        for (Hero hero : heroes) {
            heroNames.add(hero.heroName);
        }
        Collections.sort(heroNames, String.CASE_INSENSITIVE_ORDER);
        for (String name : heroNames) {
            hero1Dropdown.addItem(name);
            hero2Dropdown.addItem(name);
        }
    }

    // Finds and returns a Hero object by name.
    private Hero getHeroByName(String name) {
        if (name == null || name.startsWith("--"))
            return null;
        for (Hero hero : heroes) {
            if (hero.heroName.equals(name))
                return hero;
        }
        return null;
    }

    // Updates the stats panel for a given hero and displays the hero image.
    private void updateHeroStats(Hero hero, JPanel panel) {
        panel.removeAll();
        if (hero == null) {
            panel.revalidate();
            panel.repaint();
            return;
        }
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        // Attempt to load and display the hero image.
        try {
            String imgPath = "ALL HEROES/" + hero.heroName + ".png";
            File file = new File(imgPath);
            if (file.exists()) {
                BufferedImage img = ImageIO.read(file);
                Image scaled = img.getScaledInstance(100, 100, Image.SCALE_SMOOTH);
                JLabel imageLabel = new JLabel(new ImageIcon(scaled));
                imageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
                panel.add(imageLabel);
                panel.add(Box.createVerticalStrut(5));
            }
        } catch(Exception ex) {
            ex.printStackTrace();
        }

        JLabel nameLabel = new JLabel(hero.heroName);
        nameLabel.setFont(new Font("Arial", Font.BOLD, 14));
        nameLabel.setForeground(new Color(184, 134, 11));
        nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(nameLabel);

        JLabel roleLabel = new JLabel("Role: " + hero.role);
        roleLabel.setForeground(new Color(184, 134, 11));
        roleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(roleLabel);

        JLabel hpLabel = new JLabel("HP: " + hero.hp);
        hpLabel.setForeground(new Color(184, 134, 11));
        hpLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(hpLabel);

        JProgressBar hpBar = new JProgressBar(0, 100);
        int hpPercent = (int) Math.min((hero.hp / 10), 100);
        hpBar.setValue(hpPercent);
        hpBar.setStringPainted(true);
        hpBar.setForeground(new Color(184, 134, 11));
        panel.add(hpBar);

        JLabel physicalAtkLabel = new JLabel("Physical Attack: " + hero.physicalAtk);
        physicalAtkLabel.setForeground(new Color(184, 134, 11));
        physicalAtkLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(physicalAtkLabel);

        JLabel physicalDefLabel = new JLabel("Physical Defense: " + hero.physicalDefense);
        physicalDefLabel.setForeground(new Color(184, 134, 11));
        physicalDefLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(physicalDefLabel);

        JLabel magicDefLabel = new JLabel("Magic Defense: " + hero.magicDefense);
        magicDefLabel.setForeground(new Color(184, 134, 11));
        magicDefLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(magicDefLabel);

        JLabel winRateLabel = new JLabel("Win Rate: " + hero.winRate + "%");
        winRateLabel.setForeground(new Color(184, 134, 11));
        winRateLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(winRateLabel);

        panel.revalidate();
        panel.repaint();
    }

    // Updates the battle prediction UI and shows the winner dialog.
    private void showBattlePrediction() {
        String hero1Name = (String) hero1Dropdown.getSelectedItem();
        String hero2Name = (String) hero2Dropdown.getSelectedItem();
        if (hero1Name == null || hero2Name == null ||
            hero1Name.startsWith("--") || hero2Name.startsWith("--")) {
            hero1ProbabilityLabel.setText("");
            hero2ProbabilityLabel.setText("");
            probabilityProgressBar.setValue(0);
            return;
        }
        Hero hero1 = getHeroByName(hero1Name);
        Hero hero2 = getHeroByName(hero2Name);
        if (hero1 == null || hero2 == null)
            return;

        double hero1Score = (hero1.hp * 0.4) + (hero1.physicalAtk * 0.4) + (hero1.winRate * 0.2);
        double hero2Score = (hero2.hp * 0.4) + (hero2.physicalAtk * 0.4) + (hero2.winRate * 0.2);
        double totalScore = hero1Score + hero2Score;
        double hero1Prob = totalScore != 0 ? (hero1Score / totalScore) * 100 : 0;
        double hero2Prob = totalScore != 0 ? (hero2Score / totalScore) * 100 : 0;

        hero1ProbabilityLabel.setText(hero1.heroName + ": " + String.format("%.2f", hero1Prob) + "%");
        hero2ProbabilityLabel.setText(hero2.heroName + ": " + String.format("%.2f", hero2Prob) + "%");
        probabilityProgressBar.setValue((int) hero1Prob);
        probabilityProgressBar.setString(String.format("%.2f", hero1Prob) + "%");

        String bgPath = defaultBackgroundPath;
        if (hero1Prob > hero2Prob) {
            bgPath = "ALL HEROES/" + hero1.heroName + ".png";
        } else if (hero2Prob > hero1Prob) {
            bgPath = "ALL HEROES/" + hero2.heroName + ".png";
        }
        backgroundPanel.setBackgroundImage(bgPath);

        performBattle(hero1, hero2);
    }

    // Performs the battle computation and shows the winner dialog.
    private void performBattle(Hero hero1, Hero hero2) {
        double hero1Score = (hero1.hp * 0.4) + (hero1.physicalAtk * 0.4) + (hero1.winRate * 0.2);
        double hero2Score = (hero2.hp * 0.4) + (hero2.physicalAtk * 0.4) + (hero2.winRate * 0.2);
        if (hero1Score == hero2Score) {
            lastWinner = "";
            return;
        }
        String winnerName = hero1Score > hero2Score ? hero1.heroName : hero2.heroName;
        if (!winnerName.equals(lastWinner)) {
            lastWinner = winnerName;
            showWinnerDialog(winnerName);
        }
    }

    // Displays a dialog with the winning hero’s picture and message.
    private void showWinnerDialog(String winnerName) {
        Hero winner = getHeroByName(winnerName);
        String heroPicPath = "ALL HEROES/" + winner.heroName + ".png";
        BufferedImage heroImg = null;
        try {
            File imageFile = new File(heroPicPath);
            if (imageFile.exists()) {
                heroImg = ImageIO.read(imageFile);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        JDialog winnerDialog = new JDialog(this, "MLBB Hero Winner", true);
        winnerDialog.setSize(500, 450);
        winnerDialog.setLocationRelativeTo(this);
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(30, 0, 30));
        JLabel winnerLabel = new JLabel("The Winner is: " + winnerName, SwingConstants.CENTER);
        winnerLabel.setFont(new Font("Arial", Font.BOLD, 30));
        winnerLabel.setForeground(new Color(184, 134, 11));
        winnerLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.add(winnerLabel, BorderLayout.NORTH);
        if (heroImg != null) {
            Image scaled = heroImg.getScaledInstance(300, 300, Image.SCALE_SMOOTH);
            JLabel imageLabel = new JLabel(new ImageIcon(scaled));
            imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
            panel.add(imageLabel, BorderLayout.CENTER);
        } else {
            JLabel noImageLabel = new JLabel("No image found", SwingConstants.CENTER);
            noImageLabel.setFont(new Font("Arial", Font.BOLD, 24));
            noImageLabel.setForeground(new Color(184, 134, 11));
            panel.add(noImageLabel, BorderLayout.CENTER);
        }
        winnerDialog.add(panel);
        winnerDialog.setVisible(true);
    }

    // Computes and displays the most durable hero for the most populated role.
    private void showMostDurableHero() {
        if (heroes.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No heroes loaded. Please upload a CSV file first.");
            return;
        }
        Map<String, Integer> roleCount = new HashMap<>();
        for (Hero hero : heroes) {
            if (hero.role == null || hero.role.isEmpty())
                continue;
            roleCount.put(hero.role, roleCount.getOrDefault(hero.role, 0) + 1);
        }
        if (roleCount.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No roles found in the data.");
            return;
        }
        String mostPopulatedRole = null;
        int maxCount = 0;
        for (Map.Entry<String, Integer> entry : roleCount.entrySet()) {
            if (entry.getValue() > maxCount) {
                maxCount = entry.getValue();
                mostPopulatedRole = entry.getKey();
            }
        }
        Hero mostDurableHero = null;
        double maxDefense = -1;
        for (Hero hero : heroes) {
            if (hero.role != null && hero.role.equals(mostPopulatedRole)) {
                if (hero.physicalDefense > maxDefense) {
                    maxDefense = hero.physicalDefense;
                    mostDurableHero = hero;
                }
            }
        }
        JDialog dialog = new JDialog(this, "Most Populated Role & Most Durable Hero", true);
        dialog.setSize(500, 400);
        dialog.setLocationRelativeTo(this);
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(30, 0, 30));
        // Updated label: display role and count (maxCount heroes)
        JLabel roleLabel = new JLabel(
            String.format("Most Populated Role: %s (%d heroes)", mostPopulatedRole, maxCount),
            SwingConstants.CENTER
        );
        roleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        roleLabel.setForeground(new Color(184, 134, 11));
        roleLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panel.add(roleLabel, BorderLayout.NORTH);
        if (mostDurableHero != null) {
            JPanel centerPanel = new JPanel();
            centerPanel.setBackground(new Color(30, 0, 30));
            centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
            JLabel heroLabel = new JLabel("Most Durable Hero: " + mostDurableHero.heroName, SwingConstants.CENTER);
            heroLabel.setFont(new Font("Arial", Font.BOLD, 20));
            heroLabel.setForeground(new Color(184, 134, 11));
            heroLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            JLabel defenseLabel = new JLabel("Physical Defense: " + mostDurableHero.physicalDefense, SwingConstants.CENTER);
            defenseLabel.setFont(new Font("Arial", Font.PLAIN, 18));
            defenseLabel.setForeground(new Color(184, 134, 11));
            defenseLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            centerPanel.add(heroLabel);
            centerPanel.add(defenseLabel);
            String imgPath = "ALL HEROES/" + mostDurableHero.heroName + ".png";
            try {
                File imgFile = new File(imgPath);
                if (imgFile.exists()) {
                    BufferedImage img = ImageIO.read(imgFile);
                    Image scaled = img.getScaledInstance(200, 200, Image.SCALE_SMOOTH);
                    JLabel imgLabel = new JLabel(new ImageIcon(scaled));
                    imgLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
                    centerPanel.add(Box.createVerticalStrut(10));
                    centerPanel.add(imgLabel);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            panel.add(centerPanel, BorderLayout.CENTER);
        } else {
            JLabel noHeroLabel = new JLabel("No durable hero found.", SwingConstants.CENTER);
            noHeroLabel.setFont(new Font("Arial", Font.BOLD, 20));
            noHeroLabel.setForeground(new Color(184, 134, 11));
            panel.add(noHeroLabel, BorderLayout.CENTER);
        }
        dialog.add(panel);
        dialog.setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            HeroBattleGame game = new HeroBattleGame();
            game.setVisible(true);
        });
    }

    // --- Data class for a Hero ---
    private static class Hero {
        String heroName;
        String role;
        double hp;
        double physicalAtk;
        double physicalDefense;
        double magicDefense;
        double winRate;

        public Hero(String heroName, String role, double hp, double physicalAtk, double physicalDefense, double magicDefense, double winRate) {
            this.heroName = heroName;
            this.role = role;
            this.hp = hp;
            this.physicalAtk = physicalAtk;
            this.physicalDefense = physicalDefense;
            this.magicDefense = magicDefense;
            this.winRate = winRate;
        }
    }

    // --- Custom Panel for Background Image ---
    private class BackgroundPanel extends JPanel {
        private Image backgroundImage;

        public void setBackgroundImage(String imagePath) {
            try {
                File file = new File(imagePath);
                if (file.exists()) {
                    backgroundImage = ImageIO.read(file);
                } else {
                    backgroundImage = null;
                }
                repaint();
            } catch (IOException e) {
                e.printStackTrace();
                backgroundImage = null;
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (backgroundImage != null) {
                g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
            } else {
                g.setColor(getBackground());
                g.fillRect(0, 0, getWidth(), getHeight());
            }
            super.paintComponent(g);
        }
    }
    
    // --- Custom Styled Button with Adaptable Size ---
    // Reverting to the previous button style: indigo-to-gold gradient with white text.
    private JButton createStyledButton(String text) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Simulate a press effect.
                if (getModel().isPressed()) {
                    g2d.translate(1, 1);
                }
                
                // Gradient background from indigo (purple) to gold.
                Color topColor = new Color(75, 0, 130);  // indigo (purple)
                Color bottomColor = new Color(255, 215, 0);  // gold (yellow)
                GradientPaint gp = new GradientPaint(0, 0, topColor, 0, getHeight(), bottomColor);
                g2d.setPaint(gp);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);
                
                // Hover effect: overlay translucent white.
                if (getModel().isRollover()) {
                    g2d.setColor(new Color(255, 255, 255, 50));
                    g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);
                }
                
                // Draw the button text centered.
                FontMetrics fm = g2d.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(getText())) / 2;
                int y = (getHeight() + fm.getAscent()) / 2 - 2;
                g2d.setColor(Color.WHITE);
                g2d.drawString(getText(), x, y);
                
                g2d.dispose();
            }
            
            @Override
            protected void paintBorder(Graphics g) {
                // No border is drawn.
            }
        };
        
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setForeground(Color.WHITE);
        Font font = new Font("Arial", Font.BOLD, 16);
        button.setFont(font);
        
        FontMetrics fm = button.getFontMetrics(font);
        int textWidth = fm.stringWidth(text);
        int textHeight = fm.getHeight();
        int paddingWidth = 40;
        int paddingHeight = 20;
        button.setPreferredSize(new Dimension(textWidth + paddingWidth, textHeight + paddingHeight));
        
        return button;
    }
}
