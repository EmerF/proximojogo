package br.com.proximojogo.proximojogo.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserInfo;

import java.util.List;

import br.com.proximojogo.proximojogo.Login;
import br.com.proximojogo.proximojogo.MainActivity;
import br.com.proximojogo.proximojogo.R;
import br.com.proximojogo.proximojogo.entity.Jogador;
import br.com.proximojogo.proximojogo.helper.HelperJogador;

public class GoogleAuthFragment extends Fragment implements
        GoogleApiClient.OnConnectionFailedListener,
        View.OnClickListener {

    private static final String TAG = "GoogleActivity";
    private static final int RC_SIGN_IN = 9001;

    // [START declare_auth]
    private FirebaseAuth mAuth;
    // [END declare_auth]

    private GoogleApiClient mGoogleApiClient;
    private TextView mStatusTextView;
    private TextView mDetailTextView;
    private String logoff="";
    private HelperJogador helperJogador;
    private TextView mDetailTextView1;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_google_auth, container, false);

        // Views
        mStatusTextView =  view.findViewById(R.id.status);
        mDetailTextView =  view.findViewById(R.id.detail);

        // Button listeners
        view.findViewById(R.id.sign_in_button).setOnClickListener(this);
        view.findViewById(R.id.sign_out_button).setOnClickListener(this);
        view.findViewById(R.id.disconnect_button).setOnClickListener(this);
        // [START config_signin]
        // Configure Google Sign In
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder
                    (GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        // [END config_signin]

            mGoogleApiClient = new GoogleApiClient.Builder(getActivity())
                    .enableAutoManage(getActivity() /* FragmentActivity */, this /* OnConnectionFailedListener */)
                    .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                    .build();


        // [START initialize_auth]
        mAuth = FirebaseAuth.getInstance();
        // [END initialize_auth]
        // Inflate the layout for this fragment
        return view;
    }


    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
       if(savedInstanceState != null){
           logoff = savedInstanceState.getString("logoff");
       }

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mGoogleApiClient.stopAutoManage(getActivity());
        mGoogleApiClient.disconnect();
    }

    // [START on_start_check_user]
    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        updateUI(currentUser);
    }
    // [END on_start_check_user]


    private void updateUI(FirebaseUser user) {
        //hideProgressDialog();

        if (user != null && logoff.equals("")) {

            boolean salvouJogador = SalvarJogador(user.getUid(), user.getDisplayName());
            if (salvouJogador) {
                Toast.makeText(getActivity(), "Seja bem vindo " + user.getDisplayName() + "!!!", Toast.LENGTH_SHORT).show();
                //startActivity(new Intent(getActivity(), MainActivity.class));
                ExibeDeslogar(user);
            } else {
                ExibeDeslogar(user);

            }

            //startActivity(new Intent(getActivity(), Login.class));


        } else {

            mStatusTextView.setText(R.string.signed_out);
            mDetailTextView.setText(null);

            getActivity().findViewById(R.id.sign_in_button).setVisibility(View.VISIBLE);
            getActivity().findViewById(R.id.sign_out_and_disconnect).setVisibility(View.GONE);
        }
    }
    public boolean SalvarJogador(String userId, String userName){
        boolean salvou = false;
        Jogador jogador = new Jogador();
        jogador.set_idUser(userId);
        jogador.setTipoUser("Jogador");
        jogador.setTelefoneUser("(41)3621-6517");// este telefone vai vir da tela de login
                                                 // que vamos disparar após o login.
        jogador.setNomeUser(userName);
        jogador.setPosicao("Atacante");

        helperJogador = new HelperJogador();
        salvou = helperJogador.salvar(getView(),jogador);

        return salvou;
    }
    public void ExibeDeslogar(FirebaseUser user){
        mStatusTextView.setText(getString(R.string.google_status_fmt, user.getEmail()));
        mDetailTextView.setText(getString(R.string.firebase_status_fmt, user.getUid()));
        getActivity().findViewById(R.id.sign_in_button).setVisibility(View.GONE);
        getActivity().findViewById(R.id.sign_out_and_disconnect).setVisibility(View.VISIBLE);
    }

    // [START onactivityresult]
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            if (result.isSuccess()) {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = result.getSignInAccount();
                firebaseAuthWithGoogle(account);
            } else {
                // Google Sign In failed, update UI appropriately
                // [START_EXCLUDE]
                updateUI(null);
                Log.d("ERRO LOGIN: " , (String.valueOf(result.getStatus().getStatusCode())));
                // [END_EXCLUDE]
            }
        }
    }
    // [END onactivityresult]

    // [START auth_with_google](
    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        Log.d(TAG, "firebaseAuthWithGoogle:" + acct.getId());


        // [START_EXCLUDE silent]
         //showProgressDialog();
        // [END_EXCLUDE]

        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(getActivity(), new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithCredential:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            updateUI(user);
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            Toast.makeText(getActivity(), "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                            updateUI(null);
                        }

                        // [START_EXCLUDE]
                        // hideProgressDialog();
                        // [END_EXCLUDE]
                    }
                });
    }
    // [END auth_with_google]

    // [START signin]
    private void signIn() {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }
    // [END signin]

    private void signOut() {
        // Firebase sign out
        mAuth.signOut();

        // Google sign out
        Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback(
                new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        updateUI(null);
                    }
                });
    }

    private void revokeAccess() {
        // Firebase sign out
        mAuth.signOut();

        // Google revoke access
        Auth.GoogleSignInApi.revokeAccess(mGoogleApiClient).setResultCallback(
                new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        updateUI(null);
                    }
                });
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        // An unresolvable error has occurred and Google APIs (including Sign-In) will not
        // be available.
        Log.d(TAG, "onConnectionFailed:" + connectionResult);
        Toast.makeText(getActivity(), "Google Play Services error.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.sign_in_button) {
            signIn();
        } else if (i == R.id.sign_out_button) {
            //signOut();
            revokeAccess();
        } else if (i == R.id.disconnect_button) {
            revokeAccess();
        }
    }
    public String getTelefoneJogador(){


        return "";

    }
}
