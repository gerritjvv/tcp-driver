package tcpdriver.io;


import com.sun.org.apache.xerces.internal.impl.dv.xs.StringDV;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.TimeoutException;

/**
 * Mutable IO Utility functions used in clojure code.
 */
public class IOUtil {

    /**
     * Read 2 bytes for length, and then length bytes, return a String of format UTF-8
     *
     * @param in
     * @param timeoutMs
     * @return
     * @throws InterruptedException
     * @throws IOException
     * @throws TimeoutException
     */
    public static final String readShortString(InputStream in, long timeoutMs) throws InterruptedException, IOException, TimeoutException {
        short len = readShort(in, timeoutMs);
        byte[] bts = readBytes(in, len, timeoutMs);
        return new String(bts, "UTF-8");
    }

    /**
     * Read 8 bytes and return a double
     *
     * @param in
     * @param timeoutMs
     * @return
     * @throws InterruptedException
     * @throws IOException
     * @throws TimeoutException
     */
    public static final double readDouble(InputStream in, long timeoutMs) throws InterruptedException, IOException, TimeoutException {
        return Double.longBitsToDouble(readLong(in, timeoutMs));
    }

    /**
     * Read 4 bytes and return a float
     *
     * @param in
     * @param timeoutMs
     * @return
     * @throws InterruptedException
     * @throws IOException
     * @throws TimeoutException
     */
    public static final float readFloat(InputStream in, long timeoutMs) throws InterruptedException, IOException, TimeoutException {
        return Float.intBitsToFloat(readInt(in, timeoutMs));
    }

    /**
     * Read 2 bytes and return a short
     *
     * @param in
     * @param timeoutMs
     * @return
     * @throws InterruptedException
     * @throws IOException
     * @throws TimeoutException
     */
    public static final short readShort(InputStream in, long timeoutMs) throws InterruptedException, IOException, TimeoutException {
        byte[] bts = readBytes(in, 2, timeoutMs);
        return (short) ((bts[0] << 8) | (bts[1] & 0xff));
    }

    /**
     * Read 8 bytes and return a long
     *
     * @param in
     * @param timeoutMs
     * @return
     * @throws InterruptedException
     * @throws IOException
     * @throws TimeoutException
     */
    public static final long readLong(InputStream in, long timeoutMs) throws InterruptedException, IOException, TimeoutException {
        byte[] bts = readBytes(in, 8, timeoutMs);
        return (((long) (bts[0] & 0xff) << 56) |
                ((long) (bts[1] & 0xff) << 48) |
                ((long) (bts[2] & 0xff) << 40) |
                ((long) (bts[3] & 0xff) << 32) |
                ((long) (bts[4] & 0xff) << 24) |
                ((long) (bts[5] & 0xff) << 16) |
                ((long) (bts[6] & 0xff) << 8) |
                ((long) (bts[7] & 0xff)));
    }

    /**
     * Read 4 bytes and return an int
     *
     * @param in
     * @param timeoutMs
     * @return
     * @throws InterruptedException
     * @throws IOException
     * @throws TimeoutException
     */
    public static final int readInt(InputStream in, long timeoutMs) throws InterruptedException, IOException, TimeoutException {
        byte[] bts = readBytes(in, 4, timeoutMs);
        return (((bts[0] & 0xff) << 24) | ((bts[1] & 0xff) << 16) |
                ((bts[2] & 0xff) << 8) | (bts[3] & 0xff));
    }

    public static final byte readByte(InputStream in, long timeoutMs) throws InterruptedException, IOException, TimeoutException {
        return readBytes(in, 1, timeoutMs)[0];
    }




    /**
     * Read expectedBytes from an input stream and time out if no change on the input stream for more than timeoutMs.<br/>
     * Bytes are read from the input stream as they become available.
     *
     * @param in
     * @param expectedBytes the total number of bytes to read, a byte array of this size is created
     * @param timeoutMs
     * @return
     * @throws TimeoutException
     * @throws IOException
     */
    public static final byte[] readBytes(InputStream in, int expectedBytes, long timeoutMs) throws TimeoutException, IOException, InterruptedException {
        byte[] bytes = new byte[expectedBytes];
        int pos = 0;
        long lastTimeAvailable = System.currentTimeMillis();

        do {
            int avail = in.available();
            if (avail > 0) {
                //have an new data, read the available bytes and add to the byte array, note we use expectedBytes-pos, to calculate the remaining bytes
                //required to read
                int btsRead = in.read(bytes, pos, Math.min(avail, expectedBytes - pos));
                pos += btsRead;

                if (pos >= expectedBytes) //we've read all required bytes, exit the loop
                    break;

                //save the last time data was available
                lastTimeAvailable = System.currentTimeMillis();
            } else if ((System.currentTimeMillis() - lastTimeAvailable) > timeoutMs) {
                //check for timeout
                throw new TimeoutException("Timeout while reading data from the input stream: got only " + pos + " bytes of " + expectedBytes + " last seen " + lastTimeAvailable + " diff " + (System.currentTimeMillis() - lastTimeAvailable));
            } else {
                //sleep to simulate IO blocking and avoid consuming CPU resources on IO wait
                Thread.sleep(100);
            }

        } while (true);

        return bytes;
    }

    public static final void write(OutputStream out, double v) throws IOException {
        write(out, Double.doubleToLongBits(v));
    }

    public static final void write(OutputStream out, float v) throws IOException {
        write(out, Float.floatToIntBits(v));
    }

    public static final void write(OutputStream out, long v) throws IOException {
        write(out, new byte[]{
                (byte) (0xff & (v >> 56)),
                (byte) (0xff & (v >> 48)),
                (byte) (0xff & (v >> 40)),
                (byte) (0xff & (v >> 32)),
                (byte) (0xff & (v >> 24)),
                (byte) (0xff & (v >> 16)),
                (byte) (0xff & (v >> 8)),
                (byte) (0xff & v)
        });
    }

    public static final void write(OutputStream out, short v) throws IOException {
        write(out, new byte[]{
                (byte) (0xff & (v >> 8)),
                (byte) (0xff & v)
        });
    }

    public static final void write(OutputStream out, int v) throws IOException {
        write(out, new byte[]{
                (byte) (0xff & (v >> 24)),
                (byte) (0xff & (v >> 16)),
                (byte) (0xff & (v >> 8)),
                (byte) (0xff & v)
        });
    }

    public static final void write(OutputStream out, byte[] bts) throws IOException {
        out.write(bts);
    }

    public static final void write(OutputStream out, byte[] bts, int from, int len) throws IOException {
        out.write(bts, from, len);
    }

    public static final void writeShortString(OutputStream out, String str) throws IOException {
        write(out, (short) str.length());
        write(out, str.getBytes("UTF-8"));
    }

    public static final void write(OutputStream out, byte v) throws IOException {
        out.write(v);
    }

}