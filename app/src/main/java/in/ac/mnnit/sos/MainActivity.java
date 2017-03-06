package in.ac.mnnit.sos;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import in.ac.mnnit.sos.database.LocalDatabaseAdapter;
import in.ac.mnnit.sos.database.entity.EcontactAddress;
import in.ac.mnnit.sos.database.entity.EcontactEmail;
import in.ac.mnnit.sos.database.entity.EcontactPhone;
import in.ac.mnnit.sos.database.entity.EmergencyContact;
import in.ac.mnnit.sos.fragments.ContactFragment;
import in.ac.mnnit.sos.fragments.HomeFragment;
import in.ac.mnnit.sos.fragments.LocationFragment;
import in.ac.mnnit.sos.fragments.DialogFragmentHelper;
import in.ac.mnnit.sos.models.Address;
import in.ac.mnnit.sos.models.Contact;
import in.ac.mnnit.sos.models.Email;
import in.ac.mnnit.sos.models.Phone;
import in.ac.mnnit.sos.services.ContactServiceHelper;
import in.ac.mnnit.sos.services.LogoutUser;

public class MainActivity extends AppCompatActivity
        implements
        NavigationView.OnNavigationItemSelectedListener,
        HomeFragment.OnFragmentInteractionListener,
        ContactFragment.OnListFragmentInteractionListener,
        LocationFragment.OnFragmentInteractionListener,
        DialogFragmentHelper.OnDialogResponseListener {

    private View bottomNavigationMenuItem;
    private BottomNavigationView bottomNavigationView;
    private FragmentManager fragmentManager;
    private FragmentTransaction transaction;
    private FloatingActionButton fab;


    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int REQUEST_CODE_PICK_CONTACTS = 1;
    private static final int MY_PERMISSIONS_REQUEST_READ_CONTACTS = 2;
    private static final int PERMISSION_DIALOG_ID = 1;
    private static final String CONTACT_PERMISSION_TITLE = "Grant read contacts permission?";
    private static final String CONTACT_PERMISSION_EXPLANATION = "Permission to read contacts is needed to pick emergency contacts from your phone book.";
    private Uri uriContact;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setVisibility(View.GONE);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        bottomNavigationMenuItem = findViewById(R.id.action_home);
        bottomNavigationMenuItem.performClick();

        bottomNavigationView = (BottomNavigationView) findViewById(R.id.bottomNavigation);
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationHandler());

        fragmentManager = getFragmentManager();

        HomeFragment homeFragment = new HomeFragment();
        transaction = fragmentManager.beginTransaction();
        transaction.add(R.id.content_main, homeFragment, "homeFragment");
        transaction.commit();
    }

    private void onClickSelectContact() {
        int contactPermissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS);
        if(contactPermissionCheck == PackageManager.PERMISSION_GRANTED){
            pickContacts();
        }
        else{
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_CONTACTS)) {
                DialogFragmentHelper dialogFragmentHelper =
                        new DialogFragmentHelper(PERMISSION_DIALOG_ID, CONTACT_PERMISSION_TITLE, CONTACT_PERMISSION_EXPLANATION, "Cancel", "Ok", this, getFragmentManager());
                dialogFragmentHelper.show();
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_CONTACTS},
                        MY_PERMISSIONS_REQUEST_READ_CONTACTS);
            }
        }
    }

    public void showPermissionRequiredSnackbar(){
        Snackbar.make(findViewById(android.R.id.content), "Permission is required to pick contacts", Snackbar.LENGTH_SHORT)
                .setAction("Grant", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        openAppSettings();
                    }
                }).show();
    }

    public void openAppSettings(){
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, uri);
        startActivity(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == MY_PERMISSIONS_REQUEST_READ_CONTACTS){
            if(grantResults.length>0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                pickContacts();
            }
            else {
                showPermissionRequiredSnackbar();
            }
        }else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    public void pickContacts(){
        Intent contactsIntent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
        startActivityForResult(contactsIntent, REQUEST_CODE_PICK_CONTACTS);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == REQUEST_CODE_PICK_CONTACTS && resultCode == RESULT_OK){
            Log.d(TAG, "Response: " + data.toString());
            uriContact = data.getData();

            ContactServiceHelper contactServiceHelper = new ContactServiceHelper(getApplicationContext(), uriContact);
            try {
                Contact contact = contactServiceHelper.getContact();

                EmergencyContact eContact = new EmergencyContact(contact.getName());
                byte[] photo = contact.getHighResPhoto();
                if(photo != null)
                    eContact.setPhotoBytes(contact.getHighResPhoto());

                List<EcontactPhone> econtactPhones = new ArrayList<>();
                List<EcontactEmail> econtactEmails = new ArrayList<>();
                List<EcontactAddress> econtactAddresses = new ArrayList<>();

                for(Phone phone: contact.getPhones()){
                    econtactPhones.add(new EcontactPhone(phone.getNumber(), phone.getType()));
                }
                for(Email email: contact.getEmails()){
                    econtactEmails.add(new EcontactEmail(email.getEmailID(), email.getType()));
                }
                for(Address address: contact.getAddresses()){
                    econtactAddresses.add(new EcontactAddress(address.getAddress(), address.getType()));
                }

                LocalDatabaseAdapter localDatabaseAdapter = new LocalDatabaseAdapter(getApplicationContext());
                localDatabaseAdapter.insertEmergencyContact(eContact, econtactPhones, econtactEmails, econtactAddresses);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private Boolean exit = false;
    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else if (exit) {
            finish(); // finish activity
        } else {
            Snackbar.make(findViewById(android.R.id.content), "Tap back again to exit", Snackbar.LENGTH_SHORT)
                    .setAction("Action", null).show();
            exit = true;
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    exit = false;
                }
            }, 2 * 1000);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
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
        } else if (id == R.id.action_logout){
            LogoutUser logoutUser = new LogoutUser(getApplicationContext());
            logoutUser.logout();
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public void showEmergencyContacts(){
        ContactFragment contactFragment = new ContactFragment();
        transaction = fragmentManager.beginTransaction();
//        transaction.setCustomAnimations(R.anim.enter, R.anim.exit, R.anim.pop_enter, R.anim.pop_exit);
        transaction.replace(R.id.content_main, contactFragment, "contactFragment");
        transaction.commit();
        fab.setImageResource(R.drawable.ic_person_add_black_24dp);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onClickSelectContact();
            }
        });
        fab.setVisibility(View.VISIBLE);
    }

    public void showHome(){
        fab.setVisibility(View.GONE);
        HomeFragment homeFragment = new HomeFragment();
        transaction = fragmentManager.beginTransaction();
//        transaction.setCustomAnimations(R.anim.enter, R.anim.exit, R.anim.pop_enter, R.anim.pop_exit);
        transaction.replace(R.id.content_main, homeFragment, "homeFragment");
        transaction.commit();
    }

    public void showLocation(){
        LocationFragment locationFragment = new LocationFragment();
        transaction = fragmentManager.beginTransaction();
//        transaction.setCustomAnimations(R.anim.enter, R.anim.exit, R.anim.pop_enter, R.anim.pop_exit);
        transaction.replace(R.id.content_main, locationFragment, "locationFragment");
        transaction.commit();
        fab.setImageResource(R.drawable.ic_my_location);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                pointMyLocation();
                Snackbar.make(findViewById(android.R.id.content), "Takes to your location", Snackbar.LENGTH_SHORT).show();
            }
        });
        fab.setVisibility(View.VISIBLE);
    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }


    public void onClickDanger(View v){
        Snackbar.make(findViewById(android.R.id.content), "To be implemented after module implementation", Snackbar.LENGTH_SHORT)
                .setAction("Action", null).show();
    }

    @Override
    public void onListFragmentInteraction(Contact contact) {

    }

    @Override
    public void onContactDelete(Contact contact, int position){
        if(new LocalDatabaseAdapter(this).deleteContactByID(contact.getId())) {
            LocalDatabaseAdapter.contactsViewAdapter.onContactDelete(position);
            Toast.makeText(this, "Removed "+contact.getName(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onPositiveResponse(int dialogID) {
        if(dialogID == PERMISSION_DIALOG_ID){
            openAppSettings();
        }
    }

    @Override
    public void onNegativeResponse(int dialogID) {
        if(dialogID == PERMISSION_DIALOG_ID){
            showPermissionRequiredSnackbar();
        }
    }

    class BottomNavigationHandler implements BottomNavigationView.OnNavigationItemSelectedListener {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()){
                case R.id.action_emergency:
                    showEmergencyContacts();
                    break;
                case R.id.action_home:
                    showHome();
                    break;
                case R.id.action_locate:
                    showLocation();
                    break;
                default:
                    return false;
            }
            return true;
        }
    }
}
