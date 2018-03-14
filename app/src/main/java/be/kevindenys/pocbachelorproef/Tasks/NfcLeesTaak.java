package be.kevindenys.pocbachelorproef.Tasks;

import android.content.Context;
import android.nfc.Tag;
import android.nfc.tech.NfcV;
import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import be.kevindenys.pocbachelorproef.App;

/**
 * Created by Kevin on 7/03/2018.
 */

public class NfcLeesTaak extends AsyncTask<Tag, Void, byte[]> {

    private Context context;
    // Via NFC tools weten we de sensor byte grote (360) en de block grote (8)
    private final int AANTAL_BLOKKEN = 360 / 8;
    //byteArray maken om de sensor data in op te slaan
    private byte[] sensorInBytes = new byte[360];

    private App myApp;

    public NfcLeesTaak(Context context, App app) {
        this.context = context;
        this.myApp = app;
    }

    @Override
    protected byte[] doInBackground(Tag... tags) {
        int byteOffset = 2;
        byte[] temp;
        byte[] block = new byte[360];

        // Sensor Tag
        Tag tag = tags[0];
        // ID van de Sensor tag
        final byte[] uid = tag.getId();
        // NfcV sensor
        NfcV NfcVsensor = NfcV.get(tag);


        try {
            // connecteren
            NfcVsensor.connect();

            for (int i = 0; i < AANTAL_BLOKKEN; i++) {

                temp = new byte[]{
                        (byte) 0x60, (byte) 0x20, (byte) 0x00, (byte) 0x00,
                        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                        (byte) 0x00, (byte) 0x00, (byte) (i & 0x0ff), (byte) (0x00)
                };

                System.arraycopy(uid, 0, temp, 2, 8);

                // Loopen tot we iets ontvangen van de NfcVsensor
                while (true) {
                    try {
                        block = NfcVsensor.transceive(temp);
                        break;
                    } catch (IOException e) {
                        return null;
                    }
                }
                // 2 bytes opschuiven van de block
                block = Arrays.copyOfRange(block, byteOffset, block.length);

                for (int j = 0; j < 8; j++) {
                    sensorInBytes[i * 8 + j] = block[j];
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                NfcVsensor.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return sensorInBytes;
    }

    // En nu de magic

    @Override
    protected void onPostExecute(byte[] sensorInBytes) {
        super.onPostExecute(sensorInBytes);



        // NFC Bytes naar Hex Blocks en toevoegen aan logs
        myApp.voegToeAanLog("Blocks", bytesNaarHexBlocks(sensorInBytes));

        // Info die we weten gebruiken

        // Byte 26 bevat het nummer van de trent block
        // Trent block is de block die de actuele glucose weet
        // De trent block is de block die ook naar history geschreven zal worden
        String recentBlockStartHex = byteToHex(sensorInBytes[26]);
        myApp.voegToeAanLog("Start Index Recent", recentBlockStartHex + " => " + Integer.parseInt(recentBlockStartHex,16));

        // De index waar history zijn circle start
        String geschiedenisBlockStartHex = byteToHex(sensorInBytes[27]);
        myApp.voegToeAanLog("Start Index Geschiedenis", geschiedenisBlockStartHex + " => " + Integer.parseInt(geschiedenisBlockStartHex,16));

        int leeftijd = Integer.parseInt(bytesToHex(new byte[]{sensorInBytes[317], sensorInBytes[316]}),16);
        // De sensor leeftijd bevind zich in byte 317 en byte 316
        myApp.voegToeAanLog("Leeftijd", Integer.toString(leeftijd));

        // De sensor kan 14 dagen gebruikt worden, we kunnen dus berekenen hoelang de sensor nog gebruikt kan worden
        myApp.voegToeAanLog("14 dagen in min", Integer.toString(14 * 24 * 60));

        // Aantal minuten over
        myApp.voegToeAanLog("Tijd over in min", Integer.toString(14 * 24 * 60 - leeftijd) );

        // Glucose Ophalen
        // Recent blocks
        myApp.voegToeAanLog("Recent Blocks", bytesArrayNaarGlucoseBlocks(krijgRecenteData(sensorInBytes), Integer.parseInt(byteToHex(sensorInBytes[26]),16)));
        // Recent
        myApp.voegToeAanLog("Recent Glucose", bytesArrayNaarGlucoseLijst(krijgRecenteData(sensorInBytes), Integer.parseInt(byteToHex(sensorInBytes[26]),16)));
        // Current
        myApp.setCurrentGlucose(Integer.parseInt(getCurrentGlucoseHex(krijgRecenteData(sensorInBytes),Integer.parseInt(byteToHex(sensorInBytes[26]),16)),16));

        // Glucose Ophalen
        // Geschiedenis blocks
        myApp.voegToeAanLog("Geschiedenis Blocks", bytesArrayNaarGlucoseBlocks(krijgGeschiedenisData(sensorInBytes), Integer.parseInt(byteToHex(sensorInBytes[27]),16)));
        // Geschiedenis
        myApp.voegToeAanLog("History Glucose", bytesArrayNaarGlucoseLijst(krijgGeschiedenisData(sensorInBytes), Integer.parseInt(byteToHex(sensorInBytes[27]),16)));
        // LOG
        myApp.updateUi();


    }

    public ArrayList<String> bytesArrayNaarGlucoseBlocks(byte[] sib, int startIndex){
        ArrayList<String> glucoseBlocks = new ArrayList<>();
        // Glucose blocks zijn 6 bytes lang
        int aantalBlocks = (sib.length / 6)+1;

        for (int i = 0; i < aantalBlocks-1; i++) {
            String special = "";
            if((i+1) == startIndex){
            special = "!";
            }
            String block =  "[Glucose Block " + special + (i+1) + special + " ]: " + byteToHex(sib[i*6+0]) + byteToHex(sib[i*6+1])
                    + byteToHex(sib[i*6+2]) + byteToHex(sib[i*6+3])
                    + byteToHex(sib[i*6+4]) + byteToHex(sib[i*6+5]);
            glucoseBlocks.add(block);
    }
    return glucoseBlocks;
    }

    public String getCurrentGlucoseHex(byte[] sib, int startIndex){
        //Arrays start at 0
        startIndex--;
        return bytesToHex((new byte[]{sib[(startIndex * 6 + 1)], sib[(startIndex * 6 + 0)]}));
    }

    public ArrayList<String> bytesArrayNaarGlucoseLijst(byte[] sib, int startIndex){
        ArrayList<String> glucoseLijst = new ArrayList<>();
        // Glucose blocks zijn 6 bytes lang
        int aantalBlocks = (sib.length / 6)+1;

        for (int index = 0; index < aantalBlocks; index++) {
            int i = startIndex - index - 1;
            if (i < 0) i += aantalBlocks;

            //Hexadecimale glucose
           String glucose =  bytesToHex((new byte[]{sib[(i * 6 + 1)], sib[(i * 6 + 0)]}));
           // Decimale glucose
            int glucoseD = Integer.parseInt(glucose, 16);

           glucoseLijst.add("[" + (i+1) + "]: HEX: " + glucose + " => RAW: " + Integer.toString(glucoseD) + " => " + Double.toString(glucoseD/10));

        }

        return glucoseLijst;
    }

    public byte[] krijgRecenteData(byte [] sib){
        return Arrays.copyOfRange(sib, 28, 123);
    }
    public byte[] krijgGeschiedenisData(byte [] sib){
        return Arrays.copyOfRange(sib, 124, 315);
    }

    public List<String> bytesNaarHexBlocks(byte[] sib){
        List<String> hexBlocks = new ArrayList<>();
        for (int i = 0; i < 360; i += 8) {
            // 8 Bytes per block
            hexBlocks.add("[BLOCK " + Integer.toString(i / 8, 16) + "]: " + byteToHex(sib[i]) + byteToHex(sib[i+1])
                    + byteToHex(sib[i+2]) + byteToHex(sib[i+3])
                    + byteToHex(sib[i+4]) + byteToHex(sib[i+5])
                    + byteToHex(sib[i+6]) + byteToHex(sib[i+7]));
        }
        return hexBlocks;
    }


// https://stackoverflow.com/questions/9655181/how-to-convert-a-byte-array-to-a-hex-string-in-java
    final char[] charArray = "0123456789ABCDEF".toCharArray();
    public String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = charArray[v >>> 4];
            hexChars[j * 2 + 1] = charArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public String byteToHex(byte byte_) {
        char[] hexChars = new char[2];

        int v = byte_ & 0xFF;
        hexChars[0] = charArray[v >>> 4];
        hexChars[1] = charArray[v & 0x0F];

        return new String(hexChars);
    }

}
