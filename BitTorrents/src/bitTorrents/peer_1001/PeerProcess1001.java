package bitTorrents.peer_1001;

import bitTorrents.Message;

import java.util.*;
import java.net.*;
import java.io.*;
public class PeerProcess1001 {
    private ServerSocket serverSocket = null; //SOcket to start the server
    private List<Socket> connectionsFrom = null; //Peers which are connecting to this server
    private List<Socket> connectedTo = null;//Peers which this peers is connecting to
    private int PORT;
    private int ID;
    private HashSet<Socket> neighbours = null;// All neighbours of this peer
    private Socket optimisticUnchokedNeighbour = null; //One Optimisically Unckhoked Neighbouiur
    int numberOfPrefferedNeighbours; // # of Prefereed Neighbor (neighbour.size() <= numberOfPrefferedNeighbours)
    int n,m; //n - UnchokingInterval, m - OptimisticUnchokingInterval
    int pieceSize;
    List<Integer> fileContents;


    PeerProcess1001(){}
    PeerProcess1001(int PORT, int ID, int numberOfPrefferedNeighbours,int n,int m,int pieceSize) {
        try {
            this.PORT = PORT;
            this.ID = ID;
            this.connectionsFrom = new ArrayList<>();
            this.connectedTo = new ArrayList<>();
            this.serverSocket = new ServerSocket(this.PORT);
            this.neighbours = new HashSet<>();
            this.optimisticUnchokedNeighbour = new Socket();
            this.numberOfPrefferedNeighbours = numberOfPrefferedNeighbours;
            this.n = n;
            this.m = m;
            this.pieceSize = pieceSize;
            this.fileContents = new ArrayList<>();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    //Start a thread and invoke this function
    void startServer() {
        try {
            while (true) {
                System.out.println("Listening to..");
                Socket socket = serverSocket.accept();
                System.out.println("COnnectedddd");
                connectionsFrom.add(socket);
                List<Integer> l = new ArrayList<>();
                l.add(1);l.add(2);
                ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
                objectOutputStream.writeObject(l);
//                send(socket,new Message());
                break;
            }
        }catch (Exception e){e.printStackTrace();}
    }
    //Continously read the data coming to the socket/peer
    void read(){
        for(Socket socket:connectedTo){
            try{
                InputStream inputStream = socket.getInputStream();
                int i = 0;String s = "";
                while((i=inputStream.read()) != -1)s += (char)i;
                System.out.println(s);
                /*
                 * Process the message and send the appropriate message
                 * by creating a Message object and invoke send function
                 */
//                send(socket,new Message());
            }catch (Exception e){e.printStackTrace();continue;}
        }
        for(Socket socket:connectionsFrom){
            try{
                InputStream inputStream = socket.getInputStream();
                int i = 0;String s = "";
                while((i=inputStream.read()) != -1)s += (char)i;
                System.out.println(s + "hello");
            }catch (Exception e){e.printStackTrace();continue;}
        }
    }

    //Send a messaage
    void send(Socket socket, Message message) {
        try {
            socket.getOutputStream().write(("Hi you").getBytes());
        }catch (Exception e){e.printStackTrace();}
    }

    void unchokingInterval() {
        try {
            int count = this.n;
            Thread.sleep(this.n * 1000);
            System.out.println("Change neighbours"); //Change neighbours
        }catch(Exception e){e.printStackTrace();}
    }

    void optimisticUnchokedInterval(int m) {
        try {
            Thread.sleep(m*1000);
            System.out.println("Change optimistically unchoked neighbour"); //Change optimistically unchoked neighbour
        }catch(Exception e){e.printStackTrace();}
    }

    //Connect this socket to other peer
    boolean connect(String host,int PORT) {
        try{
            Socket socket = new Socket(host,PORT);
            connectedTo.add(socket);
            return true;
        }catch(Exception e){e.printStackTrace();return false;}
    }

    public static void main(String[] args) throws Exception{
        /*
         * 1) Get the port # from the cmd line arguments
         * 2) Connect to all the peers the came before this peer(Get info from PeerInfo.cfg(Main Thread)
         * 3) Start the server(Thread t1) and then continously read if there are any messages received(Thread t2)
         *    Send the messages accordingly after receiving
         * 4) If the user has file, divide the file into chunks(Main Thread)
         * 5) Start the timer for neighbour(Thread t3) and optimistically unchoked neighbour(Thread t4)
         */
        FileInputStream common = new FileInputStream("src/bitTorrents/Common.cfg");
        Properties properties = new Properties();
        properties.load(common);
        int numberOfPrefferedNeighbours = Integer.parseInt(properties.getProperty("NumberOfPreferredNeighbors"));
        int n = Integer.parseInt(properties.getProperty("UnchokingInterval"));
        int m = Integer.parseInt(properties.getProperty("OptimisticUnchokingInterval"));
        int pieceSize = Integer.parseInt(properties.getProperty("PieceSize"));
        String fileName = properties.getProperty("FileName",null);
        System.out.println(numberOfPrefferedNeighbours+" "+n+" "+m+" "+pieceSize+" "+fileName);
        PeerProcess1001 peerProcess = new PeerProcess1001(10003,1001,2,10,15,1000);
        peerProcess.startServer();//Thread t1
        peerProcess.read();// Thread t2

    }
}
