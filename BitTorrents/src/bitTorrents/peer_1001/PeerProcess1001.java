package bitTorrents.peer_1001;

import bitTorrents.HandshakeMessage;
import bitTorrents.Message;

import java.util.*;
import java.net.*;
import java.io.*;
public class PeerProcess1001 {
    private ServerSocket serverSocket = null; //Socket to start the server
    private List<Socket> connectionsFrom = null; //Peers which are connecting to this server
    private List<Socket> connectedTo = null;//Peers which this peers is connecting to
    private int PORT;
    private int ID;
    private HashSet<Socket> neighbours = null;// All neighbours of this peer
    private Socket optimisticUnchokedNeighbour = null; //One Optimisically Unckhoked Neighbouiur
    int numberOfPrefferedNeighbours; // # of Prefereed Neighbor (neighbour.size() <= numberOfPrefferedNeighbours)
    int n,m; //n - UnchokingInterval, m - OptimisticUnchokingInterval
    int pieceSize;
    private List<Integer> fileContents;
    private HashMap<Socket, Long> downloadRate = null;//Download rate of all peers
    private String bitfield = null;

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
            this.downloadRate = new HashMap<>();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    //Start a thread and invoke this function
    void startServer() {
        try {
            while (true) {
                System.out.println("Listening to.."+this.PORT);
                Socket socket = serverSocket.accept();
                System.out.println("Connected");
                connectionsFrom.add(socket);
                /*
                * Send the handshake signal by invoking sendHandshake
                * function
                 */
                sendHandshake(socket);
                System.out.println("Handshake Sent");
                downloadRate.put(socket,System.nanoTime());
                Thread.sleep(500);
            }
        }catch (Exception e){e.printStackTrace();}
    }
    //read the data coming to the socket/peer continuously
    void read(){
        System.out.println("Reading....");
        while(true) {
//            System.out.println(connectionsFrom);
            for (Socket socket : connectedTo) {
                try {
                    ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
                    Object object = objectInputStream.readObject();
//                    System.out.println(object);
                    if (object instanceof  bitTorrents.HandshakeMessage) {
                        downloadRate.put(socket,System.nanoTime() - downloadRate.get(socket));
                        System.out.println("Handshake Received");
                        /*
                         * Either send the handshake signal by invoking sendHandshake(socket) function
                         * or Process the message and send the appropriate message
                         * by creating a Message object and invoke send(socket,message) function
                         */
                    } else {
                        /*
                         * Process the message and send the appropriate message
                         * by creating a Message object and invoke send function
                         */
                    }
                    Thread.sleep(500);
                } catch (Exception e) {e.printStackTrace();continue;}
            }
//            System.out.println(connectionsFrom);
            for (Socket socket : connectionsFrom) {
                try {
//                    System.out.println(socket+" hi");
                    ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
                    Object object = objectInputStream.readObject();
//                    System.out.println(object);
                    if (object instanceof  bitTorrents.HandshakeMessage) {
                        downloadRate.put(socket,System.nanoTime() - downloadRate.get(socket));
                        System.out.println("Handshake Received");
                        /*
                         * Either send the handshake signal by invoking sendHandshake(socket) function
                         * or Process the message and send the appropriate message
                         * by creating a Message object and invoke send(socket,message) function
                         */
                    } else {
                        /*
                         * Process the message and send the appropriate message
                         * by creating a Message object and invoke send function
                         */
                    }
                    } catch (Exception e) {e.printStackTrace();continue;}
            }
            try{Thread.sleep(1000);}catch (Exception e){}
        }
    }
    //Change Neighbours wrt download rate
    void changeNeighbours() {
        TreeSet<Socket> set = new TreeSet<>((a,b)->(int)(downloadRate.get(a)-downloadRate.get(b)));
        neighbours.clear();
        set.addAll(downloadRate.keySet());Iterator<Socket> it = set.iterator();
        while(neighbours.size() < Math.min(set.size(),this.numberOfPrefferedNeighbours))
            neighbours.add(it.next());
    }
    //Change Handshake Signal
    void sendHandshake(Socket socket) {
        try {
            HandshakeMessage handshakeMessage = new HandshakeMessage(this.ID);
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
            objectOutputStream.writeObject(handshakeMessage);
        }catch(Exception e){e.printStackTrace();}
    }
    //Send a messaage
    void send(Socket socket, Message message) {
        try {
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
            objectOutputStream.writeObject(message);
        }catch (Exception e){e.printStackTrace();}
    }
    //Unchoking Interval Countdown
    void unchokingInterval() {
        while (true){
            try {
                Thread.sleep(this.n * 1000);
                System.out.println("Change neighbours"); //Change neighbours
                this.changeNeighbours();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    //optimistic Unchoked Interval Countdown
    void optimisticUnchokedInterval() {
        while(true) {
            try {
                Thread.sleep(this.m * 1000);
                System.out.println("Change optimistically unchoked neighbour"); //Change optimistically unchoked neighbour
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    //Connect this socket to other peer
    boolean connect(String host,int PORT) {
        try{
            Socket socket = new Socket(host,PORT);
            connectedTo.add(socket);
            /*
             * Send the handshake signal by invoking sendHandshake
             * function
             */
            sendHandshake(socket);
            System.out.println("Handshake Sent");
            downloadRate.put(socket,System.nanoTime());
            return true;
        }catch(Exception e){e.printStackTrace();return false;}
    }
    //Set Port number
    public void setPORT(int PORT) {
        this.PORT = PORT;
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

        /* Get data from common.cfg */
        int id = Integer.parseInt("1001");
        FileInputStream common = new FileInputStream("bitTorrents/Common.cfg");
        Properties properties = new Properties();
        properties.load(common);int port = 0;
        int numberOfPrefferedNeighbours = Integer.parseInt(properties.getProperty("NumberOfPreferredNeighbors"));
        int n = Integer.parseInt(properties.getProperty("UnchokingInterval"));
        int m = Integer.parseInt(properties.getProperty("OptimisticUnchokingInterval"));
        int pieceSize = Integer.parseInt(properties.getProperty("PieceSize"));
        String fileName = properties.getProperty("FileName",null);
        System.out.println(numberOfPrefferedNeighbours+" "+n+" "+m+" "+pieceSize+" "+fileName);
        PeerProcess1001 peerProcess = new PeerProcess1001(1001,id,numberOfPrefferedNeighbours,n,m,pieceSize);
         /* ****** */

        /* Get data from PeerInfo.cfg and connect */
        FileInputStream inputStream = new FileInputStream("bitTorrents/PeerInfo.cfg");
        String data = "";int i = 0;
        while((i=inputStream.read()) != -1){data += (char)i;}
        String[] peers = data.split("\n");
        for(String peer:peers){
            String[] content = peer.split(" ");
            if(Integer.parseInt(content[0]) == id){
                port = Integer.parseInt(content[2]);
                peerProcess.setPORT(port);
                break;
            }
            String peerHost = content[1];
            int peerPort = Integer.parseInt(content[2]);
            peerProcess.connect(peerHost,peerPort);
        }
        /* ****** */

        /* Define all Threads */
        Thread t1 = new Thread(()->{
            peerProcess.startServer();
        });
        Thread t2 = new Thread(()->{
            peerProcess.read();
        });
        Thread t3 = new Thread(()->{
            peerProcess.unchokingInterval();
        });
        Thread t4 = new Thread(()->{
            peerProcess.optimisticUnchokedInterval();
        });
        /* ****** */

        /* Start all Threads */
        t1.start();t2.start();
        t3.start();t4.start();
        /* ****** */
    }
}
