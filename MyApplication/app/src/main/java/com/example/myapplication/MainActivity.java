package com.example.myapplication;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.net.SocketFactory;

public class MainActivity extends AppCompatActivity {

    // Setup Server information
    protected static String serverIp = "10.0.2.2";
    protected static int serverPort = 465;

    private String okMessage = "Petición enviada correctamente";
    private String errorMessage = "Ha ocurrido un problema";
    private String key = "108079546209274483481442683641105470668825844172663843934775892731209928221929";

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

        final EditText camas = (EditText) findViewById(R.id.textNumberCamas);
        final EditText mesas = (EditText) findViewById(R.id.textNumberMesas);
        final EditText sillas = (EditText) findViewById(R.id.textNumberSillas);
        final EditText sillones = (EditText) findViewById(R.id.textNumberSillones);
        final EditText cliente = (EditText) findViewById(R.id.textNumerCliente);

        final String textCamas = camas.getText().toString();
        final String textMesas = mesas.getText().toString();
        final String textSillas = sillas.getText().toString();
        final String textSillones = sillones.getText().toString();

        final Integer numberCamas = Integer.parseInt(textCamas.isEmpty()?"0":textCamas);
        final Integer numberMesas = Integer.parseInt(textMesas.isEmpty()?"0":textMesas);
        final Integer numberSillas = Integer.parseInt(textSillas.isEmpty()?"0":textSillas);
        final Integer numberSillones = Integer.parseInt(textSillones.isEmpty()?"0":textSillones);
        final String numberCliente = cliente.getText().toString();

        if(numberCliente.isEmpty()) {
            Toast.makeText(getApplicationContext(), "Indique el número de cliente", Toast.LENGTH_SHORT).show();
        } else if( !(validate(numberCamas) && validate(numberMesas) && validate(numberSillas) && validate(numberSillones)) ) {
            Toast.makeText(getApplicationContext(), "Seleccione cantidad entre 0 y 300", Toast.LENGTH_LONG).show();
        }else if( (numberCamas+numberMesas+numberSillas+numberSillones)<1 ){
            Toast.makeText(getApplicationContext(), "Solicite al menos un elemento", Toast.LENGTH_LONG).show();
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

                                        final JSONObject messageJson = new JSONObject();
                                        messageJson.put("camas", numberCamas);
                                        messageJson.put("mesas", numberMesas);
                                        messageJson.put("sillas", numberSillas);
                                        messageJson.put("sillones", numberCliente);
                                        messageJson.put("clientNumber", numberCliente);
                                        final String message = messageJson.toString();


                                        ////////////////////////////////////////////////////////////////////////
                                        // 2. Firmar los datos
                                        ////////////////////////////////////////////////////////////////////////


                                        ////////////////////////////////////////////////////////////////////////
                                        // 3. Enviar los datos
                                        ////////////////////////////////////////////////////////////////////////

                                        AsyncTask.execute(new Runnable() {
                                            @Override
                                            public void run() {

                                                try {

                                                    // PAI 2 -> Without SSL
                                                    SocketFactory socketFactory = (SocketFactory) SocketFactory.getDefault();
                                                    Socket socket = (Socket) socketFactory.createSocket(serverIp,serverPort);

                                                    // PAI 3 -> With SSL
                                                    // SSLSocketFactory socketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
                                                    // SSLSocket socket = (SSLSocket) socketFactory.createSocket(serverIp, serverPort);

                                                    // NONCE FORM 1 -> random
                                                    // SecureRandom secureRandom = SecureRandom.getInstance("SHA1PRNG");
                                                    // Integer intNonce = secureRandom.nextInt();
                                                    // String nonce =  intNonce.toString();

                                                    // NONCE FORM 2 -> alphanumeric
                                                    String nonce = generateNonce(16);

                                                    String hmac = generateHmac(key,message,nonce);

                                                    JSONObject dataJson = new JSONObject();
                                                    dataJson.put("message", messageJson);
                                                    dataJson.put("nonce", nonce);
                                                    dataJson.put("hmac", hmac);

                                                    String data = dataJson.toString();

                                                    PrintWriter output = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
                                                    output.println(data);
                                                    output.flush();

                                                    BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                                                    String response = input.readLine();
                                                    if(response!=null && !response.isEmpty() && response.contains("OK")) {
                                                        Toast.makeText(MainActivity.this, okMessage, Toast.LENGTH_SHORT).show();
                                                    } else {
                                                        showErrorView();
                                                    }

                                                    // close connection
                                                    output.close();

                                                // Error -> Show an Error Message
                                                } catch (Exception e) {
                                                    // e.printStackTrace();
                                                    showErrorView();
                                                }
                                            }
                                        });

                                    // Error -> Show an Error Message
                                    } catch (Exception e) {
                                        // e.printStackTrace();
                                        showErrorView();
                                    }
                                }
                            }
                    )
                    .setNegativeButton(android.R.string.no, null)
                    .show();
        }
    }

    private Boolean validate(Integer number) {
        return number!=null && number>=0 && number<=300;
    }

    private void showErrorView(){
        Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
    }

    private String generateHmac(String key, String message, String nonce) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        String body = message + nonce;

        @SuppressLint({"NewApi", "LocalSuppress"})
        byte[] encodedHash = digest.digest(
                body.getBytes(StandardCharsets.UTF_8));

        return bytesToHex(encodedHash);
    }

    // from: https://www.geeksforgeeks.org/generate-random-string-of-given-size-in-java/
    private String generateNonce(Integer size) {
        // chose a Character random from this String
        String AlphaNumericString = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
                + "0123456789"
                + "abcdefghijklmnopqrstuvxyz";

        // create StringBuffer size of AlphaNumericString
        StringBuilder sb = new StringBuilder(size);

        for (int i = 0; i < size; i++) {

            // generate a random number between
            // 0 to AlphaNumericString variable length
            int index
                    = (int)(AlphaNumericString.length()
                    * Math.random());

            // add Character one by one in end of sb
            sb.append(AlphaNumericString
                    .charAt(index));
        }

        return sb.toString();
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