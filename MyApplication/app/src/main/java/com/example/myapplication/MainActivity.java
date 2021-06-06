package com.example.myapplication;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONObject;

import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

import javax.net.SocketFactory;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;


public class MainActivity extends AppCompatActivity {

    // Setup Server information
    protected static String serverIp = "192.168.1.82";
    protected static int serverPort = 465;

    private String okMessage = "Petición enviada correctamente";
    private String errorMessage = "Ha ocurrido un problema";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Capturamos el boton de Enviar
        View button = findViewById(R.id.button_send);

        // Llama al listener del boton Enviar
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDialog();
            }
        });
    }

    // Creación de un cuadro de dialogo para confirmar pedido
    private void showDialog() throws Resources.NotFoundException {
        final CheckBox sabanas = (CheckBox) findViewById(R.id.checkBox_sabanas);
        final CheckBox camas = (CheckBox) findViewById(R.id.checkBox_camas);
        final CheckBox mesas = (CheckBox) findViewById(R.id.checkBox_mesas);
        final CheckBox sillas = (CheckBox) findViewById(R.id.checkBox_sillas);
        final TextInputEditText textInputClientNumber = (TextInputEditText) findViewById(R.id.textInputEditText);

        final String clientNumber = textInputClientNumber.getText().toString();

        if (!sabanas.isChecked() && !camas.isChecked() && !mesas.isChecked() && !sillas.isChecked()) {
            Toast.makeText(getApplicationContext(), "Selecciona al menos un elemento", Toast.LENGTH_SHORT).show();
        } else if(clientNumber.isEmpty()){
            Toast.makeText(getApplicationContext(), "Indique el número de cliente", Toast.LENGTH_SHORT).show();
        } else {
            new AlertDialog.Builder(this)
                    .setTitle("Enviar")
                    .setMessage("Se va a proceder al envio")
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                                // Catch ok button and send information
                                public void onClick(DialogInterface dialog, int whichButton) {

                                    try {
                                        ////////////////////////////////////////////////////////////////////////
                                        // 1. Extraer los datos de la vista
                                        ////////////////////////////////////////////////////////////////////////
                                        List<String> lstSelected = new ArrayList<>();

                                        if( sabanas.isChecked()){ lstSelected.add("sabanas"); }
                                        if( camas.isChecked()){ lstSelected.add("camas"); }
                                        if( mesas.isChecked()){ lstSelected.add("mesas"); }
                                        if( sillas.isChecked()){ lstSelected.add("sillas"); }

                                        JSONObject messageJson = new JSONObject();
                                        messageJson.put("clientNumber", clientNumber);
                                        messageJson.put("selected", lstSelected);
                                        String message = messageJson.toString();


                                        ////////////////////////////////////////////////////////////////////////
                                        // 2. Firmar los datos
                                        ////////////////////////////////////////////////////////////////////////


                                        ////////////////////////////////////////////////////////////////////////
                                        // 3. Enviar los datos
                                        ////////////////////////////////////////////////////////////////////////

                                        // SocketFactory socketFactory = (SocketFactory) SocketFactory.getDefault();
                                        // Socket socket = (Socket) socketFactory.createSocket(serverIp,serverPort);

                                        SSLSocketFactory socketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
                                        SSLSocket socket = (SSLSocket) socketFactory.createSocket(serverIp, serverPort);

                                        String key = "108079546209274483481442683641105470668825844172663843934775892731209928221929";


                                        SecureRandom secureRandom = SecureRandom.getInstance("SHA1PRNG");
                                        Integer intNonce = secureRandom.nextInt();
                                        String nonce =  intNonce.toString();
                                        String hmac = generateHmac(key,message,nonce);

                                        JSONObject dataJson = new JSONObject();
                                        dataJson.put("message", messageJson);
                                        dataJson.put("nonce", nonce);
                                        dataJson.put("hmac", hmac);

                                        String data = dataJson.toString();

                                        Toast.makeText(MainActivity.this, okMessage, Toast.LENGTH_SHORT).show();

                                    // Error -> Show an Error Message
                                    } catch (Exception e) {
                                        // e.printStackTrace();
                                        Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
                                    }
                                }
                            }
                    )
                    .setNegativeButton(android.R.string.no, null)
                    .show();
        }
    }

    private String generateHmac(String key, String message, String nonce) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        String body = message + nonce;

        @SuppressLint({"NewApi", "LocalSuppress"})
        byte[] encodedHash = digest.digest(
                body.getBytes(StandardCharsets.UTF_8));

        return bytesToHex(encodedHash);
    }

    // from: https://www.baeldung.com/sha-256-hashing-java
    private static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (int i = 0; i < hash.length; i++) {
            String hex = Integer.toHexString(0xff & hash[i]);
            if(hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}