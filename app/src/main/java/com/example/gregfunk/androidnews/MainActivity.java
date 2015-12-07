package com.example.gregfunk.androidnews;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {

    Map<Integer, String> articleURLs = new HashMap<Integer, String>();
    Map<Integer, String> articleTitles = new HashMap<Integer, String>();
    ArrayList<Integer> articleIds = new ArrayList<Integer>();

    SQLiteDatabase articlesDB;

    ListView listView;
    ArrayList<String> titles = new ArrayList<String>();
    ArrayList<String> urls = new ArrayList<String>();
    ArrayAdapter arrayAdapter;

    public void updateListView() {
        try {
            Log.i("UI UPDATED", "done");
            Cursor c = articlesDB.rawQuery("SELECT * FROM articles ORDER BY articleID DESC", null);
            int articleIDIndex = c.getColumnIndex("articleID");
            int urlIndex = c.getColumnIndex("url");
            int titleIndex = c.getColumnIndex("title");
            c.moveToFirst();
            titles.clear();
            urls.clear();
            do {
                //Log.i("articleID", Integer.toString(c.getInt(articleIDIndex)));
                //Log.i("articleUrl", c.getString(urlIndex));
                //Log.i("articleTitle", c.getString(titleIndex));

                titles.add(c.getString(titleIndex));
                urls.add(c.getString(urlIndex));
            } while (c.moveToNext());

            arrayAdapter.notifyDataSetChanged();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        listView = (ListView) findViewById(R.id.listView);
        arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, titles);
        listView.setAdapter(arrayAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent i = new Intent(getApplicationContext(), ArticleActivity.class);
                i.putExtra("articleURL", urls.get(position));
                startActivity(i);
            }
        });

        articlesDB = this.openOrCreateDatabase("Articles", MODE_PRIVATE, null);
        articlesDB.execSQL("CREATE TABLE IF NOT EXISTS articles (id INTEGER PRIMARY KEY, articleID, INTEGER, url VARCHAR, title VARCHAR, content VARCHAR)");

        updateListView();



        DownloadTask task = new DownloadTask();

        try {
            task.execute("https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public class DownloadTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... urls) {
            String result = "";
            URL url;
            HttpURLConnection urlConnection = null;

            try {
                url = new URL(urls[0]);
                urlConnection = (HttpURLConnection) url.openConnection();
                InputStream in = urlConnection.getInputStream();
                InputStreamReader reader = new InputStreamReader(in);
                BufferedReader r = new BufferedReader(reader);
                StringBuilder total = new StringBuilder();
                String line;
                while ((line = r.readLine()) != null) {
                    total.append(line);
                }

                articlesDB.execSQL("DELETE FROM articles");

                JSONArray jsonArray = new JSONArray(result);
                for (int i=0; i < 20; i++) {
                    //Log.i("ArticleID", jsonArray.getString(i));

                    String articleID = jsonArray.getString(i);

                    url = new URL("https://hacker-news.firebaseio.com/v0/item/" + jsonArray.getString(i) + ".json?print=pretty");

                    urlConnection = (HttpURLConnection) url.openConnection();
                    in = urlConnection.getInputStream();
                    reader = new InputStreamReader(in);
                    r = new BufferedReader(reader);
                    total = new StringBuilder();
                    while ((line = r.readLine()) != null) {
                        total.append(line);
                    }
                    String articleInfo = total.toString();

                    //Log.i("articleInfo", articleInfo);

                    JSONObject jsonObject = new JSONObject(articleInfo);
                    if (jsonObject.getString("type").equals("story")) {
                        String articleTitle = jsonObject.getString("title");
                        String articleURL = jsonObject.getString("url");

                        //Log.i("articleTitle", articleTitle);
                        //Log.i("articleURL", articleURL);

                        articleIds.add(Integer.valueOf(articleID));
                        articleTitles.put(Integer.valueOf(articleID), articleTitle);
                        articleURLs.put(Integer.valueOf(articleID), articleURL);

                        String sql = "INSERT INTO articles (articleID, url, title) VALUES (?, ?, ?)";
                        SQLiteStatement statement = articlesDB.compileStatement(sql);
                        statement.bindString(1, articleID);
                        statement.bindString(2, articleURL);
                        statement.bindString(3, articleTitle);
                        statement.execute();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return result;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            updateListView();
        }
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

        return super.onOptionsItemSelected(item);
    }
}
