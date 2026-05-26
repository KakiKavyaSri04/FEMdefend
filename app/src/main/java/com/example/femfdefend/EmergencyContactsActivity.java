package com.example.femfdefend;

import android.Manifest;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.femfdefend.adapters.EmergencyContactAdapter;
import com.example.femfdefend.databinding.ActivityEmergencyContactsBinding;
import com.example.femfdefend.models.EmergencyContact;
import com.example.femfdefend.utils.FirebaseHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class EmergencyContactsActivity extends AppCompatActivity {
    private FirebaseHelper firebaseHelper;
    private AlertDialog addContactDialog;
    private TextInputEditText dialogNameInput;
    private TextInputEditText dialogPhoneInput;
    private EmergencyContactAdapter contactAdapter;

    private final ActivityResultLauncher<Intent> pickContactLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    try (Cursor cursor = getContentResolver().query(
                            result.getData().getData(),
                            new String[]{
                                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                                    ContactsContract.CommonDataKinds.Phone.NUMBER
                            },
                            null,
                            null,
                            null
                    )) {
                        if (cursor != null && cursor.moveToFirst()) {
                            String name = cursor.getString(0);
                            String phone = cursor.getString(1);
                            // Fill the dialog inputs with selected contact
                            dialogNameInput.setText(name);
                            dialogPhoneInput.setText(phone);
                        }
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        com.example.femfdefend.databinding.ActivityEmergencyContactsBinding binding = ActivityEmergencyContactsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.emergency_contacts);
        }

        firebaseHelper = FirebaseHelper.getInstance();

        // Initialize the adapter
        contactAdapter = new EmergencyContactAdapter(new EmergencyContactAdapter.OnContactClickListener() {
            @Override
            public void onCallClick(EmergencyContact contact) {
                // Handle calling the contact
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:" + contact.getPhone()));
                startActivity(intent);
            }

            @Override
            public void onDeleteClick(EmergencyContact contact) {
                // Show delete confirmation dialog
                new MaterialAlertDialogBuilder(EmergencyContactsActivity.this)
                    .setTitle("Delete Contact")
                    .setMessage("Are you sure you want to delete this emergency contact?")
                    .setPositiveButton("Delete", (dialog, which) -> firebaseHelper.deleteEmergencyContact(contact.getId())
                        .addOnSuccessListener(aVoid -> Toast.makeText(EmergencyContactsActivity.this,
                            "Contact deleted successfully", Toast.LENGTH_SHORT).show())
                        .addOnFailureListener(e -> Toast.makeText(EmergencyContactsActivity.this,
                            "Failed to delete contact", Toast.LENGTH_SHORT).show()))
                    .setNegativeButton("Cancel", null)
                    .show();
            }
        });

        // Set up RecyclerView
        binding.recyclerContacts.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerContacts.setAdapter(contactAdapter);

        // Load contacts
        loadEmergencyContacts();

        // Set up FAB click listener
        binding.fabAddContact.setOnClickListener(v -> showAddContactDialog());

        // Create the add contact dialog
        createAddContactDialog();

        // Handle back button press
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
            }
        });
    }

    private void createAddContactDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_emergency_contact, null);
        dialogNameInput = dialogView.findViewById(R.id.etName);
        dialogPhoneInput = dialogView.findViewById(R.id.etPhone);
        MaterialButton btnPickContact = dialogView.findViewById(R.id.btnPickContact);
        MaterialButton btnSave = dialogView.findViewById(R.id.btnSave);

        addContactDialog = new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.add_emergency_contact)
                .setView(dialogView)
                .create();

        btnPickContact.setOnClickListener(v -> checkContactPermission());

        btnSave.setOnClickListener(v -> {
            String name = dialogNameInput.getText() != null ? dialogNameInput.getText().toString().trim() : "";
            String phone = dialogPhoneInput.getText() != null ? dialogPhoneInput.getText().toString().trim() : "";

            if (name.isEmpty() || phone.isEmpty()) {
                Toast.makeText(this, R.string.error_fields_empty, Toast.LENGTH_SHORT).show();
                return;
            }

            addEmergencyContact(name, phone);
            addContactDialog.dismiss();
            clearDialogInputs();
        });
    }

    private void showAddContactDialog() {
        clearDialogInputs();
        addContactDialog.show();
    }

    private void clearDialogInputs() {
        if (dialogNameInput != null) dialogNameInput.setText("");
        if (dialogPhoneInput != null) dialogPhoneInput.setText("");
    }

    private void checkContactPermission() {
        Dexter.withContext(this)
                .withPermission(Manifest.permission.READ_CONTACTS)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse response) {
                        openContactPicker();
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse response) {
                        Toast.makeText(EmergencyContactsActivity.this,
                                R.string.permission_contacts_rationale,
                                Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permission, 
                            PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                }).check();
    }

    private void openContactPicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType(ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE);
        pickContactLauncher.launch(intent);
    }

    private void addEmergencyContact(String name, String phone) {
        if (name == null || name.trim().isEmpty()) {
            Toast.makeText(this, "Contact name cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        if (phone == null || phone.trim().isEmpty()) {
            Toast.makeText(this, "Phone number cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        // Clean up the phone number - remove any spaces, dashes, or parentheses
        phone = phone.replaceAll("[\\s\\-\\(\\)]", "");
        
        // Ensure the phone number starts with a country code if it doesn't
        if (!phone.startsWith("+")) {
            // Add the default country code for your region (e.g., +1 for US)
            phone = "+1" + phone;
        }

        final String finalPhone = phone;
        Log.d("EmergencyContactsActivity", "Adding emergency contact - Name: " + name + ", Phone: " + finalPhone);

        EmergencyContact contact = new EmergencyContact(
                UUID.randomUUID().toString(),
                name,
                finalPhone,
                "Friend/Family" // Default relationship
        );

        firebaseHelper.addEmergencyContact(contact)
                .addOnSuccessListener(aVoid -> {
                    Log.i("EmergencyContactsActivity", "Successfully added emergency contact: " + name);
                    Toast.makeText(this, R.string.success_contact_added, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Log.e("EmergencyContactsActivity", "Failed to add contact: " + e.getMessage(), e);
                    Toast.makeText(this,
                            "Failed to add contact: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void loadEmergencyContacts() {
        firebaseHelper.getEmergencyContactsRef()
            .addSnapshotListener((value, error) -> {
                if (error != null) {
                    Toast.makeText(this, "Error loading contacts: " + error.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                if (value != null) {
                    List<EmergencyContact> contacts = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : value) {
                        EmergencyContact contact = doc.toObject(EmergencyContact.class);
                        contacts.add(contact);
                    }
                    contactAdapter.updateContacts(contacts);
                }
            });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            getOnBackPressedDispatcher().onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
} 