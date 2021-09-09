package bitTorrents;

public class HandshakeMessage {
    private int id;
    private String handshakeHeader;
    private String zeroBits;

    public HandshakeMessage(int id) {
        this.id = id;
        this.handshakeHeader = "P2PFILESHARINGPROJ";
        this.zeroBits = "0000000000";
    }
}

