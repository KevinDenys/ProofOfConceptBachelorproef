package be.kevindenys.pocbachelorproef.Activities;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.NfcV;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import be.kevindenys.pocbachelorproef.App;
import be.kevindenys.pocbachelorproef.R;
import be.kevindenys.pocbachelorproef.Tasks.NfcLeesTaak;

public class MainActivity extends AppCompatActivity {

    private PendingIntent pendingIntent;
    private String[][] techList;
    private NfcAdapter nfcAdapter;
    private IntentFilter[] intentFilter;
    private App myApp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        myApp = (App) getApplication();
        // NFC
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        // Checken of er NFC beschikbaar is
        if (nfcAdapter != null && nfcAdapter.isEnabled()) {
            myApp.voegToeAanLog("NFC", "Toestel heeft NFC");
        } else {
            myApp.voegToeAanLog("NFC", "Toestel heeft geen NFC");
        }
        // Omdat NFC maar 1 maal gestart mag worden er voor zorgen dat deze intent maar 1 keer opgestart worden aan de hand van de FLAG_ACTIVITY_SINGLE_TOP flag$
        // https://developer.android.com/reference/android/content/Intent.html#FLAG_ACTIVITY_SINGLE_TOP
        pendingIntent = PendingIntent.getActivity(
                this,
                0,
                new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                0
        );
        // Filteren om te weten te komen wanneer er een NDEF tag beschikbaar is
        IntentFilter ndefIntent = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
        try {
            // Elke datatype accepteren
            ndefIntent.addDataType("*/*");
            intentFilter = new IntentFilter[]{ndefIntent};
        } catch (Exception e) {
            myApp.voegToeAanLog("Error", e.toString());
        }

        // Via NFC tag dump van de sensor weten we dat NFCV gebruikt moet worden
        techList = new String[][]{new String[]{NfcV.class.getName()}};
        //Text
        myApp.setCurrentView((TextView) findViewById(R.id.txtGlucose));
        myApp.setLogView((TextView) findViewById(R.id.textLog));
        //Buttons
       Button btnReset = (Button) findViewById(R.id.btnReset);
       btnReset.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View view) {
                myApp.resetView();
           }
       });
        Button btnCopy = (Button) findViewById(R.id.btnCopy);
        btnCopy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                myApp.copyDebug();
            }
        });
    }

    // NFC tutorial: https://www.youtube.com/watch?v=bbeS7FPjRNk

    @Override
    public void onNewIntent(Intent intent) {
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        new NfcLeesTaak(this, myApp).execute(tag);
    }

    @Override
    protected void onResume() {
        super.onResume();
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, intentFilter, techList);
    }

    @Override
    protected void onPause() {
        super.onPause();
        nfcAdapter.disableForegroundDispatch(this);
    }
}
