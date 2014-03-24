package hercule;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.Socket;

public class Network {
    private DatagramSocket _udp;
    private Socket _tcp;
    private BufferedReader _tcpIn;
    private PrintWriter _tcpOut;
    private InetAddress _udpHost;
    private int _udpPort;
    private boolean _state;
    
    public Network() {
        _state = false;
    }

    public void alive() {
        _tcpOut.println("ALIVE\n");
    }

    public void connect(String host, int port) throws Exception {
        try {
            _udp = new DatagramSocket();

            InetAddress addr = InetAddress.getByName(host);

            _tcp = new Socket(addr, port);
            _tcpIn = new BufferedReader
                (new InputStreamReader(_tcp.getInputStream()));
            _tcpOut = new PrintWriter(_tcp.getOutputStream(), true);

            _tcpOut.println("HELLO\n");

            _udpHost = InetAddress.getByName(_tcpIn.readLine());
            _udpPort = Integer.parseInt(_tcpIn.readLine());

            _tcpOut.println("ID:HP\n");

            if (_tcpIn.readLine().compareTo("MASTER") != 0) {
                _state = true;
                close();
                throw new Exception();
            }

            _tcpOut.println("MODE:M\n");

            _state = true;
        } catch (Exception e) {
            throw e;
        }
    }

    public void close() throws Exception {
        try {
            if (_state) {
                _tcp.close();
                _udp.close();
                _state = false;
            }
        } catch (Exception e) {
            throw e;
        }
    }

    public boolean getState() {
        return _state;
    }

    public String listen() throws Exception {
        try {
            return _tcpIn.readLine();
        } catch (Exception e) {
            throw e;
        }
    }

    public void posinit() {
        _tcpOut.println("POSINIT\n");
    }

    public void send(String data) throws Exception {
        try {
            DatagramPacket pk = new DatagramPacket(data.getBytes(),
                                                   data.length(),
                                                   _udpHost,
                                                   _udpPort);

            _udp.send(pk);
        } catch (Exception e) {
            throw e;
        }
    }
}
