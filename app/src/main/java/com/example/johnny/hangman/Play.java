package com.example.johnny.hangman;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.easyandroidanimations.library.Animation;
import com.easyandroidanimations.library.AnimationListener;
import com.easyandroidanimations.library.ExplodeAnimation;

import static com.example.johnny.hangman.RequestMethod.POST;

/**
 * Created by Johnny on 23/10/17.
 */

public class Play extends AppCompatActivity implements View.OnClickListener {

    public static Galgelogik spil = new Galgelogik();

    Button guessButton;

    TextView wordView, letters, life, score;

    ImageView image;

    EditText guessLetter;

    public static double totalTime;

    long tStart, tEnd;

    static int totalGames, won, lost;

    static String totalGamesStr, wonStr, lostStr, visiblewWord;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.play);

        tStart = SystemClock.elapsedRealtime();
        spil.setScore(0);
        spil.setStreak(0);

        guessLetter = (EditText) findViewById(R.id.guessLetter);

        guessButton = (Button) findViewById(R.id.guessButton);

        image = (ImageView) findViewById(R.id.image);

        letters = (TextView) findViewById(R.id.letters);
        wordView = (TextView) findViewById(R.id.word);
        life = (TextView) findViewById(R.id.lives);
        score = (TextView) findViewById(R.id.score);

        guessButton.setOnClickListener(this);

        letters.setText("Used letters: " + spil.getBrugteBogstaver());
        life.setText("Lives: " + spil.getAntalLiv());
        score.setText("Score: " + spil.getScore());

        class StartUpAsyncTask extends AsyncTask {
            @Override
            protected Object doInBackground(Object[] objects) {
                try {
                    RestClient Nulstilclient = new RestClient("http://ubuntu4.saluton.dk:20002/Galgeleg/rest/game/nulstil");
                    try {
                        Nulstilclient.execute(RequestMethod.GET);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    RestClient client = new RestClient("http://ubuntu4.saluton.dk:20002/Galgeleg/rest/game/getsynligtord");
                    try {
                        client.execute(RequestMethod.GET);
                        visiblewWord = client.getResponse().toString();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(Object o) {
                wordView.setText(visiblewWord);
            }
        }
        new StartUpAsyncTask().execute();



    }



    public void onClick(View v) {
        final String guessLet = guessLetter.getText().toString();

        new ExplodeAnimation(guessButton).setListener(new AnimationListener() {
            @Override
            public void onAnimationEnd(Animation animation) {
                guessButton.setVisibility(View.VISIBLE);
            }
        }).animate();


        class GuessLetterAsyncTask extends AsyncTask {
            @Override
            protected Object doInBackground(Object[] objects) {
                try {
                    RestClient guessLetterclient = new RestClient("http://ubuntu4.saluton.dk:20002/Galgeleg/rest/game/gaetbogstav");
                    try {
                        String inputJsonString = "{" +
                                "    \"bogstav\"          :" + guessLet +
                                "}";

                        guessLetterclient.setJSONString(inputJsonString);
                        guessLetterclient.addHeader("Content-Type", "appication/json"); // if required
                        guessLetterclient.execute(RequestMethod.POST);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    RestClient visibleWordclient = new RestClient("http://ubuntu4.saluton.dk:20002/Galgeleg/rest/game/getsynligtord");
                    try {
                        visibleWordclient.execute(RequestMethod.GET);
                        visiblewWord = visibleWordclient.getResponse().toString();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }

                RestClient Statusclient = new RestClient("http://ubuntu4.saluton.dk:20002/Galgeleg/rest/game/logstatus");
                System.out.println(Statusclient.getResponseCode());
                System.out.println(Statusclient.getResponse());
                return null;
            }

            @Override
            protected void onPostExecute(Object o) {
                wordView.setText(visiblewWord);
            }
        }


        if (v == guessButton) {
            if (guessLet.length() != 1) {
                guessLetter.setError("Write a letter");
            }
            new GuessLetterAsyncTask().execute();

            switch (spil.getAntalLiv()) {
                case 5:
                    image.setImageResource(R.mipmap.forkert1);
                    break;
                case 4:
                    image.setImageResource(R.mipmap.forkert2);
                    break;
                case 3:
                    image.setImageResource(R.mipmap.forkert3);
                    break;
                case 2:
                    image.setImageResource(R.mipmap.forkert4);
                    break;
                case 1:
                    image.setImageResource(R.mipmap.forkert5);
                    break;
                case 0:
                    image.setImageResource(R.mipmap.forkert6);
                    break;
                default:
                    break;
            }
            guessLetter.setText("");
        }
    }

    private void update() {
        wordView.setText(spil.getSynligtOrd());
        letters.setText("Used letters: " + spil.getBrugteBogstaver());
        life.setText("Lives: " + spil.getAntalLiv());
        score.setText("Score: " + spil.getScore());

        final SharedPreferences.Editor TotalGame = getSharedPreferences("TotalGames", Context.MODE_PRIVATE).edit();

        if (spil.erSpilletVundet()) {
            tEnd = SystemClock.elapsedRealtime();
            totalTime();

            totalGames++;
            TotalGame.putInt(totalGamesStr, totalGames);
            TotalGame.apply();

            won++;
            final SharedPreferences.Editor win = getSharedPreferences("Wins", Context.MODE_PRIVATE).edit();
            win.putInt(wonStr, won);
            win.apply();

            final MediaPlayer win_sound = MediaPlayer.create(this, R.raw.win);
            win_sound.start();

            Intent i = new Intent(this, Win.class);
            i.putExtra("currentScore", spil.getScore());
            startActivity(i);
            finish();
        } else if (spil.erSpilletTabt()) {
            totalGames++;
            TotalGame.putInt(totalGamesStr, totalGames);
            TotalGame.apply();

            lost++;
            final SharedPreferences.Editor lose = getSharedPreferences("Losses", Context.MODE_PRIVATE).edit();
            lose.putInt(lostStr, lost);
            lose.apply();

            final MediaPlayer lose_sound = MediaPlayer.create(this, R.raw.lose);
            lose_sound.start();

            Intent i = new Intent(this, Lose.class);
            i.putExtra("currentScore", spil.getScore());
            startActivity(i);
            finish();
        }
    }

    public void totalTime() {
        totalTime = (tEnd - tStart) / 1000;
    }

    public static double getTotalTime() {
        return totalTime;
    }
}
