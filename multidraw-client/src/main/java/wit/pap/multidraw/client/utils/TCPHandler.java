package wit.pap.multidraw.client.utils;

import wit.pap.multidraw.shared.communication.ClientCommands;
import wit.pap.multidraw.shared.communication.ClientMessage;
import wit.pap.multidraw.shared.communication.Message;
import wit.pap.multidraw.shared.communication.ServerMessage;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class TCPHandler extends Thread {
    private Socket socket;
    private final ObjectOutputStream outputStream;
    private final ObjectInputStream inputStream;

    private final Queue<ClientMessage> messagesToSend;
    private final Queue<ServerMessage> messagesToHandle;
    private final AtomicBoolean running;

    public TCPHandler(String serverAddress, int serverPort) throws IOException {
        this(InetAddress.getByName(serverAddress), serverPort);
    }

    public TCPHandler(InetAddress serverAddress, int serverPort) throws IOException {
        this.socket = new Socket(serverAddress, serverPort);
        this.socket.setSoTimeout(400);
        this.outputStream = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        this.outputStream.flush();
        this.inputStream = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));

        this.messagesToSend = new ConcurrentLinkedQueue<>();
        this.messagesToHandle = new ConcurrentLinkedQueue<>();
        this.running = new AtomicBoolean(false);
    }

    @Override
    public void run() {
        System.out.println("TCPHandler started!");
        this.running.set(true);
        try {
            while (this.running.get()) {
//                System.out.println("Sending...");
                sendMessages();
//                System.out.println("Sent!");
//                System.out.println("Receiving...");
                receiveMessages();
//                System.out.println("Received!");
//                System.out.println("Handling...");
                handleMessages();
//                System.out.println("Handled!");
            }
        } finally {
            close();
        }
    }

    public synchronized void queueMessage(ClientMessage message) {
        messagesToSend.offer(message); // Using offer() for thread-safe addition to the queue
        notify();
    }

    public synchronized void queueMessages(ClientMessage... messages) {
        for (ClientMessage m: messages) {
            queueMessage(m);
        }
    }

    private void sendMessage(Message message) throws IOException {
        synchronized (outputStream) {
            outputStream.writeObject(message);
            outputStream.flush();
        }
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
            } catch (SocketTimeoutException e) {

            } catch (IOException | ClassNotFoundException e) {
                System.out.println("Uh oh!");
            }
        }

        return null;
    }

    public ServerMessage receiveMessageOrNull() {
        if (this.inputStream == null)
            return null;

        synchronized (inputStream) {
            try {
                if (inputStream.available() <= 0)
                    return null;
                else
                    return (ServerMessage) inputStream.readObject();

            } catch (IOException | ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

//        return null;
    }

    private void receiveMessages() {
        if (this.inputStream == null) return;

        ServerMessage message = null;
        do {
            message = receiveMessage();

            if (message != null) {
                System.out.println(message);
                synchronized (messagesToHandle) {
                    messagesToHandle.offer(message);
                }
            }

        } while (message != null);
    }

    private void handleMessage(ServerMessage message) {
        switch (message.getServerCommand()) {
            case POKE -> {
                System.out.println("Poked!");
            }
            case ACCEPT_INT0_ROOM -> {
                System.out.println("Yay!");
            }
            case REJECT_FROM_ROOM -> {
                System.out.println("Nay!");
            }
        }
    }

    private void handleMessages() {
        synchronized (messagesToHandle) {
            while (!messagesToHandle.isEmpty()) {
                ServerMessage message = messagesToHandle.poll();
                handleMessage(message);
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
