package dev.nohasmith.venya_android_app;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.res.TypedArrayUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import android.content.Context;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import android.util.Log;

import java.util.Arrays;
import java.util.HashMap;

import static dev.nohasmith.venya_android_app.MainActivity.signinOptions;

/**
 * A simple {@link Fragment} subclass.
 */
public class LostPasswordFragment extends Fragment {
    Button submitButton;
    EditText usernameInput;
    EditText emailInput;
    EditText confirmEmailInput;
    TextView errorsView;
    TextView title;
    TextView toSignin;
    Context appContext;

    TextView toRegister;

    public LostPasswordFragment() {
        // Required empty public constructor
    }

    /*
    public LostPasswordFragment(Context context) {
        // Required empty public constructor
        appContext = context;
    }*/

    interface LostPasswordListener {
        void lostPasswordClicked(String field,String value);
    }
    private LostPasswordListener listener;

    interface ToSigninListener {
        void toSigninClicked(int position);
    }
    private ToSigninListener signinListener;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.lost_password_fragment, container, false);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof LostPasswordListener) {
            listener = (LostPasswordListener) context;
        } else {
            throw new RuntimeException(context.toString() + " (LostPasswordListener@Signin) must implement OnFragmentInteractionListener");
        }

        if (context instanceof ToSigninListener) {
            signinListener = (ToSigninListener)context;
        } else {
            throw new RuntimeException(context.toString() + " (ToSigninListener@Signin) must implement OnFragmentInteractionListener");
        }
    }
    @Override
    public void onStart() {
        super.onStart();
        View view = getView();

        appContext = view.getContext();
        Parsing.setLocale(appContext,"es");

        usernameInput = (EditText)view.findViewById(R.id.usernameLostPassword);
        emailInput = (EditText)view.findViewById(R.id.emailLostPassword);
        confirmEmailInput = (EditText)view.findViewById(R.id.confirmEmailLostPassword);
        String [] hint = new String[] {getResources().getString(R.string.form_confirm),getResources().getString(R.string.customer_email)};
        confirmEmailInput.setHint(Parsing.formatMessage(hint));

        errorsView = (TextView)view.findViewById(R.id.errorsView);
        errorsView.setText("");

        //title = (TextView)view.findViewById(R.id.lostPasswordTitle);
        //Parsing.displayTextView(appContext,title,R.string.form_enterdetails);
        //title.setText(getResources().getString(R.string.form_enterdetails));

        toSignin = (TextView)view.findViewById(R.id.toSignin);
        //String toSigninText = Parsing.formatMessage(new String[] {getResources().getString(R.string.signin_already),getResources().getString(R.string.signin_button)});
        //toSignin.setText(toSigninText);
        Parsing.displayTextView(appContext,toSignin,new int[] {R.string.signin_already,R.string.signin_button});
        toSignin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){
                int registerFragmentPosition = Parsing.getIndexOf(signinOptions,"signin");
                //Log.d("LostPasswordFragment","index of \"register\" in signinOptions = " + registerFragmentPosition);
                signinListener.toSigninClicked(registerFragmentPosition);
            }
        });

        submitButton = (Button)view.findViewById(R.id.lostPasswordButton);
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                errorsView.setText("");
                String username = usernameInput.getText().toString();
                String email = emailInput.getText().toString();
                String confirmEmail = confirmEmailInput.getText().toString().replaceAll("\\/","");
                String response = null;

                if ( ! Parsing.checkRequired(appContext,username) ) {
                    Parsing.displayTextView(appContext,errorsView,new int[] {R.string.customer_username,R.string.errors_required});
                }
                else if ( ! Parsing.checkRequired(appContext,email) ) {
                    Parsing.displayTextView(appContext,errorsView,new int[] {R.string.customer_email,R.string.errors_required});
                } else if ( ! Parsing.checkEmailFormat(appContext,email) ) {
                    Parsing.displayTextView(appContext,errorsView,new int[] {R.string.errors_badformat,R.string.customer_email});
                }
                else if ( ! Parsing.checkRequired(appContext,confirmEmail) ) {
                    Parsing.displayTextView(appContext,errorsView,new int[] {R.string.form_confirm,R.string.customer_email,R.string.errors_required});
                } else if ( ! Parsing.checkEmailFormat(appContext,confirmEmail) ) {
                    Parsing.displayTextView(appContext,errorsView,new int[] {R.string.errors_badformat,R.string.form_confirm,R.string.customer_email});
                } else {
                    // send request to the server to check the credentials
                    String reqUrl = "http://" + getResources().getString(R.string.venya_node_server) + ":" + getResources().getString(R.string.venya_node_port) +
                            "/getLostCredentials?type=customer" +
                            "&action=getid" +
                            "&username=" + username +
                            "&email=" + email;
                    MyHttpHandler httpHandler = new MyHttpHandler(appContext);
                    try {
                        response = httpHandler.execute(reqUrl).get();
                    } catch (Exception e) {
                        Log.e("LostPasswordFragment","Exception calling AsyncTask: " + e);
                        e.printStackTrace();
                    }
                    if ( response == null ) {
                        Log.e("LostPasswordFragmen.OnClick]","NULL response from server");
                    } else {
                        //Log.d("LostPasswordFragment","HTTP response from server: " + response);
                        HashMap<String, Object> parsedResponse = Parsing.parseGetCustomerResponseJson(response, appContext);
                        String status = (String) parsedResponse.get("status");

                        if ( ! status.equals(getResources().getString(R.string.success_status)) ) {
                            String errormessage = (String) parsedResponse.get("errormessage");
                            if (!errormessage.equals("emailfailed")) {
                                try {
                                    Parsing.displayTextView(appContext, errorsView, "errors_" + errormessage);
                                } catch (Exception e) {
                                    Parsing.displayTextView(appContext, errorsView, R.string.errors_unknwon);
                                }
                            } else {
                                CustomerSettings customer = (CustomerSettings) parsedResponse.get("customer");
                                //Log.d("LostPasswordFragment","ID = " + customer.getId());

                                if (!customer.getSessionid().equals(getResources().getString(R.string.sessionclosed))) {
                                    Parsing.displayTextView(appContext, errorsView, R.string.errors_sessionopened);
                                } else {
                                    String password = customer.getPassword();
                                    listener.lostPasswordClicked("password", password);
                                }
                            }
                        }
                    }
                }
            }
        });
    }

}
