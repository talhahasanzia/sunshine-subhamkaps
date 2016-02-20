
package skapsdevelopment.sunshine;


import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import skapsdevelopment.sunshine.R;


public class ForecastFragment extends Fragment {
    private ArrayAdapter<String> adapter;
    public static List<String> weekforecast = new ArrayList<>();
    //ListView listView;
    View v;

    public ForecastFragment() {

    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.forecastfragment, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_refresh) {
            FetchWeatherTask weatherTask = new FetchWeatherTask();
            weatherTask.execute("94043");
            return true;
        }
        return onOptionsItemSelected(item);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        v = inflater.inflate(R.layout.fragment_main, container, false);
        String[] data = {
                "Today-Sunny-78/98",
                "Tomorrow-Foggy-78/66",
                "Wed-Rainy-78/21",
                "Thurs-Sunny-78/96",
                "Fri-Hot-78/23",
                "Sat-Foggy-41/66"
        };
        weekforecast = new ArrayList<>(Arrays.asList(data));
        adapter = new ArrayAdapter<String>(getActivity(), R.layout.list_item_forecast, R.id.list_item_forecast_textview, weekforecast);
        ListView listView = (ListView) v.findViewById(R.id.listView_forecast);
        listView.setAdapter(adapter);
        // adapter.notifyDataSetChanged();
        return v;
    }




    class FetchWeatherTask extends AsyncTask<String, Void, String>
    {



       String getReadableDateString(long time){
            // Because the API returns a unix timestamp (measured in seconds),
            // it must be converted to milliseconds in order to be converted to valid date.
            SimpleDateFormat shortenedDateFormat = new SimpleDateFormat("EEE MMM dd");
            return shortenedDateFormat.format(time);
        }

        /**
         * Prepare the weather high/lows for presentation.
         */
        String formatHighLows(double high, double low) {
            // For presentation, assume the user doesn't care about tenths of a degree.
            long roundedHigh = Math.round(high);
            long roundedLow = Math.round(low);

            String highLowStr = roundedHigh + "/" + roundedLow;
            return highLowStr;
        }



         String[] getWeatherDataFromJson(String forecastJsonStr, int numDays)
                throws JSONException {

            // These are the names of the JSON objects that need to be extracted.
            final String OWM_LIST = "list";
            final String OWM_WEATHER = "weather";
            final String OWM_TEMPERATURE = "temp";
            final String OWM_MAX = "max";
            final String OWM_MIN = "min";
            final String OWM_DESCRIPTION = "main";

            JSONObject forecastJson = new JSONObject(forecastJsonStr);
            JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);



            String[] resultStrs = new String[numDays];
            for(int i = 0; i < weatherArray.length(); i++) {
                // For now, using the format "Day, description, hi/low"
                String day;
                String description;
                String highAndLow;

                // Get the JSON object representing the day
                JSONObject dayForecast = weatherArray.getJSONObject(i);

                // The date/time is returned as a long.  We need to convert that
                // into something human-readable, since most people won't read "1400356800" as
                // "this saturday".
                //create a Gregorian Calendar, which is in current date
                GregorianCalendar gc = new GregorianCalendar();
                //add i dates to current date of calendar
                gc.add(GregorianCalendar.DATE, i);
                //get that date, format it, and "save" it on variable day
                Date time = gc.getTime();
                SimpleDateFormat shortenedDateFormat = new SimpleDateFormat("EEE MMM dd");
                day = shortenedDateFormat.format(time);

                Log.d("date", day);
                // description is in a child array called "weather", which is 1 element long.
                JSONObject weatherObject = dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
                description = weatherObject.getString(OWM_DESCRIPTION);

                // Temperatures are in a child object called "temp".  Try not to name variables
                // "temp" when working with temperature.  It confuses everybody.
                JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
                double high = temperatureObject.getDouble(OWM_MAX);
                double low = temperatureObject.getDouble(OWM_MIN);

                highAndLow = formatHighLows(high, low);
                resultStrs[i] = day + " - " + description + " - " + highAndLow;
            }

            for (String s : resultStrs) {
                Log.v("Results", "Forecast entry: " + s);
            }
            return resultStrs;

        }

        String finalData=null;
        @Override
        protected void onPreExecute() {
            super.onPreExecute();



        }

        @Override
        protected String doInBackground(String... params) {

            try {

                Uri.Builder ub=new Uri.Builder();
                ub.scheme("http").authority("api.openweathermap.org").appendPath("data")
                        .appendPath("2.5")
                        .appendPath("forecast")
                        .appendPath("daily")
                        .appendQueryParameter("q", params[0])
                        .appendQueryParameter("mode", "json")
                        .appendQueryParameter("units","metric")
                        .appendQueryParameter("cnt","7")
                        .appendQueryParameter("APPID","b45c4a4178ceaa8cfbf6a36b2156cf34");

                URL requestUrl = new URL(ub.toString());  // set link
                HttpURLConnection connection = (HttpURLConnection)requestUrl.openConnection(); // create connection
                connection.setRequestMethod("GET");  // set HTTP Method type
                connection.connect();  // connect to server

                int responseCode = connection.getResponseCode(); // get response code from server

                if (responseCode == HttpURLConnection.HTTP_OK) { // check if server says "Ok! i will respond to your request"

                    finalData="";
                    BufferedReader reader = null; //Get buffer ready

                    InputStream inputStream = connection.getInputStream(); // get input stream from server ready

                    if (inputStream == null) { // if there is nothing in stream
                        return "";
                    }
                    reader = new BufferedReader(new InputStreamReader(inputStream));  // else pass stream data to buffer

                    String line;
                    while ((line = reader.readLine()) != null) {  // read each line

                        finalData+="\n"+line; // save them to string
                    }

                    if (finalData.length() == 0) { // check if string is empty
                        return "";
                    }


                }
                else {
                    Log.i("Unsuccessful", "Unsuccessful HTTP Response Code: " + responseCode);
                }
            } catch (MalformedURLException e) {
                Log.e("Wrong URL", "Error processing  API URL", e);
            } catch (IOException e) {
                Log.e("Error", "Error connecting to API", e);
            }

            return finalData;


        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);


            if(s!=null)
            {

                Log.d("Result is",s); // show current data in logcat
                try {
                    String[] data = getWeatherDataFromJson(s, 7);
                    List<String> dataList = new ArrayList<String>(Arrays.asList(data));
                    ListView lv = (ListView) v.findViewById(R.id.listView_forecast);
                    adapter = new ArrayAdapter<String>(getActivity(), R.layout.list_item_forecast, R.id.list_item_forecast_textview,dataList);
                    lv.setAdapter(adapter);






                }
                catch (JSONException j_ex)
                {

                    Log.d("JOSN Exception",j_ex.getMessage().toString());
                }
                catch (Exception ex)
                {

                    Log.d("Unexpexted Error",ex.getMessage().toString());

                }

            }
            else
            {
                Log.d("Result ","is null....") ;


            }
        }
    }

}

        /*@Override
        protected void onPostExecute(Object o) {
            super.onPostExecute(o);
            if(o!=null){
                adapter.clear();
                for (String dayForecastStr : o.toString()){

                }
            }

        }*/

        /*weekforecast=new ArrayList<>(); // set it to new

            for (Collection dayForecastStr: result) {
                weekforecast.addAll(dayForecastStr);

            }
            adapter = new ArrayAdapter<String>(getActivity(), R.layout.list_item_forecast, R.id.list_item_forecast_textview,weekforecast);
            ListView listView = (ListView) v.findViewById(R.id.listView_forecast);
            listView.setAdapter(adapter);*/








