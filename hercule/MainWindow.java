package hercule;

import java.awt.Color;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.concurrent.locks.ReentrantLock;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingWorker;

public class MainWindow extends JFrame {
    private ReentrantLock _lock;
    private CenterButton _centerButton;
    private DownButton _downButton;
    private LeftButton _leftButton;
    private RightButton _rightButton;
    private UpButton _upButton;
    private JCheckBox _epCheckBox;
    private JCheckBox _poCheckBox;
    private JCheckBox _baCheckBox;
    private JCheckBox _coCheckBox;
    private JMenuItem _coItem;
    private JOptionPane _dial;
    private Network _net;
    private long _time;
    private boolean _state;
    private boolean _alive;

    public MainWindow(String title) {
        super(title);
        _alive = true;
        _state = true;
        _time = System.currentTimeMillis();

        _lock = new ReentrantLock();
        _net = new Network();

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setMenu();
        setResizable(false);
        setVisible(true);
        
        if (System.getProperty("os.name").compareTo("Windows") > 0)
            setSize(270, 210);
        else
            setSize(300, 205);

        _downButton = new DownButton();
        _leftButton = new LeftButton();
        _centerButton = new CenterButton();
        _rightButton = new RightButton();
        _upButton = new UpButton();
        
        JPanel upButtonPan = new JPanel();
        upButtonPan.setLayout(new BoxLayout(upButtonPan, BoxLayout.LINE_AXIS));
        upButtonPan.setBorder(BorderFactory.createEmptyBorder(7, 0, 5, 0));
        upButtonPan.add(_upButton);

        JPanel middleButtonPan = new JPanel();
        middleButtonPan.setLayout(new BoxLayout(middleButtonPan,
                                                BoxLayout.LINE_AXIS));
        middleButtonPan.add(_leftButton);
        middleButtonPan.add(Box.createRigidArea(new Dimension(5,0)));
        middleButtonPan.add(_centerButton);
        middleButtonPan.add(Box.createRigidArea(new Dimension(5,0)));
        middleButtonPan.add(_rightButton);

        JPanel downButtonPan = new JPanel();
        downButtonPan.setLayout(new BoxLayout(downButtonPan,
                                              BoxLayout.LINE_AXIS));
        downButtonPan.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
        downButtonPan.add(_downButton);

        _epCheckBox = new JCheckBox("\u00c9paule");
        _poCheckBox = new JCheckBox("Poignet");
        _baCheckBox = new JCheckBox("Base");
        _coCheckBox = new JCheckBox("Coude");

        JPanel axesPan = new JPanel();
        axesPan.setLayout(new BoxLayout(axesPan, BoxLayout.LINE_AXIS));
        axesPan.setBorder(BorderFactory.createTitledBorder("Axes"));
        axesPan.add(_baCheckBox);
        axesPan.add(_epCheckBox);
        axesPan.add(_coCheckBox);
        axesPan.add(_poCheckBox);

        JPanel rootPan = new JPanel();
        rootPan.setLayout(new BoxLayout(rootPan, BoxLayout.PAGE_AXIS));
        rootPan.add(upButtonPan);
        rootPan.add(middleButtonPan);
        rootPan.add(downButtonPan);
        rootPan.add(axesPan);

        setContentPane(rootPan);

        NetworkMonitor monitor = new NetworkMonitor(this);
        monitor.setPriority(Thread.MIN_PRIORITY);
        monitor.start();
        NetworkListen listen = new NetworkListen(this);
        listen.setPriority(Thread.MIN_PRIORITY);
        listen.start();
    }

    public void networkListen() {
        boolean wait;

        while (_alive) {
            _lock.lock();
            if (_net.getState()) {
                _lock.unlock();
                
                try {
                    String l = _net.listen();

                    System.out.println("netListen : " + l); // debug

                    if (l.compareTo("WAIT") == 0) {
                        System.out.println("netListen : WAIT"); // debug
                        throw new Exception();
                    }
                } catch (Exception e) {
                    if (_alive && _state) {
                        _dial.showMessageDialog(null,
                                                "Connexion perdu.",
                                                "\u00c9rreur",
                                                JOptionPane.ERROR_MESSAGE);
                        _lock.lock();
                        try {
                            _net.close();
                        } catch (Exception es) {}
                        _lock.unlock();
                    }
                }
            } else
                _lock.unlock();

        }
    }

    public void networkMonitor() {
        while (_alive) {
            _lock.lock();
            if (_net.getState() != _state) {
                _state = false;
                setState(false);
                _coItem.setText("Connexion");
            }
            _lock.unlock();

            long t = System.currentTimeMillis() - _time;
            if (_state && (t > 30000)) {
                _lock.lock();
                _net.alive();
                _lock.unlock();
                _time = System.currentTimeMillis();
            }
        }
    }

    public void quit() {
        _alive = false;
        _lock.lock();
        try {
            _net.close();
        } catch (Exception e) {}
        _lock.unlock();
        dispose();
    }

    public void setState(boolean state) {
        _centerButton.setEnabled(state);
        _downButton.setEnabled(state);
        _leftButton.setEnabled(state);
        _rightButton.setEnabled(state);
        _upButton.setEnabled(state);
        _epCheckBox.setEnabled(state);
        _poCheckBox.setEnabled(state);
        _baCheckBox.setEnabled(state);
        _coCheckBox.setEnabled(state);
    }

    private void setMenu() {
        JMenuBar menuBar = new JMenuBar();
        setJMenuBar(menuBar);

        JMenu mainMenu = new JMenu();
        mainMenu.setEnabled(true);
        mainMenu.setText("Fichier");

        ItemMenuListener itemMenuL = new ItemMenuListener();

        _coItem = new JMenuItem("Connexion");
        _coItem.addActionListener(itemMenuL);

        JMenuItem quitItem = new JMenuItem("Quitter");
        quitItem.addActionListener(itemMenuL);

        mainMenu.add(_coItem);
        mainMenu.add(quitItem);

        menuBar.add(mainMenu);
    }

    private abstract class Button extends JButton implements MouseListener {
        private boolean _pressed;

        public Button() {
            super();
            _pressed = false;
            addMouseListener(this);
            setPreferredSize(new Dimension(60, 30));
            setMinimumSize(new Dimension(60, 30));
            setMaximumSize(new Dimension(60, 30));
        }

        public void mouseClicked(MouseEvent e) {}
        public void mouseEntered(MouseEvent e) {}
        public void mouseExited(MouseEvent e) {}

        public void mousePressed(MouseEvent e) {
            _pressed = true;

            final String mv = move("10", "28");

            SwingWorker sw = new SwingWorker<Object, Object>() {
                public Object doInBackground() {
                    while (_pressed) {
                        _lock.lock();
                        try {
                            _net.send(mv);
                            Thread.sleep(100);
                        } catch (Exception e) {
                            try {
                                _net.close();
                            } catch (Exception es) {}
                        }
                        _lock.unlock();
                    }

                    return null;
                }
            };

            sw.execute();
        }

        public void mouseReleased(MouseEvent e) {
            _pressed = false;
        }
        
        protected abstract String move(String step, String speed);
    }

    private class CenterButton extends Button {
        private boolean _open;

        public CenterButton() {
            super();
            setText("\u2014<");
            _open = true;
        }

        public void mousePressed(MouseEvent e) {
            String mv = move(null, null);

            _lock.lock();
            try {
                _net.send(mv);
                _net.send(mv);
            } catch (Exception ex) {
                try {
                    _net.close();
                } catch (Exception es) {}
            }            
            _lock.unlock();
        }

        protected String move(String step, String speed) {
            if (_open) {
                _open = false;
                return step = "P+511:8\n";
            } else {
                _open = true;
                return step = "P-511:8\n";
            }
        }
    }

    private class DownButton extends Button {
        public DownButton() {
            super();
            setText("\u2193");
        }

        protected String move(String step, String speed) {
            String move = "";
            int i = 0;

            if (_epCheckBox.isSelected()) {
                move += "E-" + step + ":" + speed;
                i++;
            }

            if (_coCheckBox.isSelected()) {
                if (i > 0)
                    move += ";";
                move += "C-" + step + ":" + speed;
                i++;
            }

            if (_poCheckBox.isSelected()) {
                if (i > 0)
                    move += ";";
                move += "T-" + step + ":" + speed;
            }

            move += "\n";

            return move;
        }
    }

    private class LeftButton extends Button {
        public LeftButton() {
            super();
            setText("\u2190");
        }

        protected String move(String step, String speed) {
            String move = "";
            int i = 0;

            if (_baCheckBox.isSelected()) {
                move += "B-" + step + ":" + speed;
                i++;
            }

            if (_poCheckBox.isSelected()) {
                if (i > 0)
                    move += ";";
                move += "R-" + step + ":" + speed;
                i++;
            }

            move += "\n";

            return move;
        }
    }

    private class RightButton extends Button {
        public RightButton() {
            super();
            setText("\u2192");
        }

        protected String move(String step, String speed) {
            String move = "";
            int i = 0;

            if (_baCheckBox.isSelected()) {
                move += "B+" + step + ":" + speed;
                i++;
            }

            if (_poCheckBox.isSelected()) {
                if (i > 0)
                    move += ";";
                move += "R+" + step + ":" + speed;
                i++;
            }

            move += "\n";

            return move;
        }
    }

    private class UpButton extends Button {
        public UpButton() {
            super();
            setText("\u2191");
        }

        protected String move(String step, String speed) {
            String move = "";
            int i = 0;

            if (_epCheckBox.isSelected()) {
                move += "E+" + step + ":" + speed;
                i++;
            }

            if (_coCheckBox.isSelected()) {
                if (i > 0)
                    move += ";";
                move += "C+" + step + ":" + speed;
                i++;
            }

            if (_poCheckBox.isSelected()) {
                if (i > 0)
                    move += ";";
                move += "T+" + step + ":" + speed;
            }

            move += "\n";

            return move;
        }
    }

    private class ItemMenuListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            switch (e.getActionCommand()) {
            case "Connexion":
                connect();
                break;
            case "D\u00e9connexion":
                disconnect();
                break;
            case "Quitter":
                quit();
                break;
            }
        }

        private void connect() {
            String[] address;
            
            String dial = _dial.showInputDialog(null,
                                                "Adresse du serveur (host:port) :",
                                                "Connexion",
                                                JOptionPane.PLAIN_MESSAGE);

            _lock.lock();
            try {
                if (dial != null) {
                    if (dial.isEmpty())
                        throw new Exception();

                    address = dial.split(":");

                    if (address.length != 2)
                        throw new Exception();
                  
                    _net.connect(address[0], Integer.parseInt(address[1]));

                    if (!_net.getState())
                        throw new Exception();

                    _coItem.setText("D\u00e9connexion");
                    setState(true);
                    _state = true;
                }
            } catch (Exception e) {
                _dial.showMessageDialog(null,
                                        "Connexion \u00e9chou\u00e9.",
                                        "\u00c9rreur",
                                        JOptionPane.ERROR_MESSAGE);
            }
            _lock.unlock(); 
        }

        private void disconnect() {
            _state = false;
            _coItem.setText("Connexion");

            _lock.lock();
            try {
                _net.close();
            } catch (Exception e) {}
            _lock.unlock();
        }
    }

    private class NetworkListen extends Thread {
        private MainWindow _mw;

        public NetworkListen(MainWindow mw) {
            _mw = mw;
        }

        public void run() {
            _mw.networkListen();
        }
    }

    private class NetworkMonitor extends Thread {
        private MainWindow _mw;
        
        public NetworkMonitor(MainWindow mw) {
            _mw = mw;
        }

        public void run() {
            _mw.networkMonitor();
        }
    }
}
