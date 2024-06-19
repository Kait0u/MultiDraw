package wit.pap.multidraw.client.utils;

import wit.pap.multidraw.shared.BgraImage;
import wit.pap.multidraw.shared.Utilities;
import wit.pap.multidraw.shared.communication.ClientCommands;
import wit.pap.multidraw.shared.communication.ClientMessage;
import wit.pap.multidraw.shared.communication.Message;
import wit.pap.multidraw.shared.communication.ServerMessage;
import wit.pap.multidraw.shared.globals.Globals;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.time.Instant;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class TCPHandler extends Thread {
    private Socket socket;
    private final ObjectOutputStream outputStream;
    private final ObjectInputStream inputStream;

    private final Queue<ClientMessage> messagesToSend;
    private final Queue<ServerMessage> messagesToHandle;
    private final AtomicBoolean running;

    private Runnable cbGetCanvasImage = null;
    private Consumer<BgraImage> cbSetMiddleGround = null;
    private Consumer<String> cbShowMessage = null;
    private Consumer<String> cbShowError = null;
    private Runnable cbOnClose = null;

    private Instant lastImageSync;
    private BgraImage image;

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

        this.lastImageSync = Instant.now();
        this.image = BgraImage.createTransparent(Globals.IMAGE_WIDTH, Globals.IMAGE_HEIGHT);
    }

    @Override
    public void run() {
        this.running.set(true);
        try {
            while (this.running.get()) {
                sendMessages();
                receiveMessages();
                handleMessages();
                prepareCanvasImage();
            }
        } catch (Exception e) {
            if (cbShowError != null) cbShowError.accept(e.getMessage());
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
            try {
                outputStream.writeObject(message);
                outputStream.flush();
            } catch (SocketException e) {
                if (cbShowError != null)
                    cbShowError.accept(e.getMessage());
                stopHandler();
            }
        }
    }

    private synchronized void sendMessages() {
        if (messagesToSend.isEmpty()) return;

        while (!messagesToSend.isEmpty()) {
            Message msg = messagesToSend.poll();
            try {
                sendMessage(msg);
            } catch (IOException e) {
                if (cbShowError != null) cbShowError.accept(e.getMessage());
            }
        }
    }

    public ServerMessage receiveMessage() {
        if (this.inputStream != null) {
            try {
                return (ServerMessage) this.inputStream.readObject();
            } catch (SocketException e) {
                this.running.set(false);
                if (cbShowMessage != null) cbShowMessage.accept(e.getMessage());
            } catch (SocketTimeoutException e) {

            } catch (IOException | ClassNotFoundException e) {
                if (cbShowError != null) cbShowError.accept(e.getMessage());
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
    }

    private void receiveMessages() {
        if (this.inputStream == null) return;

        ServerMessage message = null;
        do {
            message = receiveMessage();

            if (message != null) {
                synchronized (messagesToHandle) {
                    messagesToHandle.offer(message);
                }
            }

        } while (message != null);
    }

    private void handleMessage(ServerMessage message) {
        switch (message.getServerCommand()) {
            case ACCEPT_INT0_ROOM -> {}
            case REJECT_FROM_ROOM -> {
                handleRejectFromRoom(message);
            }
            case SEND_MIDDLEGROUND -> {
                handleSendMiddleGround(message);
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

    private void prepareCanvasImage() {
        if (cbGetCanvasImage == null)
            return;

        if (Duration.between(lastImageSync, Instant.now()).toSeconds() < Globals.CANVAS_SNAPSHOT_INTERVAL_SECONDS)
            return;

        cbGetCanvasImage.run();
        synchronized (image) {
            try {
                byte[] imgBytes = Utilities.serializeIntoBytes(image);
                ClientMessage imgMessage = new ClientMessage(
                        ClientCommands.SEND_IMAGE,
                        imgBytes
                );

                queueMessage(imgMessage);
                lastImageSync = Instant.now();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

    }

    public synchronized void stopHandler() {
        try {
            this.running.set(false);
            close();
            interrupt();
        } catch (Exception e) {

        }
    }

    private void close() {
        try {
            synchronized (outputStream) {
                ClientMessage msg = new ClientMessage(ClientCommands.DISCONNECT, null);
                sendMessage(msg);
                outputStream.close();
            }

            synchronized (inputStream) {
                inputStream.close();
            }

            if (socket != null) socket.close();
        } catch (IOException e) {

        }
    }

    public boolean isRunning() {
        synchronized (this.running) {
            return this.running.get();
        }
    }

    // Handlers

    private void handleRejectFromRoom(ServerMessage message) {
        if (cbShowError != null)
            cbShowError.accept(new String(message.getPayload()));
        if (cbOnClose != null) {
            cbOnClose.run();
        }
    }

    private void handleSendMiddleGround(ServerMessage message) {
        byte[] imgBytes = message.getPayload();
        try {
            BgraImage img = (BgraImage) Utilities.deserializeIntoObject(imgBytes);
            if (cbSetMiddleGround != null) {
                cbSetMiddleGround.accept(img);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    // Getters & Setters


    public void setCbSetMiddleGround(Consumer<BgraImage> cbSetMiddleGround) {
        this.cbSetMiddleGround = cbSetMiddleGround;
    }

    public void setCbShowMessage(Consumer<String> cbShowMessage) {
        this.cbShowMessage = cbShowMessage;
    }

    public void setCbShowError(Consumer<String> cbShowError) {
        this.cbShowError = cbShowError;
    }

    public void setCbOnClose(Runnable cbOnClose) {
        this.cbOnClose = cbOnClose;
    }

    public void setCbGetCanvasImage(Runnable cbGetCanvasImage) {
        this.cbGetCanvasImage = cbGetCanvasImage;
    }

    public void setImage(BgraImage image) {
        synchronized (this.image) {
            this.image = image;
        }
    }
}
