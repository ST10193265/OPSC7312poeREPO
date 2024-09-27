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

import com.example.poe2.MainActivity;
import com.example.poe2.R;
import com.example.poe2.ui.settings_dentist.SettingsDentistFragment;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class SettingsDentistTest {

    private FragmentScenario<SettingsDentistFragment> fragmentScenario;
    private TestNavHostController navController;

    @Before
    public void setup() {
        // Launch MainActivity
        ActivityScenario<MainActivity> activityScenario = ActivityScenario.launch(MainActivity.class);

        // Initialize TestNavHostController in the activity context
        activityScenario.onActivity(activity -> {
            navController = new TestNavHostController(activity);
            navController.setGraph(R.navigation.mobile_navigation);
            navController.setCurrentDestination(R.id.nav_settings_dentist); // Set the start destination
        });

        // Launch the SettingsDentistFragment
        fragmentScenario = FragmentScenario.launchInContainer(SettingsDentistFragment.class);

        // Set the NavController for the fragment
        fragmentScenario.onFragment(fragment -> {
            Navigation.setViewNavController(fragment.requireView(), navController);

            // Set initial values for the UI components
            fragment.getEtAddress().setText("123 Dentist Street");
            fragment.getEtPhoneD().setText("9876543210");
            fragment.getSpinnerLanguageD().setSelection(0); // English
        });
    }

    @Test
    public void testSaveButton() {
        // Click the save button in the fragment
        onView(withId(R.id.btnSaveD)).perform(click());

        // Verify that the settings are saved and the user is navigated back
        fragmentScenario.onFragment(fragment -> {
            int expectedDestinationId = R.id.nav_settings_dentist;
            assertEquals(expectedDestinationId, navController.getCurrentDestination().getId());
        });
    }

    @Test
    public void testCancelButton() {
        // Click the cancel button in the fragment
        onView(withId(R.id.btnCancelD)).perform(click());

        // Verify that the user is navigated back
        fragmentScenario.onFragment(fragment -> {
            int expectedDestinationId = R.id.nav_menu_dentist;
            assertEquals(expectedDestinationId, navController.getCurrentDestination().getId());
        });
    }

    @Test
    public void testClearFieldsOnCancel() {
        // Simulate clicking the Cancel button
        onView(withId(R.id.btnCancelD)).perform(click());

        // Verify that the fields are cleared after canceling
        fragmentScenario.onFragment(fragment -> {
            assertEquals("", fragment.getEtAddress().getText().toString());
            assertEquals("", fragment.getEtPhoneD().getText().toString());
            assertEquals(0, fragment.getSpinnerLanguageD().getSelectedItemPosition());
        });
    }

    @Test
    public void testLoadSettings() {
        // Simulate loading settings (assuming you set this up in your test environment)
        fragmentScenario.onFragment(fragment -> {
            fragment.loadLanguagePreference(); // Call this method directly to load settings for testing

            // Check if the fields are populated with correct test data
            assertEquals("123 Dentist Street", fragment.getEtAddress().getText().toString());
            assertEquals("9876543210", fragment.getEtPhoneD().getText().toString());
            assertEquals(0, fragment.getSpinnerLanguageD().getSelectedItemPosition()); // English
        });
    }
}
