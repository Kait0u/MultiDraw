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
    private final AtomicBoolean isConnectionDead;
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
        this.isConnectionDead = new AtomicBoolean(false);

        ObjectOutputStream tempOut = null;
        ObjectInputStream tempIn = null;

        try {
            this.socket = new Socket(serverAddress, serverPort);
            this.socket.setSoTimeout(400);
            tempOut = new ObjectOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            tempOut.flush();
            tempIn = new ObjectInputStream(new BufferedInputStream(socket.getInputStream()));
        } catch (SocketException ex) {
            markConnectionAsDead();
            cbShowError.accept(ex.getMessage());
        }

        this.inputStream = tempIn;
        this.outputStream = tempOut;

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

                stopIfDead();
            }
        } catch (Exception e) {
            markConnectionAsDead();
            if (cbShowError != null) cbShowError.accept(e.getMessage());
        }

    }

    private void stopIfDead() {
        synchronized (isConnectionDead) {
            if (isConnectionDead.get()) {
                running.set(false);
            }
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
        synchronized (isConnectionDead) {
            if (isConnectionDead.get())
                return;
        }

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
        synchronized (messagesToSend) {
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
    }

    public ServerMessage receiveMessage() {
        synchronized (isConnectionDead) {
            if (isConnectionDead.get())
                return null;
        }

        synchronized (inputStream) {
            try {
                return (ServerMessage) this.inputStream.readObject();
            } catch (SocketException e) {
                this.running.set(false);
                if (cbShowMessage != null) cbShowMessage.accept(e.getMessage());
            } catch (SocketTimeoutException ignored) {

            } catch (IOException | ClassNotFoundException e) {
                markConnectionAsDead();
                if (cbShowError != null) cbShowError.accept(e.getMessage());
            }
        }

        return null;
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
            case REJECT_FROM_ROOM -> handleRejectFromRoom(message);
            case SEND_MIDDLEGROUND -> handleSendMiddleGround(message);
            case SEND_USERS -> {}
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
        synchronized (isConnectionDead) {
            if (isConnectionDead.get())
                return;
        }

        if (cbGetCanvasImage == null)
            return;

        if (Duration.between(lastImageSync, Instant.now()).toSeconds() < Globals.CANVAS_SNAPSHOT_INTERVAL_SECONDS)
            return;

        cbGetCanvasImage.run();
        synchronized (image) {
            try {
                byte[] imgBytes = Utilities.serializeAndCompress(image);
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
        markConnectionAsDead();
        try {
            this.running.set(false);
            close();
        } catch (Exception e) {

        }
    }

    public synchronized void stopHandlerInterrupt() {
        try {
            stopHandler();
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
            BgraImage img = (BgraImage) Utilities.decompressAndDeserialize(imgBytes);
            if (cbSetMiddleGround != null) {
                cbSetMiddleGround.accept(img);
            }
        } catch (IOException | ClassNotFoundException e) {
            markConnectionAsDead();
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

    public boolean getIsConnectionDead() {
        synchronized (isConnectionDead) {
            return isConnectionDead.get();
        }
    }

    public void markConnectionAsDead() {
        synchronized (isConnectionDead) {
            isConnectionDead.set(true);
        }
    }
}
