package com.example.eventorganizer;

import network_structures.BaseMessage;
import network_structures.EventInfo;
import network_structures.NetworkMessage;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Task responsible for handling server communication, ran on separate thread
 */
public class TaskManager implements Runnable {
    /** Automatically incremented communication stream counter */
    private static long currentCommunicationStream = 1;

    /**
     * @return Next usable communication stream identifier
     */
    public synchronized static long nextCommunicationStream() {
        return currentCommunicationStream++;
    }

    /** Messages for immediate  */
    private final ConcurrentLinkedQueue<BaseMessage> incomingMessages;
    /** Lingering tasks, mostly processes waiting for additional data */
    private final ConcurrentLinkedQueue<BaseMessage> lingeringTasks;
    /** Messages enqueued for sending to server */
    private final ConcurrentLinkedQueue<NetworkMessage> messagesToSend;

    /** Server IP address */
    private static final String host = "10.0.2.2";
    /** Server port */
    private static final int port = 9999;
    /** Socket connecting with server */
    private Socket socket = null;

    /** Connection state flag */
    private boolean isConnected;
    /** Event information, mostly unchanging */
    public static EventInfo eventInfo;

    /**
     * Default constructor, does not initialize this object completely
     */
    public TaskManager() {
        this.incomingMessages = new ConcurrentLinkedQueue<>();
        this.messagesToSend = new ConcurrentLinkedQueue<>();
        this.lingeringTasks = new ConcurrentLinkedQueue<>();
    }

    /**
     * Polls next message for interpretation
     * @return Message from immediately completable queue or null if queue is empty
     */
    private BaseMessage pollIncomingMessage() {
        return incomingMessages.poll();
    }

    /**
     * Add message into immediately completable queue
     * @param message Message to enqueue
     * @return True if message was successfully added, false otherwise - only returns false if out of memory
     */
    public boolean addIncomingMessage(BaseMessage message) {
        return incomingMessages.offer(message);
    }

    /**
     * Enqueues message for sending to server
     * @param message Message to send
     * @return True if message was successfully added, false otherwise - only returns false if out of memory
     */
    private boolean sendMessage(NetworkMessage message) {
        return messagesToSend.offer(message);
    }

    /**
     * Handler method for incoming messages
     * @param message Message to process
     */
    private void handleMessage(BaseMessage message) {
        BaseMessage[] matchingAwaitingTasks = searchForMatchingLingeringTasks(message);
        if (matchingAwaitingTasks.length > 0) {
            for (BaseMessage matchingAwaitingTask : matchingAwaitingTasks) {
                if (((CallLingeringTaskInterface) matchingAwaitingTask.getData()).callLingeringTaskOn(message)) {
                    lingeringTasks.remove(matchingAwaitingTask);
                }
            }
        } else {
            switch (message.getCommand()) {
                case "login": {
                    loginToServer(message);
                } break;
                case "eventDetails": {
                    eventInfo = (EventInfo) message.getData();
                } break;
            }
        }
    }

    /**
     * Main loop
     */
    @Override
    public void run() {
        /*
         * Possible server requests:
         * 1. ping - check if server recognizes this client
         * 2. login - log in to server providing credentials in args
         * 3. eventInfo - request essential information about event, such as sectors / attractions list
         * 4. viewTickets - view my tickets and their states
         * 5. viewReservations - view my active reservation(s)
         * 6. addTicket - add my ticket to specified room queue
         * 7. removeTicket - remove specific one of my tickets
         * 8. abandonReservation - abandon one of my reservations (will result in penalty)
         * 9. update - request update on states of rooms and queues
         * 10. details - request detailed information about specific room
         * 11. grouping - answer grouping call with decision or send update with changed decision
         */
        while (true) {
            while (!isConnected) {
                BaseMessage task = pollIncomingMessage();
                if (task != null && "connect".equals(task.getCommand())) {
                    isConnected = establishConnection();
                    if (!isConnected) {
                        ((Runnable)task.getData()).run();
                    }
                }
            }

            while (isConnected) {
                BaseMessage task = pollIncomingMessage();
                if (task != null)
                    handleMessage(task);
            }
        }
    }

    /**
     * Attempts to establish connection with server
     * @return True if successfully connected, false otherwise
     */
    private boolean establishConnection() {
        try {
            this.socket = new Socket();
            this.socket.connect(new InetSocketAddress(host, port), 10000);
            new Thread(new OutputToServer(new ObjectOutputStream(socket.getOutputStream()), messagesToSend)).start();
            new Thread(new InputFromServer(new ObjectInputStream(socket.getInputStream()), this::addIncomingMessage)).start();
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    /**
     * Adds lingering task which attempts to login into server
     * @param message Incoming message which requested login, following structure is expected:
     *                    command: string "login"
     *                    args: array of two strings - first containing login, second containing password
     *                    data: array of two Runnable callbacks - first for success, second for failure
     */
    private void loginToServer(BaseMessage message) {
        long streamId = TaskManager.nextCommunicationStream();
        lingeringTasks.add(new BaseMessage(
                "login",
                null,
                (CallLingeringTaskInterface) (msg) -> {
                    if (msg.getArgs()[0].equals("true")) {
                        ((Runnable[]) message.getData())[0].run();
                    } else {
                        ((Runnable[]) message.getData())[1].run();
                    }
                    return true;
                }, streamId)
        );
        sendMessage(new NetworkMessage(message.getCommand(), message.getArgs(), null, streamId));
    }

    /**
     * Searches awaiting tasks for those waiting for given message
     * @param message Message to match awaiting tasks with
     * @return Array containing all matching tasks; if none are found, array length will be 0
     */
    private BaseMessage[] searchForMatchingLingeringTasks(BaseMessage message) {
        long comId = message.getCommunicationIdentifier();
        ArrayList<BaseMessage> awaitingTaskInterfaces = new ArrayList<>();
        for (BaseMessage awaitingTask : lingeringTasks) {
            if (awaitingTask.getCommunicationIdentifier() == comId) {
                awaitingTaskInterfaces.add(awaitingTask);
            }
        }
        BaseMessage[] returnValue = new BaseMessage[awaitingTaskInterfaces.size()];
        for (int i = 0; i < returnValue.length; ++i) {
            returnValue[i] = awaitingTaskInterfaces.get(i);
        }
        return returnValue;
    }

    /**
     * Interface for awaiting tasks to handle received messages
     */
    private interface CallLingeringTaskInterface {
        /**
         * Callback for handling received message
         * @param message Message to handle
         * @return True if task in question is done and should be removed from lingering tasks, false otherwise
         */
        boolean callLingeringTaskOn(BaseMessage message);
    }

    /**
     * Task responsible for receiving data from server, ran on separate thread
     */
    private static class InputFromServer implements Runnable {

        /** Input stream to read data from */
        private final ObjectInputStream in;
        /** Interface for passing received messages to TaskManager */
        private final PassMessageToTaskManagerInterface taskManagerInterface;

        /**
         * Basic constructor
         * @param in Input stream linked with server
         * @param taskManagerInterface Callback to pass message to
         */
        public InputFromServer(ObjectInputStream in, PassMessageToTaskManagerInterface taskManagerInterface) {
            this.in = in;
            this.taskManagerInterface = taskManagerInterface;
        }

        /**
         * Reads message from server
         * @return Message from server if one was waiting in the buffer; null otherwise
         */
        private NetworkMessage receive() {
            try {
                return (NetworkMessage) this.in.readObject();
            } catch (ClassNotFoundException e) {
                e.printStackTrace(); //do zmiany pozniej
            } catch (IOException e) {
                e.printStackTrace(); //do zmiany pozniej
            }
            return null;
        }

        /**
         * Main loop
         */
        @Override
        public void run() {
            while (true) {
                NetworkMessage message = receive();
                if (message != null) {
                    taskManagerInterface.passMessage(message);
                }
            }
        }

        /**
         * Interface used by server communication task for passing messages to TaskManager thread
         */
        private interface PassMessageToTaskManagerInterface {
            /**
             * Adds message to specific message queue
             * @param message Message to add
             * @return True if successfully added the message, false otherwise
             */
            boolean passMessage(BaseMessage message);
        }
    }

    /**
     * Task responsible for sending data to server, ran on separate thread
     */
    private static class OutputToServer implements Runnable {

        /** Output stream to write messages to */
        private final ObjectOutputStream out;
        /** Queue to send messages from */
        private final ConcurrentLinkedQueue<NetworkMessage> messagesToSend;

        /**
         * Basic constructor
         * @param out Output stream linked with server
         * @param messagesToSend Queue containing messages to send
         */
        public OutputToServer(ObjectOutputStream out, ConcurrentLinkedQueue<NetworkMessage> messagesToSend) {
            this.out = out;
            this.messagesToSend = messagesToSend;
        }

        /**
         * Sends given message to server
         * @param message Message to send
         */
        private void send(NetworkMessage message) {
            try {
                this.out.writeObject(message);
            } catch (IOException e) {
                e.printStackTrace(); //do zmiany pozniej
            }
        }

        /**
         * Main loop
         */
        @Override
        public void run() {
            while (true) {
                NetworkMessage message = messagesToSend.poll();
                if (message != null) {
                    send(message);
                }
            }
        }
    }
}
