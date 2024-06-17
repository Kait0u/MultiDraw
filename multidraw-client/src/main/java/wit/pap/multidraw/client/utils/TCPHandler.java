package wit.pap.multidraw.client.utils;

import wit.pap.multidraw.shared.communication.Message;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class TCPHandler extends Thread {
    private Socket socket;
    private ObjectOutputStream outputStream;
    private ObjectInputStream inputStream;

    private final Queue<Message> messagesToSend;
    private boolean running;

    public TCPHandler(String serverAddress, int serverPort) throws IOException {
        this(InetAddress.getByName(serverAddress), serverPort);
    }

    public TCPHandler(InetAddress serverAddress, int serverPort) throws IOException {
        this.socket = new Socket(serverAddress, serverPort);
        this.outputStream = new ObjectOutputStream(socket.getOutputStream());
        this.inputStream = new ObjectInputStream(socket.getInputStream());

        this.messagesToSend = new ConcurrentLinkedQueue<>();
        this.running = false;
    }

    @Override
    public void run() {
        System.out.println("TCPHandler started!");
        this.running = true;
        try {
            while (running) {
                sendMessages();
//                wait();
            }
        } finally {
            close();
        }
    }

    public synchronized void queueMessage(Message message) {
        messagesToSend.offer(message); // Using offer() for thread-safe addition to the queue
        notify();
    }

    private synchronized void sendMessage(Message message) throws IOException {
        outputStream.writeObject(message);
        outputStream.flush();
    }

    private synchronized void sendMessages() {
        if (messagesToSend.isEmpty()) return;

        while (!messagesToSend.isEmpty()) {
            Message msg = messagesToSend.poll();
            try {
                sendMessage(msg);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void handleMessage(Message message) {
        // Handle incoming message
        System.out.println("Received message: " + message);
        // You can process the message further here
    }

    public void stopHandler() {
        running = false;
        interrupt();
        close();
    }

    private void close() {
        try {
            if (outputStream != null) outputStream.close();
            if (inputStream != null) inputStream.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
