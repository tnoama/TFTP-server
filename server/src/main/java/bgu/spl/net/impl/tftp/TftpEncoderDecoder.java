package bgu.spl.net.impl.tftp;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import bgu.spl.net.api.MessageEncoderDecoder;

public class TftpEncoderDecoder implements MessageEncoderDecoder<byte[]> {
    // TODO: Implement here the TFTP encoder and decoder
    private byte[] bytes = new byte[1 << 10]; // start with 1k
    private int len = 0;
    private int opcode = 0;
    private short size = 0;
    private byte[] dataSize = new byte[2];

    public short byteToShort(byte[] b) {
        short b_short = (short) (((short) b[0] & 0xFF) << 8 | (short) (b[1] & 0xFF));
        return b_short;
    }

    public Byte[] shortToByte(short a) {
        Byte[] a_bytes = new Byte[] { (byte) (a >> 8), (byte) (a & 0xff) };
        return a_bytes;
    }

    @Override
    public byte[] decodeNextByte(byte nextByte) {

        if (len == 0) {// decoding first byte
            pushByte(nextByte);
            return null;
        }
        if (len == 1) {// decoding second byte
            opcode = Byte.toUnsignedInt(nextByte);
            pushByte(nextByte);
            if (opcode == 6 || opcode == 10) {
                return copyBytes(bytes, len);
            }
            return null;
        }
        if (len >= 2) {
            if (Byte.toUnsignedInt(nextByte) == 0 && opcode != 3 && opcode != 4 && opcode != 5 && opcode != 6
                    && opcode != 10) {// if we reach zero and we supposed to return, return
                return copyBytes(bytes, len);
            } else if (opcode == 3) {// data
                // saving the size of this data packet
                if (len == 2) {
                    dataSize[0] = nextByte;
                }
                if (len == 3) {
                    dataSize[1] = nextByte;
                    size = byteToShort(dataSize);
                }
                if (len >= 4 && len == size + 5) {
                    pushByte(nextByte);
                    return copyBytes(bytes, len);
                }
            } else if (opcode == 4 && len >= 3) {
                pushByte(nextByte);
                return copyBytes(bytes, len);
            } else if (opcode == 5 && len >= 4 && Byte.toUnsignedInt(nextByte) == 0) {
                return copyBytes(bytes, len);
            }

            pushByte(nextByte);
        }
        if (len >= 510) {
            return null;
        }
        return null;

    }

    @Override
    public byte[] encode(byte[] message) {
        return message;
    }

    private void pushByte(Byte nextByte) {
        if (len >= bytes.length) {
            bytes = Arrays.copyOf(bytes, len * 2);
        }

        bytes[len] = nextByte;
        len++;
    }

    private byte[] copyBytes(byte[] toCopy, int len) {
        byte[] toReturn = new byte[len];
        for (int i = 0; i < len; i++) {
            toReturn[i] = toCopy[i];
        }
        this.len = 0;
        opcode = 0;
        size = 0;
        Arrays.fill(bytes, (byte) 0);
        Arrays.fill(dataSize, (byte) 0);
        return toReturn;
    }

}