package com.example.gregfunk.androidnews;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        articlesDB = this.openOrCreateDatabase("Articles", MODE_PRIVATE, null);
        articlesDB.execSQL("CREATE TABLE IF NOT EXISTS articles (id INTEGER PRIMARY KEY, articleID, INTEGER, url VARCHAR, title VARCHAR, content VARCHAR)");

        DownloadTask task = new DownloadTask();
        String result = null;
        try {
            result = task.execute("https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty").get();

            //Log.i("result", result);

            articlesDB.execSQL("DELETE FROM articles");

            JSONArray jsonArray = new JSONArray(result);
            for (int i=0; i < 20; i++) {
                //Log.i("ArticleID", jsonArray.getString(i));

                String articleID = jsonArray.getString(i);

                DownloadTask getArticle = new DownloadTask();
                String articleInfo = getArticle.execute("https://hacker-news.firebaseio.com/v0/item/" + jsonArray.getString(i) + ".json?print=pretty").get();

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

            Cursor c = articlesDB.rawQuery("SELECT * FROM articles", null);
            int articleIDIndex = c.getColumnIndex("articleID");
            int urlIndex = c.getColumnIndex("url");
            int titleIndex = c.getColumnIndex("title");
            c.moveToFirst();
            do {
                Log.i("articleID", Integer.toString(c.getInt(articleIDIndex)));
                Log.i("articleUrl", c.getString(urlIndex));
                Log.i("articleTitle", c.getString(titleIndex));
            } while (c.moveToNext());
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
                return total.toString();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return result;
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

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
