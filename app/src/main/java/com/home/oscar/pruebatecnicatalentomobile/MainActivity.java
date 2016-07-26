package com.home.oscar.pruebatecnicatalentomobile;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    TextView textViewNorte, textViewEste, textViewOeste, textViewSur, textViewTemperatura;
    String[] objetos = new String[7]; //recuperará los valores de Norte, Este, Oeste y Sur, latitud, longitud y temperatura
    String[] coordenadas = new String[4];
    String url1 = "http://api.geonames.org/searchJSON?q=";
    String url1m = "&maxRows=20&startRow=0&lang=en&isNameRequired=true&style=FULL&username=ilgeonamessample";
    String url1complet = "";
    String mensajeSinTemp = "Sin datos para mostrar...";
    JSONObject jsonObjectCoordenadas, jsonObjectTemperaturas;
    ProgressBar progressBarTemperatura;
    AutoCompleteTextView autoCompleteTextView;
    // Google Map
    private GoogleMap googleMap;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        textViewNorte = (TextView) findViewById(R.id.textViewNorte);
        textViewEste = (TextView) findViewById(R.id.textViewEste);
        textViewOeste = (TextView) findViewById(R.id.textViewOeste);
        textViewSur = (TextView) findViewById(R.id.textViewSur);
        textViewTemperatura = (TextView) findViewById(R.id.textViewTemperatura);
        progressBarTemperatura = (ProgressBar) findViewById(R.id.progressBarTemperatura);

        // la autoEditText. Nada más cargar la app, ejecutamos
        // el método initAutoComplete para que muestre todo el
        // historial de búsquedas recientes
        autoCompleteTextView = (AutoCompleteTextView) findViewById(R.id.editTextAuto);
        initAutoComplete("history", autoCompleteTextView);

        // con cada tecla que pulsemos, iremos "rebuscando" las ciudades
        // que sigan coincidiendo con la que vamos escribiendo en el autoedittext
        autoCompleteTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                initAutoComplete("history", autoCompleteTextView);
            }

        });

        // Llamamos al mapa
        try {
            // Cargamos el mapa de Google por defecto
            initilizeMap();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    /**
     * En un fragment cargamos el mapa, si no es posible crearlo con el gestor de fragments
     * se avisará con un simple toast de ello.
     * */
    private void initilizeMap() {
        if (googleMap == null) {
            googleMap = ((MapFragment) getFragmentManager().findFragmentById(
                    R.id.map)).getMap();

            // check if map is created successfully or not
            if (googleMap == null) {
                Toast.makeText(getApplicationContext(),
                        "Upps! No se ha podido crear el mapa.", Toast.LENGTH_SHORT)
                        .show();
                Log.d("ERROR", "Mapa de Google imposible de crear");
            }

        }
    }

    // método de recarga, reinicia el mapa al volver a la app desde segundatarea
    @Override
    protected void onResume() {
        super.onResume();
        initilizeMap();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // método que guarda en las preferencias la ciudad que en ese
    // momento tenemos escrita en el edittext. Es llamada desde el botón "BUSCAR"
    private void saveHistory(String field, AutoCompleteTextView autoCompleteTextView) {

        // cargamos el editor de preferencias, llamamos al campo "nothing"
        // y guardamos la ciudad que tenemos en el edittext en ese momento
        String text = autoCompleteTextView.getText().toString();
        SharedPreferences sp = getSharedPreferences("network_url", 0);
        String longhistory = sp.getString(field, "nothing");
        if (!longhistory.contains(text + ",")) {
            StringBuilder sb = new StringBuilder(longhistory);
            sb.insert(0, text + ",");
            sp.edit().putString("history", sb.toString()).commit();
            Log.d("PREFERENCIAS", "Guardado correctamente la ciudad " + text);
        }

    }

    // método que carga las preferencias, busca la tabla "nothing"
    // y va seleccionando todas las ciudades que tenemos guardadas en él.
    // después las muestra a medida que van coincidiendo.
    // Al cargar la app, muestra TODAS las ciudades guardadaas.
    private void initAutoComplete(String field, AutoCompleteTextView autoCompleteTextView) {
        SharedPreferences sp = getSharedPreferences("network_url", 0);
        String longhistory = sp.getString("history", "nothing");
        String[] histories = longhistory.split(",");
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_dropdown_item_1line, histories);
        // Si el número de ciudades empieza a ser elevado, pasamos
        // los datos a un nuevo array para su mejor tratamiento
        if (histories.length > 50) {
            String[] newHistories = new String[50];
            System.arraycopy(histories, 0, newHistories, 0, 50);
            adapter = new ArrayAdapter<String>(this,
                    android.R.layout.simple_dropdown_item_1line, newHistories);
        }
        // método OnFocus, para ir depurando la lista a coincidir de ciudades.
        autoCompleteTextView.setAdapter(adapter);
        autoCompleteTextView
                .setOnFocusChangeListener(new View.OnFocusChangeListener() {
                    @Override
                    public void onFocusChange(View v, boolean hasFocus) {
                        AutoCompleteTextView view = (AutoCompleteTextView) v;
                        if (hasFocus) {
                            view.showDropDown();
                        }
                    }
                });
    }


    // método que se ejecuta al pulsar el botón "BUSCAR"
    public void findIt(View view){

        // guardamos la ciudad en las preferencias para tenerla a mano
        // en el futuro
        saveHistory("history", autoCompleteTextView);

        progressBarTemperatura.setVisibility(view.VISIBLE);

        url1complet = url1 + autoCompleteTextView.getText().toString().replace(' ', '+') + url1m; // montamos la URL del JSon
        // eliminando los posibles espacios en blanco y colocando en su lugar "+" para que el JSon funcione
        // y creamos una AsyncTask para llamar a los 2 JSon (coordenadas y temeratura
        new AsyncTaskLocation().execute(url1complet, autoCompleteTextView.getText().toString());
    }

    // método asynctask que ejecutará las 2 llamadas de JSon y mostrará los resultados en pantalla
    public class AsyncTaskLocation extends AsyncTask<String, String, String[]> {

        @Override
        protected void onPreExecute() {
            Log.d("ASYNCTASK", "Asynctask creada correctamente, Genius at work...");
        }

        @Override
        protected String[] doInBackground(String... url) {

            int index, i;
            Double temp = 0.0;
            String url2 = "http://api.geonames.org/weatherJSON?north=";

            try {
                // primero buscaremos las coordenadas que resulten del primer JSon
                // gracias a la clase JsonParser que directamente hace todo el trabajo sucio.
                // le pasamos la URL y nos devuelve el JSON en un JSONObject.
                jsonObjectCoordenadas = JsonParser.readJsonFromUrl(url[0]); // leemos la URL completa que le hemos pasado - posición 1 de los parámetros pasados
                Log.d("Llamada a JSON 1", "Nos devuelve el JSon " + jsonObjectCoordenadas);
                JSONObject jsonObjectCiudad;

                // si el JSon devuelto contiene datos, los guardamos
                if (!jsonObjectCoordenadas.getString("totalResultsCount").equalsIgnoreCase("0")) {
                    // recorremos esos datos y nos quedamos con el primero que coincida con el nombre
                    // de la ciudad que hemos introducido
                    for (int j = 0; j < jsonObjectCoordenadas.getJSONArray("geonames").length(); j++) {

                        jsonObjectCiudad = jsonObjectCoordenadas.getJSONArray("geonames").getJSONObject(j);

                        if (jsonObjectCiudad.getString("asciiName").equalsIgnoreCase(url[1])) {

                            // guardamos sus coordenadas
                            objetos[0] = jsonObjectCiudad.getJSONObject("bbox").getString("north");
                            objetos[1] = jsonObjectCiudad.getJSONObject("bbox").getString("east");
                            objetos[2] = jsonObjectCiudad.getJSONObject("bbox").getString("west");
                            objetos[3] = jsonObjectCiudad.getJSONObject("bbox").getString("south");
                            objetos[4] = jsonObjectCiudad.getString("lat");
                            objetos[5] = jsonObjectCiudad.getString("lng");
                            j = jsonObjectCoordenadas.getJSONArray("geonames").length() + 1; // una vez encontrado, salimos del bucle FOR (forma fea, lo sé...)

                        }
                    }
                }else{
                    // si no hubo resultados con la ciudad buscada, rellenamos con ...
                    objetos[0] = "...";
                    objetos[1] = "...";
                    objetos[2] = "...";
                    objetos[3] = "...";
                    objetos[4] = "...";
                    objetos[5] = "...";
                }

                // preparamos el segundo JSon a partir de los datos que obtuvimos en el primero
                // cogemos las coordenadas norte, sur, este y oeste. Nos limitamos a la parte entero
                // y a un solo decimal, no necesitamos más para el segundo JSon
                index = objetos[0].indexOf(".");
                coordenadas[0] = objetos[0].substring(0, index + 2);

                index = objetos[1].indexOf(".");
                coordenadas[1] = objetos[1].substring(0, index + 2);

                index = objetos[2].indexOf(".");
                coordenadas[2] = objetos[2].substring(0, index + 2);

                index = objetos[3].indexOf(".");
                coordenadas[3] = objetos[3].substring(0, index + 2);

            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }

            try {
                // a traves de las coordenadas, preparamos la segunda URL para el segundo JSON, el de la temperatura
                url2 = "http://api.geonames.org/weatherJSON?north=" + coordenadas[0] + "&south=" + coordenadas[3] + "&east=" + coordenadas[1] + "&west=" + coordenadas[2] + "&username=ilgeonamessample";
                jsonObjectTemperaturas = JsonParser.readJsonFromUrl(url2); // leemos la URL completa que le hemos pasado - posición 1 de los parámetros pasados
                Log.d("Llamada a JSON 2", "Nos devuelve el JSon " + jsonObjectTemperaturas);

                JSONObject jsonObjectStation;

                // al igual que antes, miramos si devuelte "algo" y no un simple JSon vacío.
            if (jsonObjectTemperaturas.getJSONArray("weatherObservations").length() != 0) {
                // de ser así, vamos acumulando en la variable "temp" todas las temperaturas del array,
                // previamente pasadas a DOUBLE. Miraremos que no sea un campo vacío.
                for (i = 0; i < jsonObjectTemperaturas.getJSONArray("weatherObservations").length(); i++) {

                    jsonObjectStation = jsonObjectTemperaturas.getJSONArray("weatherObservations").getJSONObject(i);
                    // miraremos que no sea un campo vacío
                    if (!"".equalsIgnoreCase(jsonObjectStation.getString("temperature"))) {
                        temp += Double.parseDouble(jsonObjectStation.getString("temperature"));
                    }
                }
                // guardamos la temperatura media
                objetos[6] = String.valueOf(temp / i);
            // si no hubo datos en el JSON, colocaremos un mensaje advirtiendo de ello
                // "SIN DATOS DE TEMPERATURA"
            }else{
                objetos[6] = mensajeSinTemp;
            }
            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }

            return objetos;
        }

        @Override
        protected void onPostExecute(String[] stringFromDoInBackground) {

            // si hubo datos en el primer JSON, los colocamos en las etiquetas correspondientes
            if (!stringFromDoInBackground[0].equalsIgnoreCase("...")) {

                textViewNorte.setText("norte: " + stringFromDoInBackground[0]);
                textViewEste.setText("este: " + stringFromDoInBackground[1]);
                textViewOeste.setText("oeste: " + stringFromDoInBackground[2]);
                textViewSur.setText("sur: " + stringFromDoInBackground[3]);
                textViewTemperatura.setText(stringFromDoInBackground[6]);
            // y rellenamos la progressBar con la temperatura obtenida
                rellenarBarraAnimada(stringFromDoInBackground[6]);
            // y con la latitud y longitud obtenida, colocamos una marca en el mapa
                // y nos "movemos" hasta dicha marca
                Log.d("Mapa de Google", "Mostrando nueva coordenada en valor LAT-LNG");

                initilizeMap();
                double latitude = Double.parseDouble(stringFromDoInBackground[4]);
                double longitude = Double.parseDouble(stringFromDoInBackground[5]);

            // creamos la "posición" a marcar en el mapa
                MarkerOptions marker = new MarkerOptions().position(new LatLng(latitude, longitude)).title("Mapa");

                // Colocamos un marcador
                // de color ROSA, por poner uno
                marker.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ROSE));

            // y dibujamos el marcador con la nueva posición en el mapa
                googleMap.addMarker(marker);
                // y nos movemos a la nueva posición marcada
                CameraPosition cameraPosition = new CameraPosition.Builder().target(
                        new LatLng(latitude, longitude)).zoom(6).build();

                googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

            }else{
                // si la ciudad no existe, o el JSON no vino con las coordenadas de la ciudad, lo avisamos
                // y limpiamos los campos de las coordenadas
                Toast.makeText(getApplicationContext(), "CIUDAD NO ENCONTRADA", Toast.LENGTH_LONG).show();
                limpiarCampos();
            }


        }
    }

    // simple método para limpiar la pantalla de los datos obsoletos
    public void limpiarCampos(){

        textViewNorte.setText("norte: ");
        textViewEste.setText("este: ");
        textViewOeste.setText("oeste: ");
        textViewSur.setText("sur: ");
        textViewTemperatura.setText("Temperatura:");
        // colocamos la progressBar a CERO y el color por defecto, GRIS
        progressBarTemperatura.setProgress(0);
        progressBarTemperatura.getProgressDrawable().setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_IN);


    }

    // método que cambia el valor de la ProgressBar según la temperatura que se le pase
    // y el color de la misma
    public void rellenarBarraAnimada(String valor){

        int valorInt;
        // Si no hubo temperatura que mostrar, dejaremos la progressBar a cero
        // aunque en el mensaje no pondremos CERO, sino "SIN DATOS"
        if (valor.equalsIgnoreCase(mensajeSinTemp)) {
            valor = "0";
           }
        // colocamos el valor de la temperatura en la progressBar
        valorInt = (int) Double.parseDouble(valor);

        progressBarTemperatura.setProgress(valorInt);
        // y en función de la temperatura, cambiaremos el color de la progressBAR
        if (valorInt < 10){
            progressBarTemperatura.getProgressDrawable().setColorFilter(Color.GRAY, PorterDuff.Mode.SRC_IN);
        }
        else if (valorInt >= 10 && valorInt < 15){
            progressBarTemperatura.getProgressDrawable().setColorFilter(Color.BLUE, PorterDuff.Mode.SRC_IN);
        }
        else if (valorInt >= 15 && valorInt < 20){
            progressBarTemperatura.getProgressDrawable().setColorFilter(Color.GREEN, PorterDuff.Mode.SRC_IN);
        }
        else{
            progressBarTemperatura.getProgressDrawable().setColorFilter(Color.RED, PorterDuff.Mode.SRC_IN);
        }

    }

}
