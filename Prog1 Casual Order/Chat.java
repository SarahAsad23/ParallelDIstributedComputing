/**
 * Sarah Asad 
 * CSS 434: Parallel and Distibuted Computing
 * 
 * Chat.java: creates a complete network between all clients, sends messages in casual 
 * order even when there are receive delays by queuing elements in various vectors 
*/

import java.net.*;  // ServerSocket, Socket
import java.io.*;   // InputStream, ObjectInputStream, ObjectOutputStream
import java.util.ArrayList; //ArrayList

public class Chat {

    // Each element i of the following arrays represent a chat member[i]
    private Socket[] sockets = null;             // connection to i
    private InputStream[] indata = null;         // used to check data from i
    private ObjectInputStream[] inputs = null;   // a message from i
    private ObjectOutputStream[] outputs = null; // a message to i
    private int[] vector = null;

    private ArrayList<int[]>   vec_queue = new ArrayList<int[]>( );    // maintains vector stamps from the others
    private ArrayList<String>  msg_queue = new ArrayList<String>( );   // maintain actual messages from the others
    private ArrayList<Integer> src_queue = new ArrayList<Integer>( );  // maintain source IDs


     /**
     * Is the main body of the Chat application. This constructor establishes
     * a socket to each remote chat member, broadcasts a local user's message
     * to all the remote chat members, and receive a message from each of them.
     *
     * @param port  IP port used to connect to a remote node as well as to
     *              accept a connection from a remote node.
     * @param rank  this local node's rank (one of 0 through to #members - 1)
     * @param hosts a list of all computing nodes that participate in chatting
     */
    public Chat(int port, int rank, String[] hosts) throws IOException {
        // print out my port, rank and local hostname
        System.out.println("port = " + port + ", rank = " + rank + ", localhost = " + hosts[rank]);

        // create sockets, inputs, outputs, and vector arrays
        sockets = new Socket[hosts.length];
        indata = new InputStream[hosts.length];
        inputs = new ObjectInputStream[hosts.length];
        outputs = new ObjectOutputStream[hosts.length];
        vector = new int[hosts.length];

        // establish a complete network
        ServerSocket server = new ServerSocket(port);
        for (int i = hosts.length - 1; i >= 0; i--) {
            if (i > rank) {
                // accept a connection from others with a higher rank
                Socket socket = server.accept();
                String src_host = socket.getInetAddress().getHostName();

                // find this source host's rank
                for (int j = 0; j < hosts.length; j++) {
                    if (src_host.startsWith(hosts[j])) {
                        // j is this source host's rank
                        System.out.println("accepted from " + src_host);

                        // store this source host j's connection, input stream and object input/output streams.
                        sockets[j] = socket;
                        indata[j] = socket.getInputStream();
                        inputs[j] = new ObjectInputStream(indata[j]);
                        outputs[j] = new ObjectOutputStream(socket.getOutputStream());
                    }
                }
            }
            if (i < rank) {
                // establish a connection to others with a lower ran
                sockets[i] = new Socket(hosts[i], port);
                System.out.println("connected to " + hosts[i]);
                // store this destination host j's connection, input stream
                // and object intput/output streams.
                outputs[i] = new ObjectOutputStream(sockets[i].getOutputStream());
                indata[i] = sockets[i].getInputStream();
                inputs[i] = new ObjectInputStream(indata[i]);
            }
        }

        // create a keyboard stream
        BufferedReader keyboard = new BufferedReader(new InputStreamReader(System.in));

        // now goes into a chat
        while (true) {
            // read a message from keyboard and broadcast it to all the others.
            if (keyboard.ready()) {
                // since keyboard is ready, read one line.
                String message = keyboard.readLine();
                // keyboard was closed by "^d"
                if (message == null) {
                    break; // terminate the program
                }

                // broadcast a message to each of the chat members.
                vector[rank] += 1;
                for (int i = 0; i < hosts.length; i++) {
                    if (i != rank) {
                        int[] vector2 = new int[vector.length];
                        for (int k = 0; k < vector.length; k++) {
                            vector2[k] = vector[k];
                        }

                        // of course I should not send a message to myself
                        if (i != rank) {
                            outputs[i].writeObject(message);
                            outputs[i].writeObject(vector2);
                            outputs[i].flush(); // make sure the message was sent
                        }
                    }
                }
            }
            
            // read a message from each of the chat members
            for (int i = 0; i < hosts.length; i++) {
                if (i != rank && indata[i].available() > 0) {
                    // to intentionally create a misordered message deliveray, 
                    // let's slow down the chat member #2.
                    if (rank == 2) {
                        try {
                            Thread.currentThread().sleep(5000); // sleep 5 sec.
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    
                // check if chat member #i has something
                if(i != rank && indata[i].available( ) > 0 )
                    // read a message from chat member #i and print it out to the monitor
                    try {
                        String message = (String) inputs[i].readObject();
                        int[] stamps = (int[]) inputs[i].readObject();

                        // If this is the next message that should be received from the peer then update the vector
                        // and display the message otherwise add it to the waiting list.
                        // vector[i] is the current index of the peer.
                        // stamps[i] is the new index received from the peer.
                        if ((vector[i] + 1) == stamps[i]) {
                            // The message has been received in order so display it.
                            System.out.println(hosts[i] + ": " + message);
                            vector[i] = stamps[i];

                            // Now look at the wait queue and see if more messages can be displayed.
                            boolean didRemoveMessage;
                            do {
                                didRemoveMessage = false;
                                for (int index = 0; index < vec_queue.size(); index++) {
                                    int theirRank = src_queue.get(index);
                                    int theirMessageIndex = vec_queue.get(index)[theirRank];
                                    int expectedIndex = vector[theirRank];

                                    if ((expectedIndex + 1) == theirMessageIndex) {
                                        //This is now the right message to display.
                                        System.out.printf("%s: [%d] %s\n", hosts[i], theirMessageIndex, msg_queue.get(index));
                                        
                                        // Remove this message from the wait queue.
                                        vec_queue.remove(index);
                                        msg_queue.remove(index);
                                        src_queue.remove(index);
                                        didRemoveMessage = true;
                                    }
                                }
                            } while (didRemoveMessage == true);
                        } else {
                            // This message is out of order so it gets added to the wait queue.
                            vec_queue.add(stamps);
                            msg_queue.add(message);
                            src_queue.add(i);
                        }
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
    

    /**
     * Is the main function that verifies the correctness of its arguments and
     * starts the application.
     *
     * @param args receives <port> <ip1> <ip2> ... where port is an IP port
     *             to establish a TCP connection and ip1, ip2, .... are a
     *             list of all computing nodes that participate in a chat.
     */
    public static void main( String[] args ) {

        // verify #args.
        if ( args.length < 2 ) {
            System.err.println( "Syntax: java Chat <port> <ip1> <ip2> ..." );
            System.exit( -1 );
        }

        // retrieve the port
        int port = 0;
        try {
            port = Integer.parseInt( args[0] );
        } catch ( NumberFormatException e ) {
            e.printStackTrace( );
            System.exit( -1 );
        }
        if ( port <= 5000 ) {
            System.err.println( "port should be 5001 or larger" );
            System.exit( -1 );
        }

        // retireve my local hostname
        String localhost = null;
        try {
            localhost = InetAddress.getLocalHost( ).getHostName( );
        } catch ( UnknownHostException e ) {
            e.printStackTrace( );
            System.exit( -1 );
        }

        // store a list of computing nodes in hosts[] and check my rank
        int rank = -1;
        String[] hosts = new String[args.length - 1];
        for ( int i = 0; i < args.length - 1; i++ ) {
            hosts[i] = args[i + 1];
            if ( localhost.startsWith( hosts[i] ) ) 
            // found myself in the i-th member of hosts
            rank = i;
        }

        // now start the Chat application
        try {
            new Chat( port, rank, hosts );
        } catch ( IOException e ) {
            e.printStackTrace( );
            System.exit( -1 );
        }
    }
}
