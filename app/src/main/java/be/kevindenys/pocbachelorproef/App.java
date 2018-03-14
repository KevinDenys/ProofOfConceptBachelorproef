package be.kevindenys.pocbachelorproef;

import android.app.Application;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Kevin on 7/03/2018.
 */

public class App extends Application {
    private byte[] sensorInBytes;
    private List<String> mijnLogs = new ArrayList<>();
    private TextView logView;
    private TextView currentView;
    private int currentGlucose = 0;
    @Override
    public void onCreate() {
        super.onCreate();

    }

    public int getCurrentGlucose() {
        return currentGlucose;
    }

    public void setCurrentGlucose(int currentGlucose) {
        this.currentGlucose = currentGlucose;
    }

    public TextView getCurrentView() {
        return currentView;
    }

    public void setCurrentView(TextView currentView) {
        this.currentView = currentView;
    }

    public void voegToeAanLog(String key, String value){
        mijnLogs.add("[" + key + "]: " + value);
    }

    public void voegToeAanLog(String key, List<String> strings){
        voegToeAanLog(key, "********************");
        mijnLogs.addAll(strings);
        voegToeAanLog("End", "********************");
    }

    public byte[] getSensorInBytes() {
        return sensorInBytes;
    }

    public void setSensorInBytes(byte[] sensorInBytes) {
        this.sensorInBytes = sensorInBytes;
    }

    public List<String> getMijnLogs() {
        return mijnLogs;
    }

    public TextView getLogView() {
        return logView;
    }

    public void setLogView(TextView logView) {
        this.logView = logView;
    }

    public void updateUi(){
        String x = "";
        for(int i=0; i<mijnLogs.size(); i++){
            x += mijnLogs.get(i) + "\n";
        }
        logView.setText(x);
        currentView.setText("Glucose: " + currentGlucose);
    }

    public void resetView(){
        mijnLogs = new ArrayList<>();
        logView.setText("Proof of concept: Kevin Denys");
    }

    public void copyDebug() {
        String x = "";
        for(int i=0; i<mijnLogs.size(); i++){
            x += mijnLogs.get(i) + "\n";
        }
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("label", x);
        clipboard.setPrimaryClip(clip);
    }
}
