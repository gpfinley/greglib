package greglib.audio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Utility methods for working with Microsoft .wav files.
 *
 * Created by greg on 5/24/18.
 */
public class Wav {

    /**
     * Prepend a WAVE header to a sequence of binary wave data.
     * TODO: test thoroughly
     * @param audio
     * @return
     */
    public static byte[] addHeader(byte[] audio, int channels, int sr, int bits) {
        byte[] wav = new byte[44 + audio.length];
        byte[] temp;
        wav[0] = 'R';
        wav[1] = 'I';
        wav[2] = 'F';
        wav[3] = 'F';
        temp = bytesOfInt(audio.length + 36);
        wav[4] = temp[0];
        wav[5] = temp[1];
        wav[6] = temp[2];
        wav[7] = temp[3];
        wav[8] = 'W';
        wav[9] = 'A';
        wav[10] = 'V';
        wav[11] = 'E';
        wav[12] = 'f';
        wav[13] = 'm';
        wav[14] = 't';
        wav[15] = ' ';
        temp = bytesOfInt(16);
        wav[16] = temp[0];
        wav[17] = temp[1];
        wav[18] = temp[2];
        wav[19] = temp[3];
        temp = bytesOfShort((short) 1);
        wav[20] = temp[0];
        wav[21] = temp[1];
        temp = bytesOfShort((short) channels);
        wav[22] = temp[0];
        wav[23] = temp[1];
        temp = bytesOfInt(sr);
        wav[24] = temp[0];
        wav[25] = temp[1];
        wav[26] = temp[2];
        wav[27] = temp[3];
        temp = bytesOfInt(channels * sr * bits / 8);
        wav[28] = temp[0];
        wav[29] = temp[1];
        wav[30] = temp[2];
        wav[31] = temp[3];
        temp = bytesOfShort((short) (channels * bits / 8));
        wav[32] = temp[0];
        wav[33] = temp[1];
        temp = bytesOfShort((short) bits);
        wav[34] = temp[0];
        wav[35] = temp[1];
        wav[36] = 'd';
        wav[37] = 'a';
        wav[38] = 't';
        wav[39] = 'a';
        temp = bytesOfInt(audio.length);
        wav[40] = temp[0];
        wav[41] = temp[1];
        wav[42] = temp[2];
        wav[43] = temp[3];
        for (int i=0; i<audio.length; i++) {
            wav[i+44] = audio[i];
        }
        return wav;
    }

    /**
     * Prepend a header to binary data and write to a wav file.
     * @param outPath
     * @param audio
     * @param channels
     * @param sr
     * @param bits
     * @throws IOException
     */
    public static void writeWav(Path outPath, byte[] audio, int channels, int sr, int bits) throws IOException {
        Files.write(outPath, addHeader(audio, channels, sr, bits));
    }

    private static byte[] bytesOfInt(int integer) {
        return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(integer).array();
    }

    private static byte[] bytesOfShort(short shortInt) {
        return ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(shortInt).array();
    }
}
