package place.test;


import place.PlaceBoard;
import place.network.PlaceRequest;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;

public class Client {

    public static void main(String[] args) throws IOException {

        if (args.length != 3) {
            System.err.println(
                    "Usage: java Client <host name> <port number> <username>");
            System.exit(1);
        }

        String hostName = args[0];
        int portNumber = Integer.parseInt(args[1]);
        String username = args[2];

        try (
                Socket user = new Socket(hostName, portNumber);
                ObjectOutputStream out =
                        new ObjectOutputStream(user.getOutputStream());
                ObjectInputStream in =
                        new ObjectInputStream(
                                user.getInputStream());
                Scanner userInput = new Scanner(System.in);
        ) {
            createLoginRequest(username, out);
            PlaceRequest<?> req = (PlaceRequest<?>) in.readUnshared();
            if (req.getType() == PlaceRequest.RequestType.LOGIN_SUCCESS) {
                System.out.println(((String) req.getData()));
                PlaceRequest<?> board = (PlaceRequest<?>) in.readUnshared();
                if(board.getType() == PlaceRequest.RequestType.BOARD) {
                    System.out.println( ((PlaceBoard) board.getData()).toString());
                }
            } else if (req.getType() == PlaceRequest.RequestType.ERROR){
                System.out.println((String) req.getData());
            }
            if(userInput.nextLine().equals("BYE")) {
                createError("DISCONNECT", out);
            }
        } catch (UnknownHostException e) {
            System.err.println("Cannot find host " + hostName);
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Couldn't get I/O for the connection to " +
                    hostName);
            System.exit(1);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static void createLoginRequest(String username, ObjectOutputStream out) throws IOException {
        PlaceRequest<String> req = new PlaceRequest<>(PlaceRequest.RequestType.LOGIN, username);
        out.writeUnshared(req);
        out.flush();
    }

    public static void createError(String errMsg, ObjectOutputStream out) throws IOException {
        PlaceRequest<String> req = new PlaceRequest<>(PlaceRequest.RequestType.ERROR, errMsg);
        out.writeUnshared(req);
        out.flush();
    }

}
