package bitTorrents.peer_1001;

import bitTorrents.Constants;
import bitTorrents.HandshakeMessage;
import bitTorrents.Message;
import bitTorrents.MyObjectOutputStream;

import java.util.*;
import java.net.*;
import java.io.*;
public class PeerProcess1001 {
    Thread t1,t2,t3,t4;
    private Constants constants = new Constants();
    ObjectInputStream objectInputStream = null;
    ObjectOutputStream objectOutputStream = null;
    private ServerSocket serverSocket = null; //Socket to start the server
    private List<Socket> connectionsFrom = null; //Peers which are connecting to this server
    private List<Socket> connectedTo = null;//Peers which this peers is connecting to
    private int PORT;
    private int ID;
    private Set<Socket> neighbours = null;// All neighbours of this peer
    private Socket optimisticUnchokedNeighbour = null; //One Optimisically Unckhoked Neighbouiur
    int numberOfPrefferedNeighbours; // # of Prefereed Neighbor (neighbour.size() <= numberOfPrefferedNeighbours)
    int n,m; //n - UnchokingInterval, m - OptimisticUnchokingInterval
    int pieceSize;
    private HashMap<Integer,List<Integer>> fileContents;//Contents of file
    private HashMap<Socket, long[]> downloadRate = null;//Download rate of all peers
    private String bitfield = null;//if contains contains, all 1s 1
    private HashMap<Socket,String> bitfields = null;
    private List<Integer> requestedIndices = new ArrayList<>();
    private HashMap<Socket,Integer> ids = new HashMap<>();
    boolean flag;

    PeerProcess1001(){}
    PeerProcess1001(int PORT, int ID, int numberOfPrefferedNeighbours, int n, int m, int pieceSize) {
        try {
            this.flag = false;
            this.PORT = PORT;
            this.ids = new HashMap<>();
            this.ID = ID;
            this.connectionsFrom = new ArrayList<>();
            this.connectedTo = new ArrayList<>();
            this.serverSocket = new ServerSocket(this.PORT);
            this.neighbours = new HashSet<>();
            this.optimisticUnchokedNeighbour = null;
            this.numberOfPrefferedNeighbours = numberOfPrefferedNeighbours;
            this.n = n;
            this.m = m;
            this.pieceSize = pieceSize;
            this.fileContents = new HashMap<>();
            this.bitfields = new HashMap<>();
            this.downloadRate = new HashMap<>();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    //Set the bitfield
    void setBitfield(int size,String file) {
        this.bitfield = new String();
        for(int i=0;i<size;i++)
            this.bitfield += file;
//        System.out.println("Bitfield:"+this.bitfield);
    }
    List<Integer> processBitField(String bitfield){
        List<Integer> pieces = new ArrayList<>();
        if(bitfield==null)return pieces;
        for(int i=0;i<=bitfield.length()-1;i++){
            if(this.bitfield==null ||(bitfield.charAt(i)=='1' && this.bitfield.charAt(i)=='0'))
                pieces.add(i);
        }
        return pieces;
    }
    //Process the file and add into list
    int processFile(String filename) {
        try{
            FileInputStream fileInputStream = new FileInputStream(filename);
            int i,count = 0;
            while((i=fileInputStream.read()) != -1){
                if(!fileContents.containsKey(count/pieceSize))fileContents.put(count/pieceSize,new ArrayList<>());
                this.fileContents.get(count/pieceSize).add(i);
                count++;
            }
            System.out.println(count+" "+fileContents.get(fileContents.size()-1).size());
            return count;
        }catch(Exception e){e.printStackTrace();return 0;}
    }
    //Start a thread and invoke this function
    void startServer() {
        try {
            while (true) {
                System.out.println("Listening to.."+this.PORT);
                Socket socket = serverSocket.accept();
                connectionsFrom.add(socket);
                /*
                 * Send the handshake signal by invoking sendHandshake
                 * function
                 */
                sendHandshake(socket);
//                System.out.println("Handshake Sent");
            }
        }catch (Exception e){e.printStackTrace();}
    }
    //read the data coming to the socket/peer continuously
    void read(){
        while(true) {
            if(this.t2.isInterrupted()){
                requestedIndices = new ArrayList<>();
                for(Socket socket:neighbours){
                    Message message = new Message();
                    message.setMessageType(constants.getCHOKE());
                    send(socket,message);
                }
                this.changeNeighbours();
                Thread.interrupted();
                System.out.println(this.t2.isInterrupted()+"hi");
                continue;
            }
//            else System.out.println("hello");
            List<Socket> cT = new ArrayList<>(this.connectedTo);
            cT.addAll(this.connectionsFrom);
            for (Socket socket : cT) {
                try {
                    socket.setSoTimeout(10);
//                    System.out.println("Hello " + ids.get(socket));
                    objectInputStream = new ObjectInputStream(socket.getInputStream());
                    Object object = objectInputStream.readObject();
//                    System.out.println("Hi");
                    if (object instanceof  bitTorrents.HandshakeMessage) {
                        HandshakeMessage handshakeMessage = (HandshakeMessage)object;
                        ids.put(socket,handshakeMessage.getId());
                        System.out.println(System.currentTimeMillis()+": Peer "+this.ID+" makes connection to Peer "+handshakeMessage.getId());
                        Message message = new Message();
                        message.setMessageType(constants.getBITFIELD());
                        message.setBitfield(this.bitfield);
                        send(socket,message);
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
                        if(object instanceof Message){
                            Message message = (Message) object;
                            if(message.getMessageType() == constants.getBITFIELD()) {
                                this.bitfields.put(socket,message.getBitfield());
                                List<Integer> pieces = processBitField(message.getBitfield());
                                if(pieces.size()==0){
                                    /* Send Not Interested Message */
                                    Message message1 = new Message();
                                    message1.setMessageType(constants.getNOT_INTERESTED());
                                    send(socket,message1);
                                }
                                else{
                                    Message message1 = new Message();
                                    message1.setMessageType(constants.getINTERESTED());
                                    send(socket,message1);
                                    Random random = new Random();
                                    while(pieces.size() > 0) {
                                        int remove = random.nextInt(pieces.size());
                                        Message message2 = new Message();
                                        message2.setMessageType(constants.getREQUEST());
                                        message2.setIndexField(pieces.get(remove));
                                        send(socket, message2);
                                        downloadRate.put(socket, new long[]{System.nanoTime(), 0});
                                        pieces.remove(remove);
                                    }
                                }
                            }
                            else if(message.getMessageType() == constants.getREQUEST()){
                                int index = message.getIndexField();
                                List<Integer> piece = this.neighbours.contains(socket)?fileContents.get(index):new ArrayList<>();
                                Message message1 = new Message();
                                message1.setMessageType(constants.getPIECE());
                                message1.setIndexField(index);
                                message1.setPayload(piece);
                                send(socket,message1);
//                                System.out.println(socket+" fhsghsodf "+downloadRate);
//                                System.out.println("Piece Sent");
                            }
                            else if(message.getMessageType() == constants.getPIECE()) {
//                                System.out.println("Piece Received "+message.getIndexField());
//                                System.out.println(socket);
                                int index = message.getIndexField();
                                char[] c = this.bitfield.toCharArray();
                                if(message.getPayload().size() != 0) {
                                    System.out.println(System.currentTimeMillis() + ": Peer " + this.ID + " has downloaded the piece " + index + " from " + ids.get(socket));
                                    c[index] = '1';
                                    this.bitfield = new String(c);
                                    this.fileContents.put(index, message.getPayload());
                                }
                                boolean flag = true;
                                for(char x:c){
                                    if(x=='0'){
                                        flag = false;
                                        break;
                                    }
                                }
                                if(flag){
                                    System.out.println("Processing file...");
                                    FileOutputStream fileOutputStream = new FileOutputStream("bitTorrents/peer_1001/tree.jpg");
                                    int size = fileContents.size();
                                    for(int i=0;i<size;i++){
                                        for(int j:fileContents.getOrDefault(i,new ArrayList<>()))
                                            fileOutputStream.write(j);
                                    }
                                    System.out.println("File transfer complete");
                                }

                                this.bitfield = new String(c);
                                if(requestedIndices.size()>0) {
                                    Random random = new Random();
                                    int remove = random.nextInt(requestedIndices.size());
                                    Message message2 = new Message();
                                    message2.setMessageType(constants.getREQUEST());
                                    message2.setIndexField(requestedIndices.get(remove));
                                    send(socket,message2);
                                    downloadRate.put(socket,new long[]{System.nanoTime(),0});
                                    requestedIndices.remove(remove);
//                                    System.out.println("Request Sent "+remove);
                                }
                            }
                            else if(message.getMessageType() == constants.getUNCHOKE()) {
                                /* bit is the bitfield what was stored  initially */
                                Message message1 = new Message();
                                message1.setMessageType((byte)4);
                                message1.setBitfield(this.bitfield);
                                send(socket,message1);
                                System.out.println(System.currentTimeMillis()+": Peer "+this.ID+" is unchoked by "+ids.get(socket));
                            }
                            else if(message.getMessageType() == -1){
                                Message message1 = new Message();
                                send(socket,message1);
                            }
                            else if(message.getMessageType() == constants.getHAVE()){
                                this.bitfields.put(socket,message.getBitfield());
                                List<Integer> pieces = processBitField(message.getBitfield());
                                if(pieces.size()==0){
                                    /* Send Not Interested Message */
                                    Message message1 = new Message();
                                    message1.setMessageType(constants.getNOT_INTERESTED());
                                    send(socket,message1);
                                }
                                else{
                                    Message message1 = new Message();
                                    message1.setMessageType(constants.getINTERESTED());
                                    send(socket,message1);
                                    List<Integer> l = new ArrayList<>();
                                    for(int i:pieces){
                                        if(!requestedIndices.contains(i)) {
                                            l.add(i);
                                            requestedIndices.add(i);
                                        }
                                    }
                                    System.out.println(requestedIndices.size());
                                    Random random = new Random();
                                    while(l.size() > 0) {
                                        int remove = random.nextInt(l.size());
                                        Message message2 = new Message();
                                        message2.setMessageType(constants.getREQUEST());
                                        message2.setIndexField(l.get(remove));
                                        send(socket, message2);
                                        requestedIndices.remove(requestedIndices.indexOf(l.get(remove)));
                                        l.remove(remove);

                                    }
                                }
                                System.out.println(System.currentTimeMillis()+": Peer "+this.ID+" received 'have' message from "+ids.get(socket));
                            }
                            else if(message.getMessageType() == constants.getINTERESTED()){
                                Message message1 = new Message();
                                send(socket,message1);
                                System.out.println(System.currentTimeMillis()+": Peer "+this.ID+" received the ‘interested’ message from "+ids.get(socket));
                            }
                            else if(message.getMessageType() == constants.getCHOKE()){
                                Message message1 = new Message();
                                send(socket,message1);
                                System.out.println(System.currentTimeMillis()+": Peer "+this.ID+" is choked by "+ids.get(socket));
                            }
                            else if(message.getMessageType() == constants.getNOT_INTERESTED()){
                                Message message1 = new Message();
                                send(socket,message1);
                                System.out.println(System.currentTimeMillis()+": Peer "+this.ID+" received the ‘not interested’ message "+ids.get(socket));
                            }
                            else send(socket,new Message());
                        }

                    }
                } catch (Exception e) {objectInputStream = null;continue;}
            }
        }

    }
    //Change Neighbours wrt download rate
    void changeNeighbours() {
        List<Socket> l = new ArrayList<>();
        l.addAll(this.connectedTo);l.addAll(this.connectionsFrom);
        this.neighbours = new HashSet<>();
        int size = l.size();
        while(this.neighbours.size() < Math.min(size,this.numberOfPrefferedNeighbours)) {
            this.neighbours.add(l.get(new Random().nextInt(l.size())));
        }
        for(Socket socket:this.neighbours){
            Message message = new Message();
            message.setMessageType(constants.getUNCHOKE());
            send(socket,message);
        }
        String s = "";
        for(Socket socket:this.neighbours)s += ids.get(socket) + ",";
        if(s.length() > 1)System.out.println(System.currentTimeMillis()+": Peer "+this.ID+" has the preferred neighbors "+s.substring(0,s.length()-1));
    }
    void changeOptimisticallyNeighbours() {
        Random random = new Random();
        List<Socket> optimistic = new ArrayList<Socket>();
        optimistic.addAll(this.connectionsFrom);
        optimistic.addAll(this.connectedTo);
        if(optimistic.size()==0)return;
        int index = random.nextInt(optimistic.size());
        while(this.optimisticUnchokedNeighbour!=null && this.neighbours.contains(optimistic.get(index))){
            index = random.nextInt(optimistic.size());
        }
        Message message = new Message();
        send(optimistic.get(index),message);
        this.optimisticUnchokedNeighbour = optimistic.get(index);
        System.out.println(System.currentTimeMillis()+": Peer "+this.ID+" has the optimistically unchoked neighbor "+ids.get(this.optimisticUnchokedNeighbour));
    }
    // Change Handshake Signal
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
            synchronized (socket) {
                System.out.println("Sending "+ids.get(socket)+" ");
                objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
                objectOutputStream.writeObject(message);
                System.out.println("Sent");
            }
        }catch (Exception e){e.printStackTrace();}
    }
    //Unchoking Interval Countdown
    void unchokingInterval() {
        while (true){
            try {
//                flag = true;
                for(int i=0;i<this.n;i++)
                    Thread.sleep( 1000);
//
//                flag = false;
//                for(Socket socket:this.neighbours){
//                    Message message = new Message();
//                    message.setMessageType(constants.getCHOKE());
////                    send(socket,message);
//                }
//                this.changeNeighbours();
                this.t2.interrupt();
//                System.out.println(this.t2.isInterrupted());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    //optimistic Unchoked Interval Countdown
    void optimisticUnchokedInterval() {
        while(true) {
            try {
//                flag = true;
                for(int i=0;i<this.m;i++)
                    Thread.sleep( 1000);
//                flag = false;
                Message message = new Message();
                message.setMessageType(constants.getCHOKE());
//                System.out.println(this.optimisticUnchokedNeighbour+" "+this.connectedTo+" "+this.connectionsFrom);
                if(this.optimisticUnchokedNeighbour!=null)
                    send(this.optimisticUnchokedNeighbour,message);
//                this.changeOptimisticallyNeighbours();
//                System.out.println("Change optimistically unchoked neighbour"); //Change optimistically unchoked neighbour
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
//            System.out.println("Handshake Sent");
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
        int id = Integer.parseInt(args[0]);
        FileInputStream common = new FileInputStream("bitTorrents/Common.cfg");
        Properties properties = new Properties();
        properties.load(common);int port = 0;
        int numberOfPrefferedNeighbours = Integer.parseInt(properties.getProperty("NumberOfPreferredNeighbors"));
        int n = Integer.parseInt(properties.getProperty("UnchokingInterval"));
        int m = Integer.parseInt(properties.getProperty("OptimisticUnchokingInterval"));
        int pieceSize = Integer.parseInt(properties.getProperty("PieceSize"));
        int fileSize = Integer.parseInt(properties.getProperty("FileSize"));
        String fileName = properties.getProperty("FileName",null);
//        System.out.println(numberOfPrefferedNeighbours+" "+n+" "+m+" "+pieceSize+" "+fileName);
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
                if(content[content.length-1].equals("1")) {
                    int size = peerProcess.processFile("bitTorrents/peer_1001/"+fileName);
                    peerProcess.setBitfield((int) (Math.ceil((double) size / (double) pieceSize)), content[content.length - 1]);
                }
                else{
                    peerProcess.setBitfield((int) (Math.ceil((double) fileSize / (double) pieceSize)), content[content.length - 1]);
                }
                break;
            }
            String peerHost = content[1];
            int peerPort = Integer.parseInt(content[2]);

            peerProcess.connect(peerHost,peerPort);
        }
        peerProcess.changeNeighbours();
        peerProcess.changeOptimisticallyNeighbours();
        /* ****** */

        /* Define all Threads */
        peerProcess.t1 = new Thread(()->{
            peerProcess.startServer();
        });
        peerProcess.t2 = new Thread(()->{
            peerProcess.read();
        });
        peerProcess.t3 = new Thread(()->{
            peerProcess.unchokingInterval();
        });
        peerProcess.t4 = new Thread(()->{
            peerProcess.optimisticUnchokedInterval();
        });
        /* ****** */

        /* Start all Threads */
        peerProcess.t1.start();peerProcess.t2.start();
        peerProcess.t3.start();
//        peerProcess.t4.start();
        /* ****** */
    }
}


