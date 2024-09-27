package com.example.opsc7312poepart2_code;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.withDecorView;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static java.util.function.Predicate.not;
import androidx.fragment.app.testing.FragmentScenario;
import androidx.navigation.Navigation;
import androidx.navigation.testing.TestNavHostController;
import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.Root;
import androidx.test.espresso.idling.CountingIdlingResource;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.ActivityTestRule;

import com.example.poe2.MainActivity;
import com.example.poe2.R;
import com.example.poe2.ui.maps_client.MapsClientFragment;
import com.google.android.gms.maps.model.LatLng;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.hamcrest.CoreMatchers.not;

import android.app.Activity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

@RunWith(AndroidJUnit4.class)
public class MapsUnitTest {

    private FragmentScenario<MapsClientFragment> fragmentScenario;
    private TestNavHostController navController;
    private ActivityScenario<MainActivity> activityScenario;
    @Rule
    public ActivityTestRule<MainActivity> activityRule = new ActivityTestRule<>(MainActivity.class);

    @Before
    public void setup() {
        // Launch MainActivity
        activityScenario = ActivityScenario.launch(MainActivity.class);

        // Initialize TestNavHostController in the activity context
        activityScenario.onActivity(activity -> {
            navController = new TestNavHostController(activity);
            navController.setGraph(com.example.poe2.R.navigation.mobile_navigation);
            navController.setCurrentDestination(com.example.poe2.R.id.nav_maps_client); // Set the start destination
        });

        // Launch the MapsClientFragment
        fragmentScenario = FragmentScenario.launchInContainer(MapsClientFragment.class);

        // Set the NavController for the fragment
        fragmentScenario.onFragment(fragment -> {
            Navigation.setViewNavController(fragment.requireView(), navController);
        });
    }


    @Test
    public void testDirectionsTextDisplayed() {
        fragmentScenario.onFragment(fragment -> {
            // Simulate setting directions in the fragment
            String simulatedDirections = "Estimated travel time: 30 mins";
            fragment.getTextViewDirection().setText(simulatedDirections);
        });

        // Now use Espresso to verify that the directions text is displayed
        onView(withId(com.example.poe2.R.id.textViewDirection))
                .check(matches(withText("Estimated travel time: 30 mins")));
    }

    @Test
    public void testLocationPermissionsRequested() {
        final CountingIdlingResource idlingResource = new CountingIdlingResource("LocationPermission");
        IdlingRegistry.getInstance().register(idlingResource);
        idlingResource.increment(); // Indicate that we're waiting

        fragmentScenario.onFragment(fragment -> {
            activityScenario.onActivity(activity -> {
                activity.runOnUiThread(() -> {
                    // Simulate the request for location permissions
                    fragment.requestLocationPermissions();
                });
            });
        });

        // Introduce a wait period for user action (for granting permissions)
        try {
            Thread.sleep(5000); // Adjust the wait time as necessary
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        idlingResource.decrement(); // Indicate that we're done waiting

        // Verify that the direction text view is displayed
        onView(withId(com.example.poe2.R.id.textViewDirection))
                .check(matches(isDisplayed()));

        IdlingRegistry.getInstance().unregister(idlingResource);
    }

    @Test
    public void testDentistSpinnerLoad() {

        try {
            Thread.sleep(5000); // Adjust the wait time as necessary
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        fragmentScenario.onFragment(fragment -> {
            // Verify that spinner is populated
            assertNotNull(fragment.getSpinnerDentists());
            assertTrue(fragment.getSpinnerDentists().getAdapter().getCount() > 0);
        });
    }

    @Test
    public void testGoNowButtonClickedWithDestination() {
        // Set a mock destination LatLng
        LatLng mockLatLng = new LatLng(37.4221, -122.0841);

        // Update destination in the fragment
        fragmentScenario.onFragment(fragment -> {
            fragment.setDestinationLatLng(mockLatLng); // Assuming you added this method in your fragment
        });

        // Perform the click on the Go Now button
        onView(withId(R.id.btnGoNow)).perform(click());

        // Verify that the Toast message for fetching directions appears
        onView(withText("Fetching directions..."))
                .inRoot(withDecorView(not(activityRule.getActivity().getWindow().getDecorView()))) // Ensure the Toast isn't from the current activity
                .check(matches(isDisplayed()));
    }

    public static Matcher<Root> withToast() {
        return new TypeSafeMatcher<Root>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("is toast");
            }

            @Override
            protected boolean matchesSafely(Root root) {
                // Check if the root window is a Toast window
                WindowManager.LayoutParams params = (WindowManager.LayoutParams) root.getDecorView().getLayoutParams();
                return params.type == WindowManager.LayoutParams.TYPE_TOAST;
            }
        };
    }

    @Test
    public void testDirectionsFetchedAndParsed() {
        String jsonResponse = "{" +
                "\"routes\": [{" +
                "\"legs\": [{" +
                "\"duration\": {\"text\": \"10 mins\"}," +  // Simulated duration
                "\"steps\": [{" +
                "\"html_instructions\": \"Head northeast on Amphitheatre Pkwy toward W Middlefield Rd\"," +
                "\"distance\": {\"text\": \"0.5 mi\"}" +
                "},{" +
                "\"html_instructions\": \"Turn right onto W Middlefield Rd\"," +
                "\"distance\": {\"text\": \"0.2 mi\"}" +
                "},{" +
                "\"html_instructions\": \"Turn left onto N De Anza Blvd\"," +
                "\"distance\": {\"text\": \"0.3 mi\"}" +
                "}" +
                "]" +
                "}]," +
                "\"overview_polyline\": {\"points\": \"abcde\"}" +
                "}]" +
                "}";

        // Launch the fragment scenario
        fragmentScenario.onFragment(fragment -> {
            // Use runOnUiThread to execute the parsing
            activityScenario.onActivity(activity -> {
                activity.runOnUiThread(() -> fragment.parseDirectionsJson(jsonResponse));
            });
        });

        // Use IdlingResource or wait for the UI update if needed
        // Example: IdlingRegistry.getInstance().register(yourIdlingResource);

        // Verify that the first step of directions is displayed
        onView(withId(com.example.poe2.R.id.textViewDirection))
                .check(matches(withText("Head northeast on Amphitheatre Pkwy toward W Middlefield Rd")));
    }

}
