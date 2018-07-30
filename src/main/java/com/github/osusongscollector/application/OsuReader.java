package com.github.osusongscollector.application;

import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public abstract class OsuReader {
	// private member
    private DataInputStream reader;
    
	// private methods
    private long readLong() throws IOException {
        // 8 bytes, little endian
        byte[] bytes = new byte[8];
        this.reader.readFully(bytes);
        ByteBuffer bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        return bb.getLong();
    }

    private int readULEB128() throws IOException {
        // variable bytes, little endian
        // MSB says if there will be more bytes. If cleared,
        // that byte is the last.
        int value = 0;
        for (int shift = 0; shift < 32; shift += 7) {
            byte b = this.reader.readByte();
            value |= ((int) b & 0x7F) << shift;

            if (b >= 0) return value; // MSB is zero. End of value.
        }
        throw new IOException("ULEB128 too large");
    }
    
    // protected methods
    protected void setReader(DataInputStream dis) {
    	this.reader = dis;
    }
    
    protected void skipBytes(int numOfBytes) throws IOException {
    	this.reader.skipBytes(numOfBytes);
    }
    
    protected void skipString() throws IOException {
    	byte kind = this.reader.readByte();
        if (kind == 0) {
        	return;
        }
        if (kind != 11) {
            throw new IOException(String.format("String format error: Expected 0x0B or 0x00, found 0x%02X", (int) kind & 0xFF));
        }
        int length = this.readULEB128();
        this.skipBytes(length);
    }
    
    protected byte readByte() throws IOException {
        // 1 byte
        return this.reader.readByte();
    }

    protected short readShort() throws IOException {
        // 2 bytes, little endian
        byte[] bytes = new byte[2];
        this.reader.readFully(bytes);
        ByteBuffer bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        return bb.getShort();
    }

    protected int readInt() throws IOException {
        // 4 bytes, little endian
        byte[] bytes = new byte[4];
        this.reader.readFully(bytes);
        ByteBuffer bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        return bb.getInt();
    }

    protected float readSingle() throws IOException {
        // 4 bytes, little endian
        byte[] bytes = new byte[4];
        this.reader.readFully(bytes);
        ByteBuffer bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        return bb.getFloat();
    }

    protected double readDouble() throws IOException {
        // 8 bytes little endian
        byte[] bytes = new byte[8];
        this.reader.readFully(bytes);
        ByteBuffer bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        return bb.getDouble();
    }

    protected boolean readBoolean() throws IOException {
        // 1 byte, zero = false, non-zero = true
        return this.reader.readBoolean();
    }

    protected String readString() throws IOException {
        // variable length
        // 00 = empty String
        // 0B <length> <char>* = normal String
        // <length> is encoded as an LEB, and is the byte length of the rest.
        // <char>* is encoded as UTF8, and is the String content.
        byte kind = this.reader.readByte();
        if (kind == 0) return "";
        if (kind != 11) {
            throw new IOException(String.format("String format error: Expected 0x0B or 0x00, found 0x%02X", (int) kind & 0xFF));
        }
        int length = this.readULEB128();
        if (length == 0) return "";
        byte[] utf8bytes = new byte[length];
        this.reader.readFully(utf8bytes);
        return new String(utf8bytes, "UTF-8");
    }

    protected long readDate() throws IOException {
        long ticks = this.readLong();
        if (ticks == 0) {
        	return 0;
        }
        long TICKS_AT_EPOCH = 621355968000000000L;
        long TICKS_PER_MILLISECOND = 10000;

        return (ticks - TICKS_AT_EPOCH)/TICKS_PER_MILLISECOND;
    }
    
    protected void closeFile() throws IOException  {
		try {
			this.reader.close();
		} catch (IOException e) {
			throw new IOException("Failed to close file.");
		}
    }
}
