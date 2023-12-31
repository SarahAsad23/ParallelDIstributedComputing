/**
 * Sarah Asad 
 * CSS 434: Parallel and Distibuted Computing
 * 
 * ChatServer.java: receives message from client and broadcasts that message
 * to all exisiting and available server connections
*/

import java.net.*; // for Socket and ServerSocket
import java.io.*; // for IOException
import java.util.*; // for Vector

public class ChatServer{
    //a list of existing client connection  
    List<Connection> connections = new ArrayList<>(); 

    /*
     * Creates a server socket with a given port and goes into an 
     * infinite loop where: 
     * 
     * 1. accepts a new connection if there is one 
     * 2. adds this connection into a list of existing connections 
     * 3. for each connection, reads a new message and write it to all exisiting connections
     * 4. deletes the connection if it is already disconnected 
     * 
     * @param port: an IP port 
     */
    
    public ChatServer(int port){
        try{
            //create a server socket 
            ServerSocket server = new ServerSocket(port); 
            //will be blocked for 500ms upon accept 
            server.setSoTimeout(500); 
            
            while(true){
                //accept a new connection 
                try{
                    Socket clientSocket = server.accept();
                    //if this connection is not null
                    if(clientSocket != null){
                        //add the new connection into the list of existing connections
                        Connection c = new Connection(clientSocket); 
                        connections.add(c); 
                    }
                }catch(SocketTimeoutException e) {}
                 
                Iterator<Connection> it = connections.iterator(); 
                while(it.hasNext()){
                    Connection r = it.next(); 
                    String msg = r.readMessage(); 
                    if(msg != null){
                        for(Connection s : connections){
                            if(!r.equals(s)){
                                s.writeMessage(msg); 
                            }
                        }
                    }
                    else{
                        if(!r.isAlive()){
                            it.remove(); 
                        }
                    }
                }
            }
        }
        catch(IOException e){
            e.printStackTrace(); 
        }
    }

    /*
     * Usage: javaa CharServer <port> 
     * @params args: a string array where args[0] includes port 
     */

     public static void main(String args[]){
        //check if args[0] has port 
        if(args.length != 1){
            System.err.println("Syntax: java ChatServer <port>"); 
            System.exit(1); 
        }

        //start a chat server 
        new ChatServer(Integer.parseInt(args[0])); 
     }

     /*
      * Represents a connection fron a different chat client 
      */

      private class Connection{
        private Socket clientsocket;    //a socket of this connection 
        private InputStream rawIn;      //a byte-stream input from client 
        private OutputStream rawOut;    //a byte-stream output to client 
        private DataInputStream in;     //a filterd input from client 
        private DataOutputStream out;   //a filtered output to client 
        private String name;            //a client name 
        private boolean alive;          //indicates if the connection is alive

        /*
         * creates a new connection with a given socket 
         * 
         * @param client: a socket representing a new chat client 
         */

        public Connection(Socket client){
            clientsocket = client; 
           
            try{
                //from socket, initializes rawIn, rawOut, in, and out 
                rawIn = client.getInputStream(); 
                rawOut = client.getOutputStream(); 
                in = new DataInputStream(rawIn);
                out = new DataOutputStream(rawOut); 

                //the first message is the client name in unicode format 
                //upon successful initialization, alive should be true 
                alive = true; 
            }
            catch(IOException e){
                alive = false; 
            }
        }

        /*
         * reads a new message in unicode format and returns it with this clients name
         * 
         * @return: a unicode message eith the clients name 
         */

        public String readMessage(){
            String s = "";
            try{ 
                //the message is available and there are still bytes to read 
                if(in.available() > 0){
                    //read the message
                    s = in.readUTF(); 
                    return s; 
                }
            }
            catch(IOException e){
                e.printStackTrace(); 
                closeConnection(); 
            }

            //otherwise, skip reading
            return null;  
             
        }

        /*
         * writes a given message through this clients socket 
         * 
         * @param message: a string to wrote to the client 
         */

        public void writeMessage(String message){ 
            //write a message 
            try{
                out.writeUTF(message); 
                //use flush() to send it immediately 
                out.flush(); 
            }
            //if an exception occurs, you can identify that this connection was gone 
            catch(IOException e){
                e.printStackTrace();
                try{
                    this.clientsocket.close();
                } 
                catch(IOException e2){
                    e2.printStackTrace(); 
                }
            }
        }

        /*
         * checks if the connection is still alive 
         */

         public boolean isAlive(){
            //if the connection is broken, return false
            if(this.clientsocket.isConnected()){
                return true; 
            }
            return false; 
         }

         /*
          * closes the client socket 
          */
         private void closeConnection() {
            try {
                this.clientsocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}




