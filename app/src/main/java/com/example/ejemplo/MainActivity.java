package com.example.ejemplo;

import android.os.PatternMatcher;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;
import android.net.Uri;

import java.nio.charset.Charset;

public class MainActivity extends AppCompatActivity {

    private boolean activateWriting =false;
    private NfcAdapter adapterNFC;
    private Tag tag;
    private PendingIntent pendingIntent;
    public static final String DEFAULT_URI = "http://localhost";
    private Uri uridefault;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        adapterNFC = NfcAdapter.getDefaultAdapter(this);
        if(adapterNFC ==null)
            return;
        pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),0);
        changeAboutVisibility();
        addListenerOnButton();
        uridefault = Uri.parse(DEFAULT_URI);
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter[] filters = new IntentFilter[1];
        filters[0] = new IntentFilter();
        filters[0].addAction(NfcAdapter.ACTION_TECH_DISCOVERED);
        filters[0].addCategory(Intent.CATEGORY_DEFAULT);
        filters[0].addDataScheme("http");
        filters[0].addDataAuthority("localhost", null);
        filters[0].addDataPath("", PatternMatcher.PATTERN_PREFIX);
        String[][] techList = new String[1][1];
        techList[0][0] = "android.nfc.tech.NfcA";
        adapterNFC.enableForegroundDispatch(this, pendingIntent, filters, techList);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (adapterNFC != null) {
            adapterNFC.disableForegroundDispatch(this);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if(activateWriting)
            outState.putByte("actWrt",(byte) 1);
        else
            outState.putByte("actWrt",(byte) 0);

    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        activateWriting = savedInstanceState.getByte("actWrt") != 0;
    }

    void addListenerOnButton() {
        final Button button = findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activateWriting = !activateWriting;
                changeAboutVisibility();
            }
        });
    }

    void changeAboutVisibility() {
        final TextView aboutTag = findViewById(R.id.textViewAbout);
        aboutTag.setVisibility(activateWriting ? View.VISIBLE : View.INVISIBLE);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        Log.d("myNFCtag", intent.getAction());
        try {
            read(intent);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (FormatException e) {
            e.printStackTrace();
        }
        if (activateWriting) getTagInfo(intent);
    }

    private void getTagInfo(Intent intent) {
        Uri uri;
        Uri [] uriList = new Uri[1];
        final EditText uriBox = findViewById(R.id.fieldURI);
        CharSequence uriText = uriBox.getText();
        uri = Uri.parse(uriText.toString());
        uriList[0] = uri;
        tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        Log.i("NFC","Detectada");
        try {
            write(uriList,tag);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (FormatException e) {
            e.printStackTrace();
        }
    }

    private void read(Intent intent) throws IOException, FormatException {
        if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(intent.getAction())) {
            tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            Ndef ndef = Ndef.get(tag);
            ndef.connect();
            NdefMessage ndefMessage = ndef.getNdefMessage();
            if (ndefMessage.getRecords() != null && ndefMessage.getRecords().length > 0) {
                Uri uri = ndefMessage.getRecords()[0].toUri();
                final EditText outputLectura = findViewById(R.id.tagContent);
                outputLectura.setText(uri.toString());
            }
            ndef.close();
        }
    }

    private void write(Uri uris[], Tag tag) throws IOException, FormatException {
        NdefRecord record;
        NdefRecord[] ndefRecords = new NdefRecord[uris.length];
        int i = 0;
        for(Uri uri : uris){
            record = buildRTDURIRecord(uri);
            ndefRecords[i++] = record;
        }
        NdefMessage message = new NdefMessage(ndefRecords);
        Ndef ndef = Ndef.get(tag);
        ndef.connect();
        ndef.writeNdefMessage(message);
        ndef.close();
    }

    private NdefRecord buildRTDURIRecord(Uri uri){
        return NdefRecord.createUri(uri);
    }

    private NdefRecord buildURIRecord(Uri uri){
        String strUri = uri.toString();
        return new NdefRecord(NdefRecord.TNF_ABSOLUTE_URI, strUri.getBytes(Charset.forName("US-ASCII")), new byte[0], new byte[0]);
    }


}