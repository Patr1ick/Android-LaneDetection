package eu.patrickgeiger.lanedetection;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;

import mehdi.sakout.aboutpage.AboutPage;
import mehdi.sakout.aboutpage.Element;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Generate the View with the About Page library
        View view = new AboutPage(this)
                .setImage(R.mipmap.ic_launcher)
                .setDescription(getString(R.string.about))
                .addGroup("Socials")
                .addGitHub("Patr1ick/Android-LaneDetection", "GitHub - Android App")
                .addGitHub("JoDi-2903/Road-Lane-Detection", "GitHub - Python Implementation")
                .addGroup("About")
                .addItem(new Element("Version 1.0", R.drawable.ic_baseline_info_48))
                .addWebsite("https://github.com/Patr1ick/Android-LaneDetection/blob/main/LICENSE.md", "License")
                .addWebsite("https://github.com/Patr1ick/Android-LaneDetection/blob/main/Libraries.md", "Libraries")
                .create();

        // Set the generated view
        setContentView(view);

    }
}