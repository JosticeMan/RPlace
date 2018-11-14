package place.server;

import place.PlaceBoard;
import place.PlaceTile;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

/**
 * CSCI-242 AP COMPUTER SCIENCE X
 * Project 2: Place
 *
 * Multithreaded server that waits for clients to connect to manipulate 'pixels' on a tile board
 *
 * @author Justin Yau
 */
public class PlaceServer {

    private int portNumber; //The port number that the server will be hosted on
    private PlaceBoard board; //The server-side version of the board
    private boolean listening; //Whether or not the server is active or not
    private HashMap<String, PlaceClientThread> clients; //A map of all the active clients that are connected to this server

    /***
     * Creates a new server that will allow players to connect to manipulate pixels on the created board
     * @param portNumber - The port number that server will be hosted on
     * @param dim - Dimensions of the board to allow users to pixelate on
     */
    public PlaceServer(int portNumber, int dim) {
        this.portNumber = portNumber;
        this.board = new PlaceBoard(dim);
        this.listening = true;
        this.clients = new HashMap<String, PlaceClientThread>();
    }

    /***
     * Sets up the server host on the port and begins accepting new clients.
     * Will start new threads for each client and their status will be updated as time progresses
     */
    private void go() {
        try (ServerSocket serverSocket = new ServerSocket(portNumber)) {
            System.out.println("Server started on port: " + portNumber + "! Now accepting users!");
            while (listening) {
                new PlaceClientThread(serverSocket.accept(), this).start();
            }
            System.out.println("Server Stopped.");
        } catch (IOException e) {
            System.err.println("Could not listen on port " + portNumber);
            System.exit(-1);
        }
    }

    /***
     * Returns the current state of the board
     * @return - The current state of the board
     */
    public synchronized PlaceBoard getBoard() {
        return this.board;
    }

    /***
     * Registers a new client to the database of clients that are currently connected
     * @param username - The current username of the client connecting
     * @param sock - The socket that the client is connecting through
     * @param thread - The thread that is handling the client connections
     * @return - Whether or not the operation was successful
     */
    public synchronized boolean addClient(String username, Socket sock, PlaceClientThread thread) {
        if(!isConnected(username)) {
            System.out.println(username + " connected! " + sock.toString());
            this.clients.put(username.toLowerCase(), thread);
            return true;
        }
        return false;
    }

    /***
     * Returns whether or not the given username is currently connected
     * @param username - The username to check avaliablity
     * @return - Whether or not the given username is currently connected
     */
    public synchronized boolean isConnected(String username) {
        return this.clients.containsKey(username.toLowerCase());
    }

    /***
     * Removes a client from the current map of connected clients.
     * Used in the routine for when the user logs off.
     * To be called in cojunction when the thread ends.
     * @param username - The username of the disconnecting user
     * @param sock - The socket of the disconnecting user
     * @return - Whether or not the operation was a success
     */
    public synchronized boolean removeClient(String username, Socket sock) {
        if(isConnected(username)) {
            System.out.println(username + " disconnected! " + sock.toString());
            this.clients.remove(username.toLowerCase());
            return true;
        }
        return false;
    }

    /***
     * Updates the server-side state of the board such that it can be passed on to new and current clients
     * @param tile - The tile to be changed
     * @return - Whether or not the operation was successful
     * @throws IOException
     */
    public synchronized boolean changeTile(PlaceTile tile) throws IOException {
        if(board.isValid(tile)) {
            board.setTile(tile);
            for(PlaceClientThread thread: this.clients.values()) {
                if(thread.isAlive()) {
                    thread.createChangedTile(tile);
                }
            }
            return true;
        }
        return false;
    }

    /***
     * Returns whether or not the server is still running and accepting clients
     * @return - Whether or not the server is still running and accepting clients
     */
    public boolean isRunning() {
        return listening;
    }

    /***
     * This method stops the server and its operations.
     */
    public void stop() {
        listening = false;
    }

    public static void main(String[] args) throws IOException {

        processArgs(args);

        new PlaceServer(Integer.parseInt(args[0]), Integer.parseInt(args[1])).go();

    }

    /***
     * Handles the command line arguments and will exit if they are incorrect
     * @param args - Command line arguments
     */
    public static void processArgs(String[] args) {
        if (args.length != 2) {
            if (Integer.parseInt(args[1]) < 1) {
                System.err.println("Usage: <dim> must be greater than or equal to 1");
                System.exit(1);
            }
            System.err.println("Usage: java PlaceServer <port number> <dim>");
            System.exit(1);
        }
    }

}
