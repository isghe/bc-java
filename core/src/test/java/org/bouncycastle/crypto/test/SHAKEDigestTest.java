package org.bouncycastle.crypto.test;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStreamReader;

import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.digests.SHAKEDigest;
import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.encoders.Hex;
import org.bouncycastle.util.test.SimpleTest;

/**
 * SHAKE Digest Test
 */
public class SHAKEDigestTest
    extends SimpleTest
{
    static class MySHAKEDigest extends SHAKEDigest
    {
        MySHAKEDigest(int bitLength)
        {
            super(bitLength);
        }

        int myDoFinal(byte[] out, int outOff, int outLen, byte partialByte, int partialBits)
        {
            return doFinal(out, outOff, outLen, partialByte, partialBits);
        }
    }

    SHAKEDigestTest()
    {
    }

    public String getName()
    {
        return "SHAKE";
    }

    public void performTest() throws Exception
    {
        testVectors();
    }

    public void testVectors() throws Exception
    {
        BufferedReader r = new BufferedReader(new InputStreamReader(
            getClass().getResourceAsStream("SHAKETestVectors.txt")));

        String line;
        while (null != (line = readLine(r)))
        {
            if (line.length() != 0)
            {
                TestVector v = readTestVector(r, line);
                runTestVector(v);
            }
        }

        r.close();
    }

    private MySHAKEDigest createDigest(String algorithm) throws Exception
    {
        if (algorithm.startsWith("SHAKE-"))
        {
            int bits = Integer.parseInt(algorithm.substring("SHAKE-".length()));
            return new MySHAKEDigest(bits);
        }
        throw new IllegalArgumentException("Unknown algorithm: " + algorithm);
    }

    private byte[] decodeBinary(String block)
    {
        int bits = block.length();
        int fullBytes = bits / 8;
        int totalBytes = (bits + 7) / 8;
        byte[] result = new byte[totalBytes];

        for (int i = 0; i < fullBytes; ++i)
        {
            String byteStr = reverse(block.substring(i * 8, (i + 1) * 8));
            result[i] = (byte)Integer.parseInt(byteStr, 2);
        }

        if (totalBytes > fullBytes)
        {
            String byteStr = reverse(block.substring(fullBytes * 8));
            result[fullBytes] = (byte)Integer.parseInt(byteStr, 2);
        }

        return result;
    }

    private String readBlock(BufferedReader r) throws IOException
    {
        StringBuffer b = new StringBuffer();
        String line;
        while ((line = readBlockLine(r)) != null)
        {
            b.append(line);
        }
        return b.toString();
    }

    private String readBlockLine(BufferedReader r) throws IOException
    {
        String line = readLine(r);
        if (line == null || line.length() == 0)
        {
            return null;
        }
        return line.replaceAll(" ", "");
    }

    private TestVector readTestVector(BufferedReader r, String header) throws IOException
    {
        String[] parts = header.split(TestVector.SAMPLE_OF);

        String algorithm = parts[0];
        int bits = Integer.parseInt(stripFromChar(parts[1], '-'));

        skipUntil(r, TestVector.MSG_HEADER);
        String messageBlock = readBlock(r);
        if (messageBlock.length() != bits)
        {
            throw new IllegalStateException("Test vector length mismatch");
        }
        byte[] message = decodeBinary(messageBlock);

        skipUntil(r, TestVector.OUTPUT_HEADER);
        byte[] output = Hex.decode(readBlock(r));

        return new TestVector(algorithm, bits, message, output);
    }

    private String readLine(BufferedReader r) throws IOException
    {
        String line = r.readLine();
        return line == null ? null : stripFromChar(line, '#').trim();
    }

    private String requireLine(BufferedReader r) throws IOException
    {
        String line = readLine(r);
        if (line == null)
        {
            throw new EOFException();
        }
        return line;
    }

    private String reverse(String s)
    {
        return new StringBuffer(s).reverse().toString();
    }

    private void runTestVector(TestVector v) throws Exception
    {
        int bits = v.getBits();
        int partialBits = bits % 8;

        byte[] expected = v.getOutput();

//        System.out.println(v.getAlgorithm() + " " + bits + "-bit");
//        System.out.println(Hex.toHexString(v.getMessage()).toUpperCase());
//        System.out.println(Hex.toHexString(expected).toUpperCase());

        int outLen = expected.length;

        MySHAKEDigest d = createDigest(v.getAlgorithm());
        byte[] output = new byte[outLen];

        byte[] m = v.getMessage();
        if (partialBits == 0)
        {
            d.update(m, 0, m.length);
            d.doFinal(output, 0, outLen);
        }
        else
        {
            d.update(m, 0, m.length - 1);
            d.myDoFinal(output, 0, outLen, m[m.length - 1], partialBits);
        }

        if (!Arrays.areEqual(expected, output))
        {
            fail(v.getAlgorithm() + " " + v.getBits() + "-bit test vector hash mismatch");
//            System.err.println(v.getAlgorithm() + " " + v.getBits() + "-bit test vector hash mismatch");
//            System.err.println(Hex.toHexString(output).toUpperCase());
        }
    }

    private void skipUntil(BufferedReader r, String header) throws IOException
    {
        String line;
        do
        {
            line = requireLine(r);
        }
        while (line.length() == 0);
        if (!line.equals(header))
        {
            throw new IOException("Expected: " + header);
        }
    }

    private String stripFromChar(String s, char c)
    {
        int i = s.indexOf(c);
        if (i >= 0)
        {
            s = s.substring(0, i);
        }
        return s;
    }

    protected Digest cloneDigest(Digest digest)
    {
        return new SHAKEDigest((SHAKEDigest)digest);
    }

    public static void main(
        String[]    args)
    {
        runTest(new SHAKEDigestTest());
    }

    private static class TestVector
    {
        private static String SAMPLE_OF = " sample of ";
        private static String MSG_HEADER = "Msg as bit string";
        private static String OUTPUT_HEADER = "Output val is";

        private String algorithm;
        private int bits;
        private byte[] message;
        private byte[] output;

        private TestVector(String algorithm, int bits, byte[] message, byte[] output)
        {
            this.algorithm = algorithm;
            this.bits = bits;
            this.message = message;
            this.output = output;
        }

        public String getAlgorithm()
        {
            return algorithm;
        }

        public int getBits()
        {
            return bits;
        }
        
        public byte[] getMessage()
        {
            return message;
        }

        public byte[] getOutput()
        {
            return output;
        }
    }
}
