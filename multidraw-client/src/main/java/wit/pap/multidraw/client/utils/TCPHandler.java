package wit.pap.multidraw.client.utils;

import wit.pap.multidraw.shared.communication.ClientCommands;
import wit.pap.multidraw.shared.communication.ClientMessage;
import wit.pap.multidraw.shared.communication.Message;
import wit.pap.multidraw.shared.communication.ServerMessage;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class TCPHandler extends Thread {
    private Socket socket;
    private ObjectOutputStream outputStream;
    private ObjectInputStream inputStream;

    private final Queue<Message> messagesToSend;
    private final AtomicBoolean running;

    public TCPHandler(String serverAddress, int serverPort) throws IOException {
        this(InetAddress.getByName(serverAddress), serverPort);
    }

    public TCPHandler(InetAddress serverAddress, int serverPort) throws IOException {
        this.socket = new Socket(serverAddress, serverPort);
        this.outputStream = new ObjectOutputStream(socket.getOutputStream());
        this.inputStream = new ObjectInputStream(socket.getInputStream());

        this.messagesToSend = new ConcurrentLinkedQueue<>();
        this.running = new AtomicBoolean(false);
    }

    @Override
    public void run() {
        System.out.println("TCPHandler started!");
        this.running.set(true);
        try {
            while (this.running.get()) {
                sendMessages();
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

    public ServerMessage receiveMessage() {
        if (this.inputStream != null) {
            try {
                return (ServerMessage) this.inputStream.readObject();
            } catch (IOException | ClassNotFoundException e) {
                System.out.println("Uh oh!");
            }
        }

        return null;
    }

    public ServerMessage receiveMessageOrNull() {
        if (this.inputStream != null) {
            try {
                if (this.inputStream.available() > 0) {
                    return (ServerMessage) this.inputStream.readObject();
                }
            } catch (IOException | ClassNotFoundException e) {
                System.out.println("Uh oh!");
            }
        }
        return null;
    }

    private void handleMessage(ServerMessage message) {
        switch (message.getServerCommand()) {
            case POKE -> {}
            case ACCEPT_INT0_ROOM -> {
                System.out.println("Yay!");
            }
            case REJECT_FROM_ROOM -> {
                System.out.println("Nay!");
            }
        }
    }

    public synchronized void stopHandler() {
        this.running.set(false);
        interrupt();
        close();
    }

    private void close() {
        try {
            if (outputStream != null) {
                ClientMessage msg = new ClientMessage(ClientCommands.DISCONNECT, null);
                outputStream.close();
            }

            if (inputStream != null)
                inputStream.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isRunning() {
        synchronized (this.running) {
            return this.running.get();
        }
    }
}
