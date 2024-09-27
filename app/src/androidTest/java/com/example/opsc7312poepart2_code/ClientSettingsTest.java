package com.example.opsc7312poepart2_code;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import static org.junit.Assert.assertEquals;

import androidx.fragment.app.testing.FragmentScenario;
import androidx.navigation.Navigation;
import androidx.navigation.testing.TestNavHostController;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import com.example.poe2.MainActivity;
import com.example.poe2.R;
import com.example.poe2.ui.client_settings.ClientSettingsFragment;

@RunWith(AndroidJUnit4.class)
public class ClientSettingsTest {

    private FragmentScenario<ClientSettingsFragment> fragmentScenario;
    private TestNavHostController navController;

    @Before
    public void setup() {
        // Launch MainActivity
        ActivityScenario<MainActivity> activityScenario = ActivityScenario.launch(MainActivity.class);

        // Initialize TestNavHostController in the activity context
        activityScenario.onActivity(activity -> {
            navController = new TestNavHostController(activity);
            navController.setGraph(R.navigation.mobile_navigation);
            navController.setCurrentDestination(R.id.nav_settings_client); // Set the start destination
        });

        // Launch the ClientSettingsFragment
        fragmentScenario = FragmentScenario.launchInContainer(ClientSettingsFragment.class);

        // Set the NavController for the fragment
        fragmentScenario.onFragment(fragment -> {
            Navigation.setViewNavController(fragment.requireView(), navController);

            // Set initial values for the UI components
            fragment.getEtEmail().setText("test@example.com");
            fragment.getEtPhone().setText("1234567890");
            fragment.getSpinnerLanguage().setSelection(1); // Afrikaans
            fragment.getSpinnerDistanceUnits().setSelection(0); // km
            fragment.getSpinnerDistanceRadius().setSelection(0); // No Limit
        });
    }

    @Test
    public void testSaveButton() {
        // Click the save button in the fragment
        onView(withId(R.id.btnSave)).perform(click());

        // Verify that the settings are saved and the user is navigated back
        fragmentScenario.onFragment(fragment -> {
            int expectedDestinationId = R.id.nav_settings_client;
            assertEquals(expectedDestinationId, navController.getCurrentDestination().getId());
        });
    }

    @Test
    public void testCancelButton() {
        // Click the cancel button in the fragment
        onView(withId(R.id.btnCancel)).perform(click());

        // Verify that the user is navigated back
        fragmentScenario.onFragment(fragment -> {
            int expectedDestinationId = R.id.nav_menu_client;
            assertEquals(expectedDestinationId, navController.getCurrentDestination().getId());
        });
    }

    @Test
    public void testClearFieldsOnCancel() {
        // Simulate clicking the Cancel button
        onView(withId(R.id.btnCancel)).perform(click());

        // Verify that the fields are cleared after canceling
        fragmentScenario.onFragment(fragment -> {
            assertEquals("", fragment.getEtEmail().getText().toString());
            assertEquals("", fragment.getEtPhone().getText().toString());
            assertEquals(0, fragment.getSpinnerLanguage().getSelectedItemPosition());
            assertEquals(0, fragment.getSpinnerDistanceUnits().getSelectedItemPosition());
            assertEquals(0, fragment.getSpinnerDistanceRadius().getSelectedItemPosition());
        });
    }

    @Test
    public void testLoadSettings() {
        // Simulate loading settings (assuming you set this up in your test environment)
        fragmentScenario.onFragment(fragment -> {
            fragment.loadSettings(); // Call this method directly to load settings for testing

            // Check if the fields are populated with correct test data
            assertEquals("test@example.com", fragment.getEtEmail().getText().toString());
            assertEquals("1234567890", fragment.getEtPhone().getText().toString());
            assertEquals(1, fragment.getSpinnerLanguage().getSelectedItemPosition()); // Afrikaans
            assertEquals(0, fragment.getSpinnerDistanceUnits().getSelectedItemPosition()); // km
            assertEquals(0, fragment.getSpinnerDistanceRadius().getSelectedItemPosition()); // No Limit
        });
    }
}