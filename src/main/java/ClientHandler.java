import java.io.*;
import java.net.Socket;
import java.util.ArrayList;

public class ClientHandler implements Runnable{
    public static ArrayList<ClientHandler> clientHandlers = new ArrayList<>();
    private Socket socket;
    private BufferedReader bufferedReader;
    private BufferedWriter bufferedWriter;
    private String clientUserName;
    
    public ClientHandler(Socket socket){
        try {
            this.socket = socket;
            /*
            * In Java: There are two types of stream: Byte streams & character stream
            * So in java character streams end with the word 'Writer'
            * And byte streams end with the word 'Stream'
            * In bellow we want to send character stream. So we take the bytes from the OutputStream and convert them to character using OutputStreamWriter
            * */
            this.bufferedWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            this.bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            // This so when user enter username and press enter.
            this.clientUserName = bufferedReader.readLine();
            clientHandlers.add(this);
            sendCurrentUsersInChatForTheUser("Number Of Online Users: " + clientHandlers.size());
            broadcastMessage("Server: " + clientUserName + " has joined the chat!");
        } catch (IOException e){
            closeEverything(socket, bufferedWriter, bufferedReader);
        }
    }

    @Override
    public void run() {
        String messageFromClient;
        while (socket.isConnected()){
            try {
                // Note that this is a blocking operation. So in single thread the server will be stuck here until it receives message from a client.
                // That is why we created this method in a separate Thread so the server won't get stuck here but the thread will.
                messageFromClient = bufferedReader.readLine();
                broadcastMessage(messageFromClient);
            } catch (IOException e){
                closeEverything(socket, bufferedWriter, bufferedReader);
                break;
            }
        }
    }

    private void closeEverything(Socket socket, BufferedWriter bufferedWriter, BufferedReader bufferedReader) {
        removeClientHandler();
        try {
            if (bufferedReader != null){
                bufferedReader.close();
            }
            if (bufferedWriter != null){
                bufferedWriter.close();
            }
            if (socket != null){
                socket.close();
            }
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    private void broadcastMessage(String messageToSend) {
        for(ClientHandler clientHandler : clientHandlers){
            try {
                if (!clientHandler.clientUserName.equals(clientUserName)){
                    clientHandler.bufferedWriter.write(messageToSend);
                    clientHandler.bufferedWriter.newLine();
                    // Usually the message won't be enough to fill the buffer. so Flush will manually send the buffer
                    // The default buffer size is 512 Bytes
                    clientHandler.bufferedWriter.flush();
                }
            }catch (IOException e){
                closeEverything(socket, bufferedWriter, bufferedReader);
            }

        }
    }

    private void sendCurrentUsersInChatForTheUser(String numberOfOnlineUsers) {
        for (ClientHandler clientHandler: clientHandlers){
            try {
                clientHandler.bufferedWriter.write(numberOfOnlineUsers);
                clientHandler.bufferedWriter.newLine();
                clientHandler.bufferedWriter.flush();
            } catch (IOException e){
                closeEverything(socket, bufferedWriter, bufferedReader);
            }
        }
    }


    // When client leaves the chat
    public void removeClientHandler(){
        clientHandlers.remove(this);
        broadcastMessage("Server: " + clientUserName + "just left the chat!");
    }


}
