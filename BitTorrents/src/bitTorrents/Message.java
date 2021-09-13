package bitTorrents;
import java.util.*;
import java.io.Serializable;
public class Message implements Serializable{
    private int messageLength;
    private byte messageType;
    private List<Integer> payload;
    private String bitfield = null;
    public Message(){}

    public void setBitfield(String bitfield) {
        this.bitfield = bitfield;
    }

    public Message(int messageLength, byte messageType, List<Integer> payload) {
        this.messageLength = messageLength;
        this.messageType = messageType;
        this.payload = payload;
    }

    public int getMessageLength() {
        return messageLength;
    }

    public void setMessageLength(int messageLength) {
        this.messageLength = messageLength;
    }

    public byte getMessageType() {
        return messageType;
    }

    public void setMessageType(byte messageType) {
        this.messageType = messageType;
    }

    public List<Integer> getPayload() {
        return payload;
    }

    public void setPayload(List<Integer> payload) {
        this.payload = payload;
    }
}

