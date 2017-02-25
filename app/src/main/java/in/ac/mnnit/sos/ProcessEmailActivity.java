package in.ac.mnnit.sos;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;

import in.ac.mnnit.sos.services.Config;
import in.ac.mnnit.sos.services.ProcessEmail;

import static com.android.volley.Request.Method.POST;

public class ProcessEmailActivity extends AppCompatActivity {

    EditText email;
    Button continueBtn;
    FrameLayout progressBarHolder;

    Config config = new Config();
    private final String processEmailUrl = config.getBaseURL().concat("process_email.php");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences sharedPreferences = getSharedPreferences("session", MODE_PRIVATE);
        if (sharedPreferences.getBoolean("loggedin", false)) {
            Intent intent = new Intent(ProcessEmailActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            startActivity(intent);
            finish();
        } else {
            setContentView(R.layout.activity_process_email);

            email = (EditText) findViewById(R.id.emailEditText);
            continueBtn = (Button) findViewById(R.id.continueButton);
            progressBarHolder = (FrameLayout) findViewById(R.id.progressBarHolder);
        }
    }

    public void onClickContinue(final View v) {
        String emailText = email.getText().toString().trim();
        if (emailText.equalsIgnoreCase("")) {
            email.setError("Enter your email ID");
        } else {
            InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);

            progressBarHolder.setVisibility(View.VISIBLE);

            final ProcessEmail processEmail = new ProcessEmail(email.getText().toString(), POST, processEmailUrl,
                    new Response.Listener<String>() {
                        @Override
                        public void onResponse(String isRegistered) {
                            progressBarHolder.setVisibility(View.GONE);

                            Intent intent;
                            if (isRegistered.equalsIgnoreCase("true")) {
                                intent = new Intent(ProcessEmailActivity.this, LoginActivity.class);
                            } else {
                                intent = new Intent(ProcessEmailActivity.this, RegisterActivity.class);
                            }
                            intent.putExtra("email", email.getText().toString());
                            startActivity(intent);
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            progressBarHolder.setVisibility(View.GONE);

                            Snackbar.make(v, "Unable to reach the server at the moment. Please try after sometime.", Snackbar.LENGTH_LONG)
                                    .setAction("Action", null).show();
                        }
                    });
            RequestQueue queue = Volley.newRequestQueue(ProcessEmailActivity.this);
            queue.add(processEmail);
        }
    }
}