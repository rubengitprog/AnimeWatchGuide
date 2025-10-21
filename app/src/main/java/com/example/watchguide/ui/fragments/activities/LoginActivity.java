package com.example.watchguide.ui.fragments.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.watchguide.R;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 100;
    private GoogleSignInClient mGoogleSignInClient;
    private SignInButton btnGoogle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Comprobar si el usuario ha logueado en Firebase
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() != null) {
            // Usuario ya logueado  va directamente  a MainActivity
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.acivity_login);
        btnGoogle = findViewById(R.id.btnGoogleSignIn);

        // Configurar Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .requestProfile()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // No pedirá login si ya está logueado en Google
        GoogleSignIn.getLastSignedInAccount(this);

        btnGoogle.setOnClickListener(v -> signIn());
    }

    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);

                com.google.firebase.auth.AuthCredential credential =
                        com.google.firebase.auth.GoogleAuthProvider.getCredential(account.getIdToken(), null);

                FirebaseAuth mAuth = FirebaseAuth.getInstance();
                mAuth.signInWithCredential(credential)
                        .addOnCompleteListener(this, task1 -> {
                            if (task1.isSuccessful()) {
                                FirebaseUser firebaseUser = mAuth.getCurrentUser();

                                if (firebaseUser != null) {
                                    String uid = firebaseUser.getUid();
                                    String email = firebaseUser.getEmail();
                                    String defaultName = firebaseUser.getDisplayName();
                                    String defaultPhotoUrl = account.getPhotoUrl() != null ? account.getPhotoUrl().toString() : null;

                                    FirebaseFirestore db = FirebaseFirestore.getInstance();
                                    DocumentReference userRef = db.collection("users").document(uid);

                                    userRef.get().addOnSuccessListener(document -> {
                                        if (!document.exists()) {
                                            // Solo la primera vez guardamos datos de Google
                                            Map<String, Object> userData = new HashMap<>();
                                            userData.put("username", defaultName);
                                            userData.put("email", email);
                                            userData.put("photoURL", defaultPhotoUrl);
                                            userData.put("role", "user"); // Asignar rol de usuario por defecto

                                            userRef.set(userData)
                                                    .addOnSuccessListener(aVoid ->
                                                            Toast.makeText(this, "Welcome " + defaultName, Toast.LENGTH_SHORT).show()
                                                    )
                                                    .addOnFailureListener(e ->
                                                            Toast.makeText(this, "Error saving new user: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                                                    );
                                        } else {
                                            // Si el usuario ya existe, verificar que tenga role asignado
                                            if (!document.contains("role")) {
                                                // Si no tiene role, asignar "user" por defecto
                                                userRef.update("role", "user");
                                            }

                                            // Recuperamos lo que haya guardado en firestore
                                            String username = document.getString("username");
                                            Toast.makeText(this, "Welcome back " + username, Toast.LENGTH_SHORT).show();
                                        }

                                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                        finish();
                                    });
                                }
                            } else {
                                Toast.makeText(this, "Firebase login error: " + task1.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });

            } catch (ApiException e) {
                Toast.makeText(this, "Login error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }
}